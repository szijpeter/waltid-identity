package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.CredentialId
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.ValidationIssue

class AcceptIssuanceUseCase(
    private val exchangeRepository: ExchangeRepository,
) {
    suspend operator fun invoke(request: IssuanceRequest): WalletResult<List<CredentialId>> {
        if (request.rawRequest.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_issuance_request",
                            message = "Issuance request must not be blank.",
                            path = "rawRequest"
                        )
                    )
                )
            )
        }
        return exchangeRepository.acceptIssuance(request)
    }
}
