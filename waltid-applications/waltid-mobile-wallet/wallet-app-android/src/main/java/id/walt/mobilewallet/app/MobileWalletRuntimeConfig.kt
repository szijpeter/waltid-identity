package id.walt.mobilewallet.app

import id.walt.mobilewallet.model.WalletId
import id.walt.mobilewallet.model.orThrow

data class MobileWalletRuntimeConfig(
    val baseUrl: String,
    val walletId: WalletId,
    val bearerToken: String? = null,
) {
    companion object {
        fun fromBuildConfig(): MobileWalletRuntimeConfig {
            val baseUrl = BuildConfig.WALLET_BASE_URL.trim()
            require(baseUrl.isNotBlank()) {
                "Missing wallet backend URL. Configure WALLET_BASE_URL (mobileWalletBaseUrl or MOBILE_WALLET_BASE_URL)."
            }

            val walletIdRaw = BuildConfig.WALLET_ID.trim()
            require(walletIdRaw.isNotBlank()) {
                "Missing wallet id. Configure WALLET_ID (mobileWalletId or MOBILE_WALLET_ID)."
            }
            val walletId = WalletId.validate(walletIdRaw, path = "WALLET_ID").orThrow()

            val bearerToken = BuildConfig.WALLET_BEARER_TOKEN.trim().ifBlank { null }
            return MobileWalletRuntimeConfig(
                baseUrl = baseUrl,
                walletId = walletId,
                bearerToken = bearerToken,
            )
        }
    }
}
