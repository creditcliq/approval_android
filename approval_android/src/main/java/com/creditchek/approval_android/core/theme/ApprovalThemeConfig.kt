package com.creditchek.approval_android.core.theme

import androidx.compose.ui.graphics.Color


data class DynamicThemeConfig(
    val primaryColor: Color = ApprovalBlue,
    val businessName: String = "CreditChek",
    val logoUrl: String? = null
)

fun parseHexColor(hexString: String?, defaultColor: Color = ApprovalBlue): Color {
    if (hexString.isNullOrBlank()) return defaultColor

    return try {
        val cleanHex = hexString.removePrefix("#")
        val colorInt = when (cleanHex.length) {
            6 -> "FF$cleanHex".toLong(16).toInt()
            8 -> cleanHex.toLong(16).toInt()
            else -> return defaultColor
        }
        Color(colorInt)
    } catch (_: Exception) {
        defaultColor
    }
}

