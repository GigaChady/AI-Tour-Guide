package ai.tour.guide.ui.components.display

import ai.tour.guide.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCarousel(
    modifier: Modifier = Modifier,
    imageUrls: List<String> = emptyList()
) {
    val validImageUrls = remember(imageUrls) {
        imageUrls.filter { it.isNotBlank() }
    }
    val imageContentDescription = stringResource(R.string.dashboard_poi_image_content_description)
    val imageLoadErrorText = stringResource(R.string.dashboard_image_load_error_text)

    if (validImageUrls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.dashboard_empty_carousel_text),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    if (validImageUrls.size == 1) {
        CarouselImage(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 16.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            imageUrl = validImageUrls.first(),
            contentDescription = imageContentDescription,
            errorText = imageLoadErrorText
        )
        return
    }

    BoxWithConstraints(modifier = modifier) {
        val preferredWidth = maxWidth * 0.8f

        HorizontalMultiBrowseCarousel(
            state = rememberCarouselState { validImageUrls.size },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 16.dp),
            preferredItemWidth = preferredWidth,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) { i ->
            CarouselImage(
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge),
                imageUrl = validImageUrls[i],
                contentDescription = imageContentDescription,
                errorText = imageLoadErrorText
            )
        }
    }
}

@Composable
private fun CarouselImage(
    modifier: Modifier = Modifier,
    imageUrl: String,
    contentDescription: String,
    errorText: String
) {
    SubcomposeAsyncImage(
        modifier = modifier,
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    )
}
