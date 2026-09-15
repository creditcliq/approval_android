package com.creditchek.approval_android

import android.content.Context
import android.content.Intent
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.SessionResult

/** Main Public Entry Point for CreditChek Approval Android SDK */
object CreditChekApproval {

    private var resultListener: ((SessionResult) -> Unit)? = null

    /**
     * Start the CreditChek Approval verification flow using a callback.
     *
     * @param context Calling Activity or Context
     * @param config Configuration options containing your publicKey, environment, etc.
     * @param onResult Optional result callback
     */
    fun start(
            context: Context,
            config: ApprovalConfig,
            onResult: ((SessionResult) -> Unit)? = null
    ) {
        resultListener = onResult
        val intent =
                createIntent(context, config).apply {
                    if (context !is android.app.Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
        context.startActivity(intent)
    }

    /** Creates an Intent to launch ApprovalActivity manually. */
    fun createIntent(context: Context, config: ApprovalConfig): Intent {
        return Intent(context, ApprovalActivity::class.java).apply {
            putExtra(ApprovalActivity.EXTRA_CONFIG, config)
        }
    }

    /**
     * Returns the standard ActivityResultContract for modern Jetpack Compose and Activity Result
     * API usage.
     */
//    fun contract(): ApprovalContract = ApprovalContract()

    /** Internal notifier called by ApprovalActivity when the flow completes. */
    internal fun notifyResult(result: SessionResult) {
        resultListener?.invoke(result)
        resultListener = null
    }
}
