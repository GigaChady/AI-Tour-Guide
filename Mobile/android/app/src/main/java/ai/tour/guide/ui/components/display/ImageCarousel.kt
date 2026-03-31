package ai.tour.guide.ui.components.display

import ai.tour.guide.R
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCarousel(modifier: Modifier = Modifier) {
    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        val contentDescription: String
    )

    val items = remember {
        listOf(
            CarouselItem(0, R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem(1, R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem( 2,R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem(3, R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem(4, R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem(5, R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem(6, R.drawable.dashboard_example_img_2, "test1"),
            CarouselItem(7, R.drawable.dashboard_example_img_2, "test1"),
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val preferredWidth = maxWidth * 0.8f

        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { items.count() },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 16.dp),
            preferredItemWidth = preferredWidth,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            val item = items[i]
            Image(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge),
                painter = painterResource(id = item.imageResId),
                contentDescription = item.contentDescription,
                contentScale = ContentScale.Crop
            )
        }
    }
}
