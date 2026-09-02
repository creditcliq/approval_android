package com.creditchek.approval_android.core.session

enum class ApprovalModule(val key: String) {
    IDENTITY("identity"), INCOME("income"), CREDIT("credit"), RECOVA("recova");

    companion object {
        fun fromKey(key: String): ApprovalModule {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: IDENTITY
        }
    }
}
