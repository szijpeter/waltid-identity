package id.walt.mobilewallet.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

private const val BLANK_IDENTIFIER_CODE = "blank_identifier"

private fun validateIdentifier(raw: String, path: String): ValidationResult<String> {
    val trimmed = raw.trim()
    return if (trimmed.isEmpty()) {
        validationFailure(BLANK_IDENTIFIER_CODE, "Identifier must not be blank.", path)
    } else {
        ValidationResult.Valid(trimmed)
    }
}

@Serializable
@JvmInline
value class WalletId(val value: String) {
    init {
        require(value.isNotBlank()) { "WalletId must not be blank." }
    }

    companion object {
        fun validate(raw: String, path: String = "walletId"): ValidationResult<WalletId> =
            when (val result = validateIdentifier(raw, path)) {
                is ValidationResult.Valid -> ValidationResult.Valid(WalletId(result.value))
                is ValidationResult.Invalid -> result
            }
    }
}

@Serializable
@JvmInline
value class CredentialId(val value: String) {
    init {
        require(value.isNotBlank()) { "CredentialId must not be blank." }
    }

    companion object {
        fun validate(raw: String, path: String = "credentialId"): ValidationResult<CredentialId> =
            when (val result = validateIdentifier(raw, path)) {
                is ValidationResult.Valid -> ValidationResult.Valid(CredentialId(result.value))
                is ValidationResult.Invalid -> result
            }
    }
}

@Serializable
@JvmInline
value class DidId(val value: String) {
    init {
        require(value.isNotBlank()) { "DidId must not be blank." }
    }

    companion object {
        fun validate(raw: String, path: String = "didId"): ValidationResult<DidId> =
            when (val result = validateIdentifier(raw, path)) {
                is ValidationResult.Valid -> ValidationResult.Valid(DidId(result.value))
                is ValidationResult.Invalid -> result
            }
    }
}

@Serializable
@JvmInline
value class KeyId(val value: String) {
    init {
        require(value.isNotBlank()) { "KeyId must not be blank." }
    }

    companion object {
        fun validate(raw: String, path: String = "keyId"): ValidationResult<KeyId> =
            when (val result = validateIdentifier(raw, path)) {
                is ValidationResult.Valid -> ValidationResult.Valid(KeyId(result.value))
                is ValidationResult.Invalid -> result
            }
    }
}
