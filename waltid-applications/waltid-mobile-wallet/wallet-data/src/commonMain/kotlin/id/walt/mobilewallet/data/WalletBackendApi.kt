package id.walt.mobilewallet.data

import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.DidMethod
import id.walt.mobilewallet.model.WalletId
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

interface WalletBackendApi {
    suspend fun listCredentials(walletId: WalletId, showDeleted: Boolean = false, showPending: Boolean = false): List<ApiWalletCredentialDto>
    suspend fun getCredential(walletId: WalletId, credentialId: String): ApiWalletCredentialDto

    suspend fun listDids(walletId: WalletId): List<ApiWalletDidDto>
    suspend fun createDid(walletId: WalletId, method: DidMethod, parameters: Map<String, String>): String
    suspend fun importDid(walletId: WalletId, request: DidImportRequest): String
    suspend fun deleteDid(walletId: WalletId, didId: DidId): Boolean
    suspend fun setDefaultDid(walletId: WalletId, didId: DidId): Boolean

    suspend fun listKeys(walletId: WalletId): List<ApiSingleKeyResponseDto>
    suspend fun generateKey(walletId: WalletId, payload: JsonObject): String
    suspend fun importKey(walletId: WalletId, material: String, alias: String?): String
    suspend fun exportKey(walletId: WalletId, keyId: String, format: String, loadPrivateKey: Boolean): String
    suspend fun sign(walletId: WalletId, keyId: String, payload: JsonElement): String
    suspend fun verify(walletId: WalletId, jwk: String, signature: String): Boolean

    suspend fun resolveCredentialOffer(walletId: WalletId, rawRequest: String): ApiResolveCredentialOfferDto
    suspend fun resolveIssuerOpenIdMetadata(walletId: WalletId, issuer: String): JsonObject
    suspend fun resolveVct(walletId: WalletId, vct: String): ApiVctMetadataDto
    suspend fun useOfferRequest(walletId: WalletId, rawRequest: String, did: DidId?): JsonElement?

    suspend fun resolvePresentationRequest(walletId: WalletId, rawRequest: String): String
    suspend fun matchCredentialsForPresentationDefinition(
        walletId: WalletId,
        presentationDefinition: JsonObject,
    ): List<ApiWalletCredentialDto>

    suspend fun usePresentationRequest(walletId: WalletId, request: ApiUsePresentationRequestDto): ApiUsePresentationResponseDto
}

class WalletApiException(
    val statusCode: Int? = null,
    override val message: String,
) : RuntimeException(message)

class KtorWalletBackendApi(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) : WalletBackendApi {

    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun listCredentials(walletId: WalletId, showDeleted: Boolean, showPending: Boolean): List<ApiWalletCredentialDto> {
        val response = httpClient.get(walletPath(walletId, "credentials")) {
            parameter("showDeleted", showDeleted)
            parameter("showPending", showPending)
        }
        return response.parseOrThrow()
    }

    override suspend fun getCredential(walletId: WalletId, credentialId: String): ApiWalletCredentialDto {
        val response = httpClient.get(walletPath(walletId, "credentials/${credentialId.encodeURLPathPart()}"))
        return response.parseOrThrow()
    }

    override suspend fun listDids(walletId: WalletId): List<ApiWalletDidDto> {
        val response = httpClient.get(walletPath(walletId, "dids"))
        return response.parseOrThrow()
    }

