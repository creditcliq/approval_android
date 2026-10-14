package com.creditchek.approval_android.core.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.session.DevelopmentDecoration
import com.creditchek.approval_android.core.theme.ApprovalCanvas
import com.creditchek.approval_android.core.theme.ApprovalTextPrimary
import com.creditchek.approval_android.core.theme.ApprovalTextSecondary
import com.creditchek.approval_android.core.theme.LocalApprovalThemeConfig


@Composable
fun ComingSoonScreen(
    moduleName: String,
    description: String? = null,
    isDevelopment: Boolean = false,
    nextButtonLabel: String = "Next Module",
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val theme = LocalApprovalThemeConfig.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ApprovalCanvas)
    ) {
        ApprovalHeader(
            title = "Approval Verification",
            onClose = onDismiss
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isDevelopment) {
                Spacer(modifier = Modifier.height(8.dp))
                DevelopmentDecoration(
                    modifier = Modifier.align(Alignment.End)
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }



            // Badge / Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(theme.primaryColor.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.primaryColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = moduleName,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = ApprovalTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description
                    ?: "This module is coming soon and will be available in an upcoming release.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = ApprovalTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            DashedBorderCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Module under construction",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = ApprovalTextPrimary
                    )
                    Text(
                        text = "Tap '$nextButtonLabel' below to continue the verification flow.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = ApprovalTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(28.dp))

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Fixed Bottom Section (Action Buttons & PoweredByCreditChek)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    text = nextButtonLabel,
                    onClick = onNext,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            PoweredByCreditChek()
        }
    }
}
