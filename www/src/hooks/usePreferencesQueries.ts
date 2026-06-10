import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import api from '@/network/axios'
import type {
  NarrationSettings,
  OnboardingQuestionsResponse,
  OnboardingAnswersRequest,
} from '@/types/preferences'

export function useNarrationSettings() {
  return useQuery<NarrationSettings>({
    queryKey: ['user', 'narration-settings'],
    queryFn: () => api.get('/user/narration-settings').then((r) => r.data),
  })
}

export function useUpdateNarrationSettings() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: NarrationSettings) => api.post('/user/narration-settings', body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user', 'narration-settings'] })
      toast.success('Ustawienia zapisane.')
    },
    onError: () => {
      toast.error('Wystąpił błąd podczas aktualizacji ustawień.')
    },
  })
}

export function useTestNarration() {
  return useMutation({
    mutationFn: () =>
      api.post('/user/narration-settings/test', {}, { responseType: 'blob' }).then((r) => r.data),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob)
      const audio = new Audio(url)
      audio.play()
      toast.success('Odtwarzanie narracji...')
    },
    onError: () => {
      toast.error(
        'Błąd odtwarzania narracji. Spróbuj ponownie później. Jeżeli błąd będzie się potwrzał, skontaktuj się z administratorem systemu.',
      )
    },
  })
}

export function useOnboardingQuestions(lang: string = 'pl') {
  return useQuery<OnboardingQuestionsResponse>({
    queryKey: ['user', 'onboarding', lang],
    queryFn: () => api.get(`/user/onboarding/questions?lang=${lang}`).then((r) => r.data),
  })
}

export function useUpdateOnboardingAnswers() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: OnboardingAnswersRequest) => api.post('/user/onboarding/answers', body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['user', 'onboarding'] })
      toast.success('Preferencje zapisane.')
    },
    onError: () => {
      toast.error('Wystąpił błąd podczas aktualizacji preferencji.')
    },
  })
}
