package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.WalletId

class ListDidsUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId): WalletResult<List<DidDescriptor>> = didRepository.listDids(walletId)
}
