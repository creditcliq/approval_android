package com.creditchek.approval_android.core.shared.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.theme.*

@Composable
fun ApprovalTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Label with Red Asterisk
        val labelText = buildAnnotatedString {
            append(label)
            if (isRequired) {
                withStyle(SpanStyle(color = ApprovalDanger)) {
                    append(" *")
                }
            }
        }

        Text(
            text = labelText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ApprovalTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Clickable wrapper if readOnly (e.g. for Date of Birth picker)
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                readOnly = readOnly,
                placeholder = {
                    Text(
                        text = hint,
                        fontSize = 14.sp,
                        color = ApprovalHint
                    )
                },
                leadingIcon = leadingIcon?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ApprovalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                trailingIcon = trailingIcon?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = ApprovalBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                                )
                        )
                    }
                },
                shape = InputFieldShape,
                isError = isError,
                keyboardOptions = keyboardOptions,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ApprovalField,
                    unfocusedContainerColor = ApprovalField,
                    disabledContainerColor = ApprovalField,
                    errorContainerColor = ApprovalField,
                    focusedBorderColor = ApprovalBlue,
                    unfocusedBorderColor = ApprovalInputBorder,
                    errorBorderColor = ApprovalDanger,
                    cursorColor = ApprovalBlue,
                    focusedTextColor = ApprovalTextPrimary,
                    unfocusedTextColor = ApprovalTextPrimary
                )
            )

            // Transparent overlay for read-only fields to intercept clicks
            if (readOnly && onClick != null) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick
                        )
                )
            }
        }

        if (isError && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                fontSize = 12.sp,
                color = ApprovalDanger
            )
        }
    }
}