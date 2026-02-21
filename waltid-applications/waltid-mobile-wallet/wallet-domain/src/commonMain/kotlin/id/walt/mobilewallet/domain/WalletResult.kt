package id.walt.mobilewallet.domain

sealed interface WalletResult<out T> {
    data class Success<T>(val value: T) : WalletResult<T>
    data class Failure(val error: WalletError) : WalletResult<Nothing>

    val isSuccess: Boolean
        get() = this is Success<*>

    val isFailure: Boolean
        get() = this is Failure
}

inline fun <T, R> WalletResult<T>.map(transform: (T) -> R): WalletResult<R> = when (this) {
    is WalletResult.Success -> WalletResult.Success(transform(value))
    is WalletResult.Failure -> this
}

inline fun <T> WalletResult<T>.onSuccess(block: (T) -> Unit): WalletResult<T> = apply {
    if (this is WalletResult.Success) {
        block(value)
    }
}

inline fun <T> WalletResult<T>.onFailure(block: (WalletError) -> Unit): WalletResult<T> = apply {
    if (this is WalletResult.Failure) {
        block(error)
    }
}

fun <T> WalletResult<T>.getOrNull(): T? = (this as? WalletResult.Success<T>)?.value

fun <T> WalletResult<T>.errorOrNull(): WalletError? = (this as? WalletResult.Failure)?.error
