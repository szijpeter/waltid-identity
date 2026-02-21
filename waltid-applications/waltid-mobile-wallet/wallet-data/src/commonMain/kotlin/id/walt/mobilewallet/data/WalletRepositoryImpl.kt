package id.walt.mobilewallet.data

import id.walt.mobilewallet.domain.CredentialRepository
import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidCreateRequest
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.KeyAlgorithm
import id.walt.mobilewallet.model.KeyBackend
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.KeyExportRequest
import id.walt.mobilewallet.model.KeyGenerateRequest
import id.walt.mobilewallet.model.KeyImportRequest
import id.walt.mobilewallet.model.KeySignRequest
import id.walt.mobilewallet.model.KeyVerifyRequest
import id.walt.mobilewallet.model.KeyVerifyResult
import id.walt.mobilewallet.model.PresentationRequest
import id.walt.mobilewallet.model.PresentationSelection
import id.walt.mobilewallet.model.PresentationSubmissionResult
import id.walt.mobilewallet.model.ValidationIssue
import id.walt.mobilewallet.model.WalletCredential
import id.walt.mobilewallet.model.WalletId
import io.ktor.http.Url
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ApiCredentialRepository(
    private val backendApi: WalletBackendApi,
) : CredentialRepository {
    override suspend fun listCredentials(walletId: WalletId): WalletResult<List<CredentialSummary>> = safeCall {
        backendApi.listCredentials(walletId).map { dto ->
            WalletApiMappers.run { dto.toCredentialSummary(walletId) }
        }
    }

    override suspend fun getCredential(walletId: WalletId, credentialId: CredentialId): WalletResult<CredentialDetail> = safeCall {
        val dto = backendApi.getCredential(walletId, credentialId.value)
        WalletApiMappers.run { dto.toCredentialDetail(walletId) }
    }

    override suspend fun listWalletCredentials(walletId: WalletId): WalletResult<List<WalletCredential>> = safeCall {
        backendApi.listCredentials(walletId).map { dto ->
            WalletApiMappers.run { dto.toWalletCredential(walletId) }
        }
    }
}

class ApiDidRepository(
    private val backendApi: WalletBackendApi,
) : DidRepository {
    override suspend fun listDids(walletId: WalletId): WalletResult<List<DidDescriptor>> = safeCall {
        backendApi.listDids(walletId).map { dto ->
            WalletApiMappers.run { dto.toDidDescriptor() }
        }
    }

    override suspend fun createDid(walletId: WalletId, request: DidCreateRequest): WalletResult<DidDescriptor> = safeCall {
        val query = buildMap {
            request.keyId?.value?.takeIf { it.isNotBlank() }?.let { put("keyId", it) }
            request.alias?.takeIf { it.isNotBlank() }?.let { put("alias", it) }
            request.useJwkJcsPub?.let { put("useJwkJcsPub", it.toString()) }
            request.domain?.takeIf { it.isNotBlank() }?.let { put("domain", it) }
            request.path?.takeIf { it.isNotBlank() }?.let { put("path", it) }
            request.network?.takeIf { it.isNotBlank() }?.let { put("network", it) }
        }

        val did = backendApi.createDid(walletId, request.method, query)
        DidDescriptor(
            id = DidId.validate(did, "did").getOrNull() ?: DidId(did),
            alias = request.alias,
            method = request.method,
            isDefault = false,
        )
    }

    override suspend fun importDid(walletId: WalletId, request: DidImportRequest): WalletResult<DidDescriptor> = safeCall {
        val response = backendApi.importDid(walletId, request)
        val didValue = response.takeIf { it.startsWith("did:") } ?: request.did
        val method = WalletApiMappers.run {
            ApiWalletDidDto(did = didValue).toDidDescriptor().method
        }
        DidDescriptor(
            id = DidId.validate(didValue, "did").getOrNull() ?: DidId(didValue),
            alias = request.alias,
            method = method,
            isDefault = false,
        )
    }

    override suspend fun deleteDid(walletId: WalletId, didId: DidId): WalletResult<Boolean> = safeCall {
        backendApi.deleteDid(walletId, didId)
    }

    override suspend fun setDefaultDid(walletId: WalletId, didId: DidId): WalletResult<Boolean> = safeCall {
        backendApi.setDefaultDid(walletId, didId)
    }
}

class ApiKeyRepository(
    private val backendApi: WalletBackendApi,
) : KeyRepository {
    override suspend fun listKeys(walletId: WalletId): WalletResult<List<KeyDescriptor>> = safeCall {
        backendApi.listKeys(walletId).map { dto ->
            WalletApiMappers.run { dto.toKeyDescriptor() }
        }
    }

    override suspend fun generateKey(walletId: WalletId, request: KeyGenerateRequest): WalletResult<KeyDescriptor> = safeCall {
        val payload = buildJsonObject {
            request.name?.takeIf { it.isNotBlank() }?.let { put("name", it) }
            put("backend", request.backend.toApiBackend())
            put("keyType", request.algorithm.toApiAlgorithm())
            put("config", request.config.toApiConfig(request.backend))
        }

        val generatedKeyId = backendApi.generateKey(walletId, payload)
        KeyDescriptor(
            id = id.walt.mobilewallet.model.KeyId.validate(generatedKeyId).getOrNull()
                ?: id.walt.mobilewallet.model.KeyId(generatedKeyId),
            alias = request.name,
            algorithm = request.algorithm,
            backend = request.backend,
        )
    }

    override suspend fun importKey(walletId: WalletId, request: KeyImportRequest): WalletResult<KeyDescriptor> = safeCall {
        val imported = backendApi.importKey(walletId, request.material, request.alias)
        KeyDescriptor(
            id = id.walt.mobilewallet.model.KeyId.validate(imported).getOrNull()
                ?: id.walt.mobilewallet.model.KeyId(imported),
            alias = request.alias,
            algorithm = KeyAlgorithm.ED25519,
            backend = KeyBackend.JWK,
        )
    }

    override suspend fun exportKey(walletId: WalletId, request: KeyExportRequest): WalletResult<String> = safeCall {
        backendApi.exportKey(
            walletId = walletId,
            keyId = request.keyId.value,
            format = request.format.name,
            loadPrivateKey = request.includePrivateKey,
        )
    }

    override suspend fun sign(walletId: WalletId, request: KeySignRequest): WalletResult<String> = safeCall {
        val payload = runCatching {
            Json.parseToJsonElement(request.payload)
        }.getOrElse { JsonPrimitive(request.payload) }
        backendApi.sign(walletId = walletId, keyId = request.keyId.value, payload = payload)
    }

    override suspend fun verify(walletId: WalletId, request: KeyVerifyRequest): WalletResult<KeyVerifyResult> = safeCall {
        val valid = backendApi.verify(walletId, request.jwk, request.signedPayload)
        KeyVerifyResult(valid = valid)
    }
}

class ApiExchangeRepository(
    private val backendApi: WalletBackendApi,
) : ExchangeRepository {
    override suspend fun resolveIssuance(request: IssuanceRequest): WalletResult<IssuancePreview> = safeCall {
        val offer = backendApi.resolveCredentialOffer(request.walletId, request.rawRequest)
        val issuer = offer.credential_issuer.orEmpty()
        val issuerHost = runCatching { Url(issuer).host }.getOrElse { issuer.ifBlank { "unknown-issuer" } }
        val issuerMetadata = if (issuer.isNotBlank()) {
            backendApi.resolveIssuerOpenIdMetadata(request.walletId, issuer)
        } else {
            buildJsonObject { }
        }

        val credentialTypes = resolveCredentialTypes(request.walletId, offer, issuerMetadata)

        IssuancePreview(
            issuerHost = issuerHost,
            credentialTypes = credentialTypes.distinct(),
            didOptions = emptyList(),
        )
    }

    override suspend fun acceptIssuance(request: IssuanceRequest): WalletResult<List<CredentialId>> = safeCall {
        val response = backendApi.useOfferRequest(request.walletId, request.rawRequest, request.did)
        val ids = mutableListOf<CredentialId>()

        if (response is JsonObject) {
            val candidates = sequenceOf("credentialIds", "credentials", "ids")
            candidates.forEach { key ->
                val jsonArray = response[key]?.jsonArray ?: return@forEach
                jsonArray.mapNotNull { it as? JsonPrimitive }
                    .mapNotNull { primitive -> primitive.contentOrNull }
                    .forEach { value ->
                        CredentialId.validate(value).getOrNull()?.let(ids::add)
                    }
            }
        }

        ids.distinctBy { it.value }
    }

    override suspend fun resolvePresentation(request: PresentationRequest): WalletResult<List<WalletCredential>> = safeCall {
        val resolvedRequest = backendApi.resolvePresentationRequest(request.walletId, request.rawRequest)
        val resolvedUrl = Url(resolvedRequest)
        val presentationDefinition = resolvedUrl.parameters["presentation_definition"]
            ?: throw IllegalArgumentException("No presentation_definition found in resolved request.")

        val presentationDefinitionJson = Json.parseToJsonElement(presentationDefinition).jsonObject
        backendApi.matchCredentialsForPresentationDefinition(request.walletId, presentationDefinitionJson)
            .map { dto -> WalletApiMappers.run { dto.toWalletCredential(request.walletId) } }
    }

    override suspend fun submitPresentation(selection: PresentationSelection): WalletResult<PresentationSubmissionResult> = safeCall {
        val response = backendApi.usePresentationRequest(
            walletId = selection.request.walletId,
            request = ApiUsePresentationRequestDto(
                presentationRequest = selection.request.rawRequest,
                selectedCredentials = selection.selectedCredentialIds.map { it.value },
                disclosures = selection.disclosures,
            )
        )

        PresentationSubmissionResult(
            success = response.errorMessage == null,
            redirectUri = response.redirectUri,
            message = response.errorMessage ?: response.message,
        )
    }

    private suspend fun resolveCredentialTypes(
        walletId: WalletId,
        offer: ApiResolveCredentialOfferDto,
        metadata: JsonObject,
    ): List<String> {
        val result = mutableListOf<String>()
        val configs = metadata["credential_configurations_supported"]?.jsonObject.orEmpty()

        if (offer.credential_configuration_ids.isNotEmpty()) {
            offer.credential_configuration_ids.forEach { configurationId ->
                val config = configs[configurationId]?.jsonObject ?: return@forEach
                result += resolveTypeFromConfig(walletId, config)
            }
            return result
        }

        val supported = metadata["credentials_supported"]?.jsonArray.orEmpty()
        offer.credentials.forEach { credentialId ->
            val matching = supported.firstOrNull { element ->
                val obj = element.jsonObject
                obj["id"]?.jsonPrimitive?.contentOrNull == credentialId
            }?.jsonObject
            if (matching != null) {
                result += resolveTypeFromConfig(walletId, matching)
            }
        }

        return result
    }

    private suspend fun resolveTypeFromConfig(walletId: WalletId, config: JsonObject): List<String> {
        val typeFromTypes = config["types"]
            ?.jsonArray
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.lastOrNull()
            ?.let(::listOf)
            .orEmpty()

        if (typeFromTypes.isNotEmpty()) {
            return typeFromTypes
        }

        val credentialDefinitionType = config["credential_definition"]
            ?.jsonObject
            ?.get("type")
            ?.jsonArray
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?.lastOrNull()
            ?.let(::listOf)
            .orEmpty()

        if (credentialDefinitionType.isNotEmpty()) {
            return credentialDefinitionType
        }

        val vct = config["vct"]?.jsonPrimitive?.contentOrNull
        if (!vct.isNullOrBlank()) {
            val metadata = runCatching { backendApi.resolveVct(walletId, vct) }.getOrNull()
            val label = metadata?.name ?: metadata?.description ?: metadata?.vct ?: vct
            return listOf(label)
        }

        return emptyList()
    }
}

private suspend inline fun <T> safeCall(crossinline block: suspend () -> T): WalletResult<T> {
    return runCatching { block() }.fold(
        onSuccess = { WalletResult.Success(it) },
        onFailure = { WalletResult.Failure(it.toWalletError()) },
    )
}

private fun Throwable.toWalletError(): WalletError = when (this) {
    is WalletApiException -> WalletError.Network(message = message, statusCode = statusCode)
    is IllegalArgumentException -> WalletError.Validation(
        issues = listOf(
            ValidationIssue(
                code = "invalid_argument",
                message = message ?: "Invalid argument.",
            )
        )
    )

    else -> WalletError.Unknown(message = message ?: "Unknown wallet error", cause = this)
}

private fun KeyGenerateRequest.configToJsonObject(): JsonObject =
    buildJsonObject {
        config.forEach { (key, value) ->
            put(key, value)
        }
    }

private fun Map<String, String>.toApiConfig(backend: KeyBackend): JsonObject {
    if (isEmpty()) return buildJsonObject { }
    return when (backend) {
        KeyBackend.AWS_ACCESS_KEY -> buildJsonObject {
            putJsonObject("auth") {
                this@toApiConfig["accessKeyId"]?.let { put("accessKeyId", it) }
                this@toApiConfig["secretAccessKey"]?.let { put("secretAccessKey", it) }
                this@toApiConfig["region"]?.let { put("region", it) }
            }
        }

        KeyBackend.AWS_ROLE_NAME -> buildJsonObject {
            putJsonObject("auth") {
                this@toApiConfig["roleName"]?.let { put("roleName", it) }
                this@toApiConfig["region"]?.let { put("region", it) }
            }
        }

        KeyBackend.AZURE_REST_API -> buildJsonObject {
            putJsonObject("auth") {
                this@toApiConfig["clientId"]?.let { put("clientId", it) }
                this@toApiConfig["clientSecret"]?.let { put("clientSecret", it) }
                this@toApiConfig["tenantId"]?.let { put("tenantId", it) }
                this@toApiConfig["keyVaultUrl"]?.let { put("keyVaultUrl", it) }
            }
        }

        KeyBackend.AZURE -> buildJsonObject {
            putJsonObject("auth") {
                this@toApiConfig["keyVaultUrl"]?.let { put("keyVaultUrl", it) }
            }
        }

        else -> buildJsonObject {
            this@toApiConfig.forEach { (key, value) ->
                put(key, value)
            }
        }
    }
}

private fun KeyBackend.toApiBackend(): String = when (this) {
    KeyBackend.JWK -> "jwk"
    KeyBackend.TSE -> "tse"
    KeyBackend.OCI_REST_API -> "oci-rest-api"
    KeyBackend.OCI -> "oci"
    KeyBackend.AWS_ACCESS_KEY, KeyBackend.AWS_ROLE_NAME -> "aws"
    KeyBackend.AZURE_REST_API -> "azure-rest-api"
    KeyBackend.AZURE -> "azure"
}

private fun KeyAlgorithm.toApiAlgorithm(): String = when (this) {
    KeyAlgorithm.ED25519 -> "Ed25519"
    KeyAlgorithm.SECP256R1 -> "secp256r1"
    KeyAlgorithm.SECP256K1 -> "secp256k1"
    KeyAlgorithm.RSA -> "RSA"
}
