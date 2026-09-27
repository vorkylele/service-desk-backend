package ru.servicedesk.web

import org.springframework.security.oauth2.jwt.Jwt

/** Идентификатор работника передаётся в subject токена. */
val Jwt.employeeId: Int get() = subject.toInt()
