package com.creditchek.approval_android.core.network

import android.os.Environment
import com.creditchek.approval_android.core.session.ApprovalEnv

object ApiConstants {

    const val DEV_BASE_URL = "https://dev.creditchek.africa/v1/"
    const val PROD_BASE_URL = "https://api.creditchek.africa/v1/"

    // Endpoints
    const val VALIDATE_PUBLIC_KEY = "auth/validate/public-key"
    const val CREATE_WIDGET_SESSION = "auth/widget-session/create"
    const val UPDATE_WIDGET_SESSION = "auth/widget-session"
    const val VERIFY_IDENTITY_DATA = "identity/verifyData"
    const val LIVENESS_HEALTH = "liveness/health"
    const val LIVENESS_VERIFY_CHALLENGE = "liveness/verify-challenge"

    // Timeouts
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 20L
    const val WRITE_TIMEOUT_SECONDS = 20L

    fun getBaseUrl(environment: ApprovalEnv): String {
        return when (environment) {
            ApprovalEnv.SANDBOX -> DEV_BASE_URL
            ApprovalEnv.PRODUCTION -> PROD_BASE_URL
        }
    }
}