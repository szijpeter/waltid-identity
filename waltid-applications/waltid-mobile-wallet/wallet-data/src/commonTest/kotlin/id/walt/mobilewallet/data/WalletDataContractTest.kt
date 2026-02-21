package id.walt.mobilewallet.data

import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.CredentialFormat
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.DidMethod
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.PresentationRequest
import id.walt.mobilewallet.model.ProtocolMode
import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.model.orThrow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WalletDataContractTest {
    @Test
    fun requestClassifierDetectsOpenId4vp10ByDcql() {
        val classified = OpenIdRequestClassifier.classify(
            "openid4vp://authorize?response_type=vp_token&dcql_query=%7B%7D"
        )

        assertEquals(id.walt.mobilewallet.model.ScannedRequestKind.PRESENTATION, classified.kind)
        assertEquals(ProtocolMode.OPENID4VP_1_0, classified.protocolMode)
    }

    @Test
    fun resolveIssuanceParsesCredentialConfigurationTypes() = runTest {
        val backend = FakeBackendApi().apply {
            offer = ApiResolveCredentialOfferDto(
                credential_issuer = "https://issuer.example.org",
                credential_configuration_ids = listOf("OpenBadgeCredential_jwt"),
            )
            issuerMetadata = Json.parseToJsonElement(
                """
                {
                  "credential_configurations_supported": {
                    "OpenBadgeCredential_jwt": {
                      "types": ["VerifiableCredential", "OpenBadgeCredential"],
                      "format": "jwt_vc_json"
                    }
                  }
                }
                """.trimIndent()
            ).jsonObject
        }

        val repository = ApiExchangeRepository(backend)
        val walletId = WalletId.validate("wallet-data-test").orThrow()
        val result = repository.resolveIssuance(
            IssuanceRequest(
                walletId = walletId,
                rawRequest = "openid-initiate-issuance://?credential_offer=abc",
            )
        )

        val preview = assertIs<id.walt.mobilewallet.domain.WalletResult.Success<id.walt.mobilewallet.model.IssuancePreview>>(result).value
        assertEquals("issuer.example.org", preview.issuerHost)
        assertEquals(listOf("OpenBadgeCredential"), preview.credentialTypes)
    }

    @Test
    fun resolvePresentationParsesMatchedCredentials() = runTest {
        val backend = FakeBackendApi().apply {
            resolvedPresentationRequest =
                "openid4vp://authorize?response_type=vp_token&presentation_definition=%7B%22id%22%3A%22pd-1%22%2C%22input_descriptors%22%3A%5B%5D%7D"
            matchedCredentials = listOf(
                ApiWalletCredentialDto(
                    id = "cred-1",
                    document = "eyJhbGciOiJub25lIn0.eyJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiT3BlbkJhZGdlQ3JlZGVudGlhbCJdfX0.",
                    format = "jwt_vc_json",
                )
            )
        }

        val repository = ApiExchangeRepository(backend)
        val walletId = WalletId.validate("wallet-data-test").orThrow()
        val result = repository.resolvePresentation(
            PresentationRequest(
                walletId = walletId,
                rawRequest = "openid4vp://authorize?response_type=vp_token",
                verifierHost = "verifier.example.org",
                requestedCredentialTypes = emptyList(),
                protocolMode = ProtocolMode.DRAFT_COMPAT,
            )
        )

        val credentials = assertIs<id.walt.mobilewallet.domain.WalletResult.Success<List<id.walt.mobilewallet.model.WalletCredential>>>(result).value
        assertEquals(1, credentials.size)
        assertEquals(CredentialFormat.W3C_JWT_VC_JSON, credentials.first().format)
        assertEquals(CredentialId.validate("cred-1").orThrow(), credentials.first().id)
    }

    @Test
    fun resolvePresentationFailsWhenNoPresentationDefinitionInResolvedRequest() = runTest {
        val backend = FakeBackendApi().apply {
            resolvedPresentationRequest = "openid4vp://authorize?response_type=vp_token"
        }

        val repository = ApiExchangeRepository(backend)
        val walletId = WalletId.validate("wallet-data-test").orThrow()
        val result = repository.resolvePresentation(
            PresentationRequest(
                walletId = walletId,
                rawRequest = "openid4vp://authorize?response_type=vp_token",
                verifierHost = "verifier.example.org",
                requestedCredentialTypes = emptyList(),
                protocolMode = ProtocolMode.DRAFT_COMPAT,
            )
        )

        val error = assertIs<WalletResult.Failure>(result).error
        val validation = assertIs<WalletError.Validation>(error)
        assertEquals("invalid_argument", validation.issues.single().code)
    }

    @Test
    fun resolvePresentationFailsWhenPresentationDefinitionIsMalformed() = runTest {
        val backend = FakeBackendApi().apply {
            resolvedPresentationRequest = "openid4vp://authorize?presentation_definition=%7Bnot-valid-json"
        }

        val repository = ApiExchangeRepository(backend)
        val walletId = WalletId.validate("wallet-data-test").orThrow()
        val result = repository.resolvePresentation(
            PresentationRequest(
                walletId = walletId,
                rawRequest = "openid4vp://authorize?response_type=vp_token",
                verifierHost = "verifier.example.org",
                requestedCredentialTypes = emptyList(),
                protocolMode = ProtocolMode.DRAFT_COMPAT,
            )
        )

        val error = assertIs<WalletResult.Failure>(result).error
        val validation = assertIs<WalletError.Validation>(error)
        assertEquals("invalid_argument", validation.issues.single().code)
    }
}