    override suspend fun createDid(walletId: WalletId, method: DidMethod, parameters: Map<String, String>): String {
        val response = httpClient.post(walletPath(walletId, "dids/create/${methodToApiValue(method)}")) {
            parameters.forEach { (key, value) -> parameter(key, value) }
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun importDid(walletId: WalletId, request: DidImportRequest): String {
        val keyPayload = request.keyMaterial ?: request.keyId ?: ""
        val body = buildJsonObject {
            put("did", request.did)
            if (keyPayload.trim().startsWith("{")) {
                val parsed = runCatching { json.parseToJsonElement(keyPayload) }.getOrNull()
                if (parsed != null) {
                    put("key", parsed)
                } else {
                    put("key", keyPayload)
                }
            } else {
                put("key", keyPayload)
            }
            request.alias?.let { put("alias", it) }
        }
        val response = httpClient.post(walletPath(walletId, "dids/import")) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun deleteDid(walletId: WalletId, didId: DidId): Boolean {
        val response = httpClient.delete(walletPath(walletId, "dids/${didId.value.encodeURLPathPart()}"))
        return response.status == HttpStatusCode.Accepted
    }

    override suspend fun setDefaultDid(walletId: WalletId, didId: DidId): Boolean {
        val response = httpClient.post(walletPath(walletId, "dids/default")) {
            parameter("did", didId.value)
        }
        return response.status == HttpStatusCode.Accepted
    }

    override suspend fun listKeys(walletId: WalletId): List<ApiSingleKeyResponseDto> {
        val response = httpClient.get(walletPath(walletId, "keys"))
        return response.parseOrThrow()
    }

    override suspend fun generateKey(walletId: WalletId, payload: JsonObject): String {
        val response = httpClient.post(walletPath(walletId, "keys/generate")) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun importKey(walletId: WalletId, material: String, alias: String?): String {
        val response = httpClient.post(walletPath(walletId, "keys/import")) {
            alias?.let { parameter("alias", it) }
            contentType(ContentType.Text.Plain)
            setBody(material)
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun exportKey(walletId: WalletId, keyId: String, format: String, loadPrivateKey: Boolean): String {
        val response = httpClient.get(walletPath(walletId, "keys/${keyId.encodeURLPathPart()}/export")) {
            parameter("format", format)
            parameter("loadPrivateKey", loadPrivateKey)
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun sign(walletId: WalletId, keyId: String, payload: JsonElement): String {
        val response = httpClient.post(walletPath(walletId, "keys/${keyId.encodeURLPathPart()}/sign")) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun verify(walletId: WalletId, jwk: String, signature: String): Boolean {
        val response = httpClient.post(walletPath(walletId, "keys/verify")) {
            parameter("JWK", jwk)
            contentType(ContentType.Text.Plain)
            setBody(signature)
        }
        return response.parseOrThrow<Boolean>()
    }

    override suspend fun resolveCredentialOffer(walletId: WalletId, rawRequest: String): ApiResolveCredentialOfferDto {
        val response = httpClient.post(walletPath(walletId, "exchange/resolveCredentialOffer")) {
            contentType(ContentType.Text.Plain)
            setBody(rawRequest)
        }
        return response.parseOrThrow()
    }

    override suspend fun resolveIssuerOpenIdMetadata(walletId: WalletId, issuer: String): JsonObject {
        val response = httpClient.get(walletPath(walletId, "exchange/resolveIssuerOpenIDMetadata")) {
            parameter("issuer", issuer)
        }
        return response.parseOrThrow()
    }

    override suspend fun resolveVct(walletId: WalletId, vct: String): ApiVctMetadataDto {
        val response = httpClient.get(walletPath(walletId, "exchange/resolveVctUrl")) {
            parameter("vct", vct)
        }
        return response.parseOrThrow()
    }

    override suspend fun useOfferRequest(walletId: WalletId, rawRequest: String, did: DidId?): JsonElement? {
        val response = httpClient.post(walletPath(walletId, "exchange/useOfferRequest")) {
            did?.let { parameter("did", it.value) }
            contentType(ContentType.Text.Plain)
            setBody(rawRequest)
        }
        return response.parseOrThrowOrNull()
    }

    override suspend fun resolvePresentationRequest(walletId: WalletId, rawRequest: String): String {
        val response = httpClient.post(walletPath(walletId, "exchange/resolvePresentationRequest")) {
            contentType(ContentType.Text.Plain)
            setBody(rawRequest)
        }
        return response.bodyTextOrThrow().normalizeResponseString()
    }

    override suspend fun matchCredentialsForPresentationDefinition(
        walletId: WalletId,
        presentationDefinition: JsonObject,
    ): List<ApiWalletCredentialDto> {
        val response = httpClient.post(walletPath(walletId, "exchange/matchCredentialsForPresentationDefinition")) {
            contentType(ContentType.Application.Json)
            setBody(presentationDefinition)
        }
        return response.parseOrThrow()
    }

    override suspend fun usePresentationRequest(
        walletId: WalletId,
        request: ApiUsePresentationRequestDto,
    ): ApiUsePresentationResponseDto {
        val response = httpClient.post(walletPath(walletId, "exchange/usePresentationRequest")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return if (response.status.isSuccess()) {
            response.parseOrThrow()
        } else {
            val body = response.bodyAsText()
            val error = runCatching { json.decodeFromString(ApiUsePresentationResponseDto.serializer(), body) }.getOrNull()
            throw WalletApiException(response.status.value, error?.errorMessage ?: body.ifBlank { response.status.description })
        }
    }

    private fun walletPath(walletId: WalletId, path: String): String {
        val encodedWalletId = walletId.value.encodeURLPathPart()
        return "$normalizedBaseUrl/wallet-api/wallet/$encodedWalletId/$path"
    }

    private suspend inline fun <reified T> HttpResponse.parseOrThrow(): T {
        if (!status.isSuccess()) {
            throw WalletApiException(
                statusCode = status.value,
                message = bodyAsText().ifBlank { status.description }
            )
        }
        return body()
    }

    private suspend inline fun <reified T> HttpResponse.parseOrThrowOrNull(): T? {
        if (!status.isSuccess()) {
            throw WalletApiException(
                statusCode = status.value,
                message = bodyAsText().ifBlank { status.description }
            )
        }
        val text = bodyAsText()
        if (text.isBlank()) {
            return null
        }
        return runCatching { json.decodeFromString<T>(text) }.getOrNull()
    }

    private suspend fun HttpResponse.bodyTextOrThrow(): String {
        if (!status.isSuccess()) {
            throw WalletApiException(
                statusCode = status.value,
                message = bodyAsText().ifBlank { status.description }
            )
        }
        return bodyAsText()
    }
}

private fun String.normalizeResponseString(): String {
    val trimmed = trim()
    return if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length >= 2) {
        trimmed.substring(1, trimmed.length - 1)
    } else {
        trimmed
    }
}

private fun methodToApiValue(method: DidMethod): String = when (method) {
    DidMethod.KEY -> "key"
    DidMethod.JWK -> "jwk"
    DidMethod.WEB -> "web"
    DidMethod.CHEQD -> "cheqd"
    DidMethod.EBSI -> "ebsi"
    DidMethod.IOTA -> "iota"
}
