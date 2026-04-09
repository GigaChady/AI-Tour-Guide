package ai.tour.guide.activity

import ai.tour.guide.ui.navigation.onboarding.OnboardingNavigationRoot
import ai.tour.guide.ui.theme.AiTourGuideTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class OnboardingActivity : ComponentActivity() {
    private var onboardingFinished = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiTourGuideTheme {
                OnboardingNavigationRoot(
                    onOnboardingFinished = {
                        this@OnboardingActivity.onboardingFinished = true
                        finish()
                    }
                )
            }
        }
    }
}
