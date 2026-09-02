package com.creditchek.approval_android

import android.content.Context
import android.content.Intent
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.SessionResult

/**
 * Public SDK Entry Point for CreditChek Approval Android SDK
 */
object CreditChekApproval {

    private var resultListener: ((SessionResult) -> Unit)? = null

    /**
     * Start the CreditChek Approval verification flow
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
        val intent = Intent(context, ApprovalActivity::class.java).apply {
            putExtra(ApprovalActivity.EXTRA_CONFIG, config)
            if (context !is android.app.Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
    }

    /**
     * Internal callback notifier used by ApprovalActivity
     */
    internal fun notifyResult(result: SessionResult) {
        resultListener?.invoke(result)
        resultListener = null
    }
}