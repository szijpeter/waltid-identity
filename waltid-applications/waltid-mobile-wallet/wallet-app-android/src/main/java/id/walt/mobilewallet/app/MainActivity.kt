package id.walt.mobilewallet.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import id.walt.mobilewallet.data.KmpSecureStateStore
import id.walt.mobilewallet.di.initKoin
import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.ui.MobileWalletStateMachine
import id.walt.mobilewallet.ui.WalletAppRoot
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    private val incomingRequests = Channel<String>(capacity = Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val startupError = runCatching {
            val runtimeConfig = MobileWalletRuntimeConfig.fromBuildConfig()
            
            val appModule = module {
                single { runtimeConfig.baseUrl }
                
                single<id.walt.mobilewallet.data.SecureStateStore> {
                    val context = get<android.content.Context>()
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()
        
                    val sharedPreferences = EncryptedSharedPreferences.create(
                        context,
                        "secure_wallet_prefs",
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
        
                    KmpSecureStateStore(SharedPreferencesSettings(sharedPreferences))
                }
                
                single {
                    var currentTokens: BearerTokens? = null
                    
                    val httpClient = HttpClient(OkHttp) {
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
                androidContext(this@MainActivity.applicationContext)
                modules(appModule)
            }
            
            // Auto-restore session from secure storage
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
        }.exceptionOrNull()

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                if (startupError != null) {
                    // Quick fallback since we removed StartupErrorScreen, 
                    // ideally we'd have a non-app fallback composable here.
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.Text("Wallet startup error", color = MaterialTheme.colorScheme.error)
                        androidx.compose.material3.Text(startupError.message ?: "Unknown error")
                    }
                } else {
                    WalletAppRoot(
                        incomingRequests = incomingRequests.receiveAsFlow(),
                    )
                }
            }
        }
        queueIncomingRequest(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        queueIncomingRequest(intent)
    }

    override fun onDestroy() {
        stopKoin()
        super.onDestroy()
    }

    private fun queueIncomingRequest(intent: Intent?) {
        val rawRequest = intent?.dataString?.trim().orEmpty()
        if (rawRequest.isNotBlank()) {
            incomingRequests.trySend(rawRequest)
        }
    }
}
