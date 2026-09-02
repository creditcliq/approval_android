package com.creditchek.approval_android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.SessionResult
import com.creditchek.approval_android.core.shared.components.IdentityToastHost
import com.creditchek.approval_android.core.shared.components.rememberToastState
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.data.IdentityRepository
import com.creditchek.approval_android.features.identity.data.IdentitySessionContext
import com.creditchek.approval_android.features.identity.data.models.FaceChallengeCapture
import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorDefaults
import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorScreen
import com.creditchek.approval_android.features.identity.presentation.screens.BvnCheckScreen
import com.creditchek.approval_android.features.identity.presentation.screens.LivelinessCameraScreen
import com.creditchek.approval_android.features.identity.presentation.screens.PhotoCaptureIntroScreen
import com.creditchek.approval_android.features.identity.presentation.screens.SelfieRetryScreen
import com.creditchek.approval_android.features.identity.presentation.screens.SplashScreen
import com.creditchek.approval_android.features.identity.presentation.screens.VerificationProcessingScreen
import com.creditchek.approval_android.features.identity.presentation.screens.VerificationSuccessScreen
import com.creditchek.approval_android.features.identity.presentation.screens.WelcomeScreen
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.security.SecureRandom

enum class ApprovalStep {
    SPLASH, ERROR, WELCOME, BVN_CHECK, PHOTO_INTRO, LIVELINESS, PROCESSING, SUCCESS, RETRY
}

class ApprovalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read Serializable config across all Android SDK versions
        val config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CONFIG, ApprovalConfig::class.java)
        } else {
            @Suppress("DEPRECATION") intent.getSerializableExtra(EXTRA_CONFIG) as? ApprovalConfig
        } ?: ApprovalConfig(publicKey = "")

        setContent {
            ApprovalTheme {
                ApprovalFlowNavigator(
                    config = config, onFinishWithResult = { result ->
                        finishWithResult(result)
                    })
            }
        }
    }

    private fun finishWithResult(result: SessionResult) {
        val data = Intent().apply {
            putExtra(EXTRA_RESULT, result)
        }
        val resultCode = when (result) {
            is SessionResult.Success -> Activity.RESULT_OK
            is SessionResult.Cancelled -> Activity.RESULT_CANCELED
            is SessionResult.Error -> Activity.RESULT_FIRST_USER
        }
        setResult(resultCode, data)
        finish()
    }

    companion object {
        const val EXTRA_CONFIG = "extra_approval_config"
        const val EXTRA_RESULT = "extra_approval_result"
    }
}

