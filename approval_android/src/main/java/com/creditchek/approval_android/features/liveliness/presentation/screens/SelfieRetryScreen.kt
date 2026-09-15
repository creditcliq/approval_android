package com.creditchek.approval_android.features.liveliness.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.presentation.components.ApprovalHeader
import com.creditchek.approval_android.features.liveliness.presentation.components.drawFaceOvalBorder

@Composable
fun SelfieRetryScreen(
    onDismiss: () -> Unit = {},
    onTryAgain: () -> Unit = {},
    reason: String = "No obstructions: remove hats,\nglasses and masks",
    isLoading: Boolean = false,
    errorMessage: String? = null
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

        // 2. Scrollable Content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Let’s Try That Again",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApprovalBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "We Need a Clearer Video Selfie",
                fontSize = 14.sp,
                color = ApprovalBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // =========================================================
            // 📸 IDEAL POSE GUIDE BOX
            // =========================================================
            Box(
                modifier = Modifier.size(width = 140.dp, height = 180.dp),
                contentAlignment = Alignment.Center
            ) {
                // ── 1. Image Background ──
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFE6ECFC)),
                    contentAlignment = Alignment.Center
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.ideal_pose),
                        contentDescription = "Ideal Pose Guide",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                }

                // ── 2. Oval Guide Stroke on top ──
                Canvas(
                    modifier = Modifier.size(width = 132.dp, height = 170.dp)
                ) {
                    drawFaceOvalBorder(
                        color = ApprovalBlue,
                        strokeWidth = 5f
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ideal Pose",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApprovalBlue
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reason / Guideline Text
            Text(
                text = reason,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApprovalBlue,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Error Message (if any)
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = ApprovalDanger,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action Button
            ApprovalButton(
                text = "Try Again",
                onClick = onTryAgain,
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelfieRetryScreenPreview() {
    ApprovalTheme {
        SelfieRetryScreen()
    }
}