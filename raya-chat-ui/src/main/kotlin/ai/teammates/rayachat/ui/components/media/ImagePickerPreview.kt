package ai.teammates.rayachat.ui.components.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

/**
 * Dynamic-width image preview grid with X remove buttons.
 * Image size calculated to fill the available width based on count.
 */
@Composable
fun ImagePickerPreview(
    images: List<ImageAsset>,
    onRemove: (Int) -> Unit,
) {
    if (images.isEmpty()) return

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val horizontalInset = 24 // outerContainer padding
    val gap = 8
    val availableWidth = screenWidth - horizontalInset
    val imageSize = ((availableWidth - gap * (images.size - 1)) / images.size).coerceAtMost(80)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp)
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(gap.dp),
    ) {
        images.forEachIndexed { index, img ->
            Box(modifier = Modifier.size(imageSize.dp)) {
                Image(
                    painter = rememberAsyncImagePainter(img.uri),
                    contentDescription = "Selected image",
                    modifier = Modifier
                        .size(imageSize.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop,
                )
                // X remove button
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .shadow(2.dp, CircleShape)
                        .background(Color(0xFFF7F9FB), CircleShape),
                ) {
                    Icon(
                        imageVector = RayaIcons.close(Color(0xFF333333), 3f),
                        contentDescription = "Remove",
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(8.dp),
                    )
                }
            }
        }
    }
}
