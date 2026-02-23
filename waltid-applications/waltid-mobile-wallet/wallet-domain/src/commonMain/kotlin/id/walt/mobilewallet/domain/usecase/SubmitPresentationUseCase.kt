package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.PresentationSelection
import id.walt.mobilewallet.model.PresentationSubmissionResult

class SubmitPresentationUseCase(
    private val exchangeRepository: ExchangeRepository,
) {
    suspend operator fun invoke(selection: PresentationSelection): WalletResult<PresentationSubmissionResult> {
        return when (val validation = selection.validate()) {
            is id.walt.mobilewallet.model.ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is id.walt.mobilewallet.model.ValidationResult.Valid ->
                exchangeRepository.submitPresentation(validation.value)
        }
    }
}
