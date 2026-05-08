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
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCarousel(
    modifier: Modifier = Modifier,
    imageUrls: List<String> = emptyList()
) {
    data class CarouselItem(
        val id: Int,
        val imageUrl: String? = null,
        @param:DrawableRes val imageResId: Int? = null,
        val contentDescription: String
    )

    val fallbackResIds = remember {
        listOf(R.drawable.dashboard_example_img_2)
    }

    val sanitizedUrls = remember(imageUrls) {
        imageUrls.map { it.trim() }.filter { it.isNotEmpty() }
    }

    val items = remember(sanitizedUrls) {
        val targetItemCount = 8
        val urlItems = repeatToSize(sanitizedUrls, targetItemCount)
        if (urlItems.isNotEmpty()) {
            urlItems.mapIndexed { index, url ->
                CarouselItem(index, imageUrl = url, contentDescription = "poi_image_$index")
            }
        } else {
            repeatToSize(fallbackResIds, targetItemCount).mapIndexed { index, resId ->
                CarouselItem(index, imageResId = resId, contentDescription = "fallback_image_$index")
            }
        }
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
            if (item.imageUrl.isNullOrBlank()) {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                    painter = painterResource(id = item.imageResId ?: R.drawable.dashboard_example_img_2),
                    contentDescription = item.contentDescription,
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                    model = item.imageUrl,
                    contentDescription = item.contentDescription,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = item.imageResId ?: R.drawable.dashboard_example_img_2),
                    error = painterResource(id = item.imageResId ?: R.drawable.dashboard_example_img_2)
                )
            }
        }
    }
}

private fun <T> repeatToSize(source: List<T>, size: Int): List<T> {
    if (source.isEmpty() || size <= 0) {
        return emptyList()
    }
    return List(size) { index -> source[index % source.size] }
}
