package id.walt.mobilewallet.domain

import id.walt.mobilewallet.model.AuthSession
import id.walt.mobilewallet.model.LoginCredentials
import id.walt.mobilewallet.model.ValidationIssue
import id.walt.mobilewallet.model.ValidationResult

class LoginUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(credentials: LoginCredentials): WalletResult<AuthSession> {
        return when (val validation = credentials.validate()) {
            is ValidationResult.Invalid ->
                WalletResult.Failure(WalletError.Validation(validation.issues))

            is ValidationResult.Valid ->
                authRepository.login(validation.value)
        }
    }
}
