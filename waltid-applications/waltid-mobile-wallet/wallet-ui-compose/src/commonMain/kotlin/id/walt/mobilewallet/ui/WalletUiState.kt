package id.walt.mobilewallet.ui

import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.PresentationResolution
import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.SecuritySettings
import id.walt.mobilewallet.model.WalletId

sealed interface WalletRoute {
    data class Login(
        val email: String = "",
        val password: String = "",
    ) : WalletRoute

    data object Dashboard : WalletRoute

    data class Scan(
        val scanInput: String = "",
    ) : WalletRoute

    data class Issuance(
        val pendingIssuance: IssuancePreview? = null,
        val rawRequest: String,
    ) : WalletRoute

    data class Presentation(
        val pendingPresentation: PresentationResolution? = null,
        val rawRequest: String,
    ) : WalletRoute

    data object DidSettings : WalletRoute
    data object KeySettings : WalletRoute
    data object Security : WalletRoute

    data class CredentialDetail(
        val credentialId: CredentialId,
        val detail: id.walt.mobilewallet.model.CredentialDetail? = null,
    ) : WalletRoute
}

data class WalletUiState(
    val walletId: WalletId? = null,
    val route: WalletRoute = WalletRoute.Login(),
    val isLoading: Boolean = false,
    val credentials: List<CredentialSummary> = emptyList(),
    val dids: List<DidDescriptor> = emptyList(),
    val keys: List<KeyDescriptor> = emptyList(),
    val securitySettings: SecuritySettings = SecuritySettings(),
    val lastError: WalletError? = null,
)
