package com.creditchek.approval_android.features.identity.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.session.AUserData
import com.creditchek.approval_android.core.session.DevelopmentDecoration
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.shared.components.ApprovalTextField
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NinCheckScreen(
    initialUserData: AUserData? = null,
    isLoading: Boolean = false,
    isDevelopment: Boolean = false,
    onBack: () -> Unit,
    onProceed: (firstName: String, lastName: String, dob: String, nin: String) -> Unit
) {
    val defaultNin = if (isDevelopment) "12345678901" else (initialUserData?.nin ?: "")
    val isFirstNameReadOnly = isLoading || !initialUserData?.firstName.isNullOrBlank()
    val isLastNameReadOnly = isLoading || !initialUserData?.lastName.isNullOrBlank()
    val isDobReadOnly = isLoading || !initialUserData?.dob.isNullOrBlank()
    val isNinReadOnly = isLoading || isDevelopment || !initialUserData?.nin.isNullOrBlank()

    var firstName by remember { mutableStateOf(initialUserData?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initialUserData?.lastName ?: "") }
    var dob by remember { mutableStateOf(initialUserData?.dob ?: "") }
    var nin by remember { mutableStateOf(defaultNin) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() - (20L * 365 * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(16.dp),
            colors = DatePickerDefaults.colors(
                containerColor = Color.White
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
                    Text("OK", color = ApprovalBlue, fontWeight = FontWeight.Bold)
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
                    containerColor = Color.White,
                    selectedDayContainerColor = ApprovalBlue,
                    todayDateBorderColor = ApprovalBlue
                )
            )
        }
    }

    /*
    // Confirmation Dialog with Price (₦70)
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ApprovalTextPrimary
                )
            },
            text = {
                Text(
                    text = "This NIN verification will incur a fee of ₦70. Do you want to proceed?",
                    fontSize = 14.sp,
                    color = ApprovalTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApprovalButton(
                        text = "Proceed",
                        onClick = {
                            showConfirmDialog = false
                            onProceed(firstName.trim(), lastName.trim(), dob.trim(), nin.trim())
                        }
                    )
                    ApprovalButton(
                        isSecondary = true,
                        text = "Cancel",
                        onClick = { showConfirmDialog = false }
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
    */

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
                    text = "NIN Check",
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

            Spacer(modifier = Modifier.size(48.dp))
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
                label = "NIN",
                value = nin,
                onValueChange = { input ->
                    if (input.length <= 11 && input.all { it.isDigit() }) {
                        nin = input
                    }
                },
                hint = "Enter NIN",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                readOnly = isNinReadOnly
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your details must match entered NIN",
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

            ApprovalButton(
                text = "Proceed",
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank() || dob.isBlank()) {
                        errorMessage = "Please fill all required fields"
                    } else if (nin.length != 11) {
                        errorMessage = "NIN must be exactly 11 digits"
                    } else {
                        errorMessage = null
                        // showConfirmDialog = true
                        onProceed(firstName.trim(), lastName.trim(), dob.trim(), nin.trim())
                    }
                },
                isLoading = isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))
            PoweredByCreditChek()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
