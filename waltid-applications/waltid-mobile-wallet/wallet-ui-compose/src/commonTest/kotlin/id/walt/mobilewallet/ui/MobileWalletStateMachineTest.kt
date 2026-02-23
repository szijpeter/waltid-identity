package id.walt.mobilewallet.ui

import id.walt.mobilewallet.domain.AcceptIssuanceUseCase
import id.walt.mobilewallet.domain.AuthRepository
import id.walt.mobilewallet.domain.CredentialRepository
import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.GenerateKeyUseCase
import id.walt.mobilewallet.domain.GetCredentialUseCase
import id.walt.mobilewallet.domain.HandleScannedRequestUseCase
import id.walt.mobilewallet.domain.ImportDidUseCase
import id.walt.mobilewallet.domain.ImportKeyUseCase
import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.LoginUseCase
import id.walt.mobilewallet.domain.ListCredentialsUseCase
import id.walt.mobilewallet.domain.ListDidsUseCase
import id.walt.mobilewallet.domain.ListKeysUseCase
import id.walt.mobilewallet.domain.ResolveIssuanceUseCase
import id.walt.mobilewallet.domain.ResolvePresentationUseCase
import id.walt.mobilewallet.domain.SetDefaultDidUseCase
import id.walt.mobilewallet.domain.SignVerifyUseCase
import id.walt.mobilewallet.domain.SubmitPresentationUseCase
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialFormat
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidCreateRequest
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.DidMethod
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.KeyAlgorithm
import id.walt.mobilewallet.model.KeyBackend
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.KeyExportRequest
import id.walt.mobilewallet.model.KeyGenerateRequest
import id.walt.mobilewallet.model.KeyId
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
import id.walt.mobilewallet.model.AuthSession
import id.walt.mobilewallet.model.LoginCredentials
import id.walt.mobilewallet.model.orThrow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class MobileWalletStateMachineTest {

    @Test
    fun bootstrapLoadsDashboardData() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()

        machine.bootstrap(walletId)
        val state = machine.state.value

        assertEquals(walletId, state.walletId)
        assertEquals(1, state.credentials.size)
        assertEquals(1, state.dids.size)
        assertEquals(1, state.keys.size)
        assertIs<WalletRoute.Dashboard>(state.route)
    }

    @Test
    fun scanIssuanceNavigatesToIssuanceFlow() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()
        machine.bootstrap(walletId)

        machine.updateScanInput("openid-initiate-issuance://offer?credential_offer=abc")
        machine.handleScanInput()

        val state = machine.state.value
        assertIs<WalletRoute.Issuance>(state.route)
        assertNotNull(state.pendingIssuance)
    }

    @Test
    fun scanPresentationNavigatesToPresentationFlow() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()
        machine.bootstrap(walletId)

        machine.updateScanInput("openid4vp://authorize?response_type=vp_token&response_uri=https://verifier.example.com")
        machine.handleScanInput()

        val state = machine.state.value
        assertIs<WalletRoute.Presentation>(state.route)
        assertNotNull(state.pendingPresentation)
    }

    @Test
    fun scanPresentationWithClientIdSchemeUsesOpenId4vp10Mode() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()
        machine.bootstrap(walletId)

        machine.updateScanInput(
            "openid4vp://authorize?response_type=vp_token&client_id_scheme=x509_san_dns&response_uri=https://verifier.example.com"
        )
        machine.handleScanInput()

        val pendingPresentation = assertNotNull(machine.state.value.pendingPresentation)
        assertEquals(ProtocolMode.OPENID4VP_1_0, pendingPresentation.request.protocolMode)
    }

    @Test
    fun scanPresentationWithPresentationDefinitionUriUsesOpenId4vp10Mode() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()
        machine.bootstrap(walletId)

        machine.updateScanInput(
            "openid4vp://authorize?response_type=vp_token&presentation_definition_uri=https://verifier.example.com/pd.json"
        )
        machine.handleScanInput()

        val pendingPresentation = assertNotNull(machine.state.value.pendingPresentation)
        assertEquals(ProtocolMode.OPENID4VP_1_0, pendingPresentation.request.protocolMode)
    }

    @Test
    fun scanUnknownRequestSetsUnsupportedError() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()
        machine.bootstrap(walletId)

        machine.updateScanInput("https://example.org/not-supported")
        machine.handleScanInput()

        val error = assertIs<WalletError.Unsupported>(machine.state.value.lastError)
        assertEquals("Unknown scan request type.", error.message)
    }

    @Test
    fun openCredentialLoadsDetail() = runTest {
        val machine = buildStateMachine()
        val walletId = WalletId.validate("wallet-ui-test").orThrow()
        machine.bootstrap(walletId)

        val credentialId = CredentialId.validate("cred-1").orThrow()
        machine.openCredentialDetail(credentialId)

        val state = machine.state.value
        assertIs<WalletRoute.CredentialDetail>(state.route)
        assertNotNull(state.credentialDetail)
    }
}

private fun buildStateMachine(): MobileWalletStateMachine {
    val credentialRepository = FakeCredentialRepository()
    val didRepository = FakeDidRepository()
    val keyRepository = FakeKeyRepository()
    val exchangeRepository = FakeExchangeRepository(credentialRepository.walletCredential)
    val authRepository = FakeAuthRepository()

    return MobileWalletStateMachine(
        loginUseCase = LoginUseCase(authRepository),
        listCredentialsUseCase = ListCredentialsUseCase(credentialRepository),
        getCredentialUseCase = GetCredentialUseCase(credentialRepository),
        handleScannedRequestUseCase = HandleScannedRequestUseCase(),
        resolveIssuanceUseCase = ResolveIssuanceUseCase(exchangeRepository, didRepository),
        acceptIssuanceUseCase = AcceptIssuanceUseCase(exchangeRepository),
        resolvePresentationUseCase = ResolvePresentationUseCase(exchangeRepository),
        submitPresentationUseCase = SubmitPresentationUseCase(exchangeRepository),
        listDidsUseCase = ListDidsUseCase(didRepository),
        setDefaultDidUseCase = SetDefaultDidUseCase(didRepository),
        listKeysUseCase = ListKeysUseCase(keyRepository),
        signVerifyUseCase = SignVerifyUseCase(keyRepository),
    )
}

