package com.creditchek.approval_android.features.identity.data.models

/**
 * 1:1 Port of Flutter's FaceVerificationStep enum from approval_flutter.zip
 */
enum class FaceVerificationStep(
    val instruction: String,
    val apiName: String
) {
    STILLNESS("Hold still", "stillness"),
    LOOK_LEFT("Look left", "look_left"),
    LOOK_RIGHT("Look right", "look_right"),
    LOOK_UP("Look up", "look_up"),
    LOOK_DOWN("Look down", "look_down"),
    BLINK_EYES("Blink your eyes", "blink"),
    SMILE("Smile", "smile"),
    OPEN_MOUTH("Open your mouth", "open_mouth");
}

/**
 * Holds the captured 600x600 JPEG data URL for a verified challenge step
 */
data class FaceChallengeCapture(
    val step: FaceVerificationStep,
    val jpegDataUrl: String
)