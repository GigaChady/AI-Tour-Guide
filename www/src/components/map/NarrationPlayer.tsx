interface NarrationPlayerProps {
  poiTitle: string
  isPlaying: boolean
  onTogglePlay: () => void
}

export function NarrationPlayer({ poiTitle, isPlaying, onTogglePlay }: NarrationPlayerProps) {
  return (
    <div className="w-full bg-surface-container-highest/95 backdrop-blur-2xl rounded-2xl border border-primary/20 p-5 flex flex-col gap-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)] pointer-events-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center border border-primary/50 relative shrink-0">
            {isPlaying && (
              <div className="absolute inset-0 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
            )}
            <span className="material-symbols-outlined text-primary">graphic_eq</span>
          </div>
          <div className="truncate pr-4">
            <h3 className="font-title-lg text-on-surface truncate">AI Guide: {poiTitle}</h3>
            <p className="font-body-md text-primary-fixed-dim text-xs uppercase tracking-wider mt-1">
              {isPlaying ? 'Live Narration' : 'Zatrzymano'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 md:gap-3 shrink-0">
          <button className="p-2 rounded-full text-primary hover:bg-primary/10 transition-colors">
            <span className="material-symbols-outlined text-2xl">replay_10</span>
          </button>
          <button
            onClick={onTogglePlay}
            className="w-12 h-12 md:w-14 md:h-14 rounded-full bg-primary text-on-primary hover:bg-white transition-all duration-300 shadow-[0_0_15px_rgba(208,188,255,0.6)] flex items-center justify-center"
          >
            <span className="material-symbols-outlined text-3xl">
              {isPlaying ? 'pause' : 'play_arrow'}
            </span>
          </button>
          <button className="p-2 rounded-full text-primary hover:bg-primary/10 transition-colors">
            <span className="material-symbols-outlined text-2xl">forward_10</span>
          </button>
        </div>
      </div>

      <div className="bg-surface-container-low rounded-xl p-4 border border-outline-variant/30 overflow-hidden relative h-40 mt-2">
        <p className="font-body-md text-on-surface-variant leading-relaxed opacity-50 mb-2">
          ...zbudowany na przełomie wieków, ten obiekt służył jako główne centrum wydarzeń, goszcząc
          najważniejsze osobistości w regionie...
        </p>
        <p className="font-body-lg text-on-surface leading-relaxed text-primary-fixed-dim">
          Zwróć uwagę na misternie zdobioną fasadę po twojej lewej stronie. Została ona
          zaprojektowana nie tylko w celach estetycznych, ale również jako symbol potęgi dawnych
          włodarzy miasta.
        </p>
        <div className="absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-surface-container-low to-transparent"></div>
      </div>
    </div>
  )
}
