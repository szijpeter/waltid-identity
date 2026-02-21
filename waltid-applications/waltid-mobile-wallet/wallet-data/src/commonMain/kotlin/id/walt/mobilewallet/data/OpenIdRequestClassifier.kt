package id.walt.mobilewallet.data

import id.walt.mobilewallet.model.ProtocolMode
import id.walt.mobilewallet.model.ScannedRequestKind

data class ClassifiedRequest(
    val kind: ScannedRequestKind,
    val protocolMode: ProtocolMode,
)

object OpenIdRequestClassifier {
    fun classify(rawRequest: String): ClassifiedRequest {
        val trimmed = rawRequest.trim()
        val lower = trimmed.lowercase()

        val kind = when {
            lower.startsWith("openid-initiate-issuance://") -> ScannedRequestKind.ISSUANCE
            lower.startsWith("openid-credential-offer://") -> ScannedRequestKind.ISSUANCE
            lower.contains("credential_offer=") -> ScannedRequestKind.ISSUANCE
            lower.startsWith("openid4vp://") -> ScannedRequestKind.PRESENTATION
            lower.contains("response_type=vp_token") -> ScannedRequestKind.PRESENTATION
            lower.contains("presentation_definition=") -> ScannedRequestKind.PRESENTATION
            else -> ScannedRequestKind.UNKNOWN
        }

        val protocolMode = when {
            lower.contains("dcql_query=") -> ProtocolMode.OPENID4VP_1_0
            lower.contains("client_id_scheme=") -> ProtocolMode.OPENID4VP_1_0
            lower.contains("presentation_definition_uri=") -> ProtocolMode.OPENID4VP_1_0
            kind == ScannedRequestKind.PRESENTATION -> ProtocolMode.DRAFT_COMPAT
            else -> ProtocolMode.DRAFT_COMPAT
        }

        return ClassifiedRequest(kind = kind, protocolMode = protocolMode)
    }
}
