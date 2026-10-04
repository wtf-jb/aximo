package io.github.wtfjb.aximo.ui.catalog

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.theme.Spacing
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Start and end position of a library exercise, side by side
 * (`assets/exercise_images/<id>/0.webp` and `1.webp`). Shows nothing while
 * loading and for entries without photos.
 */
@Composable
fun ExerciseImages(libraryId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val images by produceState<List<ImageBitmap>>(emptyList(), libraryId) {
        value = withContext(Dispatchers.IO) { loadImages(context, libraryId) }
    }
    if (images.isEmpty()) return

    val descriptions = listOf(stringResource(R.string.catalog_image_start), stringResource(R.string.catalog_image_end))
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        images.forEachIndexed { index, image ->
            Image(
                bitmap = image,
                contentDescription = descriptions.getOrNull(index),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(IMAGE_ASPECT)
                    .clip(MaterialTheme.shapes.small),
            )
        }
    }
}

private fun loadImages(context: Context, libraryId: String): List<ImageBitmap> =
    (0..1).mapNotNull { index ->
        try {
            context.assets.open("$IMAGE_FOLDER/$libraryId/$index.webp").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
        } catch (e: IOException) {
            null
        }
    }

/** The photos are 3:2. */
private const val IMAGE_ASPECT = 1.5f
private const val IMAGE_FOLDER = "exercise_images"