private class FakeBackendApi : WalletBackendApi {
    var offer: ApiResolveCredentialOfferDto = ApiResolveCredentialOfferDto()
    var issuerMetadata: JsonObject = buildJsonObject { }
    var resolvedPresentationRequest: String = ""
    var matchedCredentials: List<ApiWalletCredentialDto> = emptyList()

    override suspend fun listCredentials(walletId: WalletId, showDeleted: Boolean, showPending: Boolean): List<ApiWalletCredentialDto> =
        error("Not used in this test")

    override suspend fun getCredential(walletId: WalletId, credentialId: String): ApiWalletCredentialDto =
        error("Not used in this test")

    override suspend fun listDids(walletId: WalletId): List<ApiWalletDidDto> = emptyList()
    override suspend fun createDid(walletId: WalletId, method: DidMethod, parameters: Map<String, String>): String =
        error("Not used in this test")

    override suspend fun importDid(walletId: WalletId, request: DidImportRequest): String =
        error("Not used in this test")

    override suspend fun deleteDid(walletId: WalletId, didId: DidId): Boolean = true
    override suspend fun setDefaultDid(walletId: WalletId, didId: DidId): Boolean = true
    override suspend fun listKeys(walletId: WalletId): List<ApiSingleKeyResponseDto> = emptyList()
    override suspend fun generateKey(walletId: WalletId, payload: JsonObject): String = "key-1"
    override suspend fun importKey(walletId: WalletId, material: String, alias: String?): String = "key-1"
    override suspend fun exportKey(walletId: WalletId, keyId: String, format: String, loadPrivateKey: Boolean): String = "exported"
    override suspend fun sign(walletId: WalletId, keyId: String, payload: kotlinx.serialization.json.JsonElement): String = "signed"
    override suspend fun verify(walletId: WalletId, jwk: String, signature: String): Boolean = true

    override suspend fun resolveCredentialOffer(walletId: WalletId, rawRequest: String): ApiResolveCredentialOfferDto = offer

    override suspend fun resolveIssuerOpenIdMetadata(walletId: WalletId, issuer: String): JsonObject = issuerMetadata

    override suspend fun resolveVct(walletId: WalletId, vct: String): ApiVctMetadataDto =
        ApiVctMetadataDto(name = "Resolved VCT")

    override suspend fun useOfferRequest(
        walletId: WalletId,
        rawRequest: String,
        did: DidId?,
    ): kotlinx.serialization.json.JsonElement? = buildJsonObject {
        put("credentialIds", kotlinx.serialization.json.buildJsonArray {
            add(JsonPrimitive("cred-1"))
        })
    }

    override suspend fun resolvePresentationRequest(walletId: WalletId, rawRequest: String): String =
        resolvedPresentationRequest

    override suspend fun matchCredentialsForPresentationDefinition(
        walletId: WalletId,
        presentationDefinition: JsonObject,
    ): List<ApiWalletCredentialDto> = matchedCredentials

    override suspend fun usePresentationRequest(
        walletId: WalletId,
        request: ApiUsePresentationRequestDto,
    ): ApiUsePresentationResponseDto = ApiUsePresentationResponseDto(redirectUri = "https://example.org")
}
