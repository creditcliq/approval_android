package com.creditchek.approval_android.features.identity.data

import com.creditchek.approval_android.features.identity.data.models.BvnDetails

/**
 * Holds in-memory session credentials and user details during the verification lifecycle.
 */
data class IdentitySessionContext(
    // 1. Session and API keys returned from public key validation
    val publicKey: String = "",
    val secretKey: String = "",
    val sessionId: String = "",

    // 2. Merchant / Business name to display in the header
    val businessName: String = "",

    // 3. BVN details and reference photo retrieved in Bit 2
    val bvnDetails: BvnDetails? = null
)



