# CreditChek Approval Mobile SDK — Integration Guide

> **Official Mobile SDK Documentation**: Fast, secure, and seamless identity & biometric verification for **Android**, **iOS**, **Flutter**, and **React Native**.

---

## 1. Overview

The **CreditChek Approval Mobile SDK** allows you to verify customer identities in minutes directly within your mobile applications. 

### How It Works:
```
┌─────────────────────────┐      ┌─────────────────────────┐      ┌─────────────────────────┐
│ 1. Merchant Backend     │ ──▶  │ 2. Mobile SDK Flow      │ ──▶  │ 3. Verification Result  │
│ Calls CreditChek API:   │      │ (Host App launches SDK) │      │ Host App & Backend:     │
│ POST /widget-session/   │      │ • Validates Session ID  │      │ • Verified Session ID   │
│ └── Returns: sessionId  │      │ • BVN & Active Liveness │      │ • Status: Passed/Failed │
└─────────────────────────┘      └─────────────────────────┘      └─────────────────────────┘
```

1. **Session Creation (Backend)**: Your backend calls CreditChek's Widget Session API (`POST /v1/auth/widget-session/create`) with your secret/public key and desired services (`["bvn", "liveness"]`) to generate a unique `sessionId`.
2. **Launch Verification (Mobile SDK)**: Your mobile app receives the `sessionId` from your backend and initializes the SDK using `ApprovalConfig(publicKey: "...", sessionId: "...")`.
3. **Identity & Liveness**: The user verifies their BVN demographics and completes interactive facial liveness gestures.
4. **Instant Result**: The SDK returns the completed `sessionId` to your application and updates your backend via webhooks.

---

## 2. Platform Setup & Integration

Select your mobile stack below:

---

### 🤖 Android (Kotlin / Jetpack Compose & XML)

#### Requirements:
- **Min SDK:** 24 (Android 7.0+)
- **Compile SDK:** 34+
- **Kotlin:** 1.9+ / 2.0+

#### 1. Add Repository & Dependency
In your `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

In your `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.creditcliq:approval_android:1.0.0+1")
}
```

#### 2. Permissions (`AndroidManifest.xml`)
Ensure your app declares Camera and Internet access:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

#### 3. Launch the SDK

##### In Jetpack Compose (Recommended):
```kotlin
import com.creditchek.approval_android.CreditChekApproval
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.SessionResult

@Composable
fun VerificationScreen(backendSessionId: String) {
    val approvalLauncher = rememberLauncherForActivityResult(CreditChekApproval.contract()) { result ->
        when (result) {
            is SessionResult.Success -> {
                // Verification passed! Use sessionId to confirm with your backend
                println("Verified Session ID: ${result.sessionId}")
            }
            is SessionResult.Cancelled -> {
                println("User dismissed verification")
            }
            is SessionResult.Error -> {
                println("Verification failed (${result.code}): ${result.message}")
            }
        }
    }

    Button(onClick = {
        val config = ApprovalConfig(
            publicKey = "YOUR_PUBLIC_KEY",
            sessionId = backendSessionId, // Obtained from your backend
            environment = ApprovalEnv.SANDBOX // Use ApprovalEnv.PRODUCTION in release
        )
        approvalLauncher.launch(config)
    }) {
        Text("Verify Identity")
    }
}
```

##### In Traditional Activity (Java / Kotlin):
```kotlin
CreditChekApproval.start(
    context = this,
    config = ApprovalConfig(
        publicKey = "YOUR_PUBLIC_KEY",
        sessionId = backendSessionId, // Obtained from your backend
        environment = ApprovalEnv.SANDBOX
    )
) { result ->
    when (result) {
        is SessionResult.Success -> handleSuccess(result.sessionId)
        is SessionResult.Cancelled -> handleDismiss()
        is SessionResult.Error -> handleError(result.message)
    }
}
```

---

### 🍏 iOS (Swift & SwiftUI)

#### Requirements:
- **iOS Deployment Target:** iOS 15.0+
- **Swift:** 5.9+
- **Xcode:** 15.0+

#### 1. Add Swift Package
In Xcode: **File** ➔ **Add Package Dependencies...** ➔ Paste repository URL:
```
https://github.com/creditcliq/approval_ios_init.git
```

#### 2. Permissions (`Info.plist`)
Add the Camera usage key with a clear explanation:
```xml
<key>NSCameraUsageDescription</key>
<string>We need access to your camera to verify your identity with live face detection.</string>
```

#### 3. Launch the SDK in SwiftUI:
```swift
import SwiftUI
import approval_ios

struct VerificationView: View {
    @State private var isPresentingApproval = false
    let backendSessionId: String // Generated by your backend

    var body: some View {
        Button("Verify Identity") {
            isPresentingApproval = true
        }
        .sheet(isPresented: $isPresentingApproval) {
            ApprovalView(
                config: ApprovalConfig(
                    publicKey: "YOUR_PUBLIC_KEY",
                    sessionId: backendSessionId, // Obtained from your backend
                    environment: .sandbox
                )
            ) { result in
                switch result {
                case .success(let sessionId, let message):
                    print("Verification Complete: \(sessionId), \(message)")
                case .cancelled:
                    print("User dismissed verification")
                case .error(let code, let message):
                    print("Error (\(code)): \(message)")
                }
            }
        }
    }
}
```

---

### 💙 Flutter (Dart)

#### Requirements:
- **Flutter:** 3.16+
- **Dart:** 3.0+

#### 1. Add Dependency
In your `pubspec.yaml`:
```yaml
dependencies:
  approval_flutter:
    git:
      url: https://github.com/creditcliq/approval_flutter.git
```

#### 2. Launch the SDK:
```dart
import 'package:flutter/material.dart';
import 'package:approval_flutter/approval_flutter.dart';

void startVerification(BuildContext context, String backendSessionId) async {
  final result = await ApprovalFlutter.start(
    context,
    config: ApprovalConfig(
      publicKey: 'YOUR_PUBLIC_KEY',
      sessionId: backendSessionId, // Obtained from your backend
      environment: ApprovalEnvironment.sandbox,
    ),
  );

  if (result is SessionResultSuccess) {
    print('Verified Session ID: ${result.sessionId}');
  } else if (result is SessionResultCancelled) {
    print('User cancelled verification');
  } else if (result is SessionResultError) {
    print('Error: ${result.message}');
  }
}
```

---

### ⚛️ React Native (TypeScript / JavaScript)

#### Requirements:
- **React Native:** 0.70+
- **iOS:** 15.0+ / **Android:** API 24+

#### 1. Install Package
```bash
npm install @creditchek/approval-react-native
# or
yarn add @creditchek/approval-react-native
```

#### 2. iOS CocoaPods Setup
```bash
cd ios && pod install && cd ..
```

#### 3. Launch the SDK:
```tsx
import React from 'react';
import { Button, View, Alert } from 'react-native';
import { CreditChekApproval, Environment } from '@creditchek/approval-react-native';

export default function VerificationScreen({ backendSessionId }: { backendSessionId: string }) {
  const handleStartVerification = async () => {
    try {
      const result = await CreditChekApproval.start({
        publicKey: 'YOUR_PUBLIC_KEY',
        sessionId: backendSessionId, // Obtained from your backend
        environment: Environment.SANDBOX, // Use Environment.PRODUCTION in release
      });

      if (result.status === 'success') {
        Alert.alert('Verification Successful', `Session ID: ${result.sessionId}`);
      } else if (result.status === 'cancelled') {
        console.log('User dismissed verification');
      }
    } catch (error: any) {
      Alert.alert('Verification Error', error.message || 'An error occurred');
    }
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Button title="Verify Identity" onPress={handleStartVerification} />
    </View>
  );
}
```

---

## 3. Configuration Reference (`ApprovalConfig`)

| Property | Type | Default | Description |
| :--- | :--- | :---: | :--- |
| **`publicKey`** *(Required)* | `String` | — | Your public API key from the CreditChek Dashboard. |
| **`sessionId`** *(Required)* | `String` | — | Unique session UUID created by your backend via `/v1/auth/widget-session/create`. |
| **`environment`** | `Enum / String` | `SANDBOX` | Set to `.PRODUCTION` when going live. |
| **`modules`** *(Optional)* | `List<ApprovalModule>` | `[.identity]` | Modules to execute (`identity`, `liveliness`). |
| **`userData`** *(Optional)* | `AUserData?` | `null` | Optional pre-fill data (first name, last name, BVN, date of birth, email, phone). |

---

## 4. Result Handling (`SessionResult`)

When the verification flow concludes, the SDK returns one of three outcomes:

| Result | Parameters | Description |
| :--- | :--- | :--- |
| **`Success`** | `sessionId: String` | Identity and face liveness were verified successfully. Pass `sessionId` to your backend to confirm verification status. |
| **`Cancelled`** | — | The user closed or dismissed the verification sheet before completion. |
| **`Error`** | `code: String, message: String` | The session failed due to network errors or invalid credentials. |

---

## 5. Security & Privacy

- **On-Device Face Alignment**: Biometric gestures are detected locally using hardware acceleration.
- **Encrypted Transmission**: All data is securely transferred over TLS 1.3 encryption.
- **Zero Local Biometric Storage**: Photo frames are strictly held in-memory during verification and immediately discarded upon completion.

---

## 6. Support & Resources

- **CreditChek Developer Portal**: [https://developer.creditchek.africa](https://developer.creditchek.africa)
- **API Dashboard**: [https://dashboard.creditchek.africa](https://dashboard.creditchek.africa)
- **Email Support**: support@creditchek.africa
