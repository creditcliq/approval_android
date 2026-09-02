package com.creditchek.approval_android.features.identity.data.models

import com.google.gson.annotations.SerializedName

data class BvnResponse(
    @SerializedName("status") val status: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: BvnDetails?
)

data class BvnDetails(
    @SerializedName("bvn") val bvn: String?,
    @SerializedName("firstName") val firstName: String?,
    @SerializedName("lastName") val lastName: String?,
    @SerializedName("middleName") val middleName: String?,
    @SerializedName("dateOfBirth") val dateOfBirth: String?,
    @SerializedName("photo") val photo: String?, // Base64 BVN picture for facial match
    @SerializedName("gender") val gender: String?,
    @SerializedName("phones") val phones: List<String>?
)