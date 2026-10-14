package com.creditchek.approval_android.core.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.theme.ApprovalTextPrimary
import com.creditchek.approval_android.core.theme.LocalApprovalThemeConfig

@Composable
fun ApprovalHeader(
    title: String = "Approval Verification",
    isBack: Boolean = false,
    showLogo: Boolean = true,
    onClose: () -> Unit
) {
    val theme = LocalApprovalThemeConfig.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left-aligned Back or Close/Cancel Button
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = if (isBack) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                contentDescription = if (isBack) "Back" else "Close verification",
                tint = if (isBack) theme.primaryColor else ApprovalTextPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Center Title
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = ApprovalTextPrimary
        )

        // Top Right Brand Logo
        if (showLogo) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                BrandLogo(
                    logoSource = theme.logoUrl,
                    contentDescription = theme.businessName,
                    modifier = Modifier
                        .height(28.dp)
                        .widthIn(max = 80.dp)
                )
            }
        }
    }
}