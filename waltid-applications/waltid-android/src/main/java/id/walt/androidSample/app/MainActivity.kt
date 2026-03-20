package id.walt.androidSample.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import id.walt.androidSample.app.deeplink.WalletDeepLinkBus
import id.walt.androidSample.app.navigation.AppNavHost
import id.walt.androidSample.theme.WaltIdAndroidSampleTheme

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialDeepLink = intent?.dataString
        WalletDeepLinkBus.publish(initialDeepLink)

        setContent {
            WaltIdAndroidSampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(
                        navController = rememberNavController(),
                        initialDeepLink = initialDeepLink,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        WalletDeepLinkBus.publish(intent.dataString)
    }
}
