package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
fun WelcomeScreen(
    isLoading: Boolean = false,
    onStartVerification: () -> Unit,
    onDismiss: () -> Unit,
    onPrivacyClick: () -> Unit = {},
    onTermsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ApprovalCanvas)
    ) {
        ApprovalHeader(
            title = "Approval Verification", onClose = onDismiss
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Let’s get you verified",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = ApprovalTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "It takes less than 2 minutes. Use your device to\ntake photos or record:",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = ApprovalTextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            DashedBorderCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    WelcomeTipItem(
                        title = "Gov ID Ready:", body = "Have your official ID handy"
                    )
                    WelcomeTipItem(
                        title = "Good Lighting:", body = "Ensure your area is well-lit"
                    )
                    WelcomeTipItem(
                        title = "Clear Selfie:", body = "No glasses or hat for your photo"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(28.dp))

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
                    text = "Let’s Go",
                    onClick = onStartVerification,
                    isLoading = isLoading,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val legalText = buildAnnotatedString {
                append("Your privacy and security are our top priorities. By proceeding, you agree to our ")
                withStyle(SpanStyle(color = ApprovalBlue, fontWeight = FontWeight.SemiBold)) {
                    append("Privacy Policy")
                }
                append(" and ")
                withStyle(SpanStyle(color = ApprovalBlue, fontWeight = FontWeight.SemiBold)) {
                    append("Terms & Conditions")
                }
                append(".")
            }

            Text(
                text = legalText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                color = ApprovalTextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            //TODO() update this to the icon
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WelcomeTipItem(
    title: String, body: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = ApprovalSuccess,
            modifier = Modifier.size(22.dp)
        )

        val tipText = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = ApprovalTextPrimary)) {
                append("$title ")
            }
            withStyle(SpanStyle(color = ApprovalTextSecondary)) {
                append(body)
            }
        }

        Text(
            text = tipText, fontSize = 14.sp, lineHeight = 18.sp
        )
    }
}


@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    WelcomeScreen(onStartVerification = {}, isLoading = false, onDismiss = {}, onPrivacyClick = {})
}











