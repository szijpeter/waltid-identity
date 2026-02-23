package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.KeyExportRequest
import id.walt.mobilewallet.model.WalletId

class ExportKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: KeyExportRequest): WalletResult<String> =
        keyRepository.exportKey(walletId, request)
}
