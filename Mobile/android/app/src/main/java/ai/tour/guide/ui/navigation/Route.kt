package ai.tour.guide.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Route : NavKey {
    @Serializable
    data object Dashboard : Route, NavKey

    @Serializable
    data object Profile : Route, NavKey

    @Serializable
    data object Settings : Route, NavKey

    @Serializable
    data object AccountSettings : Route, NavKey

    @Serializable
    data object TourAudioPlayer : Route, NavKey

    @Serializable
    data object MapUserPosition : Route, NavKey

    @Serializable
    data object TripEndSummary : Route, NavKey

    @Serializable
    data object OnboardingFinishStepScreen : Route, NavKey

    @Serializable
    data object OnboardingRegisterStepScreen : Route, NavKey

    @Serializable
    data object OnboardingWelcomeStepScreen : Route, NavKey

    @Serializable
    data object OnboardingLoginStepScreen : Route, NavKey

    @Serializable
    data object OnboardingPreferencesStepScreen : Route, NavKey
}
