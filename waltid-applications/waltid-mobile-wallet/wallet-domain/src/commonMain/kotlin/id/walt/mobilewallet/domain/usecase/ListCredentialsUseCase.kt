package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.CredentialRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.CredentialSummary
import id.walt.mobilewallet.model.WalletId

class ListCredentialsUseCase(
    private val credentialRepository: CredentialRepository,
) {
    suspend operator fun invoke(walletId: WalletId): WalletResult<List<CredentialSummary>> =
        credentialRepository.listCredentials(walletId)
}
