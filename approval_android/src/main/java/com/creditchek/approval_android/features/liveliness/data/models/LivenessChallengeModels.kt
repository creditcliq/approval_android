package com.creditchek.approval_android.features.liveliness.data.models

import com.google.gson.annotations.SerializedName

/**
 * 1:1 Request Body for POST /liveness/verify-challenge
 */
data class ChallengeVerifyRequest(
    @SerializedName("bvnImage") val bvnImage: String,
    @SerializedName("step") val step: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("frame") val frame: String, // 600x600 Base64 JPEG data URL
    @SerializedName("restart") val restart: Boolean = false
)

data class ValidationSuccess(
    @SerializedName("status") val status: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: ValidationData?
)

data class ValidationData(
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("overall_passed") val overallPassed: Boolean?,
    @SerializedName("liveness_passed") val livenessPassed: Boolean?,
    @SerializedName("identity_passed") val identityPassed: Boolean?,
    @SerializedName("can_retry") val canRetry: Boolean?,
    @SerializedName("step") val step: String?,
    @SerializedName("passed") val passed: Boolean?,
    @SerializedName("next_step") val nextStep: String?,
    @SerializedName("next_prompt") val nextPrompt: String?,
    @SerializedName("steps_done") val stepsDone: Int?,
    @SerializedName("steps_total") val stepsTotal: Int?
)