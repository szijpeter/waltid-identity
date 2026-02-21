package id.walt.mobilewallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProtocolMode {
    @SerialName("openid4vp_1_0")
    OPENID4VP_1_0,

    @SerialName("draft_compat")
    DRAFT_COMPAT,
}

@Serializable
enum class ScannedRequestKind {
    @SerialName("issuance")
    ISSUANCE,

    @SerialName("presentation")
    PRESENTATION,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class ScannedRequest(
    val raw: String,
    val kind: ScannedRequestKind,
) {
    fun validate(path: String = "scannedRequest"): ValidationResult<ScannedRequest> {
        return if (raw.isBlank()) {
            validationFailure("blank_request", "Request must not be blank.", "$path.raw")
        } else {
            ValidationResult.Valid(this)
        }
    }
}

@Serializable
data class IssuanceRequest(
    val walletId: WalletId,
    val rawRequest: String,
    val did: DidId? = null,
)

@Serializable
data class IssuancePreview(
    val issuerHost: String,
    val credentialTypes: List<String>,
    val didOptions: List<DidDescriptor> = emptyList(),
)

@Serializable
data class PresentationRequest(
    val walletId: WalletId,
    val rawRequest: String,
    val verifierHost: String,
    val requestedCredentialTypes: List<String>,
    val protocolMode: ProtocolMode,
)

@Serializable
data class PresentationSelection(
    val request: PresentationRequest,
    val selectedCredentialIds: List<CredentialId>,
    val disclosures: Map<String, List<String>> = emptyMap(),
) {
    fun validate(path: String = "presentationSelection"): ValidationResult<PresentationSelection> {
        return if (selectedCredentialIds.isEmpty()) {
            validationFailure(
                code = "no_selected_credentials",
                message = "At least one credential must be selected for presentation.",
                path = "$path.selectedCredentialIds"
            )
        } else {
            ValidationResult.Valid(this)
        }
    }
}

@Serializable
data class PresentationSubmissionResult(
    val success: Boolean,
    val redirectUri: String? = null,
    val message: String? = null,
)
