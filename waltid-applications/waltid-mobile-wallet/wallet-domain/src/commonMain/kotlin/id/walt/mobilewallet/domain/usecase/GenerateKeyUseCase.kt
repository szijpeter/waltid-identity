package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.KeyGenerateRequest
import id.walt.mobilewallet.model.WalletId

class GenerateKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: KeyGenerateRequest): WalletResult<KeyDescriptor> =
        keyRepository.generateKey(walletId, request)
}
