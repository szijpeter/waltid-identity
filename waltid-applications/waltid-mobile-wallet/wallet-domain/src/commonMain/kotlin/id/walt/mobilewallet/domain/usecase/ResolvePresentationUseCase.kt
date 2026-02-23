package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.PresentationResolution
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.domain.map
import id.walt.mobilewallet.model.PresentationRequest
import id.walt.mobilewallet.model.ValidationIssue

class ResolvePresentationUseCase(
    private val exchangeRepository: ExchangeRepository,
) {
    suspend operator fun invoke(request: PresentationRequest): WalletResult<PresentationResolution> {
        if (request.rawRequest.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_presentation_request",
                            message = "Presentation request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }

        return exchangeRepository.resolvePresentation(request).map { matched ->
            PresentationResolution(request = request, matchedCredentials = matched)
        }
    }
}
