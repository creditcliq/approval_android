package com.creditchek.approval_android.features.liveliness.presentation.screens//package com.creditchek.approval_android.features.liveliness.presentation.screens
//
//import androidx.activity.compose.BackHandler
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.platform.LocalContext
//import com.creditchek.approval_android.ApprovalLivelinessStep
//import com.creditchek.approval_android.core.network.NetworkQualityEstimator
//import com.creditchek.approval_android.core.session.ApprovalConfig
//import com.creditchek.approval_android.core.session.SessionResult
//import com.creditchek.approval_android.core.shared.components.IdentityToastHost
//import com.creditchek.approval_android.core.shared.components.rememberToastState
//import com.creditchek.approval_android.features.identity.data.IdentityRepository
//import com.creditchek.approval_android.features.identity.data.IdentitySessionContext
//import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorDefaults
//import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorScreen
//import com.creditchek.approval_android.features.identity.presentation.screens.SplashScreen
//import com.creditchek.approval_android.features.identity.presentation.screens.VerificationSuccessScreen
//import com.creditchek.approval_android.features.liveliness.data.models.FaceVerificationStep
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlin.time.Duration.Companion.milliseconds
//
//@Composable
//fun ApprovalLivelinessFlowNavigator(
//    config: ApprovalConfig,
//    onFinishWithResult: (SessionResult) -> Unit,
//    nextStep: ApprovalLivelinessStep? = null,
//    previousSession: IdentitySessionContext? = null
//) {
//
//    val coroutineScope = rememberCoroutineScope()
//    val repository = remember(config.environment) {
//        IdentityRepository(config.environment)
//    }
//    val toastState = rememberToastState()
//
//
//    var currentStep by remember { mutableStateOf(nextStep ?: ApprovalLivelinessStep.SPLASH) }
//    var sessionContext: IdentitySessionContext by remember {
//        mutableStateOf(
//            previousSession ?: IdentitySessionContext(publicKey = config.publicKey)
//        )
//    }
//
//    var isIntroLoading by remember { mutableStateOf(false) }
//    var retryReason by remember { mutableStateOf("No obstructions: remove hats,\nglasses and masks") }
//    var finalVerificationPassed by remember { mutableStateOf(false) }
//    var isRetryFlow by remember { mutableStateOf(false) }
//
//
//
//
//    BackHandler {
//        when (currentStep) {
//            ApprovalLivelinessStep.SPLASH, ApprovalLivelinessStep.PHOTO_INTRO, ApprovalLivelinessStep.ERROR -> {
//                onFinishWithResult(SessionResult.Cancelled)
//            }
//
//            ApprovalLivelinessStep.LIVELINESS -> {
//                currentStep = ApprovalLivelinessStep.PHOTO_INTRO
//            }
//
//            ApprovalLivelinessStep.RETRY -> {
//                currentStep = ApprovalLivelinessStep.PHOTO_INTRO
//            }
//
//            ApprovalLivelinessStep.PROCESSING, ApprovalLivelinessStep.SUCCESS -> {
//                // Disable back during processing or success
//            }
//        }
//    }
//    when (currentStep) {
//        // ── 1. Splash Screen (Validates Public Key First) ──
//        ApprovalLivelinessStep.SPLASH -> {
//            SplashScreen(validateAction = {
//                val result = repository.validatePublicKey(config.publicKey)
//                result.map { keyData ->
//                    sessionContext = sessionContext.copy(
//                        secretKey = keyData.app.liveSecretKey,
//                        businessName = keyData.app.appName,
//                        sessionId = config.sessionId ?: ""
//                    )
//                }
//            }, onValidationSuccess = {
//                // Valid key -> move to Welcome Screen
//                coroutineScope.launch {
//                    val getSessionResult = repository.getWidgetSession(
//                        sessionId = config.sessionId ?: "",
//                        secretKey = sessionContext.secretKey
//                    )
////                    getSessionResult
//                }
//                currentStep = ApprovalLivelinessStep.PHOTO_INTRO
//            }, onValidationError = { errorMessage ->
//                currentStep = ApprovalLivelinessStep.ERROR
//            })
//        }
//
//
//        // ── Error Screen ──
//        ApprovalLivelinessStep.ERROR -> {
//            ApprovalErrorScreen(
//                title = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_TITLE,
//                message = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE,
//                onDismiss = {
//                    onFinishWithResult(
//                        SessionResult.Error(
//                            code = "INVALID_PUBLIC_KEY",
//                            message = ApprovalErrorDefaults.INVALID_PUBLIC_KEY_MESSAGE
//                        )
//                    )
//                },
//            )
//        }
//    }
//
//    IdentityToastHost(toastState = toastState)
//}