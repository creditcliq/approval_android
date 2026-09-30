package com.creditchek.approval_android.core.session

import java.io.Serializable

enum class ApprovalEnv {
    DEVELOPMENT,
    PRODUCTION;

    val isDevelopment: Boolean
        get() = this == DEVELOPMENT
}

/** Runtime configuration for a CreditChek approval session. */
data class ApprovalConfig(
        val publicKey: String,
        val modules: List<ApprovalModule> = listOf(ApprovalModule.IDENTITY),
        val userData: AUserData? = null,
        val sessionId: String,
        val environment: ApprovalEnv = ApprovalEnv.PRODUCTION
) : Serializable {
        fun validate(): String? {
            return  when {
                publicKey.isBlank() -> "ApprovalConfig.publicKey cannot be empty"
                modules.isEmpty() ->  "ApprovalConfig.modules cannot be empty"
                sessionId.isBlank() -> "ApprovalConfig.sessionId cannot be empty"
                else -> null
            }
        }
}
