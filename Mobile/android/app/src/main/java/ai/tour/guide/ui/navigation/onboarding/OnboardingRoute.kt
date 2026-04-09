package ai.tour.guide.ui.navigation.onboarding

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface OnboardingRoute : NavKey {
    @Serializable
    data object Welcome : OnboardingRoute, NavKey

    @Serializable
    data object Login : OnboardingRoute, NavKey

    @Serializable
    data object Register : OnboardingRoute, NavKey

    @Serializable
    data object Preferences : OnboardingRoute, NavKey

    @Serializable
    data object Finish : OnboardingRoute, NavKey
}