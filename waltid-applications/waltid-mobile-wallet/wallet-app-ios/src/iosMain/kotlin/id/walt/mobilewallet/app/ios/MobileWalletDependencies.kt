package id.walt.mobilewallet.app.ios

import com.russhwolf.settings.KeychainSettings
import dev.icerock.moko.biometry.BiometryAuthenticator
import id.walt.mobilewallet.data.ApiAuthRepository
import id.walt.mobilewallet.data.ApiCredentialRepository
import id.walt.mobilewallet.data.ApiDidRepository
import id.walt.mobilewallet.data.ApiExchangeRepository
import id.walt.mobilewallet.data.ApiKeyRepository
import id.walt.mobilewallet.data.InMemorySecurityRepository
import id.walt.mobilewallet.data.KmpSecureStateStore
import id.walt.mobilewallet.data.KtorWalletBackendApi
import id.walt.mobilewallet.domain.AcceptIssuanceUseCase
import id.walt.mobilewallet.domain.GetCredentialUseCase
import id.walt.mobilewallet.domain.HandleScannedRequestUseCase
import id.walt.mobilewallet.domain.ListCredentialsUseCase
import id.walt.mobilewallet.domain.ListDidsUseCase
import id.walt.mobilewallet.domain.ListKeysUseCase
import id.walt.mobilewallet.domain.LoginUseCase
import id.walt.mobilewallet.domain.ResolveIssuanceUseCase
import id.walt.mobilewallet.domain.ResolvePresentationUseCase
import id.walt.mobilewallet.domain.SetDefaultDidUseCase
import id.walt.mobilewallet.domain.SignVerifyUseCase
import id.walt.mobilewallet.domain.SubmitPresentationUseCase
import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.ui.MobileWalletStateMachine
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun createMobileWalletDependencies(baseUrl: String): MobileWalletDependencies {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val keychainSettings = KeychainSettings(service = "waltid_mobile_wallet")
    val secureStore = KmpSecureStateStore(keychainSettings)
    val securityRepository = InMemorySecurityRepository()

    var currentTokens: BearerTokens? = null

    val httpClient = HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Auth) {
            bearer {
                loadTokens { currentTokens }
            }
        }
    }

    val backendApi = KtorWalletBackendApi(
        baseUrl = baseUrl,
        httpClient = httpClient,
        json = json,
    )

    val authRepository = ApiAuthRepository(
        backendApi = backendApi,
        secureStateStore = secureStore,
        onTokenReceived = { token ->
            currentTokens = BearerTokens(token, "")
        }
    )

    val credentialRepository = ApiCredentialRepository(backendApi)
    val didRepository = ApiDidRepository(backendApi)
    val keyRepository = ApiKeyRepository(backendApi)
    val exchangeRepository = ApiExchangeRepository(backendApi)

    val stateMachine = MobileWalletStateMachine(
        loginUseCase = LoginUseCase(authRepository),
        listCredentialsUseCase = ListCredentialsUseCase(credentialRepository),
        getCredentialUseCase = GetCredentialUseCase(credentialRepository),
        handleScannedRequestUseCase = HandleScannedRequestUseCase(),
        resolveIssuanceUseCase = ResolveIssuanceUseCase(
            exchangeRepository = exchangeRepository,
            didRepository = didRepository,
        ),
        acceptIssuanceUseCase = AcceptIssuanceUseCase(exchangeRepository),
        resolvePresentationUseCase = ResolvePresentationUseCase(exchangeRepository),
        submitPresentationUseCase = SubmitPresentationUseCase(exchangeRepository),
        listDidsUseCase = ListDidsUseCase(didRepository),
        setDefaultDidUseCase = SetDefaultDidUseCase(didRepository),
        listKeysUseCase = ListKeysUseCase(keyRepository),
        signVerifyUseCase = SignVerifyUseCase(keyRepository),
    )

    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch {
        val savedToken = secureStore.get("auth_token")
        val savedWalletId = secureStore.get("auth_wallet_id")

        if (savedToken != null && savedWalletId != null) {
            currentTokens = BearerTokens(savedToken, "")
            stateMachine.bootstrap(WalletId(savedWalletId))
        }
    }

    return MobileWalletDependencies(
        stateMachine = stateMachine,
        httpClient = httpClient,
    )
}

class MobileWalletDependencies(
    val stateMachine: MobileWalletStateMachine,
    private val httpClient: HttpClient,
) {
    fun close() {
        httpClient.close()
    }
}
