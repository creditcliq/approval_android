package com.creditchek.approval_android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalModule
import com.creditchek.approval_android.core.session.SessionResult
import com.creditchek.approval_android.core.theme.ApprovalTheme
import com.creditchek.approval_android.features.identity.presentation.ApprovalFlowNavigator

sealed interface ApprovalStep {
    enum class Identity : ApprovalStep {
        SPLASH, ERROR, WELCOME, BVN_CHECK, SUCCESS, LIVELINESS
    }

    enum class Liveliness  : ApprovalStep {
        SPLASH, PHOTO_INTRO, LIVELINESS, PROCESSING, SUCCESS, RETRY, ERROR
    }
}

//typealias ApprovalLivelinessStep = ApprovalStep.Liveliness

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
                    config = config,
                    onFinishWithResult = { result ->
                        finishWithResult(result)
                    }
                )
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






