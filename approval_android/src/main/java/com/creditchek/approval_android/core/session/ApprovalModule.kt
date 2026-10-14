package com.creditchek.approval_android.core.session

enum class ApprovalModule(val key: String) {
    IDENTITY("identity"), INCOME("income"), CREDIT("credit"), RECOVA("recova"), LIVELINESS("liveliness");

//    companion object {
//        fun resolveExecutionOrder(requested: List<ApprovalModule>): List<ApprovalModule> {
//            val distinct = requested.distinct().toMutableList()
//
//            val hasIdentity = distinct.contains(IDENTITY)
//            val hasLiveliness = distinct.contains(LIVELINESS)
//
//            if (hasIdentity && hasLiveliness) {
//                val identityIndex = distinct.indexOf(IDENTITY)
//                val livelinessIndex = distinct.indexOf(LIVELINESS)
//
//                if(livelinessIndex < identityIndex) {
//                    distinct.remove(LIVELINESS)
//                    val newIdentityIndex = distinct.indexOf(IDENTITY)
//                    distinct.add(newIdentityIndex + 1, LIVELINESS)
//                }
//            }
//            return distinct
//        }
//    }

    companion object {
        fun fromKey(rawKey: String): ApprovalModule? {
            return when (rawKey.trim().lowercase()) {
                "identity" -> IDENTITY
                "income" -> INCOME
                "credit" -> CREDIT
                "recova" -> RECOVA
                "liveness", "liveliness" -> LIVELINESS
                else -> null
            }
        }

        fun fromOrderList(orderStrings: List<String>): List<ApprovalModule> {
            val modules = orderStrings.mapNotNull { fromKey(it) }.distinct().toMutableList()

            // Enforce identity precedes liveliness if both are configured
            if (modules.contains(IDENTITY) && modules.contains(LIVELINESS)) {
                modules.remove(LIVELINESS)
                val identityIndex = modules.indexOf(IDENTITY)
                modules.add(identityIndex + 1, LIVELINESS)
            }
            return modules
        }
    }
}
