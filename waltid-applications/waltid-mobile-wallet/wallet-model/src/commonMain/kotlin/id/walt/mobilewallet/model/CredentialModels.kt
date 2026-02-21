package id.walt.mobilewallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class CredentialFormat {
    @SerialName("jwt_vc_json")
    W3C_JWT_VC_JSON,

    @SerialName("dc+sd-jwt")
    SD_JWT_VC,

    @SerialName("mso_mdoc")
    MSO_MDOC,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class WalletCredential(
    val walletId: WalletId,
    val id: CredentialId,
    val format: CredentialFormat,
    val document: String,
    val disclosures: List<String> = emptyList(),
    val manifest: JsonObject? = null,
    val parsedDocument: JsonObject? = null,
) {
    fun validate(path: String = "walletCredential"): ValidationResult<WalletCredential> {
        return if (document.isBlank()) {
            validationFailure(
                code = "blank_credential_document",
                message = "Credential document must not be blank.",
                path = "$path.document"
            )
        } else {
            ValidationResult.Valid(this)
        }
    }
}

@Serializable
data class CredentialSummary(
    val id: CredentialId,
    val format: CredentialFormat,
    val title: String,
    val subtitle: String? = null,
    val issuerName: String? = null,
    val isExpired: Boolean = false,
)

@Serializable
data class CredentialDetail(
    val summary: CredentialSummary,
    val credential: WalletCredential,
    val issuerDid: String? = null,
    val issuerServiceEndpoint: String? = null,
    val issuanceDate: String? = null,
    val expirationDate: String? = null,
)
