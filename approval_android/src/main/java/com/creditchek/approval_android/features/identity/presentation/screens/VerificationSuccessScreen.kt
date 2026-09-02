package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.ApprovalCanvas
import com.creditchek.approval_android.core.theme.ApprovalSuccessDark
import com.creditchek.approval_android.core.theme.ApprovalSuccessRingDark
import com.creditchek.approval_android.core.theme.ApprovalSuccessRingLight
import com.creditchek.approval_android.core.theme.ApprovalTextPrimary
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.presentation.components.ApprovalHeader


@Composable
fun VerificationSuccessScreen(
    onDismiss: () -> Unit = {},
    onProceed: () -> Unit = {}
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(5f))

            // Concentric Green Success Badge
            SuccessMark()

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = "Verification Was Successful",
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ApprovalTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(6f))

            // Proceed Action Button
            ApprovalButton(
                text = "Proceed",
                onClick = onProceed,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Replicates Flutter's 3-layer concentric green circle badge
 */
@Composable
private fun SuccessMark() {
    // Outer Ring (Light Green #61E49D)
    Box(
        modifier = Modifier
            .size(86.dp)
            .clip(CircleShape)
            .background(ApprovalSuccessRingLight)
            .padding(9.dp),
        contentAlignment = Alignment.Center
    ) {
        // Middle Ring (Medium Green #0AB56D)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(ApprovalSuccessRingDark)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Inner Core (Deep Green #008550) + Check Icon
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(ApprovalSuccessDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Verification Successful",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationSuccessScreenPreview() {
    ApprovalTheme {
        VerificationSuccessScreen()
    }
}