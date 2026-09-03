package com.creditchek.approval_android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.SessionResult

/**
 * Modern Android Activity Result Contract for CreditChek Approval SDK.
 *
 * Usage in Jetpack Compose:
 * ```kotlin
 * val launcher = rememberLauncherForActivityResult(ApprovalContract()) { result ->
 *     when (result) {
 *         is SessionResult.Success -> println("Session ID: ${result.sessionId}")
 *         is SessionResult.Cancelled -> println("User dismissed")
 *         is SessionResult.Error -> println("Error: ${result.message}")
 *     }
 * }
 *
 * Button(onClick = {
 *     launcher.launch(ApprovalConfig(publicKey = "YOUR_PUBLIC_KEY"))
 * }) {
 *     Text("Verify Identity")
 * }
 * ```
 */
class ApprovalContract : ActivityResultContract<ApprovalConfig, SessionResult>() {

    override fun createIntent(context: Context, input: ApprovalConfig): Intent {
        return Intent(context, ApprovalActivity::class.java).apply {
            putExtra(ApprovalActivity.EXTRA_CONFIG, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): SessionResult {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(ApprovalActivity.EXTRA_RESULT, SessionResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(ApprovalActivity.EXTRA_RESULT) as? SessionResult
        }

        return result ?: when (resultCode) {
            Activity.RESULT_OK -> SessionResult.Success(sessionId = "")
            Activity.RESULT_CANCELED -> SessionResult.Cancelled
            else -> SessionResult.Cancelled
        }
    }
}