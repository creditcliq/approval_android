# CreditChek Approval Android SDK

Official Android Native SDK for **CreditChek Approval**, providing active face liveness detection, BVN demographic verification, and identity checks in your Android apps.

---

## 📋 Requirements
- **Minimum SDK**: 24 (Android 7.0+)
- **Compile SDK**: 34+
- **Java / JVM**: Version 17
- **Kotlin**: 1.9+ / 2.0+

---

## 🚀 Installation & Integration Process

### 1. Add Repository
In your project root `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

*(If using Groovy DSL `settings.gradle`: `maven { url 'https://jitpack.io' }`)*

### 2. Add Dependency
In your module `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.creditcliq:approval_android:1.0.0+1")
}
```

### 3. Configure Android Permissions
In `app/src/main/AndroidManifest.xml`, declare Camera and Internet permissions:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
</manifest>
```

### 4. ProGuard / R8 Rules (Release Builds)
In `app/proguard-rules.pro`:

```proguard
# CreditChek Approval Android SDK
-keep class com.creditchek.approval_android.** { *; }
-keep interface com.creditchek.approval_android.** { *; }

# Google ML Kit Face Detection
-keep class com.google.mlkit.vision.face.** { *; }

# CameraX
-keep class androidx.camera.** { *; }
```

---

## 💡 Usage

> [!IMPORTANT]
> **Prerequisite**: Generate a `sessionId` on your server first by calling CreditChek's Widget Session API (`POST /v1/auth/widget-session/create`). Pass that `sessionId` into `ApprovalConfig`.

### A. Jetpack Compose (Recommended)

```kotlin
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import com.creditchek.approval_android.CreditChekApproval
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.AUserData
import com.creditchek.approval_android.core.session.SessionResult

@Composable
fun IdentityVerificationScreen(backendSessionId: String) {
    val approvalLauncher = rememberLauncherForActivityResult(
        contract = CreditChekApproval.contract()
    ) { result ->
        when (result) {
            is SessionResult.Success -> {
                // Identity verification passed!
                val sessionId = result.sessionId
                println("Verification Succeeded! Session: $sessionId")
            }
            is SessionResult.Cancelled -> {
                println("User cancelled the verification flow")
            }
            is SessionResult.Error -> {
                println("Error [${result.code}]: ${result.message}")
            }
        }
    }

    Button(onClick = {
        val config = ApprovalConfig(
            publicKey = "YOUR_CREDITCHEK_PUBLIC_KEY",
            sessionId = backendSessionId, // Obtained from your backend
            environment = ApprovalEnv.SANDBOX, // or ApprovalEnv.PRODUCTION
            userData = AUserData(
                firstName = "John",
                lastName = "Doe",
                email = "john.doe@example.com",
                bvn = "12345678901",
                phone = "+2348012345678"
            )
        )
        approvalLauncher.launch(config)
    }) {
        Text("Start Verification")
    }
}
```

### B. Traditional Activity (Kotlin / Java)

```kotlin
import com.creditchek.approval_android.CreditChekApproval
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.SessionResult

CreditChekApproval.start(
    context = this,
    config = ApprovalConfig(
        publicKey = "YOUR_CREDITCHEK_PUBLIC_KEY",
        sessionId = backendSessionId, // Obtained from your backend
        environment = ApprovalEnv.SANDBOX
    )
) { result ->
    when (result) {
        is SessionResult.Success -> {
            println("Verification Succeeded! Session: ${result.sessionId}")
        }
        is SessionResult.Cancelled -> {
            println("Cancelled")
        }
        is SessionResult.Error -> {
            println("Error: ${result.message}")
        }
    }
}
```

---

## 🚀 Publishing & Distribution (GitHub & JitPack)

This library is published via JitPack from the GitHub repository `https://github.com/creditcliq/approval_android`.

### How to Release a New Version:
1. Update `version` in `approval_android/build.gradle.kts`:
   ```kotlin
   version = "1.0.1" // or new version / tag
   ```
2. Commit and push your changes to GitHub:
   ```bash
   git add .
   git commit -m "Release v1.0.1"
   git push origin main
   ```
3. Tag the release and push:
   ```bash
   git tag 1.0.1
   git push origin 1.0.1
   ```
4. Verify the build at [https://jitpack.io/#creditcliq/approval_android](https://jitpack.io/#creditcliq/approval_android).
5. Consumers (including `approval_flutter`) update their dependency:
   ```kotlin
   implementation("com.github.creditcliq:approval_android:1.0.1")
   ```

For local testing workflows, see **[ANDROID_FLUTTER_GUIDE.md](../approval_flutter/ANDROID_FLUTTER_GUIDE.md)**.
