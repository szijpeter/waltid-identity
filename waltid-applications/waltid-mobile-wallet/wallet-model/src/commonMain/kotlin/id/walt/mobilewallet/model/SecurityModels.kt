package id.walt.mobilewallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecuritySettings(
    val appLockEnabled: Boolean = false,
    val biometricRequiredForCredentialExport: Boolean = false,
    val integrityWarningDismissedAtEpochSeconds: Long? = null,
)

@Serializable
enum class DeviceIntegrityLevel {
    @SerialName("trusted")
    TRUSTED,

    @SerialName("warning")
    WARNING,

    @SerialName("compromised")
    COMPROMISED,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class DeviceIntegritySignal(
    val level: DeviceIntegrityLevel,
    val reason: String? = null,
)
