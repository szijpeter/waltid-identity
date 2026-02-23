package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.ProtocolMode
import id.walt.mobilewallet.model.ScannedRequest
import id.walt.mobilewallet.model.ScannedRequestKind
import id.walt.mobilewallet.model.ValidationIssue

data class ScannedRequestClassification(
    val kind: ScannedRequestKind,
    val protocolMode: ProtocolMode,
)

class HandleScannedRequestUseCase {
    fun classify(rawRequest: String): ScannedRequestClassification {
        val normalized = rawRequest.trim()
        val lower = normalized.lowercase()
        return ScannedRequestClassification(
            kind = detectKind(lower),
            protocolMode = detectProtocolMode(lower),
        )
    }

    operator fun invoke(rawRequest: String): WalletResult<ScannedRequest> {
        val normalized = rawRequest.trim()
        if (normalized.isEmpty()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_request",
                            message = "Scanned request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }

        val kind = classify(normalized).kind
        return ScannedRequest(raw = normalized, kind = kind).validate().toWalletResult()
    }

    private fun detectKind(lower: String): ScannedRequestKind {
        return when {
            lower.startsWith("openid-initiate-issuance://") -> ScannedRequestKind.ISSUANCE
            lower.startsWith("openid-credential-offer://") -> ScannedRequestKind.ISSUANCE
            lower.contains("credential_offer=") -> ScannedRequestKind.ISSUANCE
            lower.startsWith("openid4vp://") -> ScannedRequestKind.PRESENTATION
            lower.contains("response_type=vp_token") -> ScannedRequestKind.PRESENTATION
            lower.contains("presentation_definition=") -> ScannedRequestKind.PRESENTATION
            else -> ScannedRequestKind.UNKNOWN
        }
    }

    private fun detectProtocolMode(lower: String): ProtocolMode = when {
        lower.contains("dcql_query=") -> ProtocolMode.OPENID4VP_1_0
        lower.contains("client_id_scheme=") -> ProtocolMode.OPENID4VP_1_0
        lower.contains("presentation_definition_uri=") -> ProtocolMode.OPENID4VP_1_0
        else -> ProtocolMode.DRAFT_COMPAT
    }
}

internal fun <T> id.walt.mobilewallet.model.ValidationResult<T>.toWalletResult(): WalletResult<T> = when (this) {
    is id.walt.mobilewallet.model.ValidationResult.Valid -> WalletResult.Success(value)
    is id.walt.mobilewallet.model.ValidationResult.Invalid -> WalletResult.Failure(WalletError.Validation(issues))
}
