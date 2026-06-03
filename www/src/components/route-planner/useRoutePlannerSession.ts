import { useCallback, useEffect, useReducer, useRef } from 'react'
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

export interface SavedPoi {
  name: string
  lat: number
  lng: number
  description: string | null
}

export function toSavedPoi(poi: ReceivedPoi): SavedPoi {
  return { name: poi.name, lat: poi.lat, lng: poi.lng, description: poi.desc }
}

export interface RoutePlannerSession {
  status: SessionStatus
  sessionId: string | null
  pois: ReceivedPoi[]
  isPoisLoading: boolean
  startSession: () => void
  sendPlanningLocation: (lat: number, lng: number) => void
  clearPois: () => void
  closeSession: () => void
}

interface SessionState {
  status: SessionStatus
  sessionId: string | null
  pois: ReceivedPoi[]
  isPoisLoading: boolean
}

type SessionAction =
  | { type: 'CONNECTING' }
  | { type: 'SESSION_STARTED'; sessionId: string }
  | { type: 'PLANNING_READY' }
  | { type: 'POIS_RECEIVED'; pois: ReceivedPoi[] }
  | { type: 'POIS_LOADING' }
  | { type: 'CLEAR_POIS' }
  | { type: 'RESET' }
  | { type: 'ERROR' }

const initialState: SessionState = {
  status: 'idle',
  sessionId: null,
  pois: [],
  isPoisLoading: false,
}

function sessionReducer(state: SessionState, action: SessionAction): SessionState {
  switch (action.type) {
    case 'CONNECTING':
      return { ...state, status: 'connecting' }
    case 'SESSION_STARTED':
      return { ...state, sessionId: action.sessionId }
    case 'PLANNING_READY':
      return { ...state, status: 'ready' }
    case 'POIS_RECEIVED':
      return { ...state, pois: action.pois, isPoisLoading: false }
    case 'POIS_LOADING':
      return { ...state, isPoisLoading: true }
    case 'CLEAR_POIS':
      return { ...state, pois: [] }
    case 'ERROR':
      return { ...state, status: 'error' }
    case 'RESET':
      return initialState
  }
}

export function useRoutePlannerSession(): RoutePlannerSession {
  const [state, dispatch] = useReducer(sessionReducer, initialState)
  const wsRef = useRef<WebSocket | null>(null)

  const handleMessage = useCallback((event: MessageEvent) => {
    const msg = JSON.parse(event.data as string)
    if (msg.type === 'session_start') {
      dispatch({ type: 'SESSION_STARTED', sessionId: msg.session_id })
      wsRef.current?.send(JSON.stringify({ type: 'start_planning' }))
    } else if (msg.type === 'planning_started') {
      dispatch({ type: 'PLANNING_READY' })
    } else if (msg.type === 'pois') {
      dispatch({ type: 'POIS_RECEIVED', pois: msg.data.map((p: ReceivedPoi) => ({ ...p, narration_id: msg.narration_id })) })
    } else if (msg.detail === 'timeout') {
      wsRef.current?.close()
    }
  }, [])

  const startSession = useCallback(() => {
    if (wsRef.current) return
    const token = useAuthStore.getState().accessToken
    if (!token) return

    dispatch({ type: 'CONNECTING' })
    const ws = new WebSocket(`${config.wsUrl}/route/ws`)
    wsRef.current = ws

    ws.onopen = () => ws.send(JSON.stringify({ token }))
    ws.onmessage = handleMessage
    ws.onerror = () => dispatch({ type: 'ERROR' })
    ws.onclose = () => {
      wsRef.current = null
      dispatch({ type: 'RESET' })
    }
  }, [handleMessage])

  const sendPlanningLocation = useCallback((lat: number, lng: number) => {
    const ws = wsRef.current
    if (!ws || ws.readyState !== WebSocket.OPEN) return
    dispatch({ type: 'POIS_LOADING' })
    ws.send(JSON.stringify({ type: 'start_planning', lat, lng }))
  }, [])

  useEffect(() => {
    return () => {
      wsRef.current?.close()
    }
  }, [])

  const clearPois = useCallback(() => dispatch({ type: 'CLEAR_POIS' }), [])

  const closeSession = useCallback(() => {
    wsRef.current?.close()
  }, [])

  return { ...state, startSession, sendPlanningLocation, clearPois, closeSession }
}
