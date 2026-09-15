package com.creditchek.approval_android.features.liveliness.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.ApprovalBlue
import com.creditchek.approval_android.core.theme.ApprovalCanvas
import com.creditchek.approval_android.core.theme.ApprovalTextPrimary
import com.creditchek.approval_android.core.theme.ApprovalTextSecondary
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.presentation.components.ApprovalHeader
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun VerificationProcessingScreen(
    onDismiss: () -> Unit = {},
    verifyAction: (suspend () -> Boolean)? = null,
    onSuccess: () -> Unit = {},
    onFailure: () -> Unit = {}
) {
    // 1. Run Verification Coroutine on Mount
    LaunchedEffect(Unit) {
        val isPassed = try {
            if (verifyAction != null) {
                verifyAction()
            } else {
                // Default fallback delay (simulating processing)
                delay(2000.milliseconds)
                true
            }
        } catch (_: Exception) {
            false
        }

        if (isPassed) {
            onSuccess()
        } else {
            onFailure()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ApprovalCanvas)
    ) {
        // Top Header
        ApprovalHeader(
            title = "Approval Verification",
            onClose = onDismiss
        )

        // Center Loading & Text
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = ApprovalBlue,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Completing Verification",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = ApprovalTextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Please wait while we verify your captures.",
                fontSize = 14.sp,
                color = ApprovalTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        PoweredByCreditChek()
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun VerificationProcessingScreenPreview() {
    ApprovalTheme {
        VerificationProcessingScreen()
    }
}