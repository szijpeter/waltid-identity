package id.walt.mobilewallet.app.ios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Entry point for iOS Compose testing.
 *
 * Currently, the full suite of UI flows (Dashboard, Scan, Issuance, Presentation)
 * is implemented natively in `wallet-app-android`. To achieve full Compose parity on iOS,
 * those shared compose components (Screens) should be moved to `wallet-ui-compose`.
 *
 * For now, iOS can either implement UI in SwiftUI consuming `MobileWalletDependencies`
 * or use this bridged controller once the UI screens are extracted to `wallet-ui-compose`.
 */
fun MainViewController(dependencies: MobileWalletDependencies): UIViewController = ComposeUIViewController {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("walt.id Mobile Wallet iOS App Parity (Compose Bridge)")
    }
}