private class FakeAuthRepository : AuthRepository {
    override suspend fun login(credentials: LoginCredentials): WalletResult<AuthSession> =
        WalletResult.Success(AuthSession(token = "fake-token", walletId = WalletId.validate("wallet-ui-test").orThrow()))
}

private class FakeCredentialRepository : CredentialRepository {
    private val walletId = WalletId.validate("wallet-ui-test").orThrow()
    val walletCredential = WalletCredential(
        walletId = walletId,
        id = CredentialId.validate("cred-1").orThrow(),
        format = CredentialFormat.W3C_JWT_VC_JSON,
        document = "eyJhbGciOiJub25lIn0.eyJ2YyI6eyJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiT3BlbkJhZGdlQ3JlZGVudGlhbCJdfX0.",
    )
    private val summary = CredentialSummary(
        id = walletCredential.id,
        format = walletCredential.format,
        title = "OpenBadgeCredential",
    )
    private val detail = CredentialDetail(
        summary = summary,
        credential = walletCredential,
    )

    override suspend fun listCredentials(walletId: WalletId): WalletResult<List<CredentialSummary>> =
        WalletResult.Success(listOf(summary))

    override suspend fun getCredential(walletId: WalletId, credentialId: CredentialId): WalletResult<CredentialDetail> =
        WalletResult.Success(detail)

    override suspend fun listWalletCredentials(walletId: WalletId): WalletResult<List<WalletCredential>> =
        WalletResult.Success(listOf(walletCredential))
}

private class FakeDidRepository : DidRepository {
    private var defaultDid = DidId.validate("did:key:z6Mki123").orThrow()
    private val dids = mutableListOf(
        DidDescriptor(
            id = defaultDid,
            alias = "Default DID",
            method = DidMethod.KEY,
            isDefault = true,
        )
    )

    override suspend fun listDids(walletId: WalletId): WalletResult<List<DidDescriptor>> = WalletResult.Success(dids)

    override suspend fun createDid(walletId: WalletId, request: DidCreateRequest): WalletResult<DidDescriptor> {
        val created = DidDescriptor(
            id = DidId.validate("did:${request.method.name.lowercase()}:created").orThrow(),
            alias = request.alias,
            method = request.method,
            isDefault = false,
        )
        dids += created
        return WalletResult.Success(created)
    }

    override suspend fun importDid(walletId: WalletId, request: DidImportRequest): WalletResult<DidDescriptor> {
        val imported = DidDescriptor(
            id = DidId.validate(request.did).orThrow(),
            alias = request.alias,
            method = DidMethod.KEY,
            isDefault = false,
        )
        dids += imported
        return WalletResult.Success(imported)
    }

    override suspend fun deleteDid(walletId: WalletId, didId: DidId): WalletResult<Boolean> {
        dids.removeAll { it.id == didId }
        return WalletResult.Success(true)
    }

    override suspend fun setDefaultDid(walletId: WalletId, didId: DidId): WalletResult<Boolean> {
        defaultDid = didId
        dids.replaceAll { it.copy(isDefault = it.id == didId) }
        return WalletResult.Success(true)
    }
}

private class FakeKeyRepository : KeyRepository {
    private val key = KeyDescriptor(
        id = KeyId.validate("key-1").orThrow(),
        alias = "Main key",
        algorithm = KeyAlgorithm.ED25519,
        backend = KeyBackend.JWK,
    )

    override suspend fun listKeys(walletId: WalletId): WalletResult<List<KeyDescriptor>> = WalletResult.Success(listOf(key))

    override suspend fun generateKey(walletId: WalletId, request: KeyGenerateRequest): WalletResult<KeyDescriptor> =
        WalletResult.Success(key)

    override suspend fun importKey(walletId: WalletId, request: KeyImportRequest): WalletResult<KeyDescriptor> =
        WalletResult.Success(key)

    override suspend fun exportKey(walletId: WalletId, request: KeyExportRequest): WalletResult<String> =
        WalletResult.Success("exported")

    override suspend fun sign(walletId: WalletId, request: KeySignRequest): WalletResult<String> =
        WalletResult.Success("signed")

    override suspend fun verify(walletId: WalletId, request: KeyVerifyRequest): WalletResult<KeyVerifyResult> =
        WalletResult.Success(KeyVerifyResult(valid = true))
}

private class FakeExchangeRepository(
    private val walletCredential: WalletCredential,
) : ExchangeRepository {
    override suspend fun resolveIssuance(request: IssuanceRequest): WalletResult<IssuancePreview> =
        WalletResult.Success(
            IssuancePreview(
                issuerHost = "issuer.example.org",
                credentialTypes = listOf("OpenBadgeCredential"),
            )
        )

    override suspend fun acceptIssuance(request: IssuanceRequest): WalletResult<List<CredentialId>> =
        WalletResult.Success(listOf(walletCredential.id))

    override suspend fun resolvePresentation(request: PresentationRequest): WalletResult<List<WalletCredential>> =
        WalletResult.Success(listOf(walletCredential))

    override suspend fun submitPresentation(selection: PresentationSelection): WalletResult<PresentationSubmissionResult> =
        WalletResult.Success(
            PresentationSubmissionResult(
                success = true,
                redirectUri = "https://verifier.example.org/success",
            )
        )
}
