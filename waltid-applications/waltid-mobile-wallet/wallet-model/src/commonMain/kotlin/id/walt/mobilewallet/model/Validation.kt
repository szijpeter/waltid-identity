package id.walt.mobilewallet.model

import kotlinx.serialization.Serializable

@Serializable
data class ValidationIssue(
    val code: String,
    val message: String,
    val path: String? = null,
)

sealed interface ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>

    data class Invalid(val issues: List<ValidationIssue>) : ValidationResult<Nothing> {
        init {
            require(issues.isNotEmpty()) { "Validation issues must not be empty." }
        }
    }

    val isValid: Boolean
        get() = this is Valid<*>

    fun getOrNull(): T? = (this as? Valid<T>)?.value
}

class ModelValidationException(val issues: List<ValidationIssue>) : IllegalArgumentException(
    issues.joinToString(separator = "; ") { issue ->
        val prefix = issue.path?.let { "$it: " } ?: ""
        "$prefix${issue.message}"
    }
)

fun <T> ValidationResult<T>.orThrow(): T = when (this) {
    is ValidationResult.Valid -> value
    is ValidationResult.Invalid -> throw ModelValidationException(issues)
}

internal fun validationFailure(code: String, message: String, path: String? = null): ValidationResult.Invalid =
    ValidationResult.Invalid(listOf(ValidationIssue(code = code, message = message, path = path)))
