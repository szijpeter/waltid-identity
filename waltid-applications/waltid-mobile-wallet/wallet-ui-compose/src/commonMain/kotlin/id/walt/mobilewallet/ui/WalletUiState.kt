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
    data object Login : WalletRoute
    data object Dashboard : WalletRoute
    data object Scan : WalletRoute
    data object Issuance : WalletRoute
    data object Presentation : WalletRoute
    data object DidSettings : WalletRoute
    data object KeySettings : WalletRoute
    data object Security : WalletRoute
    data class CredentialDetail(val credentialId: CredentialId) : WalletRoute
}

data class WalletUiState(
    val walletId: WalletId? = null,
    val route: WalletRoute = WalletRoute.Login,
    val isLoading: Boolean = false,
    val loginEmail: String = "",
    val loginPassword: String = "",
    val scanInput: String = "",
    val credentials: List<CredentialSummary> = emptyList(),
    val credentialDetail: CredentialDetail? = null,
    val dids: List<DidDescriptor> = emptyList(),
    val keys: List<KeyDescriptor> = emptyList(),
    val pendingIssuance: IssuancePreview? = null,
    val pendingPresentation: PresentationResolution? = null,
    val securitySettings: SecuritySettings = SecuritySettings(),
    val lastError: WalletError? = null,
)
