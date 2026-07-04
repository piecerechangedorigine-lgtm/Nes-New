package com.novafinance.core.domain.model

/**
 * Explicit success/error wrapper for every domain-layer operation.
 * Forces callers (ViewModels) to handle failure states instead of
 * relying on exceptions bubbling up into Compose.
 */
sealed class NovaResult<out T> {
    data class Success<T>(val data: T) : NovaResult<T>()
    data class Error(val failure: NovaFailure) : NovaResult<Nothing>()
    data object Loading : NovaResult<Nothing>()
}

/**
 * Closed set of failure types the UI layer can branch on to show
 * meaningful, specific error states instead of a generic "Something went wrong".
 */
sealed class NovaFailure(val message: String) {
    data object NetworkUnavailable : NovaFailure("No network connection")
    data object Unauthorized : NovaFailure("Session expired, please sign in again")
    data class DataNotFound(val entity: String) : NovaFailure("$entity not found")
    data class Unknown(val cause: Throwable) : NovaFailure(cause.message ?: "Unexpected error")
}

inline fun <T> NovaResult<T>.onSuccess(action: (T) -> Unit): NovaResult<T> {
    if (this is NovaResult.Success) action(data)
    return this
}

inline fun <T> NovaResult<T>.onError(action: (NovaFailure) -> Unit): NovaResult<T> {
    if (this is NovaResult.Error) action(failure)
    return this
}
