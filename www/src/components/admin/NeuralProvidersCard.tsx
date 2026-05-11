import { useState } from 'react'

interface SelectFieldProps {
  label: string
  options: string[]
}

function SelectField({ label, options }: SelectFieldProps) {
  return (
    <div>
      <label className="block font-label-lg text-label-lg text-on-surface-variant mb-2">
        {label}
      </label>
      <div className="relative">
        <select className="w-full appearance-none bg-surface-container-highest border-0 rounded-full py-4 pl-6 pr-12 font-body-lg text-body-lg text-on-surface focus:ring-2 focus:ring-primary focus:outline-none shadow-inner">
          {options.map((opt) => (
            <option key={opt}>{opt}</option>
          ))}
        </select>
        <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-on-surface-variant">
          <span className="material-symbols-outlined">expand_more</span>
        </div>
      </div>
    </div>
  )
}

export function NeuralProvidersCard() {
  const [streaming, setStreaming] = useState(true)

  return (
    <div className="bg-surface-container rounded-[2rem] p-lg border border-white/5 shadow-lg">
      <div className="flex items-center gap-3 mb-6">
        <span className="material-symbols-outlined text-primary text-3xl">neurology</span>
        <h3 className="font-title-lg text-title-lg text-on-surface">Dostawcy neuronowi</h3>
      </div>
      <div className="space-y-6">
        <SelectField
          label="Główny silnik LLM"
          options={['GPT-4 Turbo', 'Claude 3.5 Sonnet', 'Llama 3 (Local Hosted)']}
        />
        <SelectField
          label="Synteza głosu (TTS)"
          options={['ElevenLabs V2', 'OpenAI TTS', 'Google Cloud TTS']}
        />
        <div className="pt-4 border-t border-white/5 flex items-center justify-between">
          <div>
            <div className="font-title-md text-title-md text-on-surface">Tryb strumieniowania</div>
            <div className="font-body-md text-body-md text-on-surface-variant">
              Włącz wyjście token po tokenie
            </div>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              className="sr-only peer"
              checked={streaming}
              onChange={(e) => setStreaming(e.target.checked)}
            />
            <div className="w-14 h-8 bg-surface-container-highest rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[4px] after:left-[4px] after:bg-white after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-primary" />
          </label>
        </div>
      </div>
    </div>
  )
}
