package id.walt.mobilewallet.model

import kotlinx.serialization.Serializable

private const val BLANK_EMAIL_CODE = "blank_email"
private const val BLANK_PASSWORD_CODE = "blank_password"

/**
 * Login credentials submitted by the user.
 *
 * Registration flow (POST /wallet-api/auth/create) is deferred to a follow-up phase;
 * this model is login-only for now.
 */
@Serializable
data class LoginCredentials(
    val email: String,
    val password: String,
) {
    fun validate(emailPath: String = "email", passwordPath: String = "password"): ValidationResult<LoginCredentials> {
        val issues = mutableListOf<ValidationIssue>()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isBlank()) {
            issues += ValidationIssue(code = BLANK_EMAIL_CODE, message = "Email must not be blank.", path = emailPath)
        }
        if (trimmedPassword.isBlank()) {
            issues += ValidationIssue(code = BLANK_PASSWORD_CODE, message = "Password must not be blank.", path = passwordPath)
        }

        return if (issues.isEmpty()) {
            ValidationResult.Valid(LoginCredentials(email = trimmedEmail, password = trimmedPassword))
        } else {
            ValidationResult.Invalid(issues)
        }
    }
}

/**
 * Authenticated session returned after successful login and wallet listing.
 */
data class AuthSession(
    val token: String,
    val walletId: WalletId,
)

/**
 * A wallet entry from the account wallet listing endpoint.
 */
data class WalletListingEntry(
    val id: WalletId,
    val name: String? = null,
    val createdOn: String? = null,
)
