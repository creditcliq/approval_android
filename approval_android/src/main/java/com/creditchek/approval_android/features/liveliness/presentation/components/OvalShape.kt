package com.creditchek.approval_android.features.liveliness.presentation.components

import android.graphics.PathMeasure
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Builds the face-oval bezier Path matching FaceOvalShape.
 */
fun faceOvalPath(size: Size): Path = Path().apply {
    val w = size.width
    val h = size.height

    moveTo(w * 0.5f, 0f)
    // Top-right curve
    cubicTo(w * 0.85f, 0f, w, h * 0.20f, w, h * 0.42f)
    // Bottom-right curve tapering to chin
    cubicTo(w, h * 0.72f, w * 0.78f, h, w * 0.5f, h)
    // Bottom-left curve tapering up from chin
    cubicTo(w * 0.22f, h, 0f, h * 0.72f, 0f, h * 0.42f)
    // Top-left curve returning to top
    cubicTo(0f, h * 0.20f, w * 0.15f, 0f, w * 0.5f, 0f)
    close()
}

/**
 * Draws the animated face oval border with live sweeping progress.
 * 1:1 match with Flutter's FaceGuidePainter.
 */
fun DrawScope.drawFaceOvalBorder(
    color: Color,
    strokeWidth: Float = 12f,
    backgroundColor: Color? = null,
    progress: Float = 1f
) {
    val path = faceOvalPath(size)

    // 1. Background ghost ring (shown when face is aligned)
    if (backgroundColor != null) {
        drawPath(
            path = path,
            color = backgroundColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    val visibleProgress = progress.coerceIn(0f, 1f)

    // 2. Full ring when idle or complete
    if (visibleProgress >= 1f) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        return
    }

    if (visibleProgress <= 0f) return

    // 3. Path-Metric Progress Segment (Sweeps smoothly around the oval)
    val androidPath = path.asAndroidPath()
    val pathMeasure = PathMeasure(androidPath, false)
    val totalLength = pathMeasure.length
    val segmentAndroidPath = android.graphics.Path()

    pathMeasure.getSegment(0f, totalLength * visibleProgress, segmentAndroidPath, true)

    drawPath(
        path = segmentAndroidPath.asComposePath(),
        color = color,
        style = Stroke(width = strokeWidth + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}