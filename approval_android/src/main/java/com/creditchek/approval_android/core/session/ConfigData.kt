package com.creditchek.approval_android.core.session

import com.google.gson.annotations.SerializedName

data class ConfigDataResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: AppConfig?
)

data class AppConfig(
    @SerializedName("_id")
    val id: String,

    @SerializedName("businessId")
    val businessId: String,

    @SerializedName("appId")
    val appId: AppInfo,

    @SerializedName("name")
    val name: String,

    @SerializedName("type")
    val type: String,

    @SerializedName("color")
    val color: String?,

    @SerializedName("multipleApproval")
    val multipleApproval: Boolean,

    @SerializedName("identitySDK")
    val identitySDK: Boolean,

    @SerializedName("creditSDK")
    val creditSDK: Boolean,

    @SerializedName("incomeSDK")
    val incomeSDK: Boolean,

    @SerializedName("recovaSDK")
    val recovaSDK: Boolean,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("updatedAt")
    val updatedAt: String,

    @SerializedName("__v")
    val v: Int? = null,

    @SerializedName("order")
    val order: List<String> = emptyList(),

    @SerializedName("displayName")
    val displayName: String?,

    @SerializedName("supportEmail")
    val supportEmail: String?,

    @SerializedName("environment")
    val environment: String?,

    @SerializedName("livenessSDK")
    val livenessSDK: Boolean,

    @SerializedName("logo")
    val logo: String?,

    @SerializedName("services")
    val services: List<String> = emptyList(),

    @SerializedName("status")
    val status: String? = null
)

data class AppInfo(
    @SerializedName("_id")
    val id: String,

    @SerializedName("appName")
    val appName: String?,

    @SerializedName("displayName")
    val displayName: String?
)