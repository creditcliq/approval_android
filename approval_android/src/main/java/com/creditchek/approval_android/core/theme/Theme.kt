package com.creditchek.approval_android.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Color Scheme for the SDK
private val ApprovalColorScheme = lightColorScheme(
    primary = ApprovalBlue,
    onPrimary = Color.White,
    primaryContainer = ApprovalMutedButton,
    onPrimaryContainer = ApprovalBlue,
    background = ApprovalCanvas,
    onBackground = ApprovalTextPrimary,
    surface = Color.White,
    onSurface = ApprovalTextPrimary,
    error = ApprovalDanger,
    onError = Color.White
)

/**
 * Main Theme wrapper for the Approval Android SDK.
 * Wrap your screens with this Composable to apply the design system.
 */
@Composable
fun ApprovalTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApprovalColorScheme,
        typography = ApprovalTypography,
        shapes = ApprovalShapes,
        content = content
    )
}