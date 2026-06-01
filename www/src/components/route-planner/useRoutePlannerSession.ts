import { useCallback, useEffect, useRef, useState } from 'react'
import { config } from '@/config'
import { useAuthStore } from '@/store/authStore'

export type SessionStatus = 'idle' | 'connecting' | 'ready' | 'error'

export interface ReceivedPoi {
  name: string
  lat: number
  lng: number
  photos: string[]
  desc: string | null
  narration_id: string
}

export interface RoutePlannerSession {
  status: SessionStatus
  sessionId: string | null
  pois: ReceivedPoi[]
  isPoisLoading: boolean
  startSession: () => void
  sendPlanningLocation: (lat: number, lng: number) => void
  clearPois: () => void
}

export function useRoutePlannerSession(): RoutePlannerSession {
  const [status, setStatus] = useState<SessionStatus>('idle')
  const [sessionId, setSessionId] = useState<string | null>(null)
  const [pois, setPois] = useState<ReceivedPoi[]>([])
  const [isPoisLoading, setIsPoisLoading] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)

  const handleMessage = useCallback((event: MessageEvent) => {
    const msg = JSON.parse(event.data as string)
    if (msg.type === 'session_start') {
      setSessionId(msg.session_id)
      wsRef.current?.send(JSON.stringify({ type: 'start_planning' }))
    } else if (msg.type === 'planning_started') {
      setStatus('ready')
    } else if (msg.type === 'pois') {
      setPois(msg.data.map((p: ReceivedPoi) => ({ ...p, narration_id: msg.narration_id })))
      setIsPoisLoading(false)
    }
  }, [])

  const startSession = useCallback(() => {
    if (wsRef.current) return
    const token = useAuthStore.getState().accessToken
    if (!token) return

    setStatus('connecting')
    const ws = new WebSocket(`${config.wsUrl}/route/ws`)
    wsRef.current = ws

    ws.onopen = () => ws.send(JSON.stringify({ token }))
    ws.onmessage = handleMessage
    ws.onerror = () => setStatus('error')
    ws.onclose = () => {
      wsRef.current = null
      setStatus('idle')
    }
  }, [handleMessage])

  const sendPlanningLocation = useCallback((lat: number, lng: number) => {
    const ws = wsRef.current
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    setIsPoisLoading(true)
    ws.send(JSON.stringify({ type: 'start_planning', lat, lng }))
  }, [])

  useEffect(() => {
    return () => {
      wsRef.current?.close()
    }
  }, [])

  const clearPois = useCallback(() => setPois([]), [])

  return { status, sessionId, pois, isPoisLoading, startSession, sendPlanningLocation, clearPois }
}
