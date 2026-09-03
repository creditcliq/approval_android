# ==========================================================================
# CreditChek Approval Android SDK - Consumer ProGuard / R8 Rules
# ==========================================================================

# ── 1. Public SDK Entry Points & Contracts ────────────────────────────────
-keep class com.creditchek.approval_android.CreditChekApproval { *; }
-keep class com.creditchek.approval_android.ApprovalContract { *; }
-keep class com.creditchek.approval_android.ApprovalActivity { *; }
-keep class com.creditchek.approval_android.core.session.** { *; }

# ── 2. Gson & JSON Data Models (Prevent field name obfuscation) ────────────
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.creditchek.approval_android.features.identity.data.models.** { *; }
-keepclassmembers enum com.creditchek.approval_android.features.identity.data.models.** { *; }
-keepclassmembers enum com.creditchek.approval_android.core.session.** { *; }

# ── 3. Retrofit 2 & API Interfaces ─────────────────────────────────────────
-keep interface com.creditchek.approval_android.features.identity.data.IdentityApi { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# ── 4. ML Kit Face Detection & CameraX ─────────────────────────────────────
-keep class com.google.mlkit.vision.face.** { *; }
-keep class androidx.camera.** { *; }