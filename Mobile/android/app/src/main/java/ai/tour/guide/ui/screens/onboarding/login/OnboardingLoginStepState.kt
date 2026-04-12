package ai.tour.guide.ui.screens.onboarding.login

data class OnboardingLoginStepState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
) {
    companion object {
        fun default() = OnboardingLoginStepState(
            email = "",
            password = "",
            errorMessage = null,
            isLoading = false
        )
    }
}