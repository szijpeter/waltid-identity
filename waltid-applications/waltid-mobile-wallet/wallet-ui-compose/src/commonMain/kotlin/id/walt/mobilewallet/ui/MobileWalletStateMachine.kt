package id.walt.mobilewallet.ui

import id.walt.mobilewallet.domain.usecase.AcceptIssuanceUseCase
import id.walt.mobilewallet.domain.usecase.GetCredentialUseCase
import id.walt.mobilewallet.domain.usecase.HandleScannedRequestUseCase
import id.walt.mobilewallet.domain.usecase.ListCredentialsUseCase
import id.walt.mobilewallet.domain.usecase.ListDidsUseCase
import id.walt.mobilewallet.domain.usecase.ListKeysUseCase
import id.walt.mobilewallet.domain.LoginUseCase
import id.walt.mobilewallet.domain.usecase.ResolveIssuanceUseCase
import id.walt.mobilewallet.domain.usecase.ResolvePresentationUseCase
import id.walt.mobilewallet.domain.usecase.SetDefaultDidUseCase
import id.walt.mobilewallet.domain.usecase.SignVerifyUseCase
import id.walt.mobilewallet.domain.usecase.SubmitPresentationUseCase
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.domain.getOrNull
import id.walt.mobilewallet.domain.onFailure
import id.walt.mobilewallet.domain.onSuccess
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.KeyVerifyRequest
import id.walt.mobilewallet.model.LoginCredentials
import id.walt.mobilewallet.model.PresentationRequest
import id.walt.mobilewallet.model.PresentationSelection
import id.walt.mobilewallet.model.ScannedRequestKind
import id.walt.mobilewallet.model.WalletId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MobileWalletStateMachine(
    private val loginUseCase: LoginUseCase,
    private val listCredentialsUseCase: ListCredentialsUseCase,
    private val getCredentialUseCase: GetCredentialUseCase,
    private val handleScannedRequestUseCase: HandleScannedRequestUseCase,
    private val resolveIssuanceUseCase: ResolveIssuanceUseCase,
    private val acceptIssuanceUseCase: AcceptIssuanceUseCase,
    private val resolvePresentationUseCase: ResolvePresentationUseCase,
    private val submitPresentationUseCase: SubmitPresentationUseCase,
    private val listDidsUseCase: ListDidsUseCase,
    private val setDefaultDidUseCase: SetDefaultDidUseCase,
    private val listKeysUseCase: ListKeysUseCase,
    private val signVerifyUseCase: SignVerifyUseCase,
) {
    private val _state = MutableStateFlow(WalletUiState())
    val state: StateFlow<WalletUiState> = _state

    fun updateLoginEmail(value: String) {
        _state.update { it.copy(loginEmail = value) }
    }

    fun updateLoginPassword(value: String) {
        _state.update { it.copy(loginPassword = value) }
    }

    suspend fun submitLogin() {
        if (state.value.isLoading) return
        _state.update { it.copy(isLoading = true, lastError = null) }
        val credentials = LoginCredentials(
            email = state.value.loginEmail,
            password = state.value.loginPassword,
        )
        loginUseCase(credentials)
            .onSuccess { session ->
                _state.update {
                    it.copy(
                        loginEmail = "",
                        loginPassword = "",
                        isLoading = false,
                    )
                }
                bootstrap(session.walletId)
            }
            .onFailure { error ->
                consumeError(error)
                _state.update { it.copy(isLoading = false) }
            }
    }

    suspend fun bootstrap(walletId: WalletId) {
        _state.update {
            it.copy(
                walletId = walletId,
                route = WalletRoute.Dashboard,
                isLoading = true,
                lastError = null,
            )
        }

        listCredentialsUseCase(walletId).onSuccess { credentials ->
            _state.update { current ->
                current.copy(credentials = credentials)
            }
        }.onFailure(::consumeError)

        listDidsUseCase(walletId).onSuccess { dids ->
            _state.update { current ->
                current.copy(dids = dids)
            }
        }.onFailure(::consumeError)

        listKeysUseCase(walletId).onSuccess { keys ->
            _state.update { current ->
                current.copy(keys = keys)
            }
        }.onFailure(::consumeError)

        _state.update { it.copy(isLoading = false) }
    }

    suspend fun refreshDashboard() {
        val walletId = state.value.walletId ?: return
        _state.update { it.copy(isLoading = true, route = WalletRoute.Dashboard, lastError = null) }
        listCredentialsUseCase(walletId).onSuccess { credentials ->
            _state.update { current -> current.copy(credentials = credentials) }
        }.onFailure(::consumeError)
        _state.update { it.copy(isLoading = false) }
    }

    fun updateScanInput(value: String) {
        _state.update { it.copy(scanInput = value, route = WalletRoute.Scan, lastError = null) }
    }

    suspend fun submitIncomingRequest(rawRequest: String) {
        if (state.value.isLoading) return
        updateScanInput(rawRequest)
        handleScanInput()
    }

    fun dismissError() {
        _state.update { it.copy(lastError = null) }
    }

    fun cancelCurrentFlow() {
        _state.update {
            it.copy(
                route = WalletRoute.Dashboard,
                pendingIssuance = null,
                pendingPresentation = null,
                scanInput = "",
                isLoading = false,
                lastError = null,
            )
        }
    }

    suspend fun handleScanInput() {
        if (state.value.isLoading) return
        val walletId = state.value.walletId ?: return
        val raw = state.value.scanInput
        val classification = handleScannedRequestUseCase.classify(raw)
        _state.update { it.copy(isLoading = true, lastError = null) }

        when (val scanResult = handleScannedRequestUseCase(raw)) {
            is WalletResult.Failure -> consumeError(scanResult.error)
            is WalletResult.Success -> when (classification.kind) {
                ScannedRequestKind.ISSUANCE -> resolveIssuance(walletId, raw)
                ScannedRequestKind.PRESENTATION -> resolvePresentation(walletId, raw, classification.protocolMode)
                ScannedRequestKind.UNKNOWN -> _state.update {
                    it.copy(lastError = WalletError.Unsupported("Unknown scan request type."))
                }
            }
        }

        _state.update { it.copy(isLoading = false) }
    }

    suspend fun acceptIssuance(didId: DidId? = null) {
        if (state.value.isLoading) return
        val walletId = state.value.walletId ?: return
        val request = state.value.scanInput
        _state.update { it.copy(isLoading = true, lastError = null) }
        acceptIssuanceUseCase(
            IssuanceRequest(
                walletId = walletId,
                rawRequest = request,
                did = didId,
            )
        ).onSuccess {
            _state.update { current ->
                current.copy(
                    route = WalletRoute.Dashboard,
                    pendingIssuance = null,
                    scanInput = "",
                )
            }
            refreshDashboard()
        }.onFailure(::consumeError)
        _state.update { it.copy(isLoading = false) }
    }

    suspend fun submitPresentation(selectedCredentialIds: List<CredentialId>, disclosures: Map<String, List<String>>) {
        if (state.value.isLoading) return
        val pending = state.value.pendingPresentation ?: return
        _state.update { it.copy(isLoading = true, lastError = null) }
        submitPresentationUseCase(
            PresentationSelection(
                request = pending.request,
                selectedCredentialIds = selectedCredentialIds,
                disclosures = disclosures,
            )
        ).onSuccess {
            _state.update { current ->
                current.copy(
                    route = WalletRoute.Dashboard,
                    pendingPresentation = null,
                    scanInput = "",
                )
            }
        }.onFailure(::consumeError)
        _state.update { it.copy(isLoading = false) }
    }

    suspend fun openCredentialDetail(credentialId: CredentialId) {
        val walletId = state.value.walletId ?: return
        _state.update { it.copy(isLoading = true, lastError = null) }
        getCredentialUseCase(walletId, credentialId).onSuccess { detail ->
            _state.update { current ->
                current.copy(
                    route = WalletRoute.CredentialDetail(credentialId),
                    credentialDetail = detail,
                    isLoading = false,
                )
            }
        }.onFailure {
            consumeError(it)
            _state.update { current -> current.copy(isLoading = false) }
        }
    }

    suspend fun openDidSettings() {
        val walletId = state.value.walletId ?: return
        _state.update { it.copy(isLoading = true, lastError = null) }
        listDidsUseCase(walletId).onSuccess { dids ->
            _state.update { current ->
                current.copy(route = WalletRoute.DidSettings, dids = dids, isLoading = false)
            }
        }.onFailure {
            consumeError(it)
            _state.update { current -> current.copy(isLoading = false) }
        }
    }

    suspend fun setDefaultDid(didId: DidId) {
        val walletId = state.value.walletId ?: return
        setDefaultDidUseCase(walletId, didId).onSuccess {
            openDidSettings()
        }.onFailure(::consumeError)
    }

    suspend fun openKeySettings() {
        val walletId = state.value.walletId ?: return
        _state.update { it.copy(isLoading = true, lastError = null) }
        listKeysUseCase(walletId).onSuccess { keys ->
            _state.update { current ->
                current.copy(route = WalletRoute.KeySettings, keys = keys, isLoading = false)
            }
        }.onFailure {
            consumeError(it)
            _state.update { current -> current.copy(isLoading = false) }
        }
    }

    suspend fun verifySignature(jwk: String, signature: String): Boolean {
        val walletId = state.value.walletId ?: return false
        return signVerifyUseCase.verify(
            walletId = walletId,
            request = KeyVerifyRequest(
                jwk = jwk,
                signedPayload = signature,
            )
        ).getOrNull()?.valid ?: false
    }

    private suspend fun resolveIssuance(walletId: WalletId, raw: String) {
        resolveIssuanceUseCase(IssuanceRequest(walletId = walletId, rawRequest = raw))
            .onSuccess { preview ->
                _state.update { current ->
                    current.copy(
                        route = WalletRoute.Issuance,
                        pendingIssuance = preview,
                        pendingPresentation = null,
                    )
                }
            }
            .onFailure(::consumeError)
    }

    private suspend fun resolvePresentation(
        walletId: WalletId,
        raw: String,
        protocolMode: id.walt.mobilewallet.model.ProtocolMode,
    ) {
        val host = extractHost(raw)
        resolvePresentationUseCase(
            PresentationRequest(
                walletId = walletId,
                rawRequest = raw,
                verifierHost = host,
                requestedCredentialTypes = emptyList(),
                protocolMode = protocolMode,
            )
        ).onSuccess { resolution ->
            _state.update { current ->
                current.copy(
                    route = WalletRoute.Presentation,
                    pendingPresentation = resolution,
                    pendingIssuance = null,
                )
            }
        }.onFailure(::consumeError)
    }

    private fun consumeError(error: WalletError) {
        _state.update { it.copy(lastError = error) }
    }

    private fun extractHost(rawRequest: String): String {
        val lower = rawRequest.lowercase()
        val responseUri = regexExtract(lower, "(response_uri|redirect_uri)=([^&]+)")
        if (responseUri.isNotBlank()) {
            return responseUri.substringAfter("://").substringBefore('/').substringBefore('?')
        }
        return rawRequest.substringAfter("://", "unknown").substringBefore('/').substringBefore('?')
    }

    private fun regexExtract(input: String, pattern: String): String {
        val regex = Regex(pattern)
        return regex.find(input)?.groupValues?.getOrNull(2).orEmpty()
    }
}
