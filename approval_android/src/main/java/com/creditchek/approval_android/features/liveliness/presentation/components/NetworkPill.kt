package com.creditchek.approval_android.features.liveliness.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.network.NetworkQuality
import com.creditchek.approval_android.core.theme.ApprovalCanvas
import com.creditchek.approval_android.core.theme.ApprovalDanger
import com.creditchek.approval_android.core.theme.ApprovalSuccess
import com.creditchek.approval_android.core.theme.ApprovalWarning

@Composable
fun NetworkStatusPill(
    modifier: Modifier = Modifier,
    quality: NetworkQuality = NetworkQuality.EXCELLENT
) {
    val (statusText, statusColor) = when (quality) {
        NetworkQuality.EXCELLENT -> "Excellent network" to ApprovalSuccess
        NetworkQuality.MODERATE -> "Moderate network" to ApprovalWarning
        NetworkQuality.UNSTABLE -> "Unstable network" to ApprovalDanger
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .border(
                width = 1.dp,
                color = ApprovalCanvas,
                shape = CircleShape
            ),
        shape = CircleShape,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            NetworkSignalBarsIcon(
                color = statusColor,
                modifier = Modifier.size(width = 16.dp, height = 12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun NetworkSignalBarsIcon(
    modifier: Modifier = Modifier,
    color: Color = ApprovalSuccess
) {
    Canvas(modifier = modifier) {
        val barCount = 4
        val spacing = size.width * 0.12f
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val heightRatios = floatArrayOf(0.35f, 0.55f, 0.78f, 1.0f)
        for (i in 0 until barCount) {
            val barH = size.height * heightRatios[i]
            val left = i * (barWidth + spacing)
            val top = size.height - barH
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = cornerRadius
            )
        }
    }
}