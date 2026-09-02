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

// ── 1. Engine UI State ────────────────────────────────────────────────
data class LivelinessState(
    val isCameraReady: Boolean = false,
    val isFaceAligned: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentStep: FaceVerificationStep = FaceVerificationStep.STILLNESS,
    val completedStepCount: Int = 0,
    val verificationProgress: Float = 0f,
    val guidance: String = "Starting camera...",
    val isVerificationComplete: Boolean = false,
    val serverVerificationPassed: Boolean = false
)

// ── 2. Real-Time Challenge Camera Engine ──────────────────────────────
class LivelinessCameraEngine(
    private val coroutineScope: CoroutineScope,
    private val onStepCapture: (suspend (FaceChallengeCapture) -> Result<ValidationData>)? = null,
    private val onStateChanged: (LivelinessState) -> Unit
) {
    private var state = LivelinessState()
        set(value) {
            field = value
            onStateChanged(value)
        }

    private var previewViewRef: PreviewView? = null
    private val verificationSteps = FaceVerificationStep.values()

    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.20f)
            .enableTracking()
            .build()
        FaceDetection.getClient(options)
    }

    private var completedStepCount = 0
    private var matchingFrames = 0
    private var stillnessStartedAt: Long? = null
    private val stillnessDurationMs = 3000L
    private var blinkSawClosedEyes = false
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

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                evaluateFaces(faces)
            }
            .addOnFailureListener { e ->
                Log.w("LivelinessEngine", "Face detection error: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun evaluateFaces(faces: List<Face>) {
        if (faces.isEmpty()) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                verificationProgress = calculateProgress(0f),
                guidance = "No face detected"
            )
            return
        }

        if (faces.size > 1) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                verificationProgress = calculateProgress(0f),
                guidance = "Only one face should be visible"
            )
            return
        }

        val face = faces.first()
        val isCentred = face.boundingBox.width() > 80 && face.boundingBox.height() > 100
        if (!isCentred) {
            resetTracking()
            state = state.copy(
                isFaceAligned = false,
                verificationProgress = calculateProgress(0f),
                guidance = "Center your face in the frame"
            )
            return
        }

        if (state.isVerificationComplete) return

        val step = verificationSteps[completedStepCount]
        val now = System.currentTimeMillis()

        // ── Evaluate Exact Challenge Gate matching Flutter ──
        val matched = when (step) {
            FaceVerificationStep.STILLNESS -> {
                val yaw = Math.abs(face.headEulerAngleY)
                val pitch = Math.abs(face.headEulerAngleX)
                val roll = Math.abs(face.headEulerAngleZ)
                yaw < 8f && pitch < 8f && roll < 8f
            }
            FaceVerificationStep.LOOK_LEFT -> face.headEulerAngleY < -15f
            FaceVerificationStep.LOOK_RIGHT -> face.headEulerAngleY > 15f
            FaceVerificationStep.LOOK_UP -> face.headEulerAngleX > 9f
            FaceVerificationStep.LOOK_DOWN -> face.headEulerAngleX < -9f
            FaceVerificationStep.BLINK_EYES -> {
                val left = face.leftEyeOpenProbability ?: 1f
                val right = face.rightEyeOpenProbability ?: 1f
                if (left < 0.25f && right < 0.25f) {
                    blinkSawClosedEyes = true
                }
                blinkSawClosedEyes && (left > 0.65f && right > 0.65f)
            }
            FaceVerificationStep.SMILE -> (face.smilingProbability ?: 0f) >= 0.65f
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
            state = state.copy(
                isFaceAligned = true,
                currentStepIndex = completedStepCount,
                currentStep = step,
                verificationProgress = calculateProgress(stepProgress),
                guidance = "Step ${completedStepCount + 1} of ${verificationSteps.size}: ${step.instruction}"
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

        // 🌐 2. Send request to /liveness/verify-challenge immediately in background
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