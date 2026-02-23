package id.walt.mobilewallet.data

import id.walt.mobilewallet.domain.AuthRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.AuthSession
import id.walt.mobilewallet.model.LoginCredentials
import id.walt.mobilewallet.model.WalletId

/**
 * [AuthRepository] that authenticates against the wallet backend API.
 *
 * After a successful login the received token is forwarded to [onTokenReceived]
 * so the Ktor `Auth / bearer` plugin picks it up for all subsequent requests.
 */
class ApiAuthRepository(
    private val backendApi: WalletBackendApi,
    private val secureStateStore: SecureStateStore,
    private val onTokenReceived: (String) -> Unit,
) : AuthRepository {

    override suspend fun login(credentials: LoginCredentials): WalletResult<AuthSession> = safeCall {
        val loginResponse = backendApi.login(credentials.email, credentials.password)
        val token = loginResponse.token
            ?: throw WalletApiException(message = "Login response did not contain a token.")

        val walletListing = backendApi.listWallets()
        val firstWallet = walletListing.wallets.firstOrNull()
            ?: throw WalletApiException(message = "No wallets found for this account.")

        val walletId = WalletId(firstWallet.id)

        // Persist token and wallet ID for session restoration
        secureStateStore.put("auth_token", token)
        secureStateStore.put("auth_wallet_id", walletId.value)

        // Hand token to the Ktor Auth plugin via the callback.
        onTokenReceived(token)

        AuthSession(token = token, walletId = walletId)
    }
}
