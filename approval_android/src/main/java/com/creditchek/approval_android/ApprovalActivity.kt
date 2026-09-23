package com.creditchek.approval_android

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.SessionResult
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.presentation.ApprovalFlowNavigator
import com.creditchek.approval_android.features.identity.presentation.screens.ApprovalErrorScreen

sealed interface ApprovalStep {
    enum class Identity : ApprovalStep {
        SPLASH, ERROR, WELCOME, BVN_CHECK, SUCCESS, LIVELINESS
    }

    enum class Liveliness : ApprovalStep {
        SPLASH, PHOTO_INTRO, LIVELINESS, PROCESSING, SUCCESS, RETRY, ERROR
    }
}

//typealias ApprovalLivelinessStep = ApprovalStep.Liveliness

class ApprovalActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read Serializable config across all Android SDK versions
        // 1. Safely retrieve the config from Intent
        val config = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_CONFIG, ApprovalConfig::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra(EXTRA_CONFIG) as? ApprovalConfig
            }
        } catch (e: Exception) {
            null
        }

        // 2. Validate configuration
        val validationError = when {
            config == null -> "Configuration was not provided or could not be loaded."
            else -> config.validate()
        }

        setContent {
            ApprovalTheme {
                if (validationError != null) {
                    // 3. Display the Error Screen instead of crashing
                    ApprovalErrorScreen(
                        title = "Configuration Error",
                        message = validationError,
                        actionLabel = "Close",
                        onDismiss = {
                            finishWithResult(
                                SessionResult.Error(
                                    code = "INVALID_CONFIG",
                                    message = validationError
                                )
                            )
                        }
                    )
                } else {
                    ApprovalFlowNavigator(config = config!!) { result ->
                        finishWithResult(result)
                    }
                }
            }
        }
    }

    private fun finishWithResult(result: SessionResult) {
        // 1. Notify static callback listener (if launched via CreditChekApproval.start)
        CreditChekApproval.notifyResult(result)

        // 2. Set Activity result for ActivityResultContract
        val data = Intent().apply {
            putExtra(EXTRA_RESULT, result)
        }
        val resultCode = when (result) {
            is SessionResult.Success -> RESULT_OK
            is SessionResult.Cancelled -> RESULT_CANCELED
            is SessionResult.Error -> RESULT_FIRST_USER
        }
        setResult(resultCode, data)
        finish()
    }

    companion object {
        const val EXTRA_CONFIG = "extra_approval_config"
        const val EXTRA_RESULT = "extra_approval_result"
    }
}






