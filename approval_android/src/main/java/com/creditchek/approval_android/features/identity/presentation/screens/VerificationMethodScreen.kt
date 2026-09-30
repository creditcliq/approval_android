package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.session.DevelopmentDecoration
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.*

enum class VerificationDocument(val key: String, val title: String) {
    BVN("BVN", "Bank Verification Number (BVN)"),
    NIN("NIN", "National Identity Number (NIN)")
}

@Composable
fun VerificationMethodScreen(
    isDevelopment: Boolean = false,
    selectedMethod: String? = null,
    onBack: () -> Unit,
    onProceed: (String) -> Unit
) {
    var selectedDoc by remember {
        mutableStateOf<VerificationDocument?>(
            when (selectedMethod) {
                "NIN" -> VerificationDocument.NIN
                "BVN" -> VerificationDocument.BVN
                else -> null
            }
        )
    }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(ApprovalCanvas)
    ) {
        // 1. Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ApprovalBlue,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify your identity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApprovalTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Select your preferred means of verification",
                    fontSize = 13.sp,
                    color = ApprovalTextSecondary
                )
            }

            Spacer(modifier = Modifier.size(48.dp))
        }

        // 2. Main Content
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

            // Dropdown Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select document",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApprovalTextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ApprovalField, shape = InputFieldShape)
                            .border(
                                width = 1.dp,
                                color = if (expanded) ApprovalBlue else ApprovalInputBorder,
                                shape = InputFieldShape
                            )
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = selectedDoc?.title ?: "Select document",
                            fontSize = 14.sp,
                            color = if (selectedDoc != null) ApprovalTextPrimary else ApprovalHint,
                            fontWeight = if (selectedDoc != null) FontWeight.Medium else FontWeight.Normal
                        )

                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown icon",
                            tint = ApprovalTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color.White, shape = RoundedCornerShape(12.dp))
                    ) {
                        VerificationDocument.entries.forEach { doc ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = doc.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (doc == selectedDoc) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (doc == selectedDoc) ApprovalBlue else ApprovalTextPrimary
                                        )
                                        if (doc == selectedDoc) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = ApprovalBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedDoc = doc
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            ApprovalButton(
                text = "Proceed",
                onClick = { selectedDoc?.let { onProceed(it.key) } },
                enabled = selectedDoc != null
            )

            Spacer(modifier = Modifier.height(24.dp))
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
