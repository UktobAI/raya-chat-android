package ai.teammates.rayachat.ui.components.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import ai.teammates.rayachat.core.models.ImageAsset
import ai.teammates.rayachat.ui.components.common.RayaIcons
import androidx.compose.foundation.clickable

/**
 * Image preview grid above the composer — matches web widget UI.
 * Fixed-size thumbnails with small X close button at top-right corner.
 */
@Composable
fun ImagePickerPreview(
    images: List<ImageAsset>,
    onRemove: (Int) -> Unit,
) {
    if (images.isEmpty()) return

    val thumbSize = 100 // Fixed size matching web widget
    val gap = 10

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(gap.dp, Alignment.Start),
    ) {
        images.forEachIndexed { index, img ->
            Box(
                modifier = Modifier
                    .size(thumbSize.dp)
                    .padding(top = 10.dp, end = 10.dp) // Space for X button overflow
            ) {
                // Thumbnail
                Image(
                    painter = rememberAsyncImagePainter(img.uri),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )

                // X close button — small circle at top-right
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .clickable { onRemove(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RayaIcons.close(Color.White, 2.5f),
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
    }
}
