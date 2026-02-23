package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.DidRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.DidCreateRequest
import id.walt.mobilewallet.model.DidDescriptor
import id.walt.mobilewallet.model.DidMethod
import id.walt.mobilewallet.model.WalletId

class CreateDidUseCase(
    private val didRepository: DidRepository,
) {
    suspend operator fun invoke(walletId: WalletId, request: DidCreateRequest): WalletResult<DidDescriptor> {
        val supportedMethods = setOf(DidMethod.KEY, DidMethod.JWK, DidMethod.WEB)
        if (request.method !in supportedMethods) {
            return WalletResult.Failure(
                WalletError.Unsupported(
                    message = "DID method ${request.method.name.lowercase()} is not in phase-1 scope."
                )
            )
        }
        return didRepository.createDid(walletId, request)
    }
}
