export interface Step1PlanNameProps {
  name: string
  setName: (v: string) => void
}

export function Step1PlanName({ name, setName }: Step1PlanNameProps) {
  return (
    <div className="flex flex-col">
        <label className="font-body-md text-body-md text-on-surface-variant">Nazwa trasy</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Podaj nazwę"
          className="w-full bg-surface-container px-4 py-3 rounded-xl border border-outline-variant/40 text-on-surface font-body-lg placeholder:text-on-surface-variant/40 focus:outline-none focus:border-primary/60 transition-colors"
          autoFocus
        />
    </div>
  )
}
