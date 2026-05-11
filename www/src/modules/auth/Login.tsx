import { useNavigate } from 'react-router-dom'

export function Login() {
  const navigate = useNavigate()

  return (
    <div className="h-screen w-full bg-background flex items-center justify-center p-6">
      <div className="w-full max-w-[24rem]">
        <div className="bg-surface-container rounded-2xl border border-outline-variant/20 p-10 relative overflow-hidden">
          <div className="absolute inset-0 bg-primary/5 pointer-events-none" />

          <div className="relative z-10 flex flex-col items-center text-center">
            <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-primary/10 text-primary mb-6">
              <span
                className="material-symbols-outlined text-[40px]"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                explore
              </span>
            </div>

            <h1 className="font-headline-lg text-headline-lg text-on-surface">AI Tour Guide</h1>
            <p className="font-body-md text-body-md text-on-surface-variant mt-2 mb-10">
              Sign in to access your dashboard.
            </p>

            <button
              onClick={() => navigate('/')}
              className="w-full flex items-center justify-center gap-3 bg-primary text-on-primary font-label-lg text-label-lg py-3.5 rounded-full hover:bg-primary-fixed-dim transition-all active:scale-95"
            >
              <span
                className="material-symbols-outlined text-[20px]"
                style={{ fontVariationSettings: "'FILL' 0" }}
              >
                passkey
              </span>
              Continue with SSO
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
