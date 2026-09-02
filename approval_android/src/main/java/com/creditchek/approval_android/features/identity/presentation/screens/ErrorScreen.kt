package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.R
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.ApprovalBlue
import com.creditchek.approval_android.core.theme.ApprovalCanvas
import com.creditchek.approval_android.core.theme.ApprovalDanger
import com.creditchek.approval_android.core.theme.ApprovalTextSecondary
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.presentation.components.ApprovalHeader

// ==========================================
// 🚨 ApprovalErrorScreen
// 1:1 port of Flutter's ApprovalErrorScreen
// ==========================================

@Composable
fun ApprovalErrorScreen(
    title: String,
    message: String,
    actionLabel: String = "Close",
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ApprovalCanvas)
    ) {
        // 1. Top Header
        ApprovalHeader(
            title = "Approval Verification",
            onClose = onDismiss
        )

        // 2. Body
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo Header
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_logo),
                        contentDescription = "CreditChek Logo",
//                        modifier = Modifier.fillMaxSize()
                    )

            }

            Spacer(modifier = Modifier.weight(1f))

            // Error Mark (Concentric Red Ring Badge)
            ErrorMark()

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ApprovalDanger,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description Message
            Text(
                text = message,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = ApprovalTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 3. Action Buttons
            if (onRetry != null) {
                ApprovalButton(
                    text = "Try Again",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            ApprovalButton(
                text = actionLabel,
                onClick = onDismiss,
                isSecondary = onRetry != null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Replicates Flutter's _ErrorMark circular badge:
 * - Soft light-pink background (#FFE8EC)
 * - Inner red border ring (#FF2543)
 * - Close cross icon (#FF2543)
 */
@Composable
private fun ErrorMark() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFE8EC)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(width = 3.dp, color = ApprovalDanger, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Verification Error",
                tint = ApprovalDanger,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ==========================================
// 🛠️ Preset Error Builders
// ==========================================
object ApprovalErrorDefaults {
    const val INVALID_PUBLIC_KEY_TITLE = "Invalid public key"
    const val INVALID_PUBLIC_KEY_MESSAGE =
        "We could not verify this integration. Please check the public key and try again."

    const val NETWORK_ERROR_TITLE = "Connection Error"
    const val NETWORK_ERROR_MESSAGE =
        "Unable to connect to verification servers. Please check your internet connection and try again."
}

// ──────────────────────────────────────────
// Previews
// ──────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ApprovalErrorScreenPreview() {
    ApprovalTheme {
        ApprovalErrorScreen(
            title = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_TITLE,
            message = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE,
            actionLabel = "Close",
            onDismiss = {},
            onRetry = {}
        )
    }
}