import { useAdminConfig, useAdminProviders, useUpdateAdminConfig } from '@/hooks/useAdminQueries'
import type { ProviderOption } from '@/types/admin'

interface SelectFieldProps {
  label: string
  options: ProviderOption[]
  value: string
  onChange: (value: string) => void
  disabled?: boolean
}

function SelectField({ label, options, value, onChange, disabled }: SelectFieldProps) {
  return (
    <div>
      <label className="block font-label-lg text-label-lg text-on-surface-variant mb-2">
        {label}
      </label>
      <div className="relative">
        <select
          className="w-full appearance-none bg-surface-container-highest border-0 rounded-full py-4 pl-6 pr-12 font-body-lg text-body-lg text-on-surface focus:ring-2 focus:ring-primary focus:outline-none shadow-inner disabled:opacity-50"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
        >
          {options.map((opt) => (
            <option key={opt.key} value={opt.key}>
              {opt.label}
            </option>
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
  const { data: providers } = useAdminProviders()
  const { data: config } = useAdminConfig()
  const { mutate: updateConfig, isPending } = useUpdateAdminConfig()

  const llmValue = config?.llm_provider ?? ''
  const ttsValue = config?.tts_provider ?? ''

  return (
    <div className="bg-surface-container rounded-[2rem] p-lg border border-white/5 shadow-lg">
      <div className="flex items-center gap-3 mb-6">
        <span className="material-symbols-outlined text-primary text-3xl">neurology</span>
        <h3 className="font-title-lg text-title-lg text-on-surface">Narracja AI</h3>
      </div>
      <div className="space-y-6">
        <SelectField
          label="Główny silnik LLM"
          options={providers?.llm_providers ?? []}
          value={llmValue}
          onChange={(value) => updateConfig({ llm_provider: value })}
          disabled={isPending || !providers}
        />
        <SelectField
          label="Synteza głosu (TTS)"
          options={providers?.tts_providers ?? []}
          value={ttsValue}
          onChange={(value) => updateConfig({ tts_provider: value })}
          disabled={isPending || !providers}
        />
      </div>
    </div>
  )
}
