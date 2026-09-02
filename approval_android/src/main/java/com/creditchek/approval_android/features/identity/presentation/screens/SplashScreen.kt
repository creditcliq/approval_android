package com.creditchek.approval_android.features.identity.presentation.screens


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.creditchek.approval_android.R
import com.creditchek.approval_android.core.theme.ApprovalTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(
    onValidationSuccess: () -> Unit,
    onValidationError: (String) -> Unit,
    validateAction: suspend () -> Result<Unit>
) {
    // ── 1. Scale Pulse Animation (0.85 to 1.1) matching Flutter ──
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_anim"
    )

    // ── 2. Run Public Key Validation on Launch ──
    LaunchedEffect(Unit) {
        val result = validateAction()
        delay(1000.milliseconds) // 1-second brand delay matching Flutter splash

        result.onSuccess {
            onValidationSuccess()
        }.onFailure { error ->
            onValidationError(error.message ?: "Invalid Public Key")
        }
    }

    // ── 3. Centered Pulsing Logo ──
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
               Image(
                    painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "CreditChek Logo",
                    modifier = Modifier.fillMaxSize()
                )

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    ApprovalTheme {
        SplashScreen(
            onValidationSuccess = {},
            onValidationError = {},
            validateAction = { Result.success(Unit) }
        )
    }
}