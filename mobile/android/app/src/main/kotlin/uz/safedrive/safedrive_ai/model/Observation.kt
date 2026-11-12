package uz.safedrive.safedrive_ai.model

data class Observation(
    val timeMs: Long,
    val faceCount: Int,
    val leftEyeOpen: Float? = null,
    val rightEyeOpen: Float? = null,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val trackingId: Int? = null,
    val sideEyeOpen: Boolean? = null,
    val sideEyeOpenness: Float? = null,
    val lighting: Lighting = Lighting.NORMAL,
)

data class MonitorState(
    val status: String = "stopped",
    val message: String = "Ready to monitor",
    val active: Boolean = false,
    val eyesOpen: Double? = null,
    val closedMs: Long = 0,
    val yaw: Double? = null,
    val pitch: Double? = null,
    val perclos: Double? = null,
    val alarm: Boolean = false,
    val alerts: Int = 0,
    val assistant: String = "Voice reminders",
    val sideEye: Boolean = false,
    val lighting: String = "normal",
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "status" to status, "message" to message, "active" to active,
        "lighting" to lighting, "eyesOpen" to eyesOpen, "sideEye" to sideEye, "closedMs" to closedMs, "yaw" to yaw,
        "pitch" to pitch, "perclos" to perclos, "alarm" to alarm,
        "alerts" to alerts, "assistant" to assistant,
    )
}
