import { useState } from 'react'
import { SaveButton } from '@/components/ui/SaveButton'
import { SliderField } from '@/components/ui/SliderField'
import { SegmentedButton } from '@/components/ui/SegmentedButton'
import { InterestToggleButton } from '@/components/preferences/InterestToggleButton'

// Mock categories
const INTEREST_CATEGORIES = [
  { id: 'architecture', label: 'Architektura', icon: 'architecture' },
  { id: 'history', label: 'Miejsca historyczne', icon: 'museum' },
  { id: 'food', label: 'Miejsca kulinarne', icon: 'restaurant' },
  { id: 'nature', label: 'Natura i Parki', icon: 'park' },
]

interface NarrationSectionProps {
  pitch: number
  setPitch: (val: number) => void
  speed: number
  setSpeed: (val: number) => void
}

function NarrationSection({ pitch, setPitch, speed, setSpeed }: NarrationSectionProps) {
  return (
    <section className="flex flex-col gap-6">
      <div>
        <h2 className="font-title-lg text-title-lg text-primary-container mb-2">Narracja</h2>
        <p className="font-body-md text-body-md text-on-surface-variant">
          Ustawienia generatora głosu (Text To Speech)
        </p>
      </div>

      <div className="flex flex-col gap-3">
        <label className="font-body-md text-body-md text-on-surface-variant">Język</label>
        <button className="w-full flex items-center justify-between bg-surface-container p-4 rounded-xl border border-outline-variant/30 hover:bg-surface-container-high transition-colors">
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-on-surface-variant">volume_up</span>
            <span className="font-body-lg text-body-lg text-on-surface">Polski (PL)</span>
          </div>
          <span className="material-symbols-outlined text-on-surface-variant">arrow_drop_down</span>
        </button>
      </div>

      <SliderField
        label="Wysokość głosu"
        min={-1}
        max={1}
        step={0.1}
        value={pitch}
        onChange={setPitch}
      />
      <SliderField
        label="Prędkość odtwarzania"
        min={0.5}
        max={2}
        step={0.1}
        value={speed}
        onChange={setSpeed}
      />

      <button className="w-full py-3 rounded-full bg-surface-container text-on-surface font-title-md flex items-center justify-center gap-2 hover:bg-surface-container-high border border-outline-variant transition-colors">
        <span className="material-symbols-outlined" data-weight="fill">
          play_arrow
        </span>
        Testuj narrację
      </button>
    </section>
  )
}

interface PlaybackSectionProps {
  detailsLevel: 'small' | 'medium' | 'high'
  setDetailsLevel: (val: 'small' | 'medium' | 'high') => void
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
          Dostosuj zachowanie audioprzewodnika
        </p>
      </div>

      <SegmentedButton
        description="Poziom szczegółowości generowanych opisów (np. ilość kontekstu historycznego)"
        value={detailsLevel}
        onChange={setDetailsLevel}
        options={[
          { label: 'Mały', value: 'small' },
          { label: 'Średni', value: 'medium' },
          { label: 'Wysoki', value: 'high' },
        ]}
      />

      <SegmentedButton
        description="Automatycznie rozpoczynaj odtwarzanie po zbliżeniu się do punktu na mapie"
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

interface InterestsSectionProps {
  selectedInterests: string[]
  toggleInterest: (id: string) => void
}

function InterestsSection({ selectedInterests, toggleInterest }: InterestsSectionProps) {
  return (
    <>
      <div className="flex items-center gap-3 mb-4">
        <span className="material-symbols-outlined text-tertiary-container text-3xl">tune</span>
        <h2 className="font-title-lg text-title-lg text-tertiary-container">
          Priorytety zainteresowań
        </h2>
      </div>

      <div className="flex flex-wrap gap-3">
        {INTEREST_CATEGORIES.map((category) => (
          <InterestToggleButton
            key={category.id}
            label={category.label}
            icon={category.icon}
            isActive={selectedInterests.includes(category.id)}
            onClick={() => toggleInterest(category.id)}
          />
        ))}
      </div>
    </>
  )
}

// Main component

export function Preferences() {
  // Playback settings
  const [pitch, setPitch] = useState<number>(0)
  const [speed, setSpeed] = useState<number>(1.2)
  const [detailsLevel, setDetailsLevel] = useState<'small' | 'medium' | 'high'>('medium')
  const [autoPlay, setAutoPlay] = useState<boolean>(true)
  // Preferences
  const [selectedInterests, setSelectedInterests] = useState<string[]>(['architecture', 'history'])

  const toggleInterest = (id: string) => {
    setSelectedInterests((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id],
    )
  }

  return (
    <main className="flex-1 overflow-y-auto bg-background p-8 md:p-12">
      <div className="max-w-5xl mx-auto w-full">
        <header className="mb-8">
          <h1 className="font-headline-lg text-headline-lg text-primary-container mb-2">
            Preferencje i Synchronizacja
          </h1>
          <p className="font-body-lg text-body-lg text-on-surface-variant">
            Dostosuj swojego asystenta AI oraz spersonalizuj doświadczenia z podróży.
          </p>
        </header>

        <div className="grid grid-cols-1 gap-8">
          {/* Playback */}
          <div className="bg-surface-container-low rounded-xl p-6 md:p-8 flex flex-col gap-8 border border-outline-variant/20">
            <NarrationSection pitch={pitch} setPitch={setPitch} speed={speed} setSpeed={setSpeed} />

            <PlaybackSection
              detailsLevel={detailsLevel}
              setDetailsLevel={setDetailsLevel}
              autoPlay={autoPlay}
              setAutoPlay={setAutoPlay}
            />

            <SaveButton
              label="Zapisz ustawienia"
              className="mt-2"
              onClick={() =>
                console.log('Saved settings:', { pitch, speed, detailsLevel, autoPlay })
              }
            />
          </div>

          {/* Preferences */}
          <div className="bg-surface-container-low rounded-xl p-6 md:p-8 flex flex-col gap-6 border border-outline-variant/20">
            <InterestsSection
              selectedInterests={selectedInterests}
              toggleInterest={toggleInterest}
            />

            <SaveButton
              label="Zapisz preferencje"
              className="mt-auto"
              onClick={() => console.log('Saved preferences:', selectedInterests)}
            />
          </div>
        </div>
      </div>
    </main>
  )
}
