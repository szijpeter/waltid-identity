package id.walt.mobilewallet.app.ios

import com.russhwolf.settings.KeychainSettings
import id.walt.mobilewallet.data.KmpSecureStateStore
import id.walt.mobilewallet.di.initKoin
import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.ui.MobileWalletStateMachine
import id.walt.mobilewallet.ui.WalletAppRoot
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(baseUrl: String): UIViewController {
    val incomingRequests = MutableSharedFlow<String>(extraBufferCapacity = 64)
    
    val appModule = module {
        single { baseUrl }
        
        single<id.walt.mobilewallet.data.SecureStateStore> { 
            val keychainSettings = KeychainSettings(service = "waltid_mobile_wallet")
            KmpSecureStateStore(keychainSettings)
        }
        
        single {
            var currentTokens: BearerTokens? = null
            
            val httpClient = HttpClient(Darwin) {
                install(ContentNegotiation) {
                    json(get<Json>())
                }
                install(Auth) {
                    bearer {
                        loadTokens { currentTokens }
                    }
                }
            }
            
            Pair(httpClient, { token: String -> currentTokens = BearerTokens(token, "") })
        }
        
        single<HttpClient> { get<Pair<HttpClient, (String) -> Unit>>().first }
        single<(String) -> Unit> { get<Pair<HttpClient, (String) -> Unit>>().second }
    }

    initKoin {
        modules(appModule)
    }

    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch {
        val koin = org.koin.core.context.GlobalContext.get()
        val secureStore = koin.get<id.walt.mobilewallet.data.SecureStateStore>()
        val updateToken = koin.get<(String) -> Unit>()
        val stateMachine = koin.get<MobileWalletStateMachine>()
        
        val savedToken = secureStore.get("auth_token")
        val savedWalletId = secureStore.get("auth_wallet_id")
        
        if (savedToken != null && savedWalletId != null) {
            updateToken(savedToken)
            stateMachine.bootstrap(WalletId(savedWalletId))
        }
    }

    return ComposeUIViewController {
        WalletAppRoot(incomingRequests = incomingRequests.asSharedFlow())
    }
}
