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

# ── 4. ML Kit Face Detection, Common & CameraX ─────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class * implements com.google.firebase.components.ComponentRegistrar
-keep class * implements com.google.android.datatransport.runtime.backends.BackendFactory

-keepclassmembers class * {
    @com.google.android.gms.common.annotation.KeepForSdk <fields>;
    @com.google.android.gms.common.annotation.KeepForSdk <methods>;
}

-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── 5. TensorFlow Lite & LiteRT ────────────────────────────────────────────
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
-keep class com.google.ai.edge.litert.** { *; }
-dontwarn com.google.ai.edge.litert.**