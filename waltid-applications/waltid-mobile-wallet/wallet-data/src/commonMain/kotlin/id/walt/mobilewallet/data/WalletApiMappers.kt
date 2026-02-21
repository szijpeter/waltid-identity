@file:OptIn(ExperimentalEncodingApi::class)

package id.walt.mobilewallet.data

import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialFormat
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidMethod
import id.walt.mobilewallet.model.KeyAlgorithm
import id.walt.mobilewallet.model.KeyBackend
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.KeyId
import id.walt.mobilewallet.model.WalletCredential
import id.walt.mobilewallet.model.WalletId
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object WalletApiMappers {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun ApiWalletCredentialDto.toWalletCredential(walletId: WalletId): WalletCredential {
        val credentialId = CredentialId.validate(id).getOrNull() ?: CredentialId(id)
        return WalletCredential(
            walletId = walletId,
            id = credentialId,
            format = format.toMobileCredentialFormat(),
            document = document,
            disclosures = disclosures?.split("~")?.filter { it.isNotBlank() }.orEmpty(),
            manifest = manifest.toJsonObjectOrNull(),
            parsedDocument = parsedDocument ?: parseDocumentFromJwt(document),
        )
    }

    fun ApiWalletCredentialDto.toCredentialSummary(walletId: WalletId): CredentialSummary {
        val walletCredential = toWalletCredential(walletId)
        val parsed = walletCredential.parsedDocument ?: buildJsonObject { }
        val title = parseTitle(parsed, walletCredential.format)
        val subtitle = parsed["name"]?.jsonPrimitive?.contentOrNull
            ?: parsed["description"]?.jsonPrimitive?.contentOrNull
        val issuer = parseIssuer(parsed)

        return CredentialSummary(
            id = walletCredential.id,
            format = walletCredential.format,
            title = title,
            subtitle = subtitle,
            issuerName = issuer,
            isExpired = false,
        )
    }

    fun ApiWalletCredentialDto.toCredentialDetail(walletId: WalletId): CredentialDetail {
        val credential = toWalletCredential(walletId)
        val summary = toCredentialSummary(walletId)
        val parsed = credential.parsedDocument ?: buildJsonObject { }

        return CredentialDetail(
            summary = summary,
            credential = credential,
            issuerDid = parseIssuer(parsed),
            issuerServiceEndpoint = parseIssuerServiceEndpoint(credential.manifest, parsed),
            issuanceDate = parseDateField(parsed, "issuanceDate", "validFrom"),
            expirationDate = parseDateField(parsed, "expirationDate", "validUntil"),
        )
    }

    fun ApiWalletDidDto.toDidDescriptor(): DidDescriptor {
        val didId = DidId.validate(did).getOrNull() ?: DidId(did)
        return DidDescriptor(
            id = didId,
            alias = alias.takeIf { it.isNotBlank() },
            method = did.guessDidMethod(),
            isDefault = default,
        )
    }

    fun ApiSingleKeyResponseDto.toKeyDescriptor(): KeyDescriptor {
        val keyId = KeyId.validate(keyId.id).getOrNull() ?: KeyId(keyId.id)
        return KeyDescriptor(
            id = keyId,
            alias = name,
            algorithm = algorithm.toKeyAlgorithm(),
            backend = cryptoProvider.toKeyBackend(),
        )
    }

    private fun JsonElement?.toJsonObjectOrNull(): JsonObject? {
        return when (this) {
            null -> null
            is JsonObject -> this
            is JsonPrimitive -> {
                if (!isString) null else runCatching {
                    json.parseToJsonElement(content).jsonObject
                }.getOrNull()
            }

            else -> null
        }
    }

    private fun parseDocumentFromJwt(document: String): JsonObject? {
        if (!document.contains(".")) return null
        val payload = document.split(".").getOrNull(1) ?: return null
        val normalized = payload
            .replace('-', '+')
            .replace('_', '/')
            .let { value -> value + "=".repeat((4 - (value.length % 4)) % 4) }
        val decoded = runCatching { Base64.Default.decode(normalized).decodeToString() }.getOrNull() ?: return null
        return runCatching { json.parseToJsonElement(decoded).jsonObject }.getOrNull()
    }

    private fun parseTitle(parsed: JsonObject, format: CredentialFormat): String {
        val typeArray = parsed["type"] as? JsonArray
        val type = typeArray?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }?.lastOrNull()
        val vct = parsed["vct"]?.jsonPrimitive?.contentOrNull
        val docType = parsed["docType"]?.jsonPrimitive?.contentOrNull
        return type ?: vct ?: docType ?: when (format) {
            CredentialFormat.W3C_JWT_VC_JSON -> "W3C Credential"
            CredentialFormat.SD_JWT_VC -> "SD-JWT VC"
            CredentialFormat.MSO_MDOC -> "mDoc"
            CredentialFormat.UNKNOWN -> "Credential"
        }
    }

    private fun parseIssuer(parsed: JsonObject): String? {
        val issuer = parsed["issuer"] ?: return null
        return if (issuer is JsonObject) {
            issuer["id"]?.jsonPrimitive?.contentOrNull
        } else {
            issuer.jsonPrimitive.contentOrNull
        }
    }

    private fun parseIssuerServiceEndpoint(manifest: JsonObject?, parsed: JsonObject): String? {
        val fromManifest = manifest
            ?.get("input")
            ?.jsonObject
            ?.get("credentialIssuer")
            ?.jsonPrimitive
            ?.contentOrNull
        if (!fromManifest.isNullOrBlank()) return fromManifest
        return parsed["credentialIssuer"]?.jsonPrimitive?.contentOrNull
    }

    private fun parseDateField(parsed: JsonObject, primary: String, fallback: String): String? =
        parsed[primary]?.jsonPrimitive?.contentOrNull ?: parsed[fallback]?.jsonPrimitive?.contentOrNull

    private fun String.toMobileCredentialFormat(): CredentialFormat {
        return when (lowercase()) {
            "jwt_vc", "jwt_vc_json", "jwt_vc_json_ld", "ldp_vc" -> CredentialFormat.W3C_JWT_VC_JSON
            "sd_jwt_vc", "dc+sd-jwt", "vc+sd-jwt", "sd_jwt_dc" -> CredentialFormat.SD_JWT_VC
            "mso_mdoc" -> CredentialFormat.MSO_MDOC
            else -> CredentialFormat.UNKNOWN
        }
    }

    private fun String.toKeyAlgorithm(): KeyAlgorithm {
        return when (uppercase()) {
            "ED25519" -> KeyAlgorithm.ED25519
            "SECP256R1", "P-256" -> KeyAlgorithm.SECP256R1
            "SECP256K1" -> KeyAlgorithm.SECP256K1
            "RSA" -> KeyAlgorithm.RSA
            else -> KeyAlgorithm.ED25519
        }
    }

    private fun String.toKeyBackend(): KeyBackend {
        return when (lowercase()) {
            "jwk" -> KeyBackend.JWK
            "tse" -> KeyBackend.TSE
            "oci-rest-api" -> KeyBackend.OCI_REST_API
            "oci" -> KeyBackend.OCI
            "aws-access-key" -> KeyBackend.AWS_ACCESS_KEY
            "aws-role-name" -> KeyBackend.AWS_ROLE_NAME
            "aws" -> KeyBackend.AWS_ACCESS_KEY
            "azure-rest-api" -> KeyBackend.AZURE_REST_API
            "azure" -> KeyBackend.AZURE
            else -> KeyBackend.JWK
        }
    }

    private fun String.guessDidMethod(): DidMethod {
        val normalized = lowercase()
        return when {
            normalized.startsWith("did:key:") -> DidMethod.KEY
            normalized.startsWith("did:jwk:") -> DidMethod.JWK
            normalized.startsWith("did:web:") -> DidMethod.WEB
            normalized.startsWith("did:cheqd:") -> DidMethod.CHEQD
            normalized.startsWith("did:ebsi:") -> DidMethod.EBSI
            normalized.startsWith("did:iota:") -> DidMethod.IOTA
            else -> DidMethod.KEY
        }
    }
}
