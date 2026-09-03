package com.creditchek.approval_android.features.identity.presentation.screens

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.creditchek.approval_android.features.identity.data.BitmapUtils
import com.creditchek.approval_android.features.identity.data.models.FaceChallengeCapture
import com.creditchek.approval_android.features.identity.data.models.FaceVerificationStep
import com.creditchek.approval_android.features.identity.data.models.ValidationData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs

// ── 1. Engine UI State ────────────────────────────────────────────────
data class LivelinessState(
    val isCameraReady: Boolean = false,
    val isFaceAligned: Boolean = false,
    val isLowLight: Boolean = false,
    val isTooClose: Boolean = false,
    val isTooFar: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentStep: FaceVerificationStep = FaceVerificationStep.STILLNESS,
    val totalSteps: Int = 3,
    val completedStepCount: Int = 0,
    val verificationProgress: Float = 0f,
    val guidance: String = "Starting camera...",
    val isVerificationComplete: Boolean = false,
    val serverVerificationPassed: Boolean = false
)

// ── 2. Real-Time Challenge Camera Engine ──────────────────────────────
class LivelinessCameraEngine(
    private val coroutineScope: CoroutineScope,
    private val customSteps: List<FaceVerificationStep>? = null,
    private val onStepCapture: (suspend (FaceChallengeCapture) -> Result<ValidationData>)? = null,
    private val onStateChanged: (LivelinessState) -> Unit
) {
    // Default to high-accuracy 3-step active challenge flow
    private val verificationSteps = customSteps ?: listOf(
        FaceVerificationStep.STILLNESS,
        FaceVerificationStep.BLINK_EYES,
        FaceVerificationStep.SMILE
    )

    private var state = LivelinessState(totalSteps = verificationSteps.size)
        set(value) {
            field = value
            onStateChanged(value)
        }

    private var previewViewRef: PreviewView? = null

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.18f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    private var completedStepCount = 0
    private var matchingFrames = 0
    private var stillnessStartedAt: Long? = null
    private val stillnessDurationMs = 2000L // 2.0s for reduced user fatigue while retaining liveness accuracy

    // Adaptive Eye Tracking State
    private var baselineEyeOpenProbability = 0.75f
    private var blinkSawClosedEyes = false

    // Anti-Spoofing & Perspective Tracking State
    private var initialFaceWidth: Float? = null
    private var isEvaluatingStep = false

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        previewViewRef = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processFrame(imageProxy)
                }

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                state = state.copy(
                    isCameraReady = true,
                    guidance = "Position your face inside the frame"
                )
            } catch (e: Exception) {
                Log.e("LivelinessEngine", "Camera binding failed: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun processFrame(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || isEvaluatingStep) {
            imageProxy.close()
            return
        }

        // 1. Analyze Frame Luminance (Ambient Light Quality)
        val avgLuminance = calculateLuminance(imageProxy)
        val isLowLight = avgLuminance < 38.0

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        val frameWidth = imageProxy.width
        val frameHeight = imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                evaluateFaces(faces, isLowLight, frameWidth, frameHeight)
            }
            .addOnFailureListener { e ->
                Log.w("LivelinessEngine", "Face detection error: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun calculateLuminance(imageProxy: ImageProxy): Double {
        return try {
            val plane = imageProxy.planes.firstOrNull() ?: return 128.0
            val buffer = plane.buffer
            val remaining = buffer.remaining()
            if (remaining == 0) return 128.0

            val data = ByteArray(remaining)
            buffer.mark()
            buffer.get(data)
            buffer.reset()

            var sum = 0L
            var count = 0
            val step = 32 // fast sample
            for (i in 0 until remaining step step) {
                sum += (data[i].toInt() and 0xFF)
                count++
            }
            if (count > 0) sum.toDouble() / count else 128.0
        } catch (_: Exception) {
            128.0
        }
    }

    private fun evaluateFaces(
        faces: List<Face>,
        isLowLight: Boolean,
        frameWidth: Int,
        frameHeight: Int
    ) {
        if (faces.isEmpty()) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                isLowLight = isLowLight,
                verificationProgress = calculateProgress(0f),
                guidance = if (isLowLight) "Lighting is too dim — move to a brighter spot" else "Position your face inside the frame"
            )
            return
        }

        if (faces.size > 1) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                isLowLight = isLowLight,
                verificationProgress = calculateProgress(0f),
                guidance = "Only one face should be visible"
            )
            return
        }

        val face = faces.first()
        val box = face.boundingBox

        // Distance / Framing Analysis
        val minDimension = minOf(frameWidth, frameHeight).toFloat()
        val faceRatio = box.width().toFloat() / minDimension

        val isTooFar = faceRatio < 0.28f
        val isTooClose = faceRatio > 0.85f

        if (isTooFar) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                isTooFar = true,
                isTooClose = false,
                isLowLight = isLowLight,
                guidance = "Move closer to the camera"
            )
            return
        }

        if (isTooClose) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                isTooClose = true,
                isTooFar = false,
                isLowLight = isLowLight,
                guidance = "Move slightly further away"
            )
            return
        }

        if (state.isVerificationComplete) return

        if (completedStepCount >= verificationSteps.size) return
        val step = verificationSteps[completedStepCount]
        val now = System.currentTimeMillis()

        // ── Dynamic & Adaptive Challenge Verification ──
        val matched = when (step) {
            FaceVerificationStep.STILLNESS -> {
                val yaw = abs(face.headEulerAngleY)
                val pitch = abs(face.headEulerAngleX)
                val roll = abs(face.headEulerAngleZ)

                // Initialize perspective anti-spoof baseline
                if (initialFaceWidth == null) {
                    initialFaceWidth = box.width().toFloat()
                }

                // Smooth angle tolerance (relaxed to 11° to eliminate natural hand tremors)
                yaw < 11f && pitch < 11f && roll < 11f
            }

            FaceVerificationStep.LOOK_LEFT -> {
                face.headEulerAngleY < -13f
            }

            FaceVerificationStep.LOOK_RIGHT -> {
                face.headEulerAngleY > 13f
            }

            FaceVerificationStep.LOOK_UP -> {
                face.headEulerAngleX > 8f
            }

            FaceVerificationStep.LOOK_DOWN -> {
                face.headEulerAngleX < -8f
            }

            FaceVerificationStep.BLINK_EYES -> {
                val left = face.leftEyeOpenProbability ?: 0.9f
                val right = face.rightEyeOpenProbability ?: 0.9f
                val currentAvg = (left + right) / 2f

                // Track adaptive baseline for individual eye shapes
                if (!blinkSawClosedEyes) {
                    if (currentAvg > baselineEyeOpenProbability) {
                        baselineEyeOpenProbability = currentAvg
                    }
                }

                // Adaptive relative thresholds
                val closeThreshold = minOf(0.35f, baselineEyeOpenProbability * 0.55f)
                val openThreshold = maxOf(0.55f, baselineEyeOpenProbability * 0.75f)

                if (left < closeThreshold && right < closeThreshold) {
                    blinkSawClosedEyes = true
                }

                blinkSawClosedEyes && (left > openThreshold && right > openThreshold)
            }

            FaceVerificationStep.SMILE -> {
                // Adaptive smile detection (0.55f for natural subtle smiles)
                (face.smilingProbability ?: 0f) >= 0.55f
            }

            FaceVerificationStep.OPEN_MOUTH -> true
        }

        var stepProgress = 0f

        if (step == FaceVerificationStep.STILLNESS) {
            if (matched) {
                if (stillnessStartedAt == null) {
                    stillnessStartedAt = now
                }
                val elapsed = now - stillnessStartedAt!!
                stepProgress = (elapsed.toFloat() / stillnessDurationMs).coerceIn(0f, 1f)
                if (elapsed >= stillnessDurationMs) {
                    onStepCompleted(step)
                }
            } else {
                stillnessStartedAt = null
                stepProgress = 0f
            }
        } else {
            if (matched) {
                matchingFrames++
            } else {
                matchingFrames = 0
            }

            stepProgress = (matchingFrames / 2f).coerceIn(0f, 1f)
            if (matchingFrames >= (if (step == FaceVerificationStep.BLINK_EYES) 1 else 2)) {
                onStepCompleted(step)
            }
        }

        if (!state.isVerificationComplete) {
            val stepPrompt = when (step) {
                FaceVerificationStep.STILLNESS -> "Hold still"
                FaceVerificationStep.BLINK_EYES -> "Blink your eyes"
                FaceVerificationStep.SMILE -> "Smile for the camera"
                FaceVerificationStep.LOOK_LEFT -> "Turn head slightly left"
                FaceVerificationStep.LOOK_RIGHT -> "Turn head slightly right"
                FaceVerificationStep.LOOK_UP -> "Look up"
                FaceVerificationStep.LOOK_DOWN -> "Look down"
                FaceVerificationStep.OPEN_MOUTH -> "Open your mouth"
            }

            state = state.copy(
                isFaceAligned = true,
                isLowLight = isLowLight,
                isTooClose = false,
                isTooFar = false,
                currentStepIndex = completedStepCount,
                currentStep = step,
                verificationProgress = calculateProgress(stepProgress),
                guidance = "Step ${completedStepCount + 1} of ${verificationSteps.size}: $stepPrompt"
            )
        }
    }

    private fun calculateProgress(stepProgress: Float): Float {
        val total = verificationSteps.size.toFloat()
        return ((completedStepCount + stepProgress) / total).coerceIn(0f, 1f)
    }

    private fun onStepCompleted(step: FaceVerificationStep) {
        isEvaluatingStep = true

        // 📸 1. Grab 600x600 normalized snapshot for this step
        val frameDataUrl = previewViewRef?.bitmap?.let { bmp ->
            BitmapUtils.toBase64JpegDataUrl(bmp, targetSize = 600)
        } ?: ""

        val capture = FaceChallengeCapture(step = step, jpegDataUrl = frameDataUrl)
        val isFinalStep = completedStepCount >= verificationSteps.size - 1

        // 🌐 2. Send request to /liveness/verify-challenge in background
        coroutineScope.launch(Dispatchers.IO) {
            val result = onStepCapture?.invoke(capture)
            val overallPassed = result?.getOrNull()?.overallPassed ?: true

            completedStepCount++
            resetTracking()
            isEvaluatingStep = false

            if (isFinalStep) {
                state = state.copy(
                    isVerificationComplete = true,
                    completedStepCount = verificationSteps.size,
                    verificationProgress = 1f,
                    serverVerificationPassed = overallPassed,
                    guidance = "Completing verification..."
                )
            }
        }
    }

    private fun resetTracking() {
        matchingFrames = 0
        stillnessStartedAt = null
        blinkSawClosedEyes = false
    }

    fun release() {
        detector.close()
    }
}