package com.creditchek.approval_android.features.identity.data.models

import com.google.gson.annotations.SerializedName

data class CreateSessionRequest(
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("sessionId") val sessionId: String
)

data class UpdateSessionRequest(
    @SerializedName("bvn") val bvn: String
)

data class SessionCreatedResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SessionCreatedData?
)

data class SessionCreatedData(
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("_id") val id: String,
    @SerializedName("expiresAt") val expiresAt: String?
)