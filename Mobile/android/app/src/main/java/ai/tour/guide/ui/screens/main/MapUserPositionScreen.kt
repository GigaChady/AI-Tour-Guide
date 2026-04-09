package ai.tour.guide.ui.screens.main

import ai.tour.guide.ui.theme.AiTourGuideTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MapUserPositionScreen(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(text = "map goes here")
    }
}

@Preview(showBackground = true)
@Composable
fun MapUserPositionScreenPreview() {
    AiTourGuideTheme {
        MapUserPositionScreen()
    }
}
