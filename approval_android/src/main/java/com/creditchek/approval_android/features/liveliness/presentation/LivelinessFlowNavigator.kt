package com.creditchek.approval_android.features.liveliness.presentation

import androidx.activity.compose.BackHandler
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
import com.creditchek.approval_android.core.session.loadTheme
import com.creditchek.approval_android.core.shared.components.IdentityToastHost
import com.creditchek.approval_android.core.shared.components.rememberToastState
import com.creditchek.approval_android.core.theme.DynamicThemeConfig
import com.creditchek.approval_android.core.session.SharedSessionContext
import com.creditchek.approval_android.core.shared.components.ApprovalErrorDefaults
import com.creditchek.approval_android.core.shared.components.ApprovalErrorScreen
import com.creditchek.approval_android.core.shared.components.VerificationSuccessScreen
import com.creditchek.approval_android.features.identity.data.IdentityRepository
import com.creditchek.approval_android.features.identity.data.models.Service
import com.creditchek.approval_android.features.identity.data.models.Status
import com.creditchek.approval_android.features.liveliness.data.models.FaceVerificationStep
import com.creditchek.approval_android.features.liveliness.presentation.screens.LivelinessCameraScreen
import com.creditchek.approval_android.features.liveliness.presentation.screens.PhotoCaptureIntroScreen
import com.creditchek.approval_android.features.liveliness.presentation.screens.SelfieRetryScreen
import com.creditchek.approval_android.features.liveliness.presentation.screens.VerificationProcessingScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LivelinessFlowNavigator(
    config: ApprovalConfig,
    context: SharedSessionContext,
    onContextUpdate: (SharedSessionContext) -> Unit,
    onThemeLoaded: (DynamicThemeConfig) -> Unit,
    onComplete: (SessionResult) -> Unit,
    onError: (String) -> Unit,
    onCancel: () -> Unit

) {


    val repository = remember(context.environment) {
        IdentityRepository(context.environment)
    }

    val coroutineScope = rememberCoroutineScope()
    val toastState = rememberToastState()

    var sessionContext by remember(context) {
        mutableStateOf(
            context
        )
    }


    var currentStep by remember {
        mutableStateOf<ApprovalStep>(ApprovalStep.Liveliness.PHOTO_INTRO)
    }


    var errorMessage by remember { mutableStateOf("") }
    var errorTitle by remember { mutableStateOf("") }
    var retryReason by remember { mutableStateOf("No obstructions: remove hats,\nglasses and masks") }
    var finalVerificationPassed by remember { mutableStateOf(false) }
    var isRetryFlow by remember { mutableStateOf(false) }
    var isIntroLoading by remember { mutableStateOf(false) }
    val isDevelopment = context.environment.isDevelopment


    BackHandler {
        when (currentStep) {
            ApprovalStep.Liveliness.ERROR,
            ApprovalStep.Liveliness.PHOTO_INTRO -> {
                onCancel()
            }

            ApprovalStep.Liveliness.LIVELINESS,
            ApprovalStep.Liveliness.RETRY -> {
                currentStep = ApprovalStep.Liveliness.PHOTO_INTRO
            }

            ApprovalStep.Liveliness.PROCESSING,
            ApprovalStep.Liveliness.SUCCESS -> {
                // Disabled during processing or success
            }

            else -> onCancel()
        }
    }

    when (currentStep) {
        // 1. Photo Guidelines Screen
        ApprovalStep.Liveliness.PHOTO_INTRO -> {
            PhotoCaptureIntroScreen(onDismiss = {
                onCancel()
            }, isLoading = isIntroLoading, isDevelopment = isDevelopment, onProceed = {
                coroutineScope.launch {
                    isIntroLoading = true
                    val healthResult = repository.checkLivelinessHealth()

                    repository.updateSession(
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
                            error.message ?: "Liveness service is currently not available"
                        )
                    }
                }
            })
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
                isDevelopment = isDevelopment,
                onDismiss = { currentStep = ApprovalStep.Liveliness.PHOTO_INTRO },
                onStepCapture = { capture ->
                    val shouldRestart =
                        isRetryFlow && capture.step == FaceVerificationStep.STILLNESS
                    repository.verifyChallenge(
                        accessToken = sessionContext.secretKey,
                        bvnImage = sessionContext.identityImage ?: sessionContext.bvnDetails?.photo
                        ?: sessionContext.ninDetails?.photo ?: "",
                        step = capture.step.apiName,
                        sessionId = sessionContext.sessionId,
                        frame = capture.jpegDataUrl,
                        restart = shouldRestart,
                        idType = sessionContext.selectedIdType,
                        environment = if (isDevelopment) "development" else "production"
                    )
                },
                onVerificationComplete = { overallPassed ->
                    finalVerificationPassed = overallPassed
                    currentStep = ApprovalStep.Liveliness.PROCESSING
                })
        }

        // 4. Processing & Polling Screen
        ApprovalStep.Liveliness.PROCESSING -> {
            VerificationProcessingScreen(
                isDevelopment = isDevelopment,
                onDismiss = { onCancel() },
                onSuccess = {
                    coroutineScope.launch {
                        repository.updateSession(
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
                        repository.updateSession(
                            sessionId = sessionContext.sessionId,
                            secretKey = sessionContext.secretKey,
                            status = Status.FAILED,
                            service = Service.LIVELINESS,
                        )
                    }
                    retryReason = "Biometric match was unclear. Please try again with good lighting"
                    currentStep = ApprovalStep.Liveliness.RETRY
                },
                verifyAction = {
                    delay(1500.milliseconds)

                    finalVerificationPassed
                })
        }

        // 5. Retry Screen
        ApprovalStep.Liveliness.RETRY -> {
            SelfieRetryScreen(
                isDevelopment = isDevelopment, onDismiss = {
                onCancel()
            }, onTryAgain = {
                isRetryFlow = true
                currentStep = ApprovalStep.Liveliness.LIVELINESS
            }, reason = retryReason
            )
        }

        // 6. Success Screen
        ApprovalStep.Liveliness.SUCCESS -> {
            VerificationSuccessScreen(
                isDevelopment = isDevelopment,
                onDismiss = { onComplete(SessionResult.Success(sessionContext.sessionId)) },
                onProceed = { onComplete(SessionResult.Success(sessionContext.sessionId)) })
        }

        // 7. Error Screen
        ApprovalStep.Liveliness.ERROR -> {
            val displayMessage =
                errorMessage.ifBlank { ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE }
            val title = errorTitle.ifBlank { ApprovalErrorDefaults.INVALID_PUBLIC_KEY_TITLE }
            ApprovalErrorScreen(
                title = title,
                message = displayMessage,
                isDevelopment = isDevelopment,
                onDismiss = {
                    onError(displayMessage)
                })
        }

        else -> {}
    }

    IdentityToastHost(toastState = toastState)
}