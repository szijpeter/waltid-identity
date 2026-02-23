package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.KeyDescriptor
import id.walt.mobilewallet.model.KeyImportRequest
import id.walt.mobilewallet.model.WalletId

class ImportKeyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: KeyImportRequest): WalletResult<KeyDescriptor> =
        when (val validation = request.validate()) {
            is id.walt.mobilewallet.model.ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is id.walt.mobilewallet.model.ValidationResult.Valid ->
                keyRepository.importKey(walletId, validation.value)
        }
}
