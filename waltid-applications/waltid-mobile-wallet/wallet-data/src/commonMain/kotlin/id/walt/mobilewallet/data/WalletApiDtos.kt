package id.walt.mobilewallet.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ApiWalletCredentialDto(
    val wallet: String? = null,
    val id: String,
    val document: String,
    val disclosures: String? = null,
    val addedOn: String? = null,
    val manifest: JsonElement? = null,
    val parsedDocument: JsonObject? = null,
    val format: String = "unknown",
)

@Serializable
data class ApiWalletDidDto(
    val did: String,
    val alias: String = "",
    val document: String? = null,
    val keyId: String? = null,
    val default: Boolean = false,
)

@Serializable
data class ApiSingleKeyResponseDto(
    val algorithm: String = "",
    val cryptoProvider: String = "",
    val keyId: ApiKeyIdDto,
    val name: String? = null,
)

@Serializable
data class ApiKeyIdDto(
    val id: String,
)

@Serializable
data class ApiResolveCredentialOfferDto(
    val credential_issuer: String? = null,
    val credential_configuration_ids: List<String> = emptyList(),
    val credentials: List<String> = emptyList(),
)

@Serializable
data class ApiVctMetadataDto(
    val name: String? = null,
    val description: String? = null,
    val vct: String? = null,
)

@Serializable
data class ApiUsePresentationRequestDto(
    val did: String? = null,
    val presentationRequest: String,
    val selectedCredentials: List<String>,
    val disclosures: Map<String, List<String>>? = null,
    val note: String? = null,
)

@Serializable
data class ApiUsePresentationResponseDto(
    val redirectUri: String? = null,
    val errorMessage: String? = null,
    val message: String? = null,
)
