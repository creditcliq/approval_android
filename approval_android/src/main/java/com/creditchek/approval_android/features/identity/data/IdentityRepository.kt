package com.creditchek.approval_android.features.identity.data

import com.creditchek.approval_android.core.network.RetrofitClient
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.features.identity.data.models.*
import org.json.JSONObject
import retrofit2.HttpException

class IdentityRepository(
    environment: ApprovalEnv = ApprovalEnv.SANDBOX
) {
    private val api: IdentityApi =
        RetrofitClient.createService(IdentityApi::class.java, environment)
    // ── 1. Validate Public Key ──
    suspend fun validatePublicKey(publicKey: String): Result<ValidKeyData> =
        safeApiCall("Public key validation") {
            val response = api.validatePublicKey(
                publicKey = publicKey.trim(),
                emptyBody = emptyMap()
            )
            if (response.success && response.data != null) {
                response.data
            } else {
                throw Exception(response.message ?: "Failed to validate public key")
            }
        }
    // ── 2. Create Widget Session ──
    suspend fun createSession(publicKey: String, sessionId: String): Result<SessionCreatedData> =
        safeApiCall("Session creation") {
            val response = api.createSession(
                request = CreateSessionRequest(
                    publicKey = publicKey.trim(),
                    sessionId = sessionId.trim()
                )
            )
            if (response.success && response.data != null) {
                response.data
            } else {
                throw Exception(response.message ?: "Failed to create session")
            }
        }
    // ── 3. Verify BVN ──
    suspend fun verifyBvn(secretKey: String, bvn: String): Result<BvnDetails> =
        safeApiCall("BVN verification") {
            val response = api.verifyBvnData(
                secretKey = secretKey.trim(),
                bvn = bvn.trim()
            )
            if (response.status == true && response.data != null) {
                response.data
            } else {
                throw Exception(response.message ?: "Failed to verify BVN")
            }
        }
    // ── 4. Update Widget Session with BVN ──
    suspend fun updateSessionWithBvn(
        sessionId: String,
        secretKey: String,
        bvn: String
    ): Result<Unit> = safeApiCall("Session update") {
        val response = api.updateWidgetSession(
            sessionId = sessionId.trim(),
            secretKey = secretKey.trim(),
            request = UpdateSessionRequest(bvn = bvn.trim())
        )
        if (!response.isSuccessful) {
            throw Exception("Failed to update widget session")
        }
    }
    // ── 5. Verify Liveness Challenge ──
    suspend fun verifyChallenge(
        accessToken: String,
        bvnImage: String,
        step: String,
        sessionId: String,
        frame: String,
        restart: Boolean = false
    ): Result<ValidationData> = safeApiCall("Challenge verification") {
        val normanizedToken = accessToken.trim()
        val response = api.verifyChallenge(
            accessToken = normanizedToken,
            request = ChallengeVerifyRequest(
                bvnImage = bvnImage,
                step = step,
                sessionId = sessionId,
                frame = frame,
                restart = restart
            )
        )
        response.data ?: throw Exception(response.message ?: "Challenge verification failed")
    }


    //Returns string as the status
    suspend fun checkLivelinessHealth(): Result<Boolean> = safeApiCall("Health check") {
        val response = api.checkLivenessHealth()
        if (response.isSuccessful) {
            val status = response.body()?.get("status") as? String
            if (status == "healthy") {
                true
            } else {
                throw Exception("Liveness service is currently not available")
            }
        } else {
            throw Exception("Liveness service is currently not available")
        }
    }

    private inline fun <T> safeApiCall(
        operationName: String, block: () -> T
    ): Result<T> = try {
        Result.success(block())
    } catch (e: HttpException) {
        val backendMessage = extractHttpErrorMessage(e)
        Result.failure(Exception(backendMessage ?: "$operationName failed (${e.message})"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun extractHttpErrorMessage(exception: HttpException): String? {
        return try {
            val errorJson = exception.response()?.errorBody()?.string()
            if (!errorJson.isNullOrBlank()) {
                val jsonObject = JSONObject(errorJson)
                when {
                    jsonObject.has("message") -> jsonObject.getString("message")
                    jsonObject.has("error") -> jsonObject.getString("error")
                    else -> null
                }
            } else {
                null
            }

        } catch (_: Exception) {
            null
        }
    }
}