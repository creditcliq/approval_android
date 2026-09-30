<p align="center">
    <img title="CreditChek" height="200" src="https://docs.creditchek.africa/img/nav_logo.svg" width="50%"/>
</p>

# CreditChek Approval Android SDK

Add CreditChek identity verification to your Android app. With a few lines of code your customers can:

- verify their identity with a **BVN** or **NIN**
- complete an **active face liveness** check, matched against their BVN or NIN photo

The SDK presents native verification screens built with Jetpack Compose and CameraX, returns instant callback results, and guides the customer seamlessly through the process.

---

## Contents

1. [How integration works](#how-integration-works)
2. [Before you start](#before-you-start)
3. [Requirements](#requirements)
4. [Installation](#installation)
5. [Step 1: Create a session on your server](#step-1-create-a-session-on-your-server)
6. [Step 2: Launch the SDK](#step-2-launch-the-sdk)
7. [Step 3: Know when the customer is done](#step-3-know-when-the-customer-is-done)
8. [Step 4: Confirm the result on your server](#step-4-confirm-the-result-on-your-server)
9. [Complete example](#complete-example)
10. [API reference](#api-reference)
11. [Modules & Document Selection](#modules--document-selection)
12. [Testing](#testing)
13. [Security checklist](#security-checklist)
14. [Publishing and distribution](#publishing-and-distribution)

---

## How integration works

Every verification takes four steps. Two happen on your server and two in your Android app:

| # | Where | What happens |
|---|---|---|
| 1 | **Your server** | Creates a **widget session** with your **secret key**, and returns its `sessionId` to your app |
| 2 | **Your Android app** | Launches the SDK with your **public key** and the `sessionId` |
| 3 | **Your Android app** | The customer completes identity & liveness steps. The SDK returns the `SessionResult` callback |
| 4 | **Your server** | Reads the session with your secret key to confirm what the customer completed |

```
 Customer        Your Android app          Your server             CreditChek
    │  click "Verify"  │                        │                        │
    │─────────────────▶│  POST /api/session     │                        │
    │                  │───────────────────────▶│  1. create session     │
    │                  │                        │───────────────────────▶│
    │                  │       sessionId        │◀───────────────────────│
    │                  │◀───────────────────────│                        │
    │  2. SDK launches (publicKey + sessionId)  │                        │
    │◀─────────────────│                        │                        │
    │  completes steps │  3. onResult callback  │                        │
    │─────────────────▶│  GET /api/session/:id  │                        │
    │                  │───────────────────────▶│  4. read session       │
    │                  │                        │───────────────────────▶│
    │                  │   verified: true/false │◀───────────────────────│
    │                  │◀───────────────────────│                        │
```

**Why a server?** Creating and reading sessions needs your secret key, and the secret key must never reach the client application. This SDK runs only on the device and never touches your secret key.

---

## Before you start

**Keys.** Your keys are on the [CreditChek B2B dashboard](https://app.creditchek.africa/), in the **App** section. Each app has a live and a test pair:

| Key | Where it's used | Keep it secret? |
|---|---|---|
| **Secret key** | Your server only: creating and reading sessions | **Yes.** Never put it in Android code, strings.xml, or BuildConfig |
| **Public key** | Your Android app, passed to `ApprovalConfig` | No, it identifies your business |

Always use a secret key and a public key **from the same app and the same pair** (both live, or both test).

**API base URL** (your server): `https://api.creditchek.africa/v1`

---

## Requirements

- **Minimum SDK**: 24 (Android 7.0+)
- **Compile SDK**: 34+
- **Java / JVM**: Version 17
- **Kotlin**: 1.9+ / 2.0+

---

## Installation

### 1. Add JitPack Repository
In your root `settings.gradle.kts` (or root `build.gradle.kts`):

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
    implementation("com.github.creditcliq:approval_android:1.0.3")
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

## Step 1: Create a session on your server

Each verification needs its own widget session. The session records which services the customer must complete, and tracks their progress. Create it when the customer clicks to start: sessions are short-lived.

```http
POST https://api.creditchek.africa/v1/auth/widget-session/create
Content-Type: application/json

{
  "publicKey": "YOUR_PUBLIC_KEY",
  "services": ["nin", "liveness"]
}
```

```bash
curl -X POST https://api.creditchek.africa/v1/auth/widget-session/create \
  -H "Content-Type: application/json" \
  -d '{
    "publicKey": "YOUR_PUBLIC_KEY",
    "services": ["nin", "liveness"]
  }'
```

| Field | Type | Description |
|---|---|---|
| `publicKey` | `string` | **required** | Your CreditChek public API key |
| `services` | `("bvn" \| "nin" \| "liveness")[]` | Services for this verification. Defaults to `["bvn"]` |
| `sessionId` | `string` | Optional. Your own unique ID for the session |

> **Document Selection**: 
> - If `services` contains both `"bvn"` and `"nin"`, the customer will be presented with a document selection screen to choose their preferred ID.
> - If `services` contains only `"nin"`, the SDK opens directly to NIN verification.
> - If `services` contains only `"bvn"`, the SDK opens directly to BVN verification.

**Server Example** (Node.js / Express):

```js
app.post("/api/verification/session", async (req, res) => {
  try {
    const response = await fetch("https://api.creditchek.africa/v1/auth/widget-session/create", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        publicKey: process.env.CREDITCHEK_PUBLIC_KEY,
        services: ["nin", "liveness"],
      }),
    });
    const { data } = await response.json();
    res.json({ sessionId: data.sessionId });
  } catch (error) {
    res.status(502).json({ error: "Could not create verification session" });
  }
});
```

---

## Step 2: Launch the SDK

### A. Jetpack Compose (Recommended)

Use `rememberLauncherForActivityResult` with `CreditChekApproval.contract()`:

```kotlin
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import com.creditchek.approval_android.CreditChekApproval
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.ApprovalModule
import com.creditchek.approval_android.core.session.AUserData
import com.creditchek.approval_android.core.session.SessionResult

@Composable
fun VerificationScreen(backendSessionId: String) {
    val approvalLauncher = rememberLauncherForActivityResult(
        contract = CreditChekApproval.contract()
    ) { result ->
        when (result) {
            is SessionResult.Success -> {
                println("Verification Completed! Session: ${result.sessionId}")
                // Step 4: Confirm result with your server
            }
            is SessionResult.Cancelled -> {
                println("User dismissed verification")
            }
            is SessionResult.Error -> {
                println("Error [${result.code}]: ${result.message}")
            }
        }
    }

    Button(onClick = {
        val config = ApprovalConfig(
            publicKey = "YOUR_CREDITCHEK_PUBLIC_KEY",
            sessionId = backendSessionId,
            environment = ApprovalEnv.DEVELOPMENT, // Use .PRODUCTION for release
            modules = listOf(ApprovalModule.IDENTITY, ApprovalModule.LIVELINESS),
            userData = AUserData(
                firstName = "John",
                lastName = "Doe",
                bvn = "12345678901",
                nin = "12345678901"
            )
        )
        approvalLauncher.launch(config)
    }) {
        Text("Verify Identity")
    }
}
```

### B. Traditional Android Activity (Kotlin / Java)

```kotlin
import com.creditchek.approval_android.CreditChekApproval
import com.creditchek.approval_android.core.session.ApprovalConfig
import com.creditchek.approval_android.core.session.ApprovalEnv
import com.creditchek.approval_android.core.session.SessionResult

CreditChekApproval.start(
    context = this,
    config = ApprovalConfig(
        publicKey = "YOUR_CREDITCHEK_PUBLIC_KEY",
        sessionId = backendSessionId,
        environment = ApprovalEnv.DEVELOPMENT
    )
) { result ->
    when (result) {
        is SessionResult.Success -> {
            println("Verification complete! Session: ${result.sessionId}")
        }
        is SessionResult.Cancelled -> {
            println("Cancelled by user")
        }
        is SessionResult.Error -> {
            println("Error [${result.code}]: ${result.message}")
        }
    }
}
```

---

## Step 3: Know when the customer is done

The SDK callback gives you a `SessionResult`:

| Result | Meaning |
|---|---|
| `SessionResult.Success(sessionId)` | The customer finished the verification flow on the device. |
| `SessionResult.Cancelled` | The customer exited the flow before finishing. |
| `SessionResult.Error(code, message)` | An error occurred (network error, invalid configuration, etc.). |

> **Important**: The SDK finishing means "the customer completed the client flow". Always decide the final outcome on your server (Step 4).

---

## Step 4: Confirm the result on your server

The session on the server is the single source of truth:

```http
GET https://api.creditchek.africa/v1/auth/widget-session/<sessionId>
token: <your secret key>
```

```js
// server.js
app.get("/api/verification/session/:sessionId", async (req, res) => {
  try {
    const session = await creditchek(`/auth/widget-session/${encodeURIComponent(req.params.sessionId)}`);
    const services = Object.values(session.services);
    const verified = services.length > 0 && services.every((s) => s?.status === "completed");
    res.json({ verified, services: session.services });
  } catch {
    res.status(502).json({ error: "Could not read verification" });
  }
});
```

To fetch demographic details for a verified session:
- **BVN Demographic Data**: `GET https://api.creditchek.africa/v1/auth/widget-session/bvn-data/<sessionId>`
- **NIN Demographic Data**: `GET https://api.creditchek.africa/v1/auth/widget-session/nin-data/<sessionId>`

---

## Complete example

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var verificationStatus by remember { mutableStateOf("Not verified") }
            val coroutineScope = rememberCoroutineScope()

            val launcher = rememberLauncherForActivityResult(
                contract = CreditChekApproval.contract()
            ) { result ->
                when (result) {
                    is SessionResult.Success -> {
                        verificationStatus = "Checking server result..."
                        coroutineScope.launch {
                            // Check your server
                            val verified = checkServerSession(result.sessionId)
                            verificationStatus = if (verified) "You're verified!" else "Verification incomplete."
                        }
                    }
                    is SessionResult.Cancelled -> {
                        verificationStatus = "Verification cancelled."
                    }
                    is SessionResult.Error -> {
                        verificationStatus = "Error: ${result.message}"
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = verificationStatus)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        val sessionId = fetchNewSessionFromBackend()
                        val config = ApprovalConfig(
                            publicKey = "YOUR_PUBLIC_KEY",
                            sessionId = sessionId,
                            environment = ApprovalEnv.DEVELOPMENT
                        )
                        launcher.launch(config)
                    }
                }) {
                    Text("Verify my identity")
                }
            }
        }
    }
}
```

---

## API reference

### `ApprovalConfig`

| Field | Type | Default | Description |
|---|---|---|---|
| `publicKey` | `String` | **required** | Your CreditChek public key |
| `sessionId` | `String` | **required** | Session UUID generated by your server |
| `environment` | `ApprovalEnv` | `ApprovalEnv.PRODUCTION` | `DEVELOPMENT` (test mode) or `PRODUCTION` |
| `modules` | `List<ApprovalModule>` | `[IDENTITY]` | `IDENTITY`, `LIVELINESS` |
| `userData` | `AUserData?` | `null` | Optional prefill data (`firstName`, `lastName`, `bvn`, `nin`, `dob`) |

### `AUserData`

| Field | Type | Description |
|---|---|---|
| `firstName` | `String?` | First name to prefill |
| `lastName` | `String?` | Last name / surname to prefill |
| `bvn` | `String?` | 11-digit BVN |
| `nin` | `String?` | 11-digit NIN |
| `dateOfBirth` / `dob` | `String?` | Date of birth (`DD/MM/YYYY`) |
| `phone` | `String?` | Customer phone number |
| `email` | `String?` | Customer email address |

---

## Modules & Document Selection

| Module | What the customer does | What they need |
|---|---|---|
| `IDENTITY` | Enters demographic details and verifies their **BVN** or **NIN**. | Their 11-digit BVN or NIN |
| `LIVELINESS` | Follows interactive facial prompts (blink, smile, turn head) via the front camera. The capture is matched against their official BVN or NIN photo. | Front camera, adequate lighting |

- **Step Skipping**: If a step is already marked `completed` in the server session, the SDK automatically skips it.
- **Document Selection Screen**: If both BVN and NIN are configured on the session, the customer is prompted to select which document to verify with. The selection defaults to empty and the button remains disabled until a selection is made.

---

## Testing

Use `ApprovalEnv.DEVELOPMENT` during integration and testing:

```kotlin
val config = ApprovalConfig(
    publicKey = "YOUR_TEST_PUBLIC_KEY",
    sessionId = testSessionId,
    environment = ApprovalEnv.DEVELOPMENT
)
```

In `DEVELOPMENT` mode:
- A floating **"TEST MODE"** badge is displayed in the top-right corner of all screens.
- Test BVN and NIN values (`12345678901`) are automatically prefilled and locked.
- Local demographic name and date-of-birth mismatch validations are bypassed for easy testing.
- Switch to `ApprovalEnv.PRODUCTION` when building for production release.

---

## Security checklist

- [ ] The secret key lives only on your backend server.
- [ ] You create one session per verification, right when the customer starts.
- [ ] Your server confirms verification status via `GET /auth/widget-session/:sessionId`, never relying solely on client callbacks.
- [ ] In production, set `environment = ApprovalEnv.PRODUCTION`.

---

## Publishing and distribution

This library is published via JitPack from `https://github.com/creditcliq/approval_android`.

### How to Release a New Version:
1. Update `version` in `approval_android/build.gradle.kts`:
   ```kotlin
   version = "1.0.1"
   ```
2. Commit and push your changes:
   ```bash
   git add .
   git commit -m "Release v1.0.1"
   git push origin main
   ```
3. Tag the release:
   ```bash
   git tag 1.0.1
   git push origin 1.0.1
   ```
4. Verify the build at [jitpack.io/#creditcliq/approval_android](https://jitpack.io/#creditcliq/approval_android).
