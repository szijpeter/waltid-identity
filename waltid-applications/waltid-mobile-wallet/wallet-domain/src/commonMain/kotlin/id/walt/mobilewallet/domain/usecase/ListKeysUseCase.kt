package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.WalletId

class ListKeysUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId): WalletResult<List<KeyDescriptor>> = keyRepository.listKeys(walletId)
}
