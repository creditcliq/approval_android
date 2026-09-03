package com.creditchek.approval_android.features.identity.data.models

import com.google.gson.annotations.SerializedName

data class ValidKeyResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: ValidKeyData?
)

data class ValidKeyData(
    @SerializedName("app") val app: ValidKeyApp,
    @SerializedName("businessName") val businessName: String,
    @SerializedName("keyType") val keyType: String,
)

data class ValidKeyApp(
    @SerializedName("_id") val id: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("liveSecretKey") val liveSecretKey: String,
    @SerializedName("status") val status: Boolean
)