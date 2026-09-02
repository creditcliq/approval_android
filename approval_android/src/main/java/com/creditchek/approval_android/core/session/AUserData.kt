package com.creditchek.approval_android.core.session

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AUserData(

    @SerializedName("firstName")
    val firstName: String = "",

    @SerializedName("lastName")
    val lastName: String = "",

    @SerializedName("bvn")
    val bvn: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("dateOfBirth")
    val dob: String? = null, // Date of birth in ISO 8601 format e.g "2000-10-01

    @SerializedName("phone")
    val phone: String? = null,
): Serializable
