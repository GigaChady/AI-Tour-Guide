import { useState } from 'react'
import { StepDots } from './StepDots'
import { Step1PlanName } from './steps/Step1PlanName'
import { Step2PickLocation } from './steps/Step2PickLocation'
import { usePlanningWizard } from './usePlanningWizard'
import type { ReceivedPoi } from './useRoutePlannerSession'

const STEP_COUNT = 2

interface PlanningWizardCardProps {
  onStart: () => void
  onPoiSelect: (poi: ReceivedPoi) => void
  onNext: (poi: ReceivedPoi) => void
  onFinish: (poi: ReceivedPoi | null) => void
  pois: ReceivedPoi[]
  isPoisLoading: boolean
  isRoutePending: boolean
  savedCount: number
  maxStops: number
}

export function PlanningWizardCard({ onStart, onPoiSelect, onNext, onFinish, pois, isPoisLoading, isRoutePending, savedCount, maxStops }: PlanningWizardCardProps) {
  const { state, setName, nextStep } = usePlanningWizard()
  const [selectedPoi, setSelectedPoi] = useState<ReceivedPoi | null>(null)

  const handleStart = () => {
    onStart()
    nextStep()
  }

  const handleSelect = (poi: ReceivedPoi) => {
    setSelectedPoi(poi)
    onPoiSelect(poi)
  }

  const handleNext = () => {
    if (!selectedPoi) return
    onNext(selectedPoi)
    setSelectedPoi(null)
  }

  return (
    <div className="absolute bottom-6 left-6 z-10 w-80 bg-surface-container-highest/95 backdrop-blur-2xl rounded-2xl border border-primary/20 shadow-[0_8px_32px_rgba(0,0,0,0.5)] p-5 flex flex-col gap-5 pointer-events-auto">
      <div className="flex items-center gap-2">
        <span className="material-symbols-outlined text-primary text-xl">route</span>
        <span className="text-xs font-semibold uppercase tracking-wider text-on-surface">
          Planowanie trasy
        </span>
        <StepDots current={state.step} total={STEP_COUNT} />
      </div>

  

      {state.step === 0 && (
        <>
          <Step1PlanName name={state.name} setName={setName} />
          <button
            onClick={handleStart}
            disabled={state.name.trim().length === 0}
            className="w-full py-2.5 rounded-full bg-primary text-on-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110 transition-all shadow-[0_0_12px_rgba(208,188,255,0.3)] flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-xl">directions</span>
            Rozpocznij planowanie
          </button>
        </>
      )}

      {state.step === 1 && (
        <>
          <Step2PickLocation pois={pois} isLoading={isPoisLoading} selectedPoi={selectedPoi} onSelect={handleSelect} />
          {savedCount >= maxStops && (
            <p className="text-xs text-on-surface-variant text-center">
              Osiągnięto limit {maxStops} przystanków. Zakończ trasę.
            </p>
          )}
          <div className="flex gap-2">
            <button
              onClick={handleNext}
              disabled={!selectedPoi || isRoutePending || savedCount >= maxStops}
              className="flex-1 py-2.5 rounded-full border border-primary text-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:bg-primary/10 transition-all flex items-center justify-center gap-2"
            >
              {isRoutePending
                ? <span className="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                : <span className="material-symbols-outlined text-xl">arrow_forward</span>
              }
              Dalej
            </button>
            <button
              onClick={() => onFinish(selectedPoi)}
              disabled={isRoutePending}
              className="flex-1 py-2.5 rounded-full bg-primary text-on-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110 transition-all shadow-[0_0_12px_rgba(208,188,255,0.3)] flex items-center justify-center gap-2"
            >
              {isRoutePending
                ? <span className="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                : <span className="material-symbols-outlined text-xl">check</span>
              }
              Gotowe
            </button>
          </div>
        </>
      )}
    </div>
  )
}
