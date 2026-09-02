package com.creditchek.approval_android.core.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==========================================
// 📐 Standard Component Shapes
// ==========================================
val ApprovalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),      // Text fields & Input boxes
    medium = RoundedCornerShape(14.dp),     // Toasts & Cards
    large = RoundedCornerShape(30.dp),      // Status pills & Badges
    extraLarge = RoundedCornerShape(100.dp) // Full capsule buttons
)

// Convenient top-level shortcuts
val InputFieldShape = RoundedCornerShape(12.dp)
val ToastCardShape = RoundedCornerShape(14.dp)
val StatusPillShape = RoundedCornerShape(30.dp)
val PrimaryButtonShape = RoundedCornerShape(12.dp)

// ==========================================
// 📷 Custom Oval Face Frame Shape
// Clips the Camera View into a smooth Oval for Face Verification
// ==========================================
val FaceOvalShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height

    moveTo(w * 0.5f, 0f)
    // Top-right curve
    cubicTo(
        w * 0.85f, 0f,
        w, h * 0.20f,
        w, h * 0.42f
    )
    // Bottom-right curve tapering down to chin
    cubicTo(
        w, h * 0.72f,
        w * 0.78f, h,
        w * 0.5f, h
    )
    // Bottom-left curve tapering up from chin
    cubicTo(
        w * 0.22f, h,
        0f, h * 0.72f,
        0f, h * 0.42f
    )
    // Top-left curve returning to top center
    cubicTo(
        0f, h * 0.20f,
        w * 0.15f, 0f,
        w * 0.5f, 0f
    )
    close()
}
