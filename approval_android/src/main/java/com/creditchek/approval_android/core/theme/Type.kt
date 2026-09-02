package com.creditchek.approval_android.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ==========================================
// ✍️ Approval Typography System
// ==========================================
val ApprovalTypography = Typography(
    // 1. Big Screen Titles (e.g. Processing, Verification Screen Headers)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = ApprovalTextPrimary
    ),

    // 2. Section Headers (e.g. "Capture your face", "Enter BVN")
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        color = ApprovalTextPrimary
    ),

    // 3. Card/Step Titles (e.g. Welcome step titles, bottom sheet headers)
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = ApprovalTextPrimary
    ),

    // 4. Default Body Text (e.g. descriptions, helper texts)
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = ApprovalTextSecondary
    ),

    // 5. Buttons (e.g. "Continue", "Retry", "Grant Permission")
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),

    // 6. Badges, Pills & Hints (e.g. Network status, input labels)
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)