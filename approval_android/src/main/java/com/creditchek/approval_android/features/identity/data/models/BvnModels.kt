package com.creditchek.approval_android.features.identity.data.models

import com.google.gson.annotations.SerializedName

data class BvnResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: BvnDetails? = null
) {
    
        val isSuccessful: Boolean
        get() = status == true || success == true

}

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

data class NinResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: NinDetails? = null
) {
    val isSuccessful: Boolean
        get() = status == true || success == true
}

data class NinDetails(
    @SerializedName("nin") val nin: String? = null,
    @SerializedName("firstName", alternate = ["firstname", "first_name"]) val firstName: String? = null,
    @SerializedName("lastName", alternate = ["surname", "lastname", "last_name"]) val lastName: String? = null,
    @SerializedName("middleName", alternate = ["middlename", "middle_name", "pmiddlename"]) val middleName: String? = null,
    @SerializedName("dateOfBirth", alternate = ["birthdate", "dob", "birth_date", "date_of_birth"]) val dateOfBirth: String? = null,
    @SerializedName("photo", alternate = ["image", "picture"]) val photo: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("telephoneno", alternate = ["phone", "telephoneNo", "telephone_no"]) val telephoneNo: String? = null
)