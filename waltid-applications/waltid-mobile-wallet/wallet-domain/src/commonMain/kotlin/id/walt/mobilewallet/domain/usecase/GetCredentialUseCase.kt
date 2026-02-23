package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.CredentialRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.CredentialDetail
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.WalletId

class GetCredentialUseCase(
    private val credentialRepository: CredentialRepository,
) {
    suspend operator fun invoke(walletId: WalletId, credentialId: CredentialId): WalletResult<CredentialDetail> =
        credentialRepository.getCredential(walletId, credentialId)
}
