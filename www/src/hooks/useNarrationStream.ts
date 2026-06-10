import { useState, useEffect, useRef, useCallback } from 'react'
import { toast } from 'sonner'

export type StreamStatus = 'connecting' | 'ready' | 'generating' | 'playing'

export interface NarrationWord {
  text: string
  offset_ms: number
  duration_ms: number
}

export interface PoiData {
  name: string
  photos: string[]
  desc?: string | null
  lat: number
  lng: number
}

function getNormalizedUuidFromBytes(buffer: ArrayBuffer) {
  const view = new Uint8Array(buffer, 0, 16)
  return Array.from(view)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('')
}

const normalizeId = (id?: string) => (id ? id.replace(/-/g, '').toLowerCase() : '')

export function useNarrationStream(options: { autoStartTour?: boolean } = { autoStartTour: true }) {
  const [status, setStatus] = useState<StreamStatus>('connecting')
  const [transcript, setTranscript] = useState<string>('')
  const [words, setWords] = useState<NarrationWord[]>([])
  const [poi, setPoi] = useState<PoiData | null>(null)
  const [audioUrl, setAudioUrl] = useState<string | null>(null)

  const wsRef = useRef<WebSocket | null>(null)
  const sessionIdRef = useRef<string | null>(null)

  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const pingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const isUnmountedRef = useRef(false)

  const isWaitingRef = useRef(false)
  const narrationsToSkipRef = useRef(0)
  const seenNarrationIdsRef = useRef<Set<string>>(new Set())
  const ignoredNarrationIdsRef = useRef<Set<string>>(new Set())
  const activeNarrationIdRef = useRef<string | null>(null)
  const connect = useCallback(function connectFn() {
    if (isUnmountedRef.current) return

    let token = ''
    const authData = localStorage.getItem('auth')

    if (authData) {
      try {
        const parsedAuth = JSON.parse(authData)
        const rawToken = parsedAuth?.state?.accessToken || ''
        token = rawToken.replace(/^Bearer\s/i, '').trim()
      } catch (error) {
        console.error('Error parsing auth data from localStorage:', error)
      }
    }

    const wsUrl = import.meta.env.VITE_WS_URL || 'ws://localhost:8000/route/ws'
    const ws = new WebSocket(wsUrl)
    ws.binaryType = 'arraybuffer'
    wsRef.current = ws

    ws.onopen = () => {
      if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ token }))

      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current)
      pingIntervalRef.current = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'ping' }))
        }
      }, 30000)
    }

    ws.onmessage = (event) => {
      if (typeof event.data === 'string') {
        const data = JSON.parse(event.data)
        const currentNid = normalizeId(data.narration_id)

        if (data.detail) {
          if (narrationsToSkipRef.current > 0) {
            narrationsToSkipRef.current -= 1
            return
          }
          isWaitingRef.current = false
          if (timeoutRef.current) clearTimeout(timeoutRef.current)
          setStatus('ready')

          const getErrorMessage = (detail: string) => {
            switch (detail) {
              case 'unauthorized':
                return 'Błąd autoryzacji.'
              case 'timeout':
                return 'Przekroczono czas odpowiedzi od serwera.'
              default:
                return detail
            }
          }
          toast.error(getErrorMessage(data.detail))
          if (data.detail === 'unauthorized') ws.close()
          return
        }

        if (data.type === 'session_start') {
          sessionIdRef.current = data.session_id

          if (options.autoStartTour) {
            ws.send(JSON.stringify({ type: 'start_tour', session_id: data.session_id }))
          } else {
            setStatus('ready')
          }
          return
        }
        if (data.type === 'tour_start') {
          setStatus('ready')
          return
        }

        if (currentNid && !seenNarrationIdsRef.current.has(currentNid)) {
          seenNarrationIdsRef.current.add(currentNid)

          if (narrationsToSkipRef.current > 0) {
            narrationsToSkipRef.current -= 1
            ignoredNarrationIdsRef.current.add(currentNid)
          } else {
            activeNarrationIdRef.current = currentNid
          }
        }

        if (currentNid) {
          if (ignoredNarrationIdsRef.current.has(currentNid)) return
          if (activeNarrationIdRef.current && currentNid !== activeNarrationIdRef.current) return
        }

        if (data.type === 'pois' && data.data?.length > 0) {
          setPoi(data.data[0])
        } else if (data.type === 'narration_transcript') {
          if (timeoutRef.current) clearTimeout(timeoutRef.current)
          setStatus('generating')
          const fullText = data.transcript.map((t: { text: string }) => t.text).join(' ')
          setTranscript(fullText)
        } else if (data.type === 'narration_words') {
          setWords(data.words)
        } else if (data.type === 'narration_done') {
          isWaitingRef.current = false
          setStatus('playing')
        }
      } else if (event.data instanceof ArrayBuffer) {
        const binNid = getNormalizedUuidFromBytes(event.data)
        if (ignoredNarrationIdsRef.current.has(binNid)) return
        if (activeNarrationIdRef.current && binNid !== activeNarrationIdRef.current) return

        const audioData = event.data.slice(20)
        const blob = new Blob([audioData], { type: 'audio/mpeg' })
        const url = URL.createObjectURL(blob)

        setAudioUrl((prev) => {
          if (prev) URL.revokeObjectURL(prev)
          return url
        })
      }
    }

    ws.onclose = () => {
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current)
      if (!isUnmountedRef.current) {
        setStatus('connecting')
        if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)
        reconnectTimeoutRef.current = setTimeout(connectFn, 3000)
      }
    }

    ws.onerror = () => {
      ws.close()
    }
  }, [])

  useEffect(() => {
    isUnmountedRef.current = false
    connect()

    return () => {
      isUnmountedRef.current = true
      if (timeoutRef.current) clearTimeout(timeoutRef.current)
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current)
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)

      if (
        wsRef.current &&
        (wsRef.current.readyState === WebSocket.OPEN ||
          wsRef.current.readyState === WebSocket.CONNECTING)
      ) {
        wsRef.current.close()
      }
    }
  }, [connect])

  const requestNarration = useCallback(
    (lat: number, lng: number) => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        if (isWaitingRef.current) narrationsToSkipRef.current += 1
        isWaitingRef.current = true
        activeNarrationIdRef.current = null

        setTranscript('')
        setWords([])
        setPoi(null)
        setAudioUrl(null)
        setStatus('generating')

        if (timeoutRef.current) clearTimeout(timeoutRef.current)
        timeoutRef.current = setTimeout(() => {
          isWaitingRef.current = false
          setStatus('ready')
          toast.error('Serwer nie odpowiada. Narracja nie została wygenerowana.')
        }, 60000)

        wsRef.current.send(JSON.stringify({ lat, lng, ai: true }))
      } else {
        toast.warning('Utracono połączenie z serwerem. Ponawianie połączenia...')
        connect()
      }
    },
    [connect],
  )

  return { status, transcript, words, poi, audioUrl, requestNarration }
}
