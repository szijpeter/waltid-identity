package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.DidId
import id.walt.mobilewallet.model.WalletId

class DeleteDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, didId: DidId): WalletResult<Boolean> =
        didRepository.deleteDid(walletId, didId)
}
