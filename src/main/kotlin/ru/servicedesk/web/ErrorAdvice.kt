package ru.servicedesk.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.servicedesk.service.ApiException

data class ApiError(val message: String)

@RestControllerAdvice
class ErrorAdvice {

    @ExceptionHandler(ApiException::class)
    fun api(e: ApiException): ResponseEntity<ApiError> = ResponseEntity.status(e.status).body(ApiError(e.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(e: MethodArgumentNotValidException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError("Заполнены не все обязательные поля"))
}
