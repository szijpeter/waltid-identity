package id.walt.mobilewallet.di

import id.walt.mobilewallet.data.InMemorySecureStateStore
import id.walt.mobilewallet.data.SecureStateStore
import io.ktor.client.HttpClient
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinModuleTest {

    @Test
    fun verifyModules() {
        val mockAppModule = module {
            single<String> { "https://example.com" }
            single<SecureStateStore> { InMemorySecureStateStore() }
            single<HttpClient> { HttpClient() }
            single<(String) -> Unit> { {} }
        }
        
        val fullModule = module {
            includes(mockAppModule, dataModule, domainModule, uiModule)
        }

        fullModule.verify(
            extraTypes = listOf(
                io.ktor.client.engine.HttpClientEngine::class,
                io.ktor.client.HttpClientConfig::class
            )
        )
    }
}
