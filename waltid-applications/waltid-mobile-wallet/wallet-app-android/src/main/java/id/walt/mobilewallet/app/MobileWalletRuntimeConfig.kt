package id.walt.mobilewallet.app

import id.walt.mobilewallet.app.BuildConfig

/**
 * Runtime configuration for the mobile wallet Android host.
 *
 * Only the base URL is required at build time. Wallet ID and bearer token
 * are obtained dynamically after a successful login.
 */
data class MobileWalletRuntimeConfig(
    val baseUrl: String,
) {
    companion object {
        fun fromBuildConfig(): MobileWalletRuntimeConfig {
            val baseUrl = BuildConfig.WALLET_BASE_URL.trim()
            require(baseUrl.isNotBlank()) {
                "Missing wallet backend URL. Set 'mobileWalletBaseUrl' in gradle.properties " +
                    "or MOBILE_WALLET_BASE_URL env var."
            }
            return MobileWalletRuntimeConfig(baseUrl = baseUrl)
        }
    }
}
