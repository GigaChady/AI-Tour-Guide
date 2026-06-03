import { useReducer } from 'react'

export interface WizardState {
  step: number
  name: string
}

export type WizardAction =
  | { type: 'SET_NAME'; payload: string }
  | { type: 'NEXT_STEP' }
  | { type: 'PREV_STEP' }
  | { type: 'RESET' }

const initialState: WizardState = {
  step: 0,
  name: '',
}

function wizardReducer(state: WizardState, action: WizardAction): WizardState {
  switch (action.type) {
    case 'SET_NAME':
      return { ...state, name: action.payload }
    case 'NEXT_STEP':
      return { ...state, step: state.step + 1 }
    case 'PREV_STEP':
      return { ...state, step: Math.max(0, state.step - 1) }
    case 'RESET':
      return initialState
  }
}

export function usePlanningWizard() {
  const [state, dispatch] = useReducer(wizardReducer, initialState)

  return {
    state,
    setName: (name: string) => dispatch({ type: 'SET_NAME', payload: name }),
    nextStep: () => dispatch({ type: 'NEXT_STEP' }),
    prevStep: () => dispatch({ type: 'PREV_STEP' }),
    reset: () => dispatch({ type: 'RESET' }),
  }
}
