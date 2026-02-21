package id.walt.mobilewallet.domain

import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidCreateRequest
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.DidMethod
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.IssuanceRequest
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
import id.walt.mobilewallet.model.ProtocolMode
import id.walt.mobilewallet.model.WalletCredential
import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.model.orThrow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class WalletUseCaseTest {

    @Test
    fun handleScannedRequestClassifiesIssuanceUrl() {
        val result = HandleScannedRequestUseCase()("openid-initiate-issuance://?credential_offer=123")
        val scanned = (result as WalletResult.Success).value
        assertEquals(id.walt.mobilewallet.model.ScannedRequestKind.ISSUANCE, scanned.kind)
    }

    @Test
    fun createDidRejectsOutOfScopeMethod() = runTest {
        val useCase = CreateDidUseCase(FakeDidRepository())
        val walletId = WalletId.validate("wallet-1").orThrow()
        val result = useCase(
            walletId = walletId,
            request = DidCreateRequest(method = DidMethod.EBSI),
        )

        assertTrue(result is WalletResult.Failure)
        assertIs<WalletError.Unsupported>((result as WalletResult.Failure).error)
    }

    @Test
    fun submitPresentationRejectsEmptySelection() = runTest {
        val useCase = SubmitPresentationUseCase(FakeExchangeRepository())
        val walletId = WalletId.validate("wallet-1").orThrow()
        val request = PresentationRequest(
            walletId = walletId,
            rawRequest = "openid4vp://authorize",
            verifierHost = "verifier.example.org",
            requestedCredentialTypes = emptyList(),
            protocolMode = ProtocolMode.DRAFT_COMPAT,
        )

        val result = useCase(
            PresentationSelection(
                request = request,
                selectedCredentialIds = emptyList(),
            )
        )

        assertTrue(result is WalletResult.Failure)
        assertIs<WalletError.Validation>((result as WalletResult.Failure).error)
    }

    @Test
    fun listCredentialsReturnsPayload() = runTest {
        val credentialRepository = object : CredentialRepository {
            override suspend fun listCredentials(walletId: WalletId): WalletResult<List<CredentialSummary>> =
                WalletResult.Success(
                    listOf(
                        CredentialSummary(
                            id = CredentialId.validate("cred-1").orThrow(),
                            format = id.walt.mobilewallet.model.CredentialFormat.W3C_JWT_VC_JSON,
                            title = "OpenBadgeCredential",
                        )
                    )
                )

            override suspend fun getCredential(
                walletId: WalletId,
                credentialId: CredentialId,
            ): WalletResult<CredentialDetail> = error("Not required for this test")

            override suspend fun listWalletCredentials(walletId: WalletId): WalletResult<List<WalletCredential>> =
                WalletResult.Success(emptyList())
        }

        val walletId = WalletId.validate("wallet-1").orThrow()
        val result = ListCredentialsUseCase(credentialRepository)(walletId)

        assertTrue(result is WalletResult.Success)
        assertFalse(result.getOrNull().isNullOrEmpty())
    }
}

private class FakeDidRepository : DidRepository {
    override suspend fun listDids(walletId: WalletId): WalletResult<List<DidDescriptor>> = WalletResult.Success(emptyList())
    override suspend fun createDid(walletId: WalletId, request: DidCreateRequest): WalletResult<DidDescriptor> =
        WalletResult.Success(
            DidDescriptor(
                id = DidId.validate("did:key:z6Mki123").orThrow(),
                method = request.method,
                alias = request.alias,
            )
        )

    override suspend fun importDid(walletId: WalletId, request: DidImportRequest): WalletResult<DidDescriptor> =
        WalletResult.Success(
            DidDescriptor(
                id = DidId.validate(request.did).orThrow(),
                method = DidMethod.KEY,
                alias = request.alias,
            )
        )

    override suspend fun deleteDid(walletId: WalletId, didId: DidId): WalletResult<Boolean> = WalletResult.Success(true)
    override suspend fun setDefaultDid(walletId: WalletId, didId: DidId): WalletResult<Boolean> = WalletResult.Success(true)
}

private class FakeExchangeRepository : ExchangeRepository {
    override suspend fun resolveIssuance(request: IssuanceRequest): WalletResult<IssuancePreview> =
        WalletResult.Success(IssuancePreview(issuerHost = "issuer.example.org", credentialTypes = listOf("ExampleCredential")))

    override suspend fun acceptIssuance(request: IssuanceRequest): WalletResult<List<CredentialId>> =
        WalletResult.Success(emptyList())

    override suspend fun resolvePresentation(request: PresentationRequest): WalletResult<List<WalletCredential>> =
        WalletResult.Success(emptyList())

    override suspend fun submitPresentation(selection: PresentationSelection): WalletResult<PresentationSubmissionResult> =
        WalletResult.Success(PresentationSubmissionResult(success = true))
}

private class FakeKeyRepository : KeyRepository {
    override suspend fun listKeys(walletId: WalletId): WalletResult<List<KeyDescriptor>> = WalletResult.Success(emptyList())
    override suspend fun generateKey(walletId: WalletId, request: KeyGenerateRequest): WalletResult<KeyDescriptor> =
        error("Not used in domain tests")

    override suspend fun importKey(walletId: WalletId, request: KeyImportRequest): WalletResult<KeyDescriptor> =
        error("Not used in domain tests")

    override suspend fun exportKey(walletId: WalletId, request: KeyExportRequest): WalletResult<String> =
        error("Not used in domain tests")

    override suspend fun sign(walletId: WalletId, request: KeySignRequest): WalletResult<String> =
        error("Not used in domain tests")

    override suspend fun verify(walletId: WalletId, request: KeyVerifyRequest): WalletResult<KeyVerifyResult> =
        error("Not used in domain tests")
}
