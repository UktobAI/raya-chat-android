package ai.teammates.rayachat.ui.components.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import kotlin.math.min

private const val MAX_THUMB_SIZE = 80 // Cap — matches RN SDK
private const val HORIZONTAL_PADDING = 32 // 16dp each side
private const val GAP = 8

/**
 * Image preview grid above the composer.
 * Dynamic sizing: (availableWidth - gaps) / count, capped at 80dp.
 * Matches RN SDK behavior exactly.
 */
@Composable
fun ImagePickerPreview(
    images: List<ImageAsset>,
    onRemove: (Int) -> Unit,
) {
    if (images.isEmpty()) return

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val count = images.size
    val availableWidth = screenWidthDp - HORIZONTAL_PADDING
    val calculated = (availableWidth - GAP * (count - 1)) / count
    val thumbSize = min(calculated, MAX_THUMB_SIZE)

    val closeIcon = remember { RayaIcons.close(Color.White, 2.5f) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(GAP.dp),
    ) {
        images.forEachIndexed { index, img ->
            Box(modifier = Modifier.size(thumbSize.dp)) {
                // Thumbnail
                Image(
                    painter = rememberAsyncImagePainter(img.uri),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )

                // X close button — white circle with dark border and dark X
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.5.dp, Color(0xFF333333), CircleShape)
                        .clickable { onRemove(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = closeIcon,
                        contentDescription = "Remove",
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }
    }
}
