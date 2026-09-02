package com.creditchek.approval_android.core.session

import java.io.Serializable

enum class ApprovalEnv {
    SANDBOX, PRODUCTION,
}

/**
 * Runtime configuration for a CreditChek approval session.
 */

data class ApprovalConfig(
    val publicKey: String,
    val modules: List<ApprovalModule> = listOf(ApprovalModule.IDENTITY),
    val userData: AUserData? = null,
    val environment: ApprovalEnv = ApprovalEnv.SANDBOX
): Serializable {
    fun validate() {
        require(publicKey.isNotBlank()) { "ApprovalConfig.publicKey cannot be empty" }
        require(modules.isNotEmpty()) { "ApprovalConfig.modules cannot be empty" }
    }
}
