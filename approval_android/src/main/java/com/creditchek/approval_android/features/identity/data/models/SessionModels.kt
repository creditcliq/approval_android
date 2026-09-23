package com.creditchek.approval_android.features.identity.data.models

import com.google.gson.annotations.SerializedName

data class CreateSessionRequest(
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("sessionId") val sessionId: String
)


enum class Status {
    @SerializedName("pending") PENDING,
    @SerializedName("completed") COMPLETED,
    @SerializedName("failed") FAILED
}

enum class Service {
    @SerializedName("bvn") BVN,
    @SerializedName("liveness") LIVELINESS
}

data class UpdateSessionRequest(
    @SerializedName("bvn") val bvn: String? = null,
    @SerializedName("service") val service: Service? = null,
    @SerializedName("status") val status: Status? = null,
)

data class SessionCreatedResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: SessionCreatedData?
)

data class SessionCreatedData(
    @SerializedName("publicKey") val publicKey: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("status") val status: String,
    @SerializedName("services") val services: Services,
    @SerializedName("_id") val id: String,
    @SerializedName("expiresAt") val expiresAt: String?
)


data class Services(
    @SerializedName("bvn")
    val bvn: ServiceStatus,

    @SerializedName("liveness")
    val liveness: ServiceStatus
)

data class ServiceStatus(
    @SerializedName("status")
    val status: Status
)