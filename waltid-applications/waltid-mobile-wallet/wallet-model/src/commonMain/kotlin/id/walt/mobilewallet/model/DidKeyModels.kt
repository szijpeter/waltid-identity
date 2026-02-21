package id.walt.mobilewallet.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DidMethod {
    @SerialName("key")
    KEY,

    @SerialName("jwk")
    JWK,

    @SerialName("web")
    WEB,

    @SerialName("cheqd")
    CHEQD,

    @SerialName("ebsi")
    EBSI,

    @SerialName("iota")
    IOTA,
}

@Serializable
data class DidDescriptor(
    val id: DidId,
    val alias: String? = null,
    val method: DidMethod,
    val isDefault: Boolean = false,
)

@Serializable
data class DidCreateRequest(
    val method: DidMethod,
    val keyId: KeyId? = null,
    val alias: String? = null,
    val useJwkJcsPub: Boolean? = null,
    val domain: String? = null,
    val path: String? = null,
    val network: String? = null,
)

@Serializable
data class DidImportRequest(
    val did: String,
    val keyMaterial: String? = null,
    val keyId: String? = null,
    val alias: String? = null,
) {
    fun validate(path: String = "didImportRequest"): ValidationResult<DidImportRequest> {
        return if (did.isBlank()) {
            validationFailure("blank_did", "DID must not be blank.", "$path.did")
        } else if (keyMaterial.isNullOrBlank() && keyId.isNullOrBlank()) {
            validationFailure(
                code = "missing_did_key_reference",
                message = "Either key material or key identifier must be provided for DID import.",
                path = "$path.keyMaterial"
            )
        } else {
            ValidationResult.Valid(this)
        }
    }
}

@Serializable
enum class KeyAlgorithm {
    @SerialName("Ed25519")
    ED25519,

    @SerialName("secp256r1")
    SECP256R1,

    @SerialName("secp256k1")
    SECP256K1,

    @SerialName("RSA")
    RSA,
}

@Serializable
enum class KeyBackend {
    @SerialName("jwk")
    JWK,

    @SerialName("tse")
    TSE,

    @SerialName("oci-rest-api")
    OCI_REST_API,

    @SerialName("oci")
    OCI,

    @SerialName("aws-access-key")
    AWS_ACCESS_KEY,

    @SerialName("aws-role-name")
    AWS_ROLE_NAME,

    @SerialName("azure-rest-api")
    AZURE_REST_API,

    @SerialName("azure")
    AZURE,
}

@Serializable
data class KeyDescriptor(
    val id: KeyId,
    val alias: String? = null,
    val algorithm: KeyAlgorithm,
    val backend: KeyBackend,
)

@Serializable
data class KeyGenerateRequest(
    val name: String? = null,
    val algorithm: KeyAlgorithm,
    val backend: KeyBackend,
    val config: Map<String, String> = emptyMap(),
)

@Serializable
data class KeyImportRequest(
    val material: String,
    val alias: String? = null,
) {
    fun validate(path: String = "keyImportRequest"): ValidationResult<KeyImportRequest> {
        return if (material.isBlank()) {
            validationFailure("blank_key_material", "Key material must not be blank.", "$path.material")
        } else {
            ValidationResult.Valid(this)
        }
    }
}

@Serializable
enum class KeyExportFormat {
    @SerialName("JWK")
    JWK,

    @SerialName("PEM")
    PEM,
}

@Serializable
data class KeyExportRequest(
    val keyId: KeyId,
    val format: KeyExportFormat,
    val includePrivateKey: Boolean = false,
)

@Serializable
data class KeySignRequest(
    val keyId: KeyId,
    val payload: String,
)

@Serializable
data class KeyVerifyRequest(
    val jwk: String,
    val signedPayload: String,
    val detachedPayload: String? = null,
)

@Serializable
data class KeyVerifyResult(
    val valid: Boolean,
    val details: String? = null,
)
