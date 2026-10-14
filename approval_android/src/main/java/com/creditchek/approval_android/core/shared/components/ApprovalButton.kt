package com.creditchek.approval_android.core.shared.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.theme.ApprovalHint
import com.creditchek.approval_android.core.theme.LocalApprovalThemeConfig
import com.creditchek.approval_android.core.theme.PrimaryButtonShape

@Composable
fun ApprovalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true,
) {
    val localTheme = LocalApprovalThemeConfig.current
    val primaryColor = localTheme.primaryColor

    val containerColor = if (isSecondary) {
        primaryColor.copy(alpha = 0.12f)
    } else {
        primaryColor
    }

    val contentColor = if (isSecondary) primaryColor else Color.White

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = PrimaryButtonShape,
        colors = ButtonDefaults.buttonColors(

            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFFE2E8F0),
            disabledContentColor = ApprovalHint
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = if (isSecondary) localTheme.primaryColor else Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}