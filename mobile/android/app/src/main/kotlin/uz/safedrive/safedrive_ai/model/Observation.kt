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
)

data class MonitorState(
    val status: String = "stopped",
    val message: String = "Kuzatuvni boshlashga tayyor",
    val active: Boolean = false,
    val eyesOpen: Double? = null,
    val closedMs: Long = 0,
    val yaw: Double? = null,
    val pitch: Double? = null,
    val perclos: Double? = null,
    val alarm: Boolean = false,
    val alerts: Int = 0,
    val assistant: String = "Ovozli eslatmalar",
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "status" to status, "message" to message, "active" to active,
        "eyesOpen" to eyesOpen, "closedMs" to closedMs, "yaw" to yaw,
        "pitch" to pitch, "perclos" to perclos, "alarm" to alarm,
        "alerts" to alerts, "assistant" to assistant,
    )
}
