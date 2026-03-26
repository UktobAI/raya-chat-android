package ai.teammates.rayachat.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object RayaTypography {
    val heading = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
    val subheading = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
    val body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)
    val bodyBold = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val small = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, lineHeight = 14.sp)
    val input = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val button = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val buttonSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
}
