
import { StepDots } from './StepDots'
import { Step1PlanName } from './steps/Step1PlanName'
import { Step2PickLocation } from './steps/Step2PickLocation'
import { Step3Search } from './steps/Step3Search'
import { usePlanningWizard } from './usePlanningWizard'
import type { ReceivedPoi } from './useRoutePlannerSession'

const STEP_COUNT = 3

interface PlanningWizardCardProps {
  onStart: () => void
  onPoiSelect: (poi: ReceivedPoi) => void
  onNext: (poi: ReceivedPoi) => void
  onFinish: (poi: ReceivedPoi | null, planName: string) => void
  pois: ReceivedPoi[]
  isPoisLoading: boolean
  isRoutePending: boolean
  savedCount: number
  maxStops: number
  selectedMapPoint?: { lat: number; lng: number } | null
  selectedPoi: ReceivedPoi | null
}

export function PlanningWizardCard({ onStart, onPoiSelect, onNext, onFinish, pois, isPoisLoading, isRoutePending, savedCount, maxStops, selectedMapPoint, selectedPoi }: PlanningWizardCardProps) {
  const { state, setName, nextStep, prevStep } = usePlanningWizard()

  const handleStart = () => {
    onStart()
    nextStep()
  }

  const handleNext = () => {
    if (!selectedPoi) return
    onNext(selectedPoi)
  }

  return (
    <div className={`bg-surface-container-highest/95 backdrop-blur-2xl rounded-2xl border border-primary/20 flex flex-col pointer-events-auto overflow-hidden${state.step > 0 ? ' flex-1 min-h-0 max-h-[50vh] p-3 gap-3' : ' p-5 gap-5'}`}>
      <div className="flex items-center gap-2">
        <span className="material-symbols-outlined text-primary text-xl">route</span>
        <span className="text-xs font-semibold uppercase tracking-wider text-on-surface">
          Planowanie trasy
        </span>
        <StepDots current={state.step} total={STEP_COUNT} />
        {state.step === 1 && (
          <button onClick={nextStep} className="ml-auto text-on-surface-variant hover:text-primary transition-colors">
            <span className="material-symbols-outlined text-xl">search</span>
          </button>
        )}
        {state.step === 2 && (
          <button onClick={prevStep} className="ml-auto text-on-surface-variant hover:text-primary transition-colors">
            <span className="material-symbols-outlined text-xl">arrow_back</span>
          </button>
        )}
      </div>

  

      {state.step === 0 && (
        <div className="flex flex-col flex-1 min-h-0 gap-4">
          <div className="flex-1 flex items-center w-full">
            <Step1PlanName name={state.name} setName={setName} />
          </div>
          <button
            onClick={handleStart}
            disabled={state.name.trim().length === 0}
            className="w-full py-2.5 rounded-full bg-primary text-on-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110 transition-all flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-xl">directions</span>
            Rozpocznij planowanie
          </button>
        </div>
      )}

      {state.step === 2 && (
        <Step3Search onConfirm={(poi) => { onNext(poi); prevStep() }} />
      )}

      {state.step === 1 && (
        <div className="flex flex-col flex-1 min-h-0 overflow-hidden gap-2">
          <div className="flex-1 min-h-0 flex flex-col overflow-hidden">
            <Step2PickLocation pois={pois} isLoading={isPoisLoading} selectedPoi={selectedPoi} onSelect={onPoiSelect} selectedMapPoint={selectedMapPoint} />
          </div>
          {savedCount >= maxStops && (
            <p className="text-xs text-on-surface-variant text-center flex-shrink-0">
              Osiągnięto limit {maxStops} przystanków. Zakończ trasę.
            </p>
          )}
          <div className="flex gap-2 flex-shrink-0">
            <button
              onClick={handleNext}
              disabled={!selectedPoi || isRoutePending || savedCount >= maxStops}
              className="flex-1 py-2 rounded-full border border-primary text-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:bg-primary/10 transition-all flex items-center justify-center gap-2"
            >
              {isRoutePending
                ? <span className="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                : <span className="material-symbols-outlined text-xl">arrow_forward</span>
              }
              Dalej
            </button>
            <button
              onClick={() => onFinish(selectedPoi, state.name)}
              disabled={isRoutePending || savedCount < 2}
              className="flex-1 py-2 rounded-full bg-primary text-on-primary font-title-md disabled:opacity-40 disabled:cursor-not-allowed hover:brightness-110 transition-all flex items-center justify-center gap-2"
            >
              {isRoutePending
                ? <span className="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                : <span className="material-symbols-outlined text-xl">check</span>
              }
              Gotowe
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
