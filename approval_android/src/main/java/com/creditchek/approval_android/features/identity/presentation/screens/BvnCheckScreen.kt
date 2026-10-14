package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.session.AUserData
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.shared.components.ApprovalTextField
import com.creditchek.approval_android.core.shared.components.BrandLogo
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.*
import java.text.SimpleDateFormat
import java.util.*

import com.creditchek.approval_android.core.session.DevelopmentDecoration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BvnCheckScreen(
    initialUserData: AUserData? = null,
    isLoading: Boolean = false,
    isDevelopment: Boolean = false,
    onBack: () -> Unit,
    onProceed: (firstName: String, lastName: String, dob: String, bvn: String) -> Unit
) {
    val defaultBvn = if (isDevelopment) "12345678901" else (initialUserData?.bvn ?: "")
    val isFirstNameReadOnly = isLoading || !initialUserData?.firstName.isNullOrBlank()
    val isLastNameReadOnly = isLoading || !initialUserData?.lastName.isNullOrBlank()
    val isDobReadOnly = isLoading || !initialUserData?.dob.isNullOrBlank()
    val isBvnReadOnly = isLoading || isDevelopment || !initialUserData?.bvn.isNullOrBlank()

    var firstName by remember { mutableStateOf(initialUserData?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialUserData?.lastName ?: "") }
    var dob by remember { mutableStateOf(initialUserData?.dob ?: "") }
    var bvn by remember { mutableStateOf(defaultBvn) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }

    val theme = LocalApprovalThemeConfig.current

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() - (20L * 365 * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = DatePickerDefaults.colors(
                containerColor = androidx.compose.ui.graphics.Color.White,
                currentYearContentColor = theme.primaryColor,
                dayContentColor = theme.primaryColor,
                weekdayContentColor = theme.primaryColor
            ),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            dob = formatter.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = theme.primaryColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = ApprovalTextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    selectedDayContainerColor = theme.primaryColor,
                    todayDateBorderColor = theme.primaryColor,
                )
            )
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = theme.primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BVN Check",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ApprovalTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Enter your details",
                    fontSize = 13.sp,
                    color = ApprovalTextSecondary
                )
            }

            BrandLogo(
                logoSource = theme.logoUrl,
                contentDescription = theme.businessName,
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(max = 80.dp)
            )
        }

        // 2. Form Content
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
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            ApprovalTextField(
                label = "First Name",
                value = firstName,
                onValueChange = { firstName = it },
                hint = "Enter First Name",
                leadingIcon = Icons.Default.Person,
                readOnly = isFirstNameReadOnly
            )

            Spacer(modifier = Modifier.height(16.dp))

            ApprovalTextField(
                label = "Last Name",
                value = lastName,
                onValueChange = { lastName = it },
                hint = "Enter Last Name",
                leadingIcon = Icons.Default.Person,
                readOnly = isLastNameReadOnly
            )

            Spacer(modifier = Modifier.height(16.dp))

            ApprovalTextField(
                label = "Date of Birth",
                value = dob,
                onValueChange = {},
                hint = "dd/mm/yyyy",
                readOnly = true,
                trailingIcon = Icons.Default.DateRange,
                onClick = { if (!isDobReadOnly) showDatePicker = true },
            )

            Spacer(modifier = Modifier.height(16.dp))

            ApprovalTextField(
                label = "BVN",
                value = bvn,
                onValueChange = { input ->
                    if (input.length <= 11 && input.all { it.isDigit() }) {
                        bvn = input
                    }
                },
                hint = "Enter BVN",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                readOnly = isBvnReadOnly
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Warning Text
            Text(
                text = "Your details must match entered BVN",
                fontSize = 13.sp,
                color = ApprovalDanger,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = ApprovalDanger,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Proceed Button
            ApprovalButton(
                text = "Proceed",
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || dob.isBlank()) {
                        errorMessage = "Please fill all required fields"
                    } else if (bvn.length != 11) {
                        errorMessage = "BVN must be exactly 11 digits"
                    } else {
                        errorMessage = null
                        // showConfirmDialog = true
                        onProceed(firstName.trim(), lastName.trim(), dob.trim(), bvn.trim())
                    }
                },
                isLoading = isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Fixed Bottom Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PoweredByCreditChek()
        }
    }
}



@Preview(showBackground = true)
@Composable
fun BvnCheckScreenPreview(){
    BvnCheckScreen(
        onBack = {},
        onProceed = { _, _, _, _ -> }
    )
}
