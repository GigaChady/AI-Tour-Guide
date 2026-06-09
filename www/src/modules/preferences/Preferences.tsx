import { useState, useEffect } from 'react'
import { SaveButton } from '@/components/ui/SaveButton'
import { SliderField } from '@/components/ui/SliderField'
import { SegmentedButton } from '@/components/ui/SegmentedButton'
import { InterestToggleButton } from '@/components/preferences/InterestToggleButton'
import type { OnboardingQuestion } from '@/types/preferences'
import {
  useNarrationSettings,
  useUpdateNarrationSettings,
  useTestNarration,
  useOnboardingQuestions,
  useUpdateOnboardingAnswers,
} from '@/hooks/usePreferencesQueries'

// Settings sections -------------------------------------------------------------------------------

interface NarrationSectionProps {
  pitch: number
  setPitch: (val: number) => void
  speed: number
  setSpeed: (val: number) => void
  onTest: () => void
  isTesting: boolean
}

function NarrationSection({
  pitch,
  setPitch,
  speed,
  setSpeed,
  onTest,
  isTesting,
}: NarrationSectionProps) {
  return (
    <section className="flex flex-col gap-6">
      <div>
        <h2 className="font-title-lg text-title-lg text-primary-container mb-2">Narracja</h2>
        <p className="font-body-md text-body-md text-on-surface-variant">
          Ustawienia głosu audioprzewodnika
        </p>
      </div>

      <div className="flex flex-col gap-3">
        <label className="font-body-md text-body-md text-on-surface-variant">Język</label>
        <button
          disabled
          className="w-full flex items-center justify-between bg-surface-container p-4 rounded-xl border border-outline-variant/30 opacity-70"
        >
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-on-surface-variant">volume_up</span>
            <span className="font-body-lg text-body-lg text-on-surface">Polski (PL)</span>
          </div>
          {/*Currently no other option except Polish - lock icon and unclickable list*/}
          <span className="material-symbols-outlined text-on-surface-variant">lock</span>
        </button>
      </div>

      <SliderField
        label="Wysokość tonu"
        min={0}
        max={100}
        step={1}
        value={pitch}
        onChange={setPitch}
      />
      <SliderField
        label="Prędkość"
        min={0}
        max={10}
        step={1}
        value={speed}
        onChange={setSpeed}
      />

      <button
        onClick={onTest}
        disabled={isTesting}
        className="w-full py-3 rounded-full bg-surface-container text-on-surface font-title-md flex items-center justify-center gap-2 hover:bg-surface-container-high border border-outline-variant transition-colors disabled:opacity-50"
      >
        {isTesting ? (
          <div className="w-5 h-5 rounded-full border-2 border-on-surface border-t-transparent animate-spin" />
        ) : (
          <span className="material-symbols-outlined" data-weight="fill">
            play_arrow
          </span>
        )}
        Testuj narrację
      </button>
    </section>
  )
}

interface PlaybackSectionProps {
  detailsLevel: string
  setDetailsLevel: (val: string) => void
  autoPlay: boolean
  setAutoPlay: (val: boolean) => void
}

function PlaybackSection({
  detailsLevel,
  setDetailsLevel,
  autoPlay,
  setAutoPlay,
}: PlaybackSectionProps) {
  return (
    <section className="mt-2 flex flex-col gap-6">
      <div>
        <h2 className="font-title-lg text-title-lg text-primary-container mb-2">Odtwarzanie</h2>
        <p className="font-body-md text-body-md text-on-surface-variant">
          Ustawienia zachowania audioprzewodnika
        </p>
      </div>

      <SegmentedButton
        description="Poziom szczegółowości generowanych opisów (np. ilość kontekstu historycznego)"
        value={detailsLevel}
        onChange={setDetailsLevel}
        options={[
          { label: 'Mało', value: 'low' },
          { label: 'Średnio', value: 'medium' },
          { label: 'Dużo', value: 'high' },
        ]}
      />

      <SegmentedButton
        description="Automatycznie rozpoczynaj odtwarzanie opisu, kiedy zbliżysz się do punktów wartych uwagi. To ustawienie wpływa tylko na zachowanie aplikacji mobilnej"
        value={autoPlay}
        onChange={setAutoPlay}
        options={[
          { label: 'Tak', value: true },
          { label: 'Nie', value: false },
        ]}
      />
    </section>
  )
}

interface PersonalizationSectionProps {
  genderQuestion?: OnboardingQuestion
  interestsQuestion?: OnboardingQuestion
  selectedGender: string
  setSelectedGender: (val: string) => void
  selectedInterests: string[]
  toggleInterest: (id: string) => void
}

