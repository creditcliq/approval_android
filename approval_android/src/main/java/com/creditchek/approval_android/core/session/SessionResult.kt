package com.creditchek.approval_android.core.session

import java.io.Serializable

sealed interface SessionResult : Serializable {
    data class Success(
        val sessionId: String, val message: String = "Verification completed successfully"
    ) : SessionResult

    data class Error(val code: String, val message: String) : SessionResult
    object Cancelled : SessionResult {
        private fun readResolve(): Any = Cancelled
    }
}