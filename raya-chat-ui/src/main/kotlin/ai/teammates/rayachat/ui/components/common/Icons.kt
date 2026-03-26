package ai.teammates.rayachat.ui.components.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** All 16 SVG icons as Compose ImageVector — ported from lucide-react icons in the RN SDK. */
object RayaIcons {

    fun send(color: Color = Color.Black): ImageVector = ImageVector.Builder(
        defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f
    ).apply {
        path(fill = SolidColor(color)) {
            moveTo(15.44f, 1.68f)
            curveToRelative(0.69f, -0.05f, 1.47f, 0.08f, 2.13f, 0.74f)
            curveToRelative(0.66f, 0.67f, 0.8f, 1.45f, 0.75f, 2.14f)
            curveToRelative(-0.03f, 0.47f, -0.15f, 1f, -0.25f, 1.4f)
            lineToRelative(-0.09f, 0.35f)
            arcToRelative(43.7f, 43.7f, 0f, false, true, -3.83f, 10.67f)
            arcTo(2.52f, 2.52f, 0f, false, true, 9.7f, 17f)
            lineToRelative(-1.65f, -3.03f)
            arcToRelative(0.83f, 0.83f, 0f, false, true, 0.14f, -1f)
            lineToRelative(3.1f, -3.1f)
            arcToRelative(0.83f, 0.83f, 0f, true, false, -1.18f, -1.17f)
            lineToRelative(-3.1f, 3.1f)
            arcToRelative(0.83f, 0.83f, 0f, false, true, -0.99f, 0.14f)
            lineTo(2.98f, 10.3f)
            arcToRelative(2.52f, 2.52f, 0f, false, true, 0.04f, -4.45f)
            arcToRelative(43.7f, 43.7f, 0f, false, true, 11.02f, -3.9f)
            curveToRelative(0.4f, -0.1f, 0.92f, -0.23f, 1.4f, -0.26f)
            close()
        }
    }.build()