function PersonalizationSection({
  genderQuestion,
  interestsQuestion,
  selectedGender,
  setSelectedGender,
  selectedInterests,
  toggleInterest,
}: PersonalizationSectionProps) {
  const getIconForInterest = (key: string) => {
    const map: Record<string, string> = {
      architecture: 'architecture',
      history: 'museum',
      curiosities: 'lightbulb',
      culture: 'theater_comedy',
      food_and_dining: 'restaurant',
      nature: 'park',
      shopping: 'shopping_bag',
    }
    return map[key] || 'star' // Default icon for unknown interests
  }

  return (
    <div className="flex flex-col gap-8">
      {genderQuestion && (
        <section>
          <h2 className="font-title-lg text-title-lg text-tertiary-container mb-4">
            Preferencje
          </h2>
          <SegmentedButton
            description={genderQuestion.title}
            value={selectedGender}
            onChange={setSelectedGender}
            options={genderQuestion.options.map((opt) => ({
              label: opt.title,
              value: opt.key,
            }))}
          />
        </section>
      )}

      {interestsQuestion && (
        <section>
          <div className="flex items-center gap-3 mb-4">
            <p className="font-body-md text-body-md text-on-surface-variant">
              {interestsQuestion.title}
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            {interestsQuestion.options.map((opt) => (
              <InterestToggleButton
                key={opt.key}
                label={opt.title}
                icon={getIconForInterest(opt.key)}
                isActive={selectedInterests.includes(opt.key)}
                onClick={() => toggleInterest(opt.key)}
              />
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

// Main component ----------------------------------------------------------------------------------

export function Preferences() {
  const { data: narrationData, isLoading: isNarrationLoading } = useNarrationSettings()
  const { data: onboardingData, isLoading: isOnboardingLoading } = useOnboardingQuestions('pl')

  const { mutate: saveNarration } = useUpdateNarrationSettings()
  const { mutate: saveOnboarding } = useUpdateOnboardingAnswers()
  const { mutate: testAudio, isPending: isTestingAudio } = useTestNarration()

  const [pitch, setPitch] = useState<number>(50)
  const [speed, setSpeed] = useState<number>(5)
  const [detailsLevel, setDetailsLevel] = useState<string>('medium')
  const [autoPlay, setAutoPlay] = useState<boolean>(true)

  const [selectedGender, setSelectedGender] = useState<string>('')
  const [selectedInterests, setSelectedInterests] = useState<string[]>([])

  useEffect(() => {
    if (narrationData && Object.keys(narrationData).length > 0) {
      setPitch(narrationData.pitch ?? 50)
      setSpeed(narrationData.speed ?? 5)
      setDetailsLevel(narrationData.detail_level ?? 'medium')
      setAutoPlay(narrationData.auto_play ?? true)
    }
  }, [narrationData])

  useEffect(() => {
    if (onboardingData) {
      setSelectedGender(onboardingData.selected_answers.gender ?? '')
      setSelectedInterests(onboardingData.selected_answers.interests ?? [])
    }
  }, [onboardingData])

  const toggleInterest = (id: string) => {
    setSelectedInterests((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    )
  }

  const handleSaveNarration = () => {
    saveNarration({
      language: 'pl',
      pitch,
      speed,
      detail_level: detailsLevel,
      auto_play: autoPlay,
    })
  }

  const handleSaveOnboarding = () => {
    saveOnboarding({
      items: [
        { question_key: 'gender', answer_key: selectedGender || null },
        { question_key: 'interests', answer_keys: selectedInterests },
      ],
    })
  }

  if (isNarrationLoading || isOnboardingLoading) {
    return (
      <main className="flex-1 overflow-y-auto bg-background p-8 md:p-12 flex justify-center items-center">
        <div className="w-10 h-10 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
      </main>
    )
  }

  const genderQuestion = onboardingData?.items.find((q) => q.key === 'gender')
  const interestsQuestion = onboardingData?.items.find((q) => q.key === 'interests')

  return (
    <main className="flex-1 overflow-y-auto bg-background p-8 md:p-12">
      <div className="max-w-5xl mx-auto w-full">
        <header className="mb-8">
          <h1 className="font-headline-lg text-headline-lg text-primary-container mb-2">
            Ustawienia
          </h1>
          <p className="font-body-lg text-body-lg text-on-surface-variant">
            Dostosuj ustawienia narracji i preferencje. Wszystkie zmiany są synchronizowane z Twoją aplikacją mobilną.
          </p>
        </header>

        <div className="grid grid-cols-1 gap-8">
          <div className="bg-surface-container-low rounded-xl p-6 md:p-8 flex flex-col gap-8 border border-outline-variant/20">
            <NarrationSection
              pitch={pitch}
              setPitch={setPitch}
              speed={speed}
              setSpeed={setSpeed}
              onTest={() => testAudio()}
              isTesting={isTestingAudio}
            />

            <PlaybackSection
              detailsLevel={detailsLevel}
              setDetailsLevel={setDetailsLevel}
              autoPlay={autoPlay}
              setAutoPlay={setAutoPlay}
            />

            <div className="mt-2 flex justify-end">
              <SaveButton label="Zapisz ustawienia" onClick={handleSaveNarration} />
            </div>
          </div>

          <div className="bg-surface-container-low rounded-xl p-6 md:p-8 flex flex-col gap-6 border border-outline-variant/20">
            <PersonalizationSection
              genderQuestion={genderQuestion}
              interestsQuestion={interestsQuestion}
              selectedGender={selectedGender}
              setSelectedGender={setSelectedGender}
              selectedInterests={selectedInterests}
              toggleInterest={toggleInterest}
            />

            <div className="mt-auto flex justify-end">
              <SaveButton label="Zapisz preferencje" onClick={handleSaveOnboarding} />
            </div>
          </div>
        </div>
      </div>
    </main>
  )
}
