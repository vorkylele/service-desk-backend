package ru.servicedesk.service

import org.springframework.http.HttpStatus

sealed class ApiException(val status: HttpStatus, override val message: String) : RuntimeException(message)

class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, message)

class ForbiddenException(message: String) : ApiException(HttpStatus.FORBIDDEN, message)

class BusinessRuleException(message: String) : ApiException(HttpStatus.CONFLICT, message)

/** Причина отказа во входе наружу не раскрывается: подбор адресов и паролей не должен получать подсказок. */
class BadCredentialsException : ApiException(HttpStatus.UNAUTHORIZED, "Неверный адрес электронной почты или пароль")
