package ai.tour.guide.activity

import ai.tour.guide.ui.navigation.AppNavigationDisplay
import ai.tour.guide.ui.theme.AiTourGuideTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiTourGuideTheme {
                AppNavigationDisplay()
            }
        }
    }
}
