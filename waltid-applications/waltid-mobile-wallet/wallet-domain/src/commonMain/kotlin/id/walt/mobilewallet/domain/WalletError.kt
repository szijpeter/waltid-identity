package id.walt.mobilewallet.domain

import id.walt.mobilewallet.model.ProtocolMode
import id.walt.mobilewallet.model.ValidationIssue

sealed interface WalletError {
    val message: String

    data class Validation(
        val issues: List<ValidationIssue>,
        override val message: String = "Validation failed.",
    ) : WalletError {
        init {
            require(issues.isNotEmpty()) { "Validation errors must include at least one issue." }
        }
    }

    data class NotFound(
        val resource: String,
        override val message: String = "$resource not found.",
    ) : WalletError

    data class Network(
        override val message: String,
        val statusCode: Int? = null,
    ) : WalletError

    data class Protocol(
        val mode: ProtocolMode? = null,
        override val message: String,
    ) : WalletError

    data class Security(
        override val message: String,
    ) : WalletError

    data class Storage(
        override val message: String,
    ) : WalletError

    data class Unsupported(
        override val message: String,
    ) : WalletError

    data class Unknown(
        override val message: String,
        val cause: Throwable? = null,
    ) : WalletError
}
