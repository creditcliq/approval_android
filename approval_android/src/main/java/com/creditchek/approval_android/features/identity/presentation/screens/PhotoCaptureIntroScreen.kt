package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.shared.components.DashedBorderCard
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.*
import com.creditchek.approval_android.features.identity.presentation.components.*
import com.creditchek.approval_android.R


@Composable
fun PhotoCaptureIntroScreen(
    onDismiss: () -> Unit,
    onProceed: () -> Unit,
    isLoading: Boolean = false
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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Center Illustration Guide Circle
            Image(
                painter = painterResource(id = R.drawable.photo_capture_guide),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Photo Capture",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApprovalTextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your face needs to be verified against your\ninformation. Please follow the guidelines below\nto ensure proper capture.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = ApprovalTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Dashed Card with Guidelines
            DashedBorderCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CaptureTipItem(
                        icon = Icons.Default.Lightbulb,
                        text = "Stay in a bright lit environment"
                    )
                    CaptureTipItem(
                        icon = Icons.Default.Face,
                        text = "Be on a white Background"
                    )
                    CaptureTipItem(
                        icon = Icons.Default.Visibility,
                        text = "Remove glasses, hats, hijabs, face masks or any other face coverings"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(28.dp))

            // 3. Action Buttons (Dismiss & Proceed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ApprovalButton(
                    text = "Dismiss",
                    onClick = onDismiss,
                    isSecondary = true,
                    modifier = Modifier.weight(1f)
                )

                ApprovalButton(
                    text = "Proceed",
                    onClick = onProceed,
                    modifier = Modifier.weight(1f),
                    isLoading = isLoading
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CaptureTipItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Circular Icon Badge
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFE6ECFC), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ApprovalBlue,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = text,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            color = ApprovalTextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PhotoCaptureIntroScreenPreview() {
    PhotoCaptureIntroScreen(onDismiss = {}, onProceed = {},)
}