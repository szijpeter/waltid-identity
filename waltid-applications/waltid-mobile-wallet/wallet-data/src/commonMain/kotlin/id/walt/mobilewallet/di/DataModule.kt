package id.walt.mobilewallet.di

import id.walt.mobilewallet.data.ApiAuthRepository
import id.walt.mobilewallet.data.ApiCredentialRepository
import id.walt.mobilewallet.data.ApiDidRepository
import id.walt.mobilewallet.data.ApiExchangeRepository
import id.walt.mobilewallet.data.ApiKeyRepository
import id.walt.mobilewallet.data.KtorWalletBackendApi
import id.walt.mobilewallet.data.WalletBackendApi
import id.walt.mobilewallet.domain.AuthRepository
import id.walt.mobilewallet.domain.CredentialRepository
import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.KeyRepository
import kotlinx.serialization.json.Json
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    single {
        KtorWalletBackendApi(
            baseUrl = get(),
            httpClient = get(),
            json = get(),
        )
    } bind WalletBackendApi::class

    single {
        ApiAuthRepository(
            backendApi = get(),
            secureStateStore = get(),
            onTokenReceived = get()
        )
    } bind AuthRepository::class

    single { ApiCredentialRepository(get()) } bind CredentialRepository::class
    single { ApiDidRepository(get()) } bind DidRepository::class
    single { ApiKeyRepository(get()) } bind KeyRepository::class
    single { ApiExchangeRepository(get()) } bind ExchangeRepository::class
}