    fun close(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(6f, 6f); lineTo(18f, 18f)
        }
    }.build()

    fun chevronLeft(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15f, 18f); lineTo(9f, 12f); lineTo(15f, 6f)
        }
    }.build()

    fun smile(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            // Circle
            moveTo(22f, 12f)
            arcTo(10f, 10f, 0f, false, true, 12f, 22f)
            arcTo(10f, 10f, 0f, false, true, 2f, 12f)
            arcTo(10f, 10f, 0f, false, true, 22f, 12f)
            close()
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(8f, 14f); curveToRelative(0f, 0f, 1.5f, 2f, 4f, 2f); curveToRelative(2.5f, 0f, 4f, -2f, 4f, -2f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(9f, 9f); lineToRelative(0.01f, 0f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15f, 9f); lineToRelative(0.01f, 0f)
        }
    }.build()

    fun mic(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 2f)
            arcToRelative(3f, 3f, 0f, false, false, -3f, 3f)
            verticalLineToRelative(7f)
            arcToRelative(3f, 3f, 0f, false, false, 6f, 0f)
            verticalLineTo(5f)
            arcToRelative(3f, 3f, 0f, false, false, -3f, -3f)
            close()
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(19f, 10f); verticalLineToRelative(2f); arcToRelative(7f, 7f, 0f, false, true, -14f, 0f); verticalLineToRelative(-2f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 19f); verticalLineToRelative(3f)
        }
    }.build()

    fun paperclip(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(21.44f, 11.05f)
            lineToRelative(-9.19f, 9.19f)
            arcToRelative(6f, 6f, 0f, false, true, -8.49f, -8.49f)
            lineToRelative(8.57f, -8.57f)
            arcTo(4f, 4f, 0f, true, true, 18f, 8.84f)
            lineToRelative(-8.59f, 8.57f)
            arcToRelative(2f, 2f, 0f, false, true, -2.83f, -2.83f)
            lineToRelative(8.49f, -8.48f)
        }
    }.build()

    fun shieldCheck(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 22f)
            curveToRelative(0f, 0f, 8f, -4f, 8f, -10f)
            verticalLineTo(5f)
            lineToRelative(-8f, -3f)
            lineToRelative(-8f, 3f)
            verticalLineToRelative(7f)
            curveToRelative(0f, 6f, 8f, 10f, 8f, 10f)
            close()
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(9f, 12f); lineToRelative(2f, 2f); lineToRelative(4f, -4f)
        }
    }.build()

    fun phoneCall(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15.05f, 5f)
            arcTo(5f, 5f, 0f, false, true, 19f, 8.95f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15.05f, 1f)
            arcTo(9f, 9f, 0f, false, true, 23f, 8.94f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(22f, 16.92f)
            verticalLineToRelative(3f)
            arcToRelative(2f, 2f, 0f, false, true, -2.18f, 2f)
            arcToRelative(19.79f, 19.79f, 0f, false, true, -8.63f, -3.07f)
            arcToRelative(19.5f, 19.5f, 0f, false, true, -6f, -6f)
            arcToRelative(19.79f, 19.79f, 0f, false, true, -3.07f, -8.67f)
            arcTo(2f, 2f, 0f, false, true, 4.11f, 2f)
            horizontalLineToRelative(3f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 1.72f)
            curveToRelative(0.127f, 0.96f, 0.362f, 1.903f, 0.7f, 2.81f)
            arcToRelative(2f, 2f, 0f, false, true, -0.45f, 2.11f)
            lineTo(8.09f, 9.91f)
            arcToRelative(16f, 16f, 0f, false, false, 6f, 6f)
            lineToRelative(1.27f, -1.27f)
            arcToRelative(2f, 2f, 0f, false, true, 2.11f, -0.45f)
            curveToRelative(0.907f, 0.338f, 1.85f, 0.573f, 2.81f, 0.7f)
            arcTo(2f, 2f, 0f, false, true, 22f, 16.92f)
            close()
        }
    }.build()

    fun user(color: Color = Color.White, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15.75f, 6f)
            arcToRelative(3.75f, 3.75f, 0f, true, true, -7.5f, 0f)
            arcToRelative(3.75f, 3.75f, 0f, false, true, 7.5f, 0f)
            close()
            moveTo(4.501f, 20.118f)
            arcToRelative(7.5f, 7.5f, 0f, false, true, 14.998f, 0f)
            arcTo(17.933f, 17.933f, 0f, false, true, 12f, 21.75f)
            curveToRelative(-2.676f, 0f, -5.216f, -0.584f, -7.499f, -1.632f)
            close()
        }
    }.build()

    fun alertTriangle(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(10.29f, 3.86f)
            lineTo(1.82f, 18f)
            arcToRelative(2f, 2f, 0f, false, false, 1.71f, 3f)
            horizontalLineToRelative(16.94f)
            arcToRelative(2f, 2f, 0f, false, false, 1.71f, -3f)
            lineTo(13.71f, 3.86f)
            arcToRelative(2f, 2f, 0f, false, false, -3.42f, 0f)
            close()
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 9f); verticalLineToRelative(4f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 17f); lineToRelative(0.01f, 0f)
        }
    }.build()

    fun messageSquareX(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(21f, 15f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            horizontalLineTo(7f)
            lineToRelative(-4f, 4f)
            verticalLineTo(5f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            horizontalLineToRelative(14f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            verticalLineToRelative(10f)
            close()
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(14.5f, 7.5f); lineTo(9.5f, 12.5f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(9.5f, 7.5f); lineTo(14.5f, 12.5f)
        }
    }.build()

    fun arrowDown(color: Color = Color.Black, strokeWidth: Float = 2f): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 5f); verticalLineTo(19f)
        }
        path(stroke = SolidColor(color), strokeLineWidth = strokeWidth, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(19f, 12f); lineTo(12f, 19f); lineTo(5f, 12f)
        }
    }.build()
}
