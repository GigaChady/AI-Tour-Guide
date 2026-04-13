package ai.tour.guide.ui.screens.onboarding.auth

data class OnboardingAuthStepState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = ""
) {
    companion object {
        fun default() = OnboardingAuthStepState()
    }
}