package com.creditchek.approval_android.core.engines

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.creditchek.approval_android.features.liveliness.data.BitmapUtils
import com.creditchek.approval_android.features.liveliness.data.models.FaceChallengeCapture
import com.creditchek.approval_android.features.liveliness.data.models.FaceVerificationStep
import com.creditchek.approval_android.features.liveliness.data.models.ValidationData
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
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
    val totalSteps: Int = 8,
    val completedStepCount: Int = 0,
    val verificationProgress: Float = 0f,
    val guidance: String = "Starting camera...",
    val isVerificationComplete: Boolean = false,
    val serverVerificationPassed: Boolean = false
)

// ── 2. Real-Time Challenge Camera Engine ──────────────────────────────
class LivelinessCameraEngine(
    private val coroutineScope: CoroutineScope,
    customSteps: List<FaceVerificationStep>? = null,
    private val onStepCapture: (suspend (FaceChallengeCapture) -> Result<ValidationData>)? = null,
    private val onStateChanged: (LivelinessState) -> Unit
) {
    // 👈 1:1 Complete 8-step sequence matching Flutter SDK
    private val verificationSteps = customSteps ?: listOf(
        FaceVerificationStep.STILLNESS,
        FaceVerificationStep.LOOK_LEFT,
        FaceVerificationStep.LOOK_RIGHT,
        FaceVerificationStep.LOOK_UP,
        FaceVerificationStep.LOOK_DOWN,
        FaceVerificationStep.BLINK_EYES,
        FaceVerificationStep.SMILE,
        FaceVerificationStep.OPEN_MOUTH
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

    // Stillness hold tracking with 500ms jitter tolerance (Flutter parity)
    private var stillnessStartedAt: Long? = null
    private var lastStillnessMatchAt: Long = 0L
    private val stillnessDurationMs = 1200L // 1.2 seconds smooth, snappy hold

    // Motion & Rapid Rotation Tracking
    private var prevEulerX: Float? = null
    private var prevEulerY: Float? = null
    private var prevEulerZ: Float? = null

    // First turn yaw to enforce mirror-invariant opposite turn between look_left and look_right
    private var firstTurnYaw: Float? = null

    // Required consecutive steady frames for directional & expression challenges
    private val requiredStableFrames = 4 // ~130ms steady hold before capture to eliminate motion blur

    // Step Transition Grace Period (Prevents residual movement from previous challenge)
    private var stepAvailableAt: Long = 0L
    private val stepTransitionGraceMs = 500L

    // Camera warmup delay to allow auto-exposure & white balance to settle after screen brightness flash
    private var cameraReadyAt: Long = 0L
    private val cameraWarmupDelayMs = 1200L

    // Adaptive Eye Tracking & Closed-Eyelid Snapshot State
    private var baselineEyeOpenProbability = 0.75f
    private var blinkSawClosedEyes = false
    private var lowestBlinkAvg = 1.0f
    private var blinkClosedFrameDataUrl: String? = null

    // Smile Tracking & Peak Snapshot State
    private var peakSmileFrameDataUrl: String? = null
    private var bestSmileScore: Float = 0f

    // Anti-Spoofing & Perspective Tracking State
    private var initialFaceWidth: Float? = null
    private var isEvaluatingStep = false

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        this.previewViewRef = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                ImageAnalyzer(previewView.width, previewView.height)
            )

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
                cameraReadyAt = System.currentTimeMillis() + cameraWarmupDelayMs
                firstTurnYaw = null
                completedStepCount = 0
                resetTracking()
                state = state.copy(
                    isCameraReady = true,
                    guidance = "Adjusting camera lighting..."
                )
            } catch (e: Exception) {
                Log.e("LivelinessCameraEngine", "Camera binding failed", e)
                state = state.copy(guidance = "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private inner class ImageAnalyzer(
        private val viewWidth: Int,
        private val viewHeight: Int
    ) : ImageAnalysis.Analyzer {

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage == null || isEvaluatingStep) {
                imageProxy.close()
                return
            }

            // Low-light detection
            val isLowLight = checkLowLight(imageProxy)

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    processFaces(faces, isLowLight, imageProxy.width, imageProxy.height)
                }
                .addOnFailureListener { e ->
                    Log.e("LivelinessCameraEngine", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun checkLowLight(image: ImageProxy): Boolean {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        var sum = 0L
        val step = 10 // sample every 10th pixel for performance
        var samples = 0
        for (i in data.indices step step) {
            sum += (data[i].toInt() and 0xFF)
            samples++
        }
        val avgLuminance = if (samples > 0) sum / samples else 100
        return avgLuminance < 40 // Low-light threshold
    }

    private fun processFaces(
        faces: List<Face>,
        isLowLight: Boolean,
        frameWidth: Int,
        frameHeight: Int
    ) {
        val now = System.currentTimeMillis()
        if (now < cameraReadyAt) {
            state = state.copy(
                isCameraReady = true,
                isFaceAligned = false,
                guidance = "Adjusting camera lighting..."
            )
            return
        }
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

        // ── Motion & Angular Velocity Tracking ──
        val curX = face.headEulerAngleX
        val curY = face.headEulerAngleY
        val curZ = face.headEulerAngleZ

        val deltaAngle = if (prevEulerX != null && prevEulerY != null && prevEulerZ != null) {
            maxOf(
                abs(curX - prevEulerX!!),
                abs(curY - prevEulerY!!),
                abs(curZ - prevEulerZ!!)
            )
        } else 0f

        prevEulerX = curX
        prevEulerY = curY
        prevEulerZ = curZ

        // If the user's head is moving fast (> 4.0 deg/frame), they are in active motion
        val isMovingFast = deltaAngle > 4.0f

        // Wait for step transition grace period so user can read/hear instruction
        if (now < stepAvailableAt) {
            return
        }

        // ── Dynamic 8-Step Challenge Verification (Backend Axis Orientation) ──
        val matched = when (step) {
            FaceVerificationStep.STILLNESS -> {
                val yaw = abs(face.headEulerAngleY)
                val pitch = abs(face.headEulerAngleX)
                val roll = abs(face.headEulerAngleZ)
                // Natural hand-held posture tolerance and stationary face
                yaw < 15f && pitch < 18f && roll < 15f && !isMovingFast
            }

            FaceVerificationStep.LOOK_LEFT -> {
                // Backend requires magnitude >= 15.0°
                abs(face.headEulerAngleY) >= 15f
            }

            FaceVerificationStep.LOOK_RIGHT -> {
                // Must exceed 15.0° and be opposite direction to the first turn
                val yaw = face.headEulerAngleY
                abs(yaw) >= 15f && (firstTurnYaw == null || (yaw * firstTurnYaw!!) < 0)
            }

            FaceVerificationStep.LOOK_UP -> {
                face.headEulerAngleX > 9f
            }

            FaceVerificationStep.LOOK_DOWN -> {
                face.headEulerAngleX < -9f
            }

            FaceVerificationStep.BLINK_EYES -> {
                val left = face.leftEyeOpenProbability ?: 0.9f
                val right = face.rightEyeOpenProbability ?: 0.9f
                val currentAvg = (left + right) / 2f

                if (!blinkSawClosedEyes) {
                    if (currentAvg > baselineEyeOpenProbability) {
                        baselineEyeOpenProbability = currentAvg
                    }
                }

                // Eyelids are closed: take snapshot immediately while eyes are shut
                // Keep recording if eyelids close even further (lowest EAR)
                if (currentAvg < 0.35f) {
                    if (!blinkSawClosedEyes || currentAvg < lowestBlinkAvg) {
                        blinkSawClosedEyes = true
                        lowestBlinkAvg = currentAvg
                        blinkClosedFrameDataUrl = previewViewRef?.bitmap?.let { bmp ->
                            BitmapUtils.toBase64JpegDataUrl(bmp, targetSize = 600)
                        }
                    }
                }

                // Blink action is satisfied when eyes have shut and then reopened
                blinkSawClosedEyes && currentAvg >= (baselineEyeOpenProbability - 0.15f)
            }

            FaceVerificationStep.SMILE -> {
                val smilingProb = face.smilingProbability ?: 0f

                // Save snapshot at peak smile (broadest & most expressive frame)
                if (smilingProb >= 0.65f && smilingProb > bestSmileScore) {
                    bestSmileScore = smilingProb
                    peakSmileFrameDataUrl = previewViewRef?.bitmap?.let { bmp ->
                        BitmapUtils.toBase64JpegDataUrl(bmp, targetSize = 600)
                    }
                }

                smilingProb >= 0.78f
            }

            FaceVerificationStep.OPEN_MOUTH -> {
                val nose = face.getLandmark(FaceLandmark.NOSE_BASE)?.position
                val bottomLip = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)?.position
                val leftCorner = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position
                val rightCorner = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position

                if (nose != null && bottomLip != null && leftCorner != null && rightCorner != null) {
                    val mouthNoseDist = abs(bottomLip.y - nose.y)
                    val mouthWidth = abs(rightCorner.x - leftCorner.x)
                    if (mouthWidth > 0) {
                        (mouthNoseDist / mouthWidth) > 0.60f
                    } else {
                        (face.smilingProbability ?: 0f) > 0.40f
                    }
                } else {
                    (face.smilingProbability ?: 0f) > 0.40f
                }
            }
        }

        val stepProgress: Float
        if (step == FaceVerificationStep.STILLNESS) {
            if (matched) {
                if (stillnessStartedAt == null || (now - lastStillnessMatchAt > 500L)) {
                    stillnessStartedAt = now
                }
                lastStillnessMatchAt = now
                val elapsed = now - stillnessStartedAt!!
                stepProgress = (elapsed.toFloat() / stillnessDurationMs).coerceIn(0f, 1f)
                if (elapsed >= stillnessDurationMs) {
                    onStepCompleted(step)
                }
            } else {
                // Allow brief 500ms jitter tolerance before resetting stillness
                if (lastStillnessMatchAt != 0L && (now - lastStillnessMatchAt > 500L)) {
                    stillnessStartedAt = null
                    lastStillnessMatchAt = 0L
                    stepProgress = 0f
                } else {
                    stepProgress = (stillnessStartedAt?.let { (now - it).toFloat() / stillnessDurationMs } ?: 0f).coerceIn(0f, 1f)
                }
            }
        } else if (step == FaceVerificationStep.BLINK_EYES) {
            if (matched) {
                matchingFrames++
            } else {
                matchingFrames = 0
            }

            stepProgress = (matchingFrames / 2f).coerceIn(0f, 1f)
            if (matchingFrames >= 2) {
                // Submit the closed-eyelid snapshot captured when eyes were shut
                onStepCompleted(step, customFrameDataUrl = blinkClosedFrameDataUrl)
            }
        } else {
            // ── Dynamic Gestures (Smooth 4-frame stability counter) ──
            if (matched) {
                if (!isMovingFast) {
                    matchingFrames++
                }
                stepProgress = (matchingFrames.toFloat() / requiredStableFrames).coerceIn(0f, 1f)

                if (matchingFrames >= requiredStableFrames) {
                    if (step == FaceVerificationStep.LOOK_LEFT) {
                        firstTurnYaw = face.headEulerAngleY
                    }
                    val frameUrl = if (step == FaceVerificationStep.SMILE) peakSmileFrameDataUrl else null
                    onStepCompleted(step, customFrameDataUrl = frameUrl)
                }
            } else {
                // Smooth decay instead of hard drop to avoid flashing the progress ring
                matchingFrames = maxOf(0, matchingFrames - 1)
                stepProgress = (matchingFrames.toFloat() / requiredStableFrames).coerceIn(0f, 1f)
            }
        }

        if (!state.isVerificationComplete) {
            state = state.copy(
                isFaceAligned = true,
                isLowLight = isLowLight,
                isTooClose = false,
                isTooFar = false,
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

    private fun onStepCompleted(
        step: FaceVerificationStep,
        customFrameDataUrl: String? = null
    ) {
        isEvaluatingStep = true

        // 📸 1. Grab 600x600 normalized snapshot (or use the pre-captured closed-eyelids snapshot)
        val frameDataUrl = customFrameDataUrl ?: (previewViewRef?.bitmap?.let { bmp ->
            BitmapUtils.toBase64JpegDataUrl(bmp, targetSize = 600)
        } ?: "")

        val capture = FaceChallengeCapture(step = step, jpegDataUrl = frameDataUrl)
        val isFinalStep = completedStepCount >= verificationSteps.size - 1

        // 🌐 2. Send request to /liveness/verify-challenge in background
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val result = onStepCapture?.invoke(capture)

                completedStepCount++
                resetTracking()

                if (isFinalStep) {
                    val finalSuccess = result?.getOrNull()?.overallPassed == true
                    state = state.copy(
                        isVerificationComplete = true,
                        completedStepCount = verificationSteps.size,
                        verificationProgress = 1f,
                        serverVerificationPassed = finalSuccess,
                        guidance = "Completing verification..."
                    )
                }
            } catch (e: Exception) {
                Log.e("LivelinessCameraEngine", "Step verification network error", e)
                completedStepCount++
                resetTracking()
                if (isFinalStep) {
                    state = state.copy(
                        isVerificationComplete = true,
                        completedStepCount = verificationSteps.size,
                        verificationProgress = 1f,
                        serverVerificationPassed = false,
                        guidance = "Completing verification..."
                    )
                }
            } finally {
                isEvaluatingStep = false
            }
        }
    }

    private fun resetTracking() {
        matchingFrames = 0
        stillnessStartedAt = null
        lastStillnessMatchAt = 0L
        blinkSawClosedEyes = false
        lowestBlinkAvg = 1.0f
        blinkClosedFrameDataUrl = null
        peakSmileFrameDataUrl = null
        bestSmileScore = 0f
        prevEulerX = null
        prevEulerY = null
        prevEulerZ = null
        stepAvailableAt = System.currentTimeMillis() + stepTransitionGraceMs
    }

    fun release() {
        detector.close()
    }
}