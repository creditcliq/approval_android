package com.creditchek.approval_android.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


val LocalApprovalThemeConfig = staticCompositionLocalOf { DynamicThemeConfig() }


// Light Color Scheme for the SDK


/**
 * Main Theme wrapper for the Approval Android SDK.
 * Wrap your screens with this Composable to apply the design system.
 */
@Composable
fun ApprovalTheme(
    themeConfig: DynamicThemeConfig = DynamicThemeConfig(),
    content: @Composable () -> Unit
) {


    val dynamicColorScheme = lightColorScheme(
        primary = themeConfig.primaryColor,
        onPrimary = Color.White,
        primaryContainer = themeConfig.primaryColor.copy(alpha = 0.15f),
        onPrimaryContainer = themeConfig.primaryColor,
        background = ApprovalCanvas,
        onBackground = ApprovalTextPrimary,
        surface = Color.White,
        onSurface = ApprovalTextPrimary,
        error = ApprovalDanger,
        onError = Color.White
    )

    CompositionLocalProvider(LocalApprovalThemeConfig provides themeConfig) {

        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = ApprovalTypography,
            shapes = ApprovalShapes,
            content = content
        )
    }

}