export interface NarrationSettings {
  language: string
  pitch: number
  speed: number
  detail_level: string
  auto_play: boolean
}

export interface OnboardingOption {
  key: string
  title: string
  body?: string | null
  trailing_content?: string | null
}

export interface OnboardingQuestion {
  key: string
  title: string
  type: 'single_choice' | 'multi_choice'
  options: OnboardingOption[]
}

export interface OnboardingSelectedAnswers {
  gender?: string | null
  interests: string[]
}

export interface OnboardingQuestionsResponse {
  items: OnboardingQuestion[]
  selected_answers: OnboardingSelectedAnswers
}

export interface OnboardingAnswerRequest {
  question_key: string
  answer_key?: string | null
  answer_keys?: string[] | null
}

export interface OnboardingAnswersRequest {
  items: OnboardingAnswerRequest[]
  detail?: string | null
}
