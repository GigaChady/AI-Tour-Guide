package ai.tour.guide.navigation.onboarding

import ai.tour.guide.ui.screens.onboarding.FinishStepScreen
import ai.tour.guide.ui.screens.onboarding.LoginStepScreen
import ai.tour.guide.ui.screens.onboarding.PreferencesStepScreen
import ai.tour.guide.ui.screens.onboarding.RegisterStepScreen
import ai.tour.guide.ui.screens.onboarding.WelcomeStepScreen
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
fun OnboardingNavigationRoot(
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backStack = rememberNavBackStack(OnboardingRoute.Welcome)
    NavDisplay(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is OnboardingRoute.Welcome -> NavEntry(key) {
                    WelcomeStepScreen(onNextClicked = {
                        backStack.clear()
                        backStack.add(OnboardingRoute.Login)
                    })
                }

                is OnboardingRoute.Login -> NavEntry(key) {
                    LoginStepScreen(
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
                    RegisterStepScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onUserRegistered = {
                            backStack.clear()
                            backStack.add(OnboardingRoute.Preferences)
                        })
                }

                is OnboardingRoute.Preferences -> NavEntry(key) {
                    PreferencesStepScreen(onNextClicked = {
                        backStack.clear()
                        backStack.add(OnboardingRoute.Finish)
                    })
                }

                is OnboardingRoute.Finish -> NavEntry(key) {
                    FinishStepScreen(onNextClicked = {
                        onOnboardingFinished()
                    })
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
