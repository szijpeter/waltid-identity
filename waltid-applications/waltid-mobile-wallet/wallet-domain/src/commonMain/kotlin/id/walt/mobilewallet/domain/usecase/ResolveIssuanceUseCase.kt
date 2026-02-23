package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.ExchangeRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.domain.getOrNull
import id.walt.mobilewallet.domain.map
import id.walt.mobilewallet.model.IssuancePreview
import id.walt.mobilewallet.model.IssuanceRequest
import id.walt.mobilewallet.model.ValidationIssue

class ResolveIssuanceUseCase(
    private val exchangeRepository: ExchangeRepository,
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(request: IssuanceRequest): WalletResult<IssuancePreview> {
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

        val preview = exchangeRepository.resolveIssuance(request)
        val dids = didRepository.listDids(request.walletId).getOrNull().orEmpty()
        return preview.map { resolved ->
            if (resolved.didOptions.isEmpty() && dids.isNotEmpty()) {
                resolved.copy(didOptions = dids)
            } else {
                resolved
            }
        }
    }
}
