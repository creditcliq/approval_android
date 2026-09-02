package com.creditchek.approval_android.core.shared.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.theme.ApprovalBlue
import com.creditchek.approval_android.core.theme.ApprovalMutedButton
import com.creditchek.approval_android.core.theme.PrimaryButtonShape

@Composable
fun ApprovalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = PrimaryButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary) ApprovalMutedButton else ApprovalBlue,
            contentColor = if (isSecondary) ApprovalBlue else Color.White,
            disabledContainerColor = ApprovalBlue.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = if (isSecondary) ApprovalBlue else Color.White,
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