package ai.tour.guide.activity

import ai.tour.guide.data.PersistedAppDataStore
import ai.tour.guide.ui.navigation.AppNavigationDisplay
import ai.tour.guide.ui.navigation.Route
import ai.tour.guide.ui.theme.AiTourGuideTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appData by applicationContext.PersistedAppDataStore.data.collectAsState(initial = null)
            AiTourGuideTheme {
                appData?.let { data ->
                    val startRoute = if (data.refreshToken == null) {
                        Route.OnboardingWelcomeStepScreen
                    } else {
                        Route.Dashboard
                    }
                    AppNavigationDisplay(initialRoute = startRoute)
                }
            }
        }
    }
}