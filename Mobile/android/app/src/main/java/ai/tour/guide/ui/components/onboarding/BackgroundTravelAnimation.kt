package ai.tour.guide.ui.components.onboarding

import ai.tour.guide.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

@Preview(showBackground = true)
@Composable
fun OnboardingAnimation(modifier: Modifier = Modifier) {
    val screenHeight = LocalWindowInfo.current.containerSize.height
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.onboarding_animation
        )
    )
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    AnimatedVisibility(
        modifier = modifier,
        visible = composition != null,
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = 1000,
                delayMillis = 0,
                easing = LinearOutSlowInEasing
            )
        ) {
            screenHeight / 2
        }
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier =Modifier
                    .scale(scale = 3.5F)
            )
        }
    }

}