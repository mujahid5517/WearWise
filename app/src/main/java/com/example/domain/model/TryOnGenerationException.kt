package com.example.domain.model

sealed class TryOnGenerationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidImage(message: String) : TryOnGenerationException(message)
    class NoImageReturned(message: String = "No image returned from generation") : TryOnGenerationException(message)
    class ApiError(message: String, cause: Throwable? = null) : TryOnGenerationException(message, cause)
}
