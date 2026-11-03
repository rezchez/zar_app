package com.zarfolio.core.common

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val cause: Throwable, val message: String? = null) : ApiResult<Nothing>
}

enum class ConnectionStatus { UNKNOWN, TESTING, CONNECTED, UNAVAILABLE }
