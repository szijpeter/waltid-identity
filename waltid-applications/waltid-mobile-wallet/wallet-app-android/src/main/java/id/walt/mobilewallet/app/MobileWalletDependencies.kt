package id.walt.mobilewallet.app

import id.walt.mobilewallet.data.ApiCredentialRepository
import id.walt.mobilewallet.data.ApiDidRepository
import id.walt.mobilewallet.data.ApiExchangeRepository
import id.walt.mobilewallet.data.ApiKeyRepository
import id.walt.mobilewallet.data.KtorWalletBackendApi
import id.walt.mobilewallet.domain.AcceptIssuanceUseCase
import id.walt.mobilewallet.domain.GetCredentialUseCase
import id.walt.mobilewallet.domain.HandleScannedRequestUseCase
import id.walt.mobilewallet.domain.ListCredentialsUseCase
import id.walt.mobilewallet.domain.ListDidsUseCase
import id.walt.mobilewallet.domain.ListKeysUseCase
import id.walt.mobilewallet.domain.ResolveIssuanceUseCase
import id.walt.mobilewallet.domain.ResolvePresentationUseCase
import id.walt.mobilewallet.domain.SetDefaultDidUseCase
import id.walt.mobilewallet.domain.SignVerifyUseCase
import id.walt.mobilewallet.domain.SubmitPresentationUseCase
import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.ui.MobileWalletStateMachine
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MobileWalletDependencies private constructor(
    val walletId: WalletId,
    val stateMachine: MobileWalletStateMachine,
    private val httpClient: HttpClient,
) {
    fun close() {
        httpClient.close()
    }

    companion object {
        fun create(config: MobileWalletRuntimeConfig): MobileWalletDependencies {
            val json = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

            val httpClient = HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(json)
                }
                install(DefaultRequest) {
                    config.bearerToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                }
            }

            val backendApi = KtorWalletBackendApi(
                baseUrl = config.baseUrl,
                httpClient = httpClient,
                json = json,
            )

            val credentialRepository = ApiCredentialRepository(backendApi)
            val didRepository = ApiDidRepository(backendApi)
            val keyRepository = ApiKeyRepository(backendApi)
            val exchangeRepository = ApiExchangeRepository(backendApi)

            val stateMachine = MobileWalletStateMachine(
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

            return MobileWalletDependencies(
                walletId = config.walletId,
                stateMachine = stateMachine,
                httpClient = httpClient,
            )
        }
    }
}
