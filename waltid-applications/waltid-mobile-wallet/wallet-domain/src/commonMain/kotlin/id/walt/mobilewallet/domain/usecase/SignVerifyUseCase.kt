package id.walt.mobilewallet.domain.usecase

import id.walt.mobilewallet.domain.KeyRepository
import id.walt.mobilewallet.domain.WalletError
import id.walt.mobilewallet.domain.WalletResult
import id.walt.mobilewallet.model.KeySignRequest
import id.walt.mobilewallet.model.KeyVerifyRequest
import id.walt.mobilewallet.model.KeyVerifyResult
import id.walt.mobilewallet.model.ValidationIssue
import id.walt.mobilewallet.model.WalletId

class SignVerifyUseCase(
    private val keyRepository: KeyRepository,
) {
    suspend fun sign(walletId: WalletId, request: KeySignRequest): WalletResult<String> {
        if (request.payload.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "blank_sign_payload",
                            message = "Payload must not be blank.",
                            path = "payload"
                        )
                    )
                )
            )
        }
        return keyRepository.sign(walletId, request)
    }

    suspend fun verify(walletId: WalletId, request: KeyVerifyRequest): WalletResult<KeyVerifyResult> {
        if (request.jwk.isBlank() || request.signedPayload.isBlank()) {
            return WalletResult.Failure(
                WalletError.Validation(
                    issues = listOf(
                        ValidationIssue(
                            code = "invalid_verify_request",
                            message = "JWK and signed payload are required.",
                            path = "verifyRequest"
                        )
                    )
                )
            )
        }
        return keyRepository.verify(walletId, request)
    }
}
