package ai.tour.guide.activity

import ai.tour.guide.ui.navigation.AppDrawerLayout
import ai.tour.guide.ui.theme.AiTourGuideTheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, OnboardingActivity::class.java))
        enableEdgeToEdge()
        setContent {
            AiTourGuideTheme {
                AppDrawerLayout()
            }
        }
    }
}
