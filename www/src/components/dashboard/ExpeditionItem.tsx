type AvatarImage = {
  type: 'image'
  src: string
  alt: string
  bgClass: string
  borderClass: string
}

type AvatarIcon = {
  type: 'icon'
  iconClass: string
  iconColor: string
  bgClass: string
  borderClass: string
}

interface ExpeditionItemProps {
  city: string
  date: string
  avatar: AvatarImage | AvatarIcon
  onClick?: () => void
}

export function ExpeditionItem({ city, date, avatar, onClick }: ExpeditionItemProps) {
  return (
    <div
      onClick={onClick}
      className="bg-cardDark rounded-2xl p-4 flex items-center justify-between hover:bg-hoverHighlight transition-colors cursor-pointer border border-gray-800/30"
    >
      <div className="flex items-center space-x-4">
        <div
          className={`w-12 h-12 rounded-full ${avatar.bgClass} flex items-center justify-center shrink-0 ${avatar.borderClass} ${avatar.type === 'image' ? 'overflow-hidden' : ''}`}
        >
          {avatar.type === 'image' ? (
            <img
              alt={avatar.alt}
              className="w-full h-full object-cover opacity-80"
              src={avatar.src}
            />
          ) : (
            <i className={`${avatar.iconClass} ${avatar.iconColor}`} />
          )}
        </div>
        <div>
          <h3 className="text-white font-medium">{city}</h3>
          <p className="text-sm text-textMuted">{date}</p>
        </div>
      </div>
      <div className="w-8 h-8 rounded-full border border-gray-600 flex items-center justify-center text-gray-400">
        <i className="fa-solid fa-chevron-right text-xs" />
      </div>
    </div>
  )
}
