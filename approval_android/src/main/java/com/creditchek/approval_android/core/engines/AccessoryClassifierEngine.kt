package com.creditchek.approval_android.core.engines

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Result of the on-device face accessory classifier.
 *
 * @param hasGlasses True if user is wearing eyeglasses or sunglasses above the activation threshold.
 * @param hasHeadwear True if user is wearing hats, caps, or head coverings above the activation threshold.
 * @param glassesScore Dequantized probability score (0.0 to 1.0) for glasses.
 * @param headwearScore Dequantized probability score (0.0 to 1.0) for headwear.
 */
data class AccessoryResult(
    val hasGlasses: Boolean = false,
    val hasHeadwear: Boolean = false,
    val glassesScore: Float = 0f,
    val headwearScore: Float = 0f
)

/**
 * High-performance on-device TensorFlow Lite classifier for detecting glasses and headwear
 * during the liveliness verification flow.
 *
 * Uses fully INT8 quantized MobileNetV3Small backbone exported from the model training pipeline.
 */
class AccessoryClassifierEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "AccessoryClassifier"
        private const val MODEL_PATH = "models/accessory_classifier.tflite"
        private const val CONFIG_PATH = "models/accessory_classifier.json"

        private const val DEFAULT_INPUT_SIZE = 160
        private const val DEFAULT_GLASSES_THRESHOLD = 0.25f
        private const val DEFAULT_HEADWEAR_THRESHOLD = 0.65f
        private const val DEFAULT_GLASSES_RELEASE_THRESHOLD = 0.15f
        private const val DEFAULT_HEADWEAR_RELEASE_THRESHOLD = 0.45f
        private const val DEFAULT_OUTPUT_SCALE = 0.00390625f
        private const val DEFAULT_OUTPUT_ZERO_POINT = 0

        // Debounce: number of consecutive positive inferences (~300ms each) before triggering UI guidance
        private const val REQUIRED_HITS_TO_TRIGGER = 3
        private const val REQUIRED_CLEARS_TO_RELEASE = 2
    }

    private var interpreter: Interpreter? = null
    private var isInitialized = false

    private var inputSize: Int = DEFAULT_INPUT_SIZE
    private var glassesThreshold: Float = DEFAULT_GLASSES_THRESHOLD
    private var headwearThreshold: Float = DEFAULT_HEADWEAR_THRESHOLD
    private var glassesReleaseThreshold: Float = DEFAULT_GLASSES_RELEASE_THRESHOLD
    private var headwearReleaseThreshold: Float = DEFAULT_HEADWEAR_RELEASE_THRESHOLD
    private var outputScale: Float = DEFAULT_OUTPUT_SCALE
    private var outputZeroPoint: Int = DEFAULT_OUTPUT_ZERO_POINT

    private var inputBuffer: ByteBuffer? = null
    private val outputArray = Array(1) { ByteArray(2) }
    private var pixelBuffer: IntArray? = null

    // Temporal smoothing & debounce counters to eliminate single-frame noise & guidance flickering
    private var smoothedGlassesScore: Float = 0f
    private var smoothedHeadwearScore: Float = 0f
    private var consecutiveGlassesHits = 0
    private var consecutiveHeadwearHits = 0
    private var consecutiveGlassesClear = 0
    private var consecutiveHeadwearClear = 0

    init {
        initClassifier()
    }

    private fun initClassifier() {
        try {
            loadConfig()

            val modelBuffer = loadModelFile(MODEL_PATH)
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            interpreter = Interpreter(modelBuffer, options)

            // Allocate direct byte buffer: 1 * inputSize * inputSize * 3 channels (uint8)
            val byteCount = 1 * inputSize * inputSize * 3
            inputBuffer = ByteBuffer.allocateDirect(byteCount).apply {
                order(ByteOrder.nativeOrder())
            }
            pixelBuffer = IntArray(inputSize * inputSize)

            isInitialized = true
            Log.i(TAG, "Accessory classifier initialized successfully (size=$inputSize, thresholds=[$glassesThreshold, $headwearThreshold])")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize accessory classifier", e)
            isInitialized = false
        }
    }

    private fun loadConfig() {
        try {
            val jsonString = context.assets.open(CONFIG_PATH).bufferedReader().use { it.readText() }
            val json = JSONObject(jsonString)

            inputSize = json.optInt("inputSize", DEFAULT_INPUT_SIZE)
            outputScale = json.optDouble("outputScale", DEFAULT_OUTPUT_SCALE.toDouble()).toFloat()
            outputZeroPoint = json.optInt("outputZeroPoint", DEFAULT_OUTPUT_ZERO_POINT)

            if (json.has("activationThresholds")) {
                val actArray = json.getJSONArray("activationThresholds")
                if (actArray.length() >= 2) {
                    glassesThreshold = actArray.getDouble(0).toFloat()
                    headwearThreshold = actArray.getDouble(1).toFloat()
                }
            } else if (json.has("thresholds")) {
                val threshArray = json.getJSONArray("thresholds")
                if (threshArray.length() >= 2) {
                    glassesThreshold = threshArray.getDouble(0).toFloat()
                    headwearThreshold = threshArray.getDouble(1).toFloat()
                }
            }

            if (json.has("releaseThresholds")) {
                val relArray = json.getJSONArray("releaseThresholds")
                if (relArray.length() >= 2) {
                    glassesReleaseThreshold = relArray.getDouble(0).toFloat()
                    headwearReleaseThreshold = relArray.getDouble(1).toFloat()
                }
            } else {
                glassesReleaseThreshold = maxOf(0.18f, glassesThreshold - 0.15f)
                headwearReleaseThreshold = maxOf(0.18f, headwearThreshold - 0.15f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load accessory config JSON, using calibrated defaults: ${e.message}")
        }
    }

    private fun loadModelFile(modelPath: String): ByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Classifies the face frame for glasses and headwear.
     *
     * @param previewBitmap The bitmap captured from the camera preview view.
     * @return [AccessoryResult] containing detection status and scores.
     */
    @Synchronized
    fun classify(previewBitmap: Bitmap): AccessoryResult {
        val tflite = interpreter
        val buffer = inputBuffer
        val pixels = pixelBuffer

        if (!isInitialized || tflite == null || buffer == null || pixels == null) {
            return AccessoryResult()
        }

        try {
            // PreviewView is already bounded to the oval camera viewport.
            // A 1:1 centered square crop contains the exact head & face region without distortion or double-cropping.
            val squareBitmap = cropCenterSquare(previewBitmap)
            val scaledBitmap = if (squareBitmap.width != inputSize || squareBitmap.height != inputSize) {
                squareBitmap.scale(inputSize, inputSize)
            } else {
                squareBitmap
            }

            if (squareBitmap != previewBitmap && squareBitmap != scaledBitmap) {
                squareBitmap.recycle()
            }

            scaledBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            if (scaledBitmap != previewBitmap) {
                scaledBitmap.recycle()
            }

            // Populate Direct ByteBuffer with RGB bytes (0..255 uint8)
            buffer.rewind()
            for (pixel in pixels) {
                val r = ((pixel shr 16) and 0xFF).toByte()
                val g = ((pixel shr 8) and 0xFF).toByte()
                val b = (pixel and 0xFF).toByte()
                buffer.put(r)
                buffer.put(g)
                buffer.put(b)
            }

            tflite.run(buffer, outputArray)

            val rawGlasses = outputArray[0][0].toInt() and 0xFF
            val rawHeadwear = outputArray[0][1].toInt() and 0xFF

            val rawGlassesScore = (rawGlasses - outputZeroPoint) * outputScale
            val rawHeadwearScore = (rawHeadwear - outputZeroPoint) * outputScale

            // Temporal score smoothing (Exponential Moving Average)
            smoothedGlassesScore = if (smoothedGlassesScore == 0f) rawGlassesScore else (smoothedGlassesScore * 0.4f + rawGlassesScore * 0.6f)
            smoothedHeadwearScore = if (smoothedHeadwearScore == 0f) rawHeadwearScore else (smoothedHeadwearScore * 0.4f + rawHeadwearScore * 0.6f)

            // Debounce glasses detection to avoid single-frame spurious spikes
            if (smoothedGlassesScore >= glassesThreshold) {
                consecutiveGlassesHits++
                consecutiveGlassesClear = 0
            } else if (smoothedGlassesScore < glassesReleaseThreshold) {
                consecutiveGlassesClear++
                if (consecutiveGlassesClear >= REQUIRED_CLEARS_TO_RELEASE) {
                    consecutiveGlassesHits = 0
                }
            }

            // Debounce headwear detection to avoid single-frame spurious spikes
            if (smoothedHeadwearScore >= headwearThreshold) {
                consecutiveHeadwearHits++
                consecutiveHeadwearClear = 0
            } else if (smoothedHeadwearScore < headwearReleaseThreshold) {
                consecutiveHeadwearClear++
                if (consecutiveHeadwearClear >= REQUIRED_CLEARS_TO_RELEASE) {
                    consecutiveHeadwearHits = 0
                }
            }

            val hasGlasses = consecutiveGlassesHits >= REQUIRED_HITS_TO_TRIGGER
            val hasHeadwear = consecutiveHeadwearHits >= REQUIRED_HITS_TO_TRIGGER

            return AccessoryResult(
                hasGlasses = hasGlasses,
                hasHeadwear = hasHeadwear,
                glassesScore = smoothedGlassesScore,
                headwearScore = smoothedHeadwearScore
            )
        } catch (e: Exception) {
            Log.e(TAG, "Inference error in accessory classifier", e)
            return AccessoryResult()
        }
    }

    /**
     * Crops a 1:1 centered square from the preview bitmap.
     */
    private fun cropCenterSquare(srcBmp: Bitmap): Bitmap {
        val width = srcBmp.width
        val height = srcBmp.height

        val dimension = minOf(width, height)
        val xOffset = ((width - dimension) / 2).coerceIn(0, maxOf(0, width - dimension))
        val maxAvailableY = maxOf(0, height - dimension)
        // In portrait camera orientation, bias crop towards the upper portion to center on the head
        val yOffset = if (maxAvailableY > 0) {
            (maxAvailableY * 0.35f).toInt().coerceIn(0, maxAvailableY)
        } else {
            0
        }

        return Bitmap.createBitmap(srcBmp, xOffset, yOffset, dimension, dimension)
    }

    fun resetState() {
        smoothedGlassesScore = 0f
        smoothedHeadwearScore = 0f
        consecutiveGlassesHits = 0
        consecutiveHeadwearHits = 0
        consecutiveGlassesClear = 0
        consecutiveHeadwearClear = 0
    }

    fun close() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing interpreter", e)
        }
        interpreter = null
        inputBuffer = null
        pixelBuffer = null
        isInitialized = false
    }
}
