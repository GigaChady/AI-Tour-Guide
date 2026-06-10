import { useEffect, useRef, useState, useMemo } from 'react'
import type { StreamStatus, NarrationWord } from '@/hooks/useNarrationStream'

interface NarrationPlayerProps {
  poiTitle: string
  transcript: string
  words: NarrationWord[]
  audioUrl: string | null
  status: StreamStatus
}

export function NarrationPlayer({
  poiTitle,
  transcript,
  words,
  audioUrl,
  status,
}: NarrationPlayerProps) {
  const audioRef = useRef<HTMLAudioElement>(null)
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentTimeMs, setCurrentTimeMs] = useState(0)

  useEffect(() => {
    if (audioUrl && audioRef.current) {
      audioRef.current.play().catch((e) => console.error('Error while playing audio', e))
    }
  }, [audioUrl])

  const togglePlay = () => {
    if (!audioRef.current) return
    if (audioRef.current.paused) {
      audioRef.current.play()
    } else {
      audioRef.current.pause()
    }
  }

  // Match TTS words with transcript to ensure correct text highlighting
  const wordEndPositions = useMemo(() => {
    if (!transcript || words.length === 0) return []

    let currentPos = 0
    const lowerTranscript = transcript.toLowerCase()

    return words.map((w) => {
      const wordText = w.text.toLowerCase()
      const idx = lowerTranscript.indexOf(wordText, currentPos)

      if (idx !== -1) {
        currentPos = idx + wordText.length
        return currentPos
      }
      return currentPos
    })
  }, [transcript, words])

  const activeWordIndex = words.findLastIndex((w) => currentTimeMs >= w.offset_ms)
  const splitIndex =
    activeWordIndex >= 0 && activeWordIndex < wordEndPositions.length
      ? wordEndPositions[activeWordIndex]
      : 0
  const readText = transcript.slice(0, splitIndex)
  const unreadText = transcript.slice(splitIndex)

  const isGenerating = status === 'connecting' || status === 'generating'

  return (
    <div className="w-full bg-surface-container-highest/95 backdrop-blur-2xl rounded-2xl border border-primary/20 p-5 flex flex-col gap-4 shadow-[0_8px_32px_rgba(0,0,0,0.5)] pointer-events-auto transition-all">
      {audioUrl && (
        <audio
          ref={audioRef}
          src={audioUrl}
          className="hidden"
          onPlay={() => setIsPlaying(true)}
          onPause={() => setIsPlaying(false)}
          onEnded={() => setIsPlaying(false)}
          onTimeUpdate={(e) => setCurrentTimeMs(e.currentTarget.currentTime * 1000)}
        />
      )}

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-primary/20 flex items-center justify-center border border-primary/50 relative shrink-0">
            {isGenerating && (
              <div className="absolute inset-0 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
            )}
            <span className="material-symbols-outlined text-primary">graphic_eq</span>
          </div>
          <div className="truncate pr-4">
            <h3 className="font-title-lg text-on-surface truncate">Lokalizacja: {poiTitle}</h3>
            <p className="font-body-md text-primary-fixed-dim text-xs uppercase tracking-wider mt-1">
              {status === 'generating'
                ? 'Generowanie narracji...'
                : isPlaying
                  ? 'Odtwarzanie narracji'
                  : 'Odtwarzanie wstrzymane'}
            </p>
          </div>
        </div>

        <div className="flex items-center shrink-0">
          <button
            onClick={togglePlay}
            disabled={!audioUrl}
            className="w-14 h-14 rounded-full bg-primary text-on-primary hover:bg-white transition-all duration-300 shadow-[0_0_15px_rgba(208,188,255,0.6)] flex items-center justify-center disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span
              className="material-symbols-outlined text-3xl"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              {isPlaying ? 'pause' : 'play_arrow'}
            </span>
          </button>
        </div>
      </div>

      <div className="bg-surface-container-low rounded-xl p-4 border border-outline-variant/30 overflow-y-auto relative h-40 mt-2 scrollbar-thin">
        {words.length > 0 && transcript ? (
          <p className="font-body-lg leading-relaxed whitespace-pre-wrap">
            <span className="text-white font-medium transition-colors duration-150">
              {readText}
            </span>
            <span className="text-on-surface-variant opacity-50 transition-colors duration-150">
              {unreadText}
            </span>
          </p>
        ) : transcript ? (
          <p className="font-body-lg text-on-surface-variant opacity-50 leading-relaxed">
            {transcript}
          </p>
        ) : (
          <p className="font-body-md text-on-surface-variant leading-relaxed opacity-50 italic">
            Kliknij dowolne miejsce na mapie, aby wygenerować narrację...
          </p>
        )}
        <div className="absolute bottom-0 left-0 right-0 h-12 bg-linear-to-t from-surface-container-low to-transparent pointer-events-none"></div>
      </div>
    </div>
  )
}
