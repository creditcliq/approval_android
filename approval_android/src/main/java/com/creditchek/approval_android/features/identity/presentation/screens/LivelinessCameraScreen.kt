package com.creditchek.approval_android.features.identity.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.creditchek.approval_android.core.engines.ApprovalTtsEngine
import com.creditchek.approval_android.core.engines.LivelinessCameraEngine
import com.creditchek.approval_android.core.engines.LivelinessState
import com.creditchek.approval_android.core.network.NetworkQuality
import com.creditchek.approval_android.core.shared.components.PoweredByCreditChek
import com.creditchek.approval_android.core.theme.*
import com.creditchek.approval_android.features.identity.data.models.FaceChallengeCapture
import com.creditchek.approval_android.features.identity.data.models.ValidationData
import com.creditchek.approval_android.features.identity.presentation.components.ApprovalHeader
import com.creditchek.approval_android.features.identity.presentation.components.NetworkStatusPill
import com.creditchek.approval_android.features.identity.presentation.components.drawFaceOvalBorder

@Composable
fun LivelinessCameraScreen(
    networkQuality: NetworkQuality = NetworkQuality.MODERATE,
    onDismiss: () -> Unit = {},
    onStepCapture: (suspend (FaceChallengeCapture) -> Result<ValidationData>)? = null,
    onVerificationComplete: (Boolean) -> Unit = {}
) {

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var livenessState by remember { mutableStateOf(LivelinessState()) }

    // 👈 1. Native Audio Guidance Engine
    val ttsEngine = remember {
        ApprovalTtsEngine(context)
    }


    val engine = remember {
        LivelinessCameraEngine(
            coroutineScope = coroutineScope,
            onStepCapture = onStepCapture,
            onStateChanged = { newState ->
                livenessState = newState
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            engine.release()
            ttsEngine.release()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 👈 2. Voice Prompts triggered on step/guidance change
    LaunchedEffect(livenessState.guidance, livenessState.isFaceAligned) {
        if (livenessState.isFaceAligned) {
            ttsEngine.speak(livenessState.guidance)
        }
    }

    LaunchedEffect(livenessState.isVerificationComplete) {
        if (livenessState.isVerificationComplete) {
            onVerificationComplete(livenessState.serverVerificationPassed)
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ApprovalCanvas)
    ) {
        // 1. Top Header
        ApprovalHeader(
            title = "Approval Verification",
            onClose = onDismiss
        )

        // 2. Camera Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NetworkStatusPill(quality = networkQuality)

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================
            // 📸 OVAL CAMERA FRAME (280 × 380 dp)
            // =========================================================
            Box(
                modifier = Modifier
                    .width(280.dp)
                    .height(380.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!hasCameraPermission) {
                    CameraPermissionDenied()
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(FaceOvalShape)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    engine.startCamera(ctx, lifecycleOwner, this)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (!livenessState.isCameraReady) {
                            CameraLoading()
                        }
                    }

                    // Progress Ring Sweeping around the Oval Frame
                    val ringColor = when {
                        !livenessState.isFaceAligned -> ApprovalBlue
                        livenessState.isLowLight || livenessState.isTooClose || livenessState.isTooFar -> ApprovalWarning
                        else -> ApprovalSuccess
                    }
                    val bgGhost = if (livenessState.isFaceAligned) ringColor.copy(alpha = 0.20f) else null
                    val currentProgress = if (livenessState.isFaceAligned) livenessState.verificationProgress else 1f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawFaceOvalBorder(
                            color = ringColor,
                            strokeWidth = 12f,
                            backgroundColor = bgGhost,
                            progress = currentProgress
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(50.dp))

            // 3. Low Light / Distance / Eye Contact Banner
            if (livenessState.isLowLight) {
                LowLightWarning()
            } else {
                EyeContactWarning()
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Dynamic Guidance Instruction Text
            val guidanceColor = when {
                !livenessState.isFaceAligned -> ApprovalTextSecondary
                livenessState.isLowLight || livenessState.isTooClose || livenessState.isTooFar -> ApprovalWarning
                else -> ApprovalBlue
            }

            Text(
                text = livenessState.guidance,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = guidanceColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.weight(0.5f))

            PoweredByCreditChek()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LowLightWarning() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = ApprovalWarning,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Environment is dim — ensure your face is well-lit",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8A5200),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EyeContactWarning() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFB26A00),
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Keep your eyes on the camera throughout each step",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8A5200),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CameraLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = ApprovalBlue,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "Starting camera...",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CameraPermissionDenied() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(FaceOvalShape)
            .background(Color(0xFF050505)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "Camera permission is required\nfor face verification",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LivelinessCameraScreenPreview() {
    ApprovalTheme {
        LivelinessCameraScreen()
    }
}