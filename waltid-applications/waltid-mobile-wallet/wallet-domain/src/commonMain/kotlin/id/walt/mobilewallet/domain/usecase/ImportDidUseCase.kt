package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidImportRequest
import id.walt.mobilewallet.model.WalletId

class ImportDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: DidImportRequest): WalletResult<DidDescriptor> =
        when (val validation = request.validate()) {
            is id.walt.mobilewallet.model.ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is id.walt.mobilewallet.model.ValidationResult.Valid ->
                didRepository.importDid(walletId, validation.value)
        }
}