@Composable
fun ApprovalFlowNavigator(
    config: ApprovalConfig, onFinishWithResult: (SessionResult) -> Unit
) {


    val coroutineScope = rememberCoroutineScope()
    val repository = remember(config.environment) {
        IdentityRepository(config.environment)
    }
    val toastState = rememberToastState()

    var sessionContext by remember {
        mutableStateOf(IdentitySessionContext(publicKey = config.publicKey))
    }
    var currentStep by remember { mutableStateOf(ApprovalStep.SPLASH) }
    var isWelcomeLoading by remember { mutableStateOf(false) }
    var isBvnLoading by remember { mutableStateOf(false) }
    var isIntroLoading by remember { mutableStateOf(false) }
    var retryReason by remember { mutableStateOf("No obstructions: remove hats,\nglasses and masks") }
//    var lastCaptures by remember { mutableStateOf<List<FaceChallengeCapture>>(emptyList()) }


    BackHandler {
        when (currentStep) {
            ApprovalStep.SPLASH, ApprovalStep.WELCOME, ApprovalStep.ERROR -> {
                onFinishWithResult(SessionResult.Cancelled)
            }

            ApprovalStep.BVN_CHECK -> {
                currentStep = ApprovalStep.WELCOME
            }

            ApprovalStep.PHOTO_INTRO -> {
                currentStep = ApprovalStep.BVN_CHECK
            }

            ApprovalStep.LIVELINESS -> {
                currentStep = ApprovalStep.PHOTO_INTRO
            }

            ApprovalStep.RETRY -> {
                currentStep = ApprovalStep.WELCOME
            }

            ApprovalStep.PROCESSING, ApprovalStep.SUCCESS -> {
                // Disable back during processing or success
            }
        }
    }


    when (currentStep) {
        // ── 1. Splash Screen (Validates Public Key First) ──
        ApprovalStep.SPLASH -> {
            SplashScreen(validateAction = {
                val result = repository.validatePublicKey(config.publicKey)
                result.map { keyData ->
                    sessionContext = sessionContext.copy(
                        secretKey = keyData.app.liveSecretKey ?: "",
                        businessName = keyData.app.appName ?: "Approval"
                    )
                }
            }, onValidationSuccess = {
                // Valid key -> move to Welcome Screen
                currentStep = ApprovalStep.WELCOME
            }, onValidationError = { errorMessage ->
                currentStep = ApprovalStep.ERROR
            })
        }
        // 2. Welcome Screen
        ApprovalStep.WELCOME -> {
            WelcomeScreen(isLoading = isWelcomeLoading, onStartVerification = {
                coroutineScope.launch {
                    isWelcomeLoading = true;
                    val clientSessionId = createSessionId()

                    val createResult = repository.createSession(
                        publicKey = config.publicKey, sessionId = clientSessionId
                    )

                    createResult.onSuccess { sessionData ->
                        sessionContext = sessionContext.copy(sessionId = sessionData.sessionId)
                        isWelcomeLoading = false
                        currentStep = ApprovalStep.BVN_CHECK
                    }.onFailure { error ->
                        isWelcomeLoading = false
                        toastState.show(error.message ?: "Unknown error")
                    }
                }
            }, onDismiss = { onFinishWithResult(SessionResult.Cancelled) })
        }

        // 3. BVN Form Screen (passes initialUserData from config)
        ApprovalStep.BVN_CHECK -> {
            BvnCheckScreen(
                isLoading = isBvnLoading,
                initialUserData = config.userData,
                onBack = { currentStep = ApprovalStep.WELCOME },
                onProceed = { firstName, lastName, dob, bvn ->
                    coroutineScope.launch {
                        isBvnLoading = true

                        val bvnResult = repository.verifyBvn(
                            secretKey = sessionContext.secretKey,
                            bvn = bvn,
                        )

                        bvnResult.onSuccess { details ->
                            sessionContext = sessionContext.copy(bvnDetails = details)

                            repository.updateSessionWithBvn(
                                sessionId = sessionContext.sessionId,
                                secretKey = sessionContext.secretKey,
                                bvn = bvn
                            )

                            isBvnLoading = false

                            currentStep = ApprovalStep.PHOTO_INTRO
                        }.onFailure { error ->
                            isBvnLoading = false
                            toastState.show(
                                error.message
                                    ?: "BVN verification failed. Please check and try again"
                            )
                        }
                    }
                })
        }

        // 4. Photo Guidelines Screen
        ApprovalStep.PHOTO_INTRO -> {
            PhotoCaptureIntroScreen(
                onDismiss = { onFinishWithResult(SessionResult.Cancelled) },
                isLoading = isIntroLoading,
                onProceed = {
                    coroutineScope.launch {
                        isIntroLoading = true
                        val healthResult = repository.checkLivelinessHealth()

                        healthResult.onSuccess { isAvailable ->
                            isIntroLoading = false
                            if (isAvailable) currentStep = ApprovalStep.LIVELINESS
                            else toastState.show("Liveness service is currently not available")
                        }.onFailure {
                                isIntroLoading = false
                                toastState.show(
                                    it.message ?: "Liveness service is currently not available"
                                )
                            }
                    }
                })
        }

        // 5. CameraX + ML Kit Liveness Screen
        ApprovalStep.LIVELINESS -> {
            var finalVerificationPassed by remember { mutableStateOf(false) }
            LivelinessCameraScreen(
                onDismiss = { currentStep = ApprovalStep.PHOTO_INTRO },
                onStepCapture = { capture ->
                    repository.verifyChallenge(
                        accessToken = sessionContext.secretKey,
                        bvnImage = sessionContext.bvnDetails?.photo ?: "",
                        step = capture.step.apiName,
                        sessionId = sessionContext.sessionId,
                        frame = capture.jpegDataUrl,
                        restart = false,
                    )
                },
                onVerificationComplete = { overallPassed ->
                    finalVerificationPassed = overallPassed
                    currentStep = ApprovalStep.PROCESSING
                })
        }

        // 6. Processing & Polling Screen
        ApprovalStep.PROCESSING -> {
            VerificationProcessingScreen(
                onDismiss = { onFinishWithResult(SessionResult.Cancelled) },
                onSuccess = { currentStep = ApprovalStep.SUCCESS },
                onFailure = {
                    retryReason = "Biometric match was unclear. Please try again with good lighting"
                    currentStep = ApprovalStep.RETRY
                },
                verifyAction = {
                    true
                })
        }

        // 7. Success Screen
        ApprovalStep.SUCCESS -> {
            VerificationSuccessScreen(
                onDismiss = { onFinishWithResult(SessionResult.Success(sessionContext.sessionId)) },
                onProceed = { onFinishWithResult(SessionResult.Success(sessionContext.sessionId)) })
        }

        // 8. Retry Screen
        ApprovalStep.RETRY -> {
            SelfieRetryScreen(
                onDismiss = { onFinishWithResult(SessionResult.Cancelled) },
                onTryAgain = { currentStep = ApprovalStep.LIVELINESS },
                reason = retryReason
            )
        }
        // ── Error Screen ──
        ApprovalStep.ERROR -> {
            ApprovalErrorScreen(
                title = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_TITLE,
                message = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE,
                onDismiss = {
                    onFinishWithResult(
                        SessionResult.Error(
                            code = "INVALID_PUBLIC_KEY",
                            message = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE
                        )
                    )
                },
            )
        }
    }

    IdentityToastHost(toastState = toastState)
}


private const val SESSION_ID_LENGTH = 16
private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
private val secureRandom = SecureRandom()
fun createSessionId(): String {
    return (1..SESSION_ID_LENGTH).map { CHARACTERS[secureRandom.nextInt(CHARACTERS.length)] }
        .joinToString("")
}