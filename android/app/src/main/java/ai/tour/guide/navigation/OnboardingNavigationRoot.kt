package ai.tour.guide.navigation

import ai.tour.guide.ui.screens.onboarding.OnboardingLoginStepActivity
import ai.tour.guide.ui.screens.onboarding.OnboardingPreferencesStepScreen
import ai.tour.guide.ui.screens.onboarding.OnboardingRegisterStepScreen
import ai.tour.guide.ui.screens.onboarding.OnboardingWelcomeStepActivity
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(OnboardingRoute.Welcome)
    NavDisplay(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is OnboardingRoute.Welcome -> NavEntry(key) {
                    OnboardingWelcomeStepActivity(onNextClicked = {
                        backStack.clear()
                        backStack.add(OnboardingRoute.Login)
                    })
                }

                is OnboardingRoute.Login -> NavEntry(key) {
                    OnboardingLoginStepActivity(
                        onRegisterSpanClicked = {
                            backStack.add(OnboardingRoute.Register)
                        },
                        onLoginFinished = {
                            backStack.clear()
                            backStack.add(OnboardingRoute.Preferences)
                        }
                    )
                }

                is OnboardingRoute.Register -> NavEntry(key) {
                    OnboardingRegisterStepScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onUserRegistered = {
                            backStack.clear()
                            backStack.add(OnboardingRoute.Preferences)
                        })
                }

                is OnboardingRoute.Preferences -> NavEntry(key) {
                    OnboardingPreferencesStepScreen()
                }

                else -> error("Invalid NavKey: $key")
            }
        },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(500)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(500)
            )
        },
        popTransitionSpec = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(500)
            ) togetherWith slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(500)
            )
        }
    )
}
