package com.creditchek.approval_android.core.session

import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.features.identity.data.models.BvnDetails
import com.creditchek.approval_android.features.identity.data.models.NinDetails
import com.creditchek.approval_android.features.identity.data.models.Services

/**
 * Holds in-memory session credentials and user details during the verification lifecycle.
 */
data class SharedSessionContext(
    // 1. Session and API keys returned from public key validation
    val publicKey: String = "",
    val secretKey: String = "",
    val sessionId: String = "",
    val widgetId: String = "",

    // 2. Merchant / Business name to display in the header
    val businessName: String = "",

    // 3. BVN & NIN details and reference photo retrieved
    val bvnDetails: BvnDetails? = null,
    val ninDetails: NinDetails? = null,

    // 4. Selected verification type: "BVN" or "NIN"
    val selectedIdType: String = "BVN",

    // 5. Services supported on the session
    val services: Services? = null,

    val environment: ApprovalEnv = ApprovalEnv.DEVELOPMENT,
) {
    val identityImage: String?
        get() = if (selectedIdType == "NIN") {
            ninDetails?.photo ?: bvnDetails?.photo
        } else {
            bvnDetails?.photo ?: ninDetails?.photo
        }
}



