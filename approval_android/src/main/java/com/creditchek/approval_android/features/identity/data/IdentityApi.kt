package com.creditchek.approval_android.features.identity.data

import com.creditchek.approval_android.core.network.ApiConstants
import com.creditchek.approval_android.features.identity.data.models.BvnResponse
import com.creditchek.approval_android.features.liveliness.data.models.ChallengeVerifyRequest
import com.creditchek.approval_android.features.identity.data.models.CreateSessionRequest
import com.creditchek.approval_android.features.identity.data.models.SessionCreatedResponse
import com.creditchek.approval_android.features.identity.data.models.UpdateSessionRequest
import com.creditchek.approval_android.features.identity.data.models.ValidKeyResponse
import com.creditchek.approval_android.features.liveliness.data.models.ValidationSuccess
import retrofit2.Response
import retrofit2.http.*


interface IdentityApi {

    @POST(ApiConstants.VALIDATE_PUBLIC_KEY)
    suspend fun validatePublicKey(
        @Header("token") publicKey: String,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): ValidKeyResponse

    @POST(ApiConstants.CREATE_WIDGET_SESSION)
    suspend fun createSession(
        @Body request: CreateSessionRequest
    ): SessionCreatedResponse

    @PUT("${ApiConstants.WIDGET_SESSION}/{sessionId}")
    suspend fun updateWidgetSession(
        @Path("sessionId") sessionId: String,
        @Header("token") secretKey: String,
        @Body request: UpdateSessionRequest
    ): Response<Map<String, Any>>


    @GET("${ApiConstants.WIDGET_SESSION}/{sessionId}")
    suspend fun getWidgetSession(
        @Path("sessionId") sessionId: String,
        @Header("token") secretKey: String
    ): Response<Map<String, Any>>

    @POST(ApiConstants.VERIFY_IDENTITY_DATA)
    suspend fun verifyBvnData(
        @Header("token") secretKey: String,
        @Query("bvn") bvn: String,
        @Body emptyBody: Map<String, String> = emptyMap()
    ): BvnResponse

    @GET(ApiConstants.LIVENESS_HEALTH)
    suspend fun checkLivenessHealth(): Response<Map<String, Any>>

    @POST(ApiConstants.LIVENESS_VERIFY_CHALLENGE)
    suspend fun verifyChallenge(
        @Header("accessToken") accessToken: String,
        @Body request: ChallengeVerifyRequest
    ): ValidationSuccess

    @GET("${ApiConstants.SESSION_BVN_DATA}/{sessionId}")
    suspend fun getSessionBvnData(
        @Path("sessionId") sessionId: String,
        @Header("token") secretKey: String,
//        @Body emptyBody: Map<String, String> = emptyMap()
    ): BvnResponse
}
