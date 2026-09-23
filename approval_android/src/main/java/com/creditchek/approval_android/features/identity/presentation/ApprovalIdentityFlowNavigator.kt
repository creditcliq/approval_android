package com.creditchek.approval_android.features.identity.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.creditchek.approval_android.ApprovalStep
import com.creditchek.approval_android.core.network.NetworkQualityEstimator
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalModule
import com.creditchek.approval_android.core.session.SessionResult
import com.creditchek.approval_android.core.shared.components.IdentityToastHost
import com.creditchek.approval_android.core.shared.components.rememberToastState
import com.creditchek.approval_android.features.identity.data.IdentityRepository
import com.creditchek.approval_android.features.identity.data.IdentitySessionContext
import com.creditchek.approval_android.features.identity.data.models.Service
import com.creditchek.approval_android.features.identity.data.models.Status
import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorDefaults
import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorScreen
import com.creditchek.approval_android.features.identity.presentation.screens.BvnCheckScreen
import com.creditchek.approval_android.features.identity.presentation.screens.SplashScreen
import com.creditchek.approval_android.features.identity.presentation.screens.VerificationSuccessScreen
import com.creditchek.approval_android.features.identity.presentation.screens.WelcomeScreen
import com.creditchek.approval_android.features.liveliness.data.models.FaceVerificationStep
import com.creditchek.approval_android.features.liveliness.presentation.screens.LivelinessCameraScreen
import com.creditchek.approval_android.features.liveliness.presentation.screens.PhotoCaptureIntroScreen
import com.creditchek.approval_android.features.liveliness.presentation.screens.SelfieRetryScreen
import com.creditchek.approval_android.features.liveliness.presentation.screens.VerificationProcessingScreen
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.creditchek.approval_android.core.shared.components.ApprovalButton
import com.creditchek.approval_android.core.theme.ApprovalTextPrimary
import com.creditchek.approval_android.core.theme.ApprovalTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ApprovalFlowNavigator(
    config: ApprovalConfig,
    onFinishWithResult: (SessionResult) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(config.environment) {
        IdentityRepository(config.environment)
    }
    val toastState = rememberToastState()

    var sessionContext by remember {
        mutableStateOf(
            IdentitySessionContext(
                publicKey = config.publicKey,
                sessionId = config.sessionId ?: ""
            )
        )
    }

    var currentStep by remember {
        mutableStateOf<ApprovalStep>(
            if (ApprovalModule.IDENTITY in config.modules) {
                ApprovalStep.Identity.SPLASH
            } else {
                ApprovalStep.Liveliness.SPLASH
            }
        )
    }

    var isWelcomeLoading by remember { mutableStateOf(false) }
    var isBvnLoading by remember { mutableStateOf(false) }
    var isIntroLoading by remember { mutableStateOf(false) }
    var retryReason by remember { mutableStateOf("No obstructions: remove hats,\nglasses and masks") }
    var finalVerificationPassed by remember { mutableStateOf(false) }
    var isRetryFlow by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var errorTitle by remember { mutableStateOf("") }
    var mismatchDialogMessage by remember { mutableStateOf<String?>(null) }
    var isFirstCallSuccess by remember { mutableStateOf(false) }

    BackHandler {
        when (currentStep) {
            // ── Identity Back Navigation ──
            ApprovalStep.Identity.SPLASH,
            ApprovalStep.Identity.WELCOME,
            ApprovalStep.Identity.ERROR -> {
                onFinishWithResult(SessionResult.Cancelled)
            }

            ApprovalStep.Identity.BVN_CHECK -> {
                currentStep = ApprovalStep.Identity.WELCOME
            }

            ApprovalStep.Identity.LIVELINESS -> {
                currentStep = ApprovalStep.Identity.BVN_CHECK
            }

            ApprovalStep.Identity.SUCCESS -> {
                // Disabled during success
            }

            // ── Liveliness Back Navigation ──
            ApprovalStep.Liveliness.SPLASH,
            ApprovalStep.Liveliness.ERROR -> {
                onFinishWithResult(SessionResult.Cancelled)
            }

            ApprovalStep.Liveliness.PHOTO_INTRO -> {
                if (ApprovalModule.IDENTITY in config.modules) {
                    currentStep = ApprovalStep.Identity.BVN_CHECK
                } else {
                    onFinishWithResult(SessionResult.Cancelled)
                }
            }

            ApprovalStep.Liveliness.LIVELINESS,
            ApprovalStep.Liveliness.RETRY -> {
                currentStep = ApprovalStep.Liveliness.PHOTO_INTRO
            }

            ApprovalStep.Liveliness.PROCESSING,
            ApprovalStep.Liveliness.SUCCESS -> {
                // Disabled during processing or success
            }
        }
    }

    when (val step = currentStep) {
        // =======================================================
        // 🆔 IDENTITY FLOW
        // =======================================================
        is ApprovalStep.Identity -> {
            when (step) {
                // 1. Splash Screen (Validates Public Key)
                ApprovalStep.Identity.SPLASH -> {
                    SplashScreen(
                        validateAction = {


                            val result = repository.validatePublicKey(config.publicKey)
                            result.map { keyData ->
                                sessionContext = sessionContext.copy(
                                    secretKey = keyData.app.liveSecretKey,
                                    businessName = keyData.app.appName
                                )
                            }
                        },
                        onValidationSuccess = {
                            coroutineScope.launch {
                                val createResult = repository.getWidgetSession(
                                    secretKey = config.publicKey,
                                    sessionId = config.sessionId
                                )

                                createResult.onSuccess { data ->

                                    currentStep =
                                        if (data.services.bvn.status != Status.COMPLETED)
                                            ApprovalStep.Identity.WELCOME
                                        else
                                            ApprovalStep.Liveliness.PHOTO_INTRO
                                }
                                createResult.onFailure { error ->
                                    val displayMsg = error.message?.takeIf { it.isNotBlank() }
                                        ?: "Unknown error occurred"
                                    errorMessage = displayMsg
                                    errorTitle = "Invalid Widget Session"
                                    currentStep = ApprovalStep.Identity.ERROR
                                }
                            }
                        },
                        onValidationError = { msg ->
                            currentStep = ApprovalStep.Identity.ERROR
                            errorMessage = msg
                        }
                    )
                }

                // 2. Welcome Screen
                ApprovalStep.Identity.WELCOME -> {
                    WelcomeScreen(
                        isLoading = isWelcomeLoading,
                        onStartVerification = {
                            coroutineScope.launch {
                                isWelcomeLoading = true
//                                val clientSessionId = createSessionId()

                                val createResult = repository.getWidgetSession(
                                    secretKey = config.publicKey,
                                    sessionId = config.sessionId
                                )

                                createResult.onSuccess { sessionData ->
                                    sessionContext =
                                        sessionContext.copy(sessionId = config.sessionId)
                                    isWelcomeLoading = false
                                    currentStep = ApprovalStep.Identity.BVN_CHECK
                                }.onFailure { error ->
                                    isWelcomeLoading = false
                                    val displayMsg = error.message?.takeIf { it.isNotBlank() }
                                        ?: "Unknown error occurred"
                                    toastState.show(displayMsg)
                                }
                            }
                        },
                        onDismiss = { onFinishWithResult(SessionResult.Cancelled) }
                    )
                }

                // 3. BVN Form Screen
                ApprovalStep.Identity.BVN_CHECK -> {
                    BvnCheckScreen(
                        isLoading = isBvnLoading,
                        initialUserData = config.userData,
                        onBack = { currentStep = ApprovalStep.Identity.WELCOME },
                        onProceed = { firstName, lastName, dob, bvn ->
                            coroutineScope.launch {
                                isBvnLoading = true


                                val bvnResult =
                                    if (isFirstCallSuccess) repository.getSessionBvnData(
                                        sessionId = sessionContext.sessionId,
                                        secretKey = sessionContext.secretKey
                                    ) else repository.verifyBvn(
                                        secretKey = sessionContext.secretKey,
                                        bvn = bvn,
                                    )

                                bvnResult.onSuccess { details ->

                                    isFirstCallSuccess = true

                                    val nameMatches = isNameMatching(
                                        inputFirst = firstName,
                                        inputLast = lastName,
                                        bvnFirst = details.firstName,
                                        bvnLast = details.lastName,
                                        bvnMiddle = details.middleName
                                    )
                                    val dateMatches = isDateMatching(dob, details.dateOfBirth)

                                    if (nameMatches && dateMatches) {
                                        sessionContext = sessionContext.copy(bvnDetails = details)

                                        repository.updateSessionWithBvn(
                                            sessionId = sessionContext.sessionId,
                                            secretKey = sessionContext.secretKey,
                                            status = Status.COMPLETED,
                                            service = Service.BVN,
                                            bvn = bvn
                                        )

                                        isBvnLoading = false

                                        currentStep =
                                            if (config.modules.contains(ApprovalModule.LIVELINESS)) {
                                                ApprovalStep.Liveliness.PHOTO_INTRO
                                            } else {
                                                ApprovalStep.Identity.SUCCESS
                                            }
                                    } else {
                                        repository.updateSessionWithBvn(
                                            sessionId = sessionContext.sessionId,
                                            secretKey = sessionContext.secretKey,
                                            status = Status.PENDING,
                                            service = Service.BVN,
                                            bvn = bvn
                                        )

                                        isBvnLoading = false
                                        mismatchDialogMessage = when {
                                            !nameMatches && !dateMatches ->
                                                "The name and date of birth entered do not match the records registered with this BVN. Please verify and try again."

                                            !nameMatches ->
                                                "The name entered does not match the records registered with this BVN. Please check the spelling of your first and last names and try again."

                                            else ->
                                                "The date of birth entered does not match the records registered with this BVN. Please check your date of birth and try again."
                                        }

                                    }

                                }.onFailure { error ->
                                    repository.updateSessionWithBvn(
                                        sessionId = sessionContext.sessionId,
                                        secretKey = sessionContext.secretKey,
                                        status = Status.FAILED,
                                        service = Service.BVN,
                                        bvn = bvn
                                    )
                                    isBvnLoading = false

                                    toastState.show(
                                        error.message
                                            ?: "BVN verification failed. Please check and try again"
                                    )
                                }
                            }
                        }
                    )
                }

                // 4. Identity Liveliness step
                ApprovalStep.Identity.LIVELINESS -> {
                    currentStep = ApprovalStep.Liveliness.PHOTO_INTRO
                }

                // 5. Success Screen
                ApprovalStep.Identity.SUCCESS -> {
                    VerificationSuccessScreen(
                        onDismiss = { onFinishWithResult(SessionResult.Success(sessionContext.sessionId)) },
                        onProceed = { onFinishWithResult(SessionResult.Success(sessionContext.sessionId)) }
                    )
                }

                // 6. Error Screen
                ApprovalStep.Identity.ERROR -> {
                    val displayMessage =
                        errorMessage.ifBlank { ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE }
                    val title =
                        errorTitle.ifBlank { ApprovalErrorDefaults.INVALID_PUBLIC_KEY_TITLE }
                    ApprovalErrorScreen(
                        title = title,
                        message = displayMessage,
                        onDismiss = {
                            onFinishWithResult(
                                SessionResult.Error(
                                    code = "ERROR",
                                    message = displayMessage
                                )
                            )
                        }
                    )
                }
            }
        }

        // =======================================================
        // 📸 LIVELINESS FLOW
        // =======================================================
        is ApprovalStep.Liveliness -> {
            when (step) {
                // 1. Standalone Liveliness Splash Screen
                ApprovalStep.Liveliness.SPLASH -> {
                    SplashScreen(
                        validateAction = {
                            val result = repository.validatePublicKey(config.publicKey)
                            result.map { keyData ->
                                sessionContext = sessionContext.copy(
                                    secretKey = keyData.app.liveSecretKey,
                                    businessName = keyData.app.appName,
                                    sessionId = config.sessionId
                                )
                            }
                        },
                        onValidationSuccess = {
                            coroutineScope.launch {
                                try {
//
                                    val result = repository.getSessionBvnData(
                                        sessionId = sessionContext.sessionId,
                                        secretKey = sessionContext.secretKey
                                    )
                                    result.onSuccess { details ->
                                        sessionContext = sessionContext.copy(bvnDetails = details)

//                                        currentStep =
//                                            if (sessionContext.bvnDetails?.photo != null) {
//
//                                                ApprovalStep.Liveliness.PHOTO_INTRO
//                                            } else {
//                                                ApprovalStep.Identity.BVN_CHECK
//                                            }

                                    }
                                    result.onFailure { error ->
                                        val displayMsg = error.message?.takeIf { it.isNotBlank() }
                                            ?: "Unknown error occurred"
                                        errorMessage = displayMsg
                                        errorTitle = "Invalid Widget Session"
                                        currentStep = ApprovalStep.Liveliness.ERROR
                                    }
                                } catch (_: Exception) {
                                    // Error handled during verification if session is invalid
                                    currentStep = ApprovalStep.Liveliness.ERROR
                                }
                            }
                            currentStep = ApprovalStep.Liveliness.PHOTO_INTRO
                        },
                        onValidationError = { msg ->
                            currentStep = ApprovalStep.Liveliness.ERROR
                            errorMessage = msg
                        }
                    )
                }

                // 2. Photo Guidelines Screen
                ApprovalStep.Liveliness.PHOTO_INTRO -> {
                    PhotoCaptureIntroScreen(
                        onDismiss = {
                            onFinishWithResult(SessionResult.Cancelled)
                        },
                        isLoading = isIntroLoading,
                        onProceed = {
                            coroutineScope.launch {
                                isIntroLoading = true
                                val healthResult = repository.checkLivelinessHealth()

                                repository.updateSessionWithBvn(
                                    sessionId = sessionContext.sessionId,
                                    secretKey = sessionContext.secretKey,
                                    status = Status.PENDING,
                                    service = Service.LIVELINESS,
                                )

                                healthResult.onSuccess { isAvailable ->
                                    isIntroLoading = false
                                    if (isAvailable) {
                                        currentStep = ApprovalStep.Liveliness.LIVELINESS
                                    } else {
                                        toastState.show("Liveness service is currently not available")
                                    }
                                }.onFailure { error ->
                                    isIntroLoading = false
                                    toastState.show(
                                        error.message
                                            ?: "Liveness service is currently not available"
                                    )
                                }
                            }
                        }
                    )
                }

                // 3. CameraX + ML Kit Liveness Screen
                ApprovalStep.Liveliness.LIVELINESS -> {
                    val context = LocalContext.current
                    val networkEstimator = remember { NetworkQualityEstimator(context) }
                    val networkQuality by networkEstimator.quality.collectAsState()

                    DisposableEffect(Unit) {
                        networkEstimator.startMonitoring()
                        onDispose { networkEstimator.stopMonitoring() }
                    }

                    LivelinessCameraScreen(
                        networkQuality = networkQuality,
                        onDismiss = { currentStep = ApprovalStep.Liveliness.PHOTO_INTRO },
                        onStepCapture = { capture ->
                            val shouldRestart =
                                isRetryFlow && capture.step == FaceVerificationStep.STILLNESS
                            repository.verifyChallenge(
                                accessToken = sessionContext.secretKey,
                                bvnImage = sessionContext.bvnDetails?.photo ?: "",
                                step = capture.step.apiName,
                                sessionId = sessionContext.sessionId,
                                frame = capture.jpegDataUrl,
                                restart = shouldRestart,
                            )
                        },
                        onVerificationComplete = { overallPassed ->
                            finalVerificationPassed = overallPassed
                            currentStep = ApprovalStep.Liveliness.PROCESSING
                        }
                    )
                }

                // 4. Processing & Polling Screen
                ApprovalStep.Liveliness.PROCESSING -> {
                    VerificationProcessingScreen(
                        onDismiss = { onFinishWithResult(SessionResult.Cancelled) },
                        onSuccess = {
                            coroutineScope.launch {
                                repository.updateSessionWithBvn(
                                    sessionId = sessionContext.sessionId,
                                    secretKey = sessionContext.secretKey,
                                    status = Status.COMPLETED,
                                    service = Service.LIVELINESS,
                                )
                            }
                            currentStep = ApprovalStep.Liveliness.SUCCESS
                        },
                        onFailure = {
                            coroutineScope.launch {
                                repository.updateSessionWithBvn(
                                    sessionId = sessionContext.sessionId,
                                    secretKey = sessionContext.secretKey,
                                    status = Status.FAILED,
                                    service = Service.LIVELINESS,
                                )
                            }
                            retryReason =
                                "Biometric match was unclear. Please try again with good lighting"
                            currentStep = ApprovalStep.Liveliness.RETRY
                        },
                        verifyAction = {
                            delay(1500.milliseconds)

                            finalVerificationPassed
                        }
                    )
                }

                // 5. Retry Screen
                ApprovalStep.Liveliness.RETRY -> {
                    SelfieRetryScreen(
                        onDismiss = {

                            onFinishWithResult(SessionResult.Cancelled)
                        },
                        onTryAgain = {
                            isRetryFlow = true
                            currentStep = ApprovalStep.Liveliness.LIVELINESS
                        },
                        reason = retryReason
                    )
                }

                // 6. Success Screen
                ApprovalStep.Liveliness.SUCCESS -> {
                    VerificationSuccessScreen(
                        onDismiss = { onFinishWithResult(SessionResult.Success(sessionContext.sessionId)) },
                        onProceed = { onFinishWithResult(SessionResult.Success(sessionContext.sessionId)) }
                    )
                }

                // 7. Error Screen
                ApprovalStep.Liveliness.ERROR -> {
                    val displayMessage =
                        errorMessage.ifBlank { ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE }
                    val title =
                        errorTitle.ifBlank { ApprovalErrorDefaults.INVALID_PUBLIC_KEY_TITLE }
                    ApprovalErrorScreen(
                        title = title,
                        message = displayMessage,
                        onDismiss = {
                            onFinishWithResult(
                                SessionResult.Error(
                                    code = "ERROR",
                                    message = displayMessage
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    // ── Mismatch Alert Dialog ──
    if (mismatchDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { mismatchDialogMessage = null },
            title = {
                Text(
                    text = "Details Mismatch",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = ApprovalTextPrimary
                )
            },
            text = {
                Text(
                    text = mismatchDialogMessage ?: "",
                    fontSize = 14.sp,
                    color = ApprovalTextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ApprovalButton(
                        text = "Try Again",
                        onClick = { mismatchDialogMessage = null }
                    )
                    ApprovalButton(
                        isSecondary = true,
                        text = "Dismiss",
                        onClick = {
                            onFinishWithResult(
                                SessionResult.Error(
                                    message = mismatchDialogMessage ?: "BVN details mismatch",
                                    code = "400 Bad request"
                                )
                            )
                        }
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    IdentityToastHost(toastState = toastState)
}

private fun isNameMatching(
    inputFirst: String,
    inputLast: String,
    bvnFirst: String?,
    bvnLast: String?,
    bvnMiddle: String?
): Boolean {
    val cleanInputFirst = inputFirst.trim().lowercase()
    val cleanInputLast = inputLast.trim().lowercase()
    val cleanBvnFirst = bvnFirst?.trim()?.lowercase().orEmpty()
    val cleanBvnLast = bvnLast?.trim()?.lowercase().orEmpty()
    val cleanBvnMiddle = bvnMiddle?.trim()?.lowercase().orEmpty()

//    val allInput = "$cleanInputFirst $cleanInputLast"
    val allBvn = "$cleanBvnFirst $cleanBvnMiddle $cleanBvnLast"

    // 1. Direct or swapped match
    val directMatch = (cleanInputFirst == cleanBvnFirst && cleanInputLast == cleanBvnLast) ||
            (cleanInputFirst == cleanBvnLast && cleanInputLast == cleanBvnFirst)
    if (directMatch) return true

    // 2. Cross-match: both input names are in BVN and both BVN names are in input
    val inputInBvn =
        (allBvn.contains(cleanInputFirst) || cleanBvnFirst.contains(cleanInputFirst)) &&
                (allBvn.contains(cleanInputLast) || cleanBvnLast.contains(cleanInputLast))

    return inputInBvn
}

private fun isDateMatching(inputDob: String, bvnDob: String?): Boolean {
    val cleanInput = inputDob.trim()
    val cleanBvn = bvnDob?.trim().orEmpty()

    if (cleanInput.isEmpty() || cleanBvn.isEmpty()) return true

    val formats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
    )

    fun parseDate(d: String): Date? {
        for (f in formats) {
            try {
                f.isLenient = false
                return f.parse(d)
            } catch (_: Exception) {
            }
        }
        return null
    }

    val d1 = parseDate(cleanInput)
    val d2 = parseDate(cleanBvn)

    return if (d1 != null && d2 != null) {
        val cal1 = Calendar.getInstance().apply { time = d1 }
        val cal2 = Calendar.getInstance().apply { time = d2 }
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    } else {
        val digits1 = cleanInput.filter { it.isDigit() }
        val digits2 = cleanBvn.filter { it.isDigit() }
        digits1.isNotEmpty() && digits1 == digits2 ||
                cleanInput.replace("/", "-").equals(cleanBvn.replace("/", "-"), ignoreCase = true)
    }
}
//
//private const val SESSION_ID_LENGTH = 16
//private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
//private val secureRandom = SecureRandom()
//
//fun createSessionId(): String {
//    return (1..SESSION_ID_LENGTH)
//        .map { CHARACTERS[secureRandom.nextInt(CHARACTERS.length)] }
//        .joinToString("")
//}
