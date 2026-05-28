interface SliderFieldProps {
  label: string
  min: number
  max: number
  step: number
  value: number
  onChange: (val: number) => void
}

export function SliderField({ label, min, max, step, value, onChange }: SliderFieldProps) {
  return (
    <div className="flex flex-col gap-3">
      <label className="font-body-md text-body-md text-on-surface-variant">{label}</label>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseFloat(e.target.value))}
        className="w-full h-2 bg-surface-container-highest rounded-full appearance-none outline-none accent-primary cursor-pointer"
      />
    </div>
  )
}
