package com.creditchek.approval_android

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.ApprovalModule
import com.creditchek.approval_android.core.session.SessionResult
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.core.theme.DynamicThemeConfig
import com.creditchek.approval_android.core.theme.parseHexColor
import com.creditchek.approval_android.features.credit.presentation.CreditFlowNavigator
import com.creditchek.approval_android.core.session.SharedSessionContext
import com.creditchek.approval_android.core.shared.components.ApprovalErrorScreen
import com.creditchek.approval_android.core.shared.components.SplashScreen
import com.creditchek.approval_android.features.identity.data.IdentityRepository
import com.creditchek.approval_android.features.identity.data.models.Status
import com.creditchek.approval_android.features.identity.presentation.IdentityFlowNavigator
import com.creditchek.approval_android.features.income.presentation.IncomeFlowNavigator
import com.creditchek.approval_android.features.liveliness.presentation.LivelinessFlowNavigator
import com.creditchek.approval_android.features.recova.presentation.RecovaFlowNavigator
import kotlinx.coroutines.launch

@Composable
fun ApprovalCoordinator(
    config: ApprovalConfig,
    onFinishWithResult: (SessionResult) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { IdentityRepository() }

    var isInitializing by remember { mutableStateOf(true) }
    var executionQueue by remember { mutableStateOf<List<ApprovalModule>>(emptyList()) }
    var currentModuleIndex by remember { mutableIntStateOf(0) }
    var themeConfig by remember { mutableStateOf(DynamicThemeConfig()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var sharedSessionContext by remember {
        mutableStateOf(
            SharedSessionContext(
                publicKey = config.publicKey,
                sessionId = config.sessionId,
                widgetId = config.widgetId
            )
        )
    }

    fun onModuleFinished() {
        if (currentModuleIndex < executionQueue.lastIndex) {
            currentModuleIndex += 1
        } else {
            onFinishWithResult(SessionResult.Success(sharedSessionContext.sessionId))
        }
    }

    BackHandler {
        if (currentModuleIndex > 0) {
            currentModuleIndex -= 1 //Step back to previous module
        } else {
            onFinishWithResult(SessionResult.Cancelled)
        }
    }


    ApprovalTheme(themeConfig = themeConfig){
        if(errorMessage != null){
            ApprovalErrorScreen(
                title = "Configuration Error",
                message = errorMessage ?: "Failed to initialize verification",
                isDevelopment = sharedSessionContext.environment.isDevelopment,
                onDismiss = {
                    onFinishWithResult(SessionResult.Error("INIT_ERROR", errorMessage ?: ""))
                }
            )
        }
        else if (isInitializing){
            // ==========================================
            // 🚀 UNIVERSAL SPLASH SCREEN
            // ==========================================
            SplashScreen(
                validateAction = {
                    runCatching {
                        // 1. Fetch widget configuration, theme, order, & environment
                        val appConfig = repository.getConfigData(config.widgetId).getOrThrow()

                        // Validate widget publish status
                        val status = appConfig.status?.trim()?.lowercase()
                        if (status != "published") {
                            throw IllegalStateException("Please publish this widget on the dashboard first before you can proceed.")
                        }

                        val env = if (appConfig.environment.equals("development", ignoreCase = true)) {
                            ApprovalEnv.DEVELOPMENT
                        } else {
                            ApprovalEnv.PRODUCTION
                        }

                        themeConfig = DynamicThemeConfig(
                            primaryColor = parseHexColor(appConfig.color),
                            businessName = appConfig.displayName.takeIf { !it.isNullOrBlank() }
                                ?: appConfig.appId.appName ?: "CreditChek",
                            logoUrl = appConfig.logo
                        )

                        executionQueue = ApprovalModule.fromOrderList(appConfig.order)
                        sharedSessionContext = sharedSessionContext.copy(
                            environment = env,
                            widgetId = config.widgetId
                        )

                        // 2. Validate Public Key
                        val keyData = repository.validatePublicKey(config.publicKey).getOrThrow()
                        sharedSessionContext = sharedSessionContext.copy(
                            secretKey = keyData.app.liveSecretKey,
                            businessName = keyData.app.appName
                        )
                        if (themeConfig.businessName == "CreditChek" && !keyData.app.appName.isNullOrBlank()) {
                            themeConfig = themeConfig.copy(businessName = keyData.app.appName)
                        }
                    }
                },
                onValidationSuccess = {
                    coroutineScope.launch {
                        // 3. Inspect Session to skip completed modules
                        repository.getWidgetSession(
                            secretKey = config.publicKey,
                            sessionId = config.sessionId
                        ).onSuccess { sessionData ->
                            sharedSessionContext = sharedSessionContext.copy(services = sessionData.services)

                            val isBvnCompleted = sessionData.services.bvn?.status == Status.COMPLETED
                            val isNinCompleted = sessionData.services.nin?.status == Status.COMPLETED
                            val isLivenessCompleted = sessionData.services.liveness?.status == Status.COMPLETED

                            // Filter out already completed modules
                            val pendingQueue = executionQueue.toMutableList().apply {
                                if (isBvnCompleted || isNinCompleted) remove(ApprovalModule.IDENTITY)
                                if (isLivenessCompleted) remove(ApprovalModule.LIVELINESS)
                            }

                            // Pre-fetch reference images if Identity was previously completed
                            if (isNinCompleted) {
                                repository.getSessionNinData(config.sessionId, sharedSessionContext.secretKey)
                                    .onSuccess { sharedSessionContext = sharedSessionContext.copy(ninDetails = it, selectedIdType = "NIN") }
                            } else if (isBvnCompleted) {
                                repository.getSessionBvnData(config.sessionId, sharedSessionContext.secretKey)
                                    .onSuccess { sharedSessionContext = sharedSessionContext.copy(bvnDetails = it, selectedIdType = "BVN") }
                            }

                            if (pendingQueue.isEmpty()) {
                                onFinishWithResult(SessionResult.Success(config.sessionId))
                            } else {
                                executionQueue = pendingQueue
                                currentModuleIndex = 0
                                isInitializing = false
                            }
                        }.onFailure {
                            errorMessage = it.message ?: "Failed to verify session"
                        }
                    }
                },
                onValidationError = { msg ->
                    errorMessage = msg
                }
            )
        } else {
            val activeModule = executionQueue.getOrNull(currentModuleIndex)

            when (activeModule) {
                ApprovalModule.IDENTITY -> {
                    IdentityFlowNavigator(
                        config = config,
                        context = sharedSessionContext,
                        onContextUpdate = { sharedSessionContext = it },
                        onThemeLoaded = { themeConfig = it },
                        onComplete = { value ->
                            onModuleFinished()
                        },
                        onError = { msg ->
                            onFinishWithResult(SessionResult.Error("IDENTITY_ERROR", message = msg))
                        },
                        onCancel = {
                            if (currentModuleIndex > 0) currentModuleIndex -= 1
                            else onFinishWithResult(SessionResult.Cancelled)
                        }
                    )
                }


                ApprovalModule.LIVELINESS -> {
                    LivelinessFlowNavigator(
                        config = config,
                        context = sharedSessionContext,
                        onContextUpdate = { sharedSessionContext = it },
                        onThemeLoaded = { themeConfig = it },
                        onComplete = { value ->
                            onModuleFinished()
                        },
                        onError = { msg ->
                            onFinishWithResult(
                                SessionResult.Error(
                                    "LIVELINESS_ERROR",
                                    message = msg
                                )
                            )
                        },
                        onCancel = {
                            if (currentModuleIndex > 0) currentModuleIndex -= 1
                            else onFinishWithResult(SessionResult.Cancelled)
                        }
                    )
                }

                ApprovalModule.INCOME -> {
                    IncomeFlowNavigator(
                        config = config,
                        context = sharedSessionContext,

                        onThemeLoaded = { themeConfig = it },
                        onComplete = { value ->
                            onModuleFinished()
                        },
                        onError = { msg ->
                            onFinishWithResult(SessionResult.Error("INCOME_ERROR", message = msg))
                        },
                        onCancel = {
                            if (currentModuleIndex > 0) currentModuleIndex -= 1
                            else onFinishWithResult(SessionResult.Cancelled)
                        }
                    )
                }

                ApprovalModule.CREDIT -> {
                    CreditFlowNavigator(
                        config = config,
                        context = sharedSessionContext,
                        onThemeLoaded = { themeConfig = it },
                        onComplete = { value ->
                            onModuleFinished()
                        },
                        onError = { msg ->
                            onFinishWithResult(SessionResult.Error("CREDIT_ERROR", message = msg))
                        },
                        onCancel = {
                            if (currentModuleIndex > 0) currentModuleIndex -= 1
                            else onFinishWithResult(SessionResult.Cancelled)
                        }
                    )

                }

                ApprovalModule.RECOVA -> {
                    RecovaFlowNavigator(
                        config = config,
                        context = sharedSessionContext,
                        onThemeLoaded = { themeConfig = it },
                        onComplete = { value ->
                            onModuleFinished()
                        },
                        onError = { msg ->
                            onFinishWithResult(SessionResult.Error("RECOVA_ERROR", message = msg))
                        },
                        onCancel = {
                            if (currentModuleIndex > 0) currentModuleIndex -= 1
                            else onFinishWithResult(SessionResult.Cancelled)
                        }
                    )

                }

                null -> {
                    onFinishWithResult(SessionResult.Success(sharedSessionContext.sessionId))
                }
            }
        }
    }
}