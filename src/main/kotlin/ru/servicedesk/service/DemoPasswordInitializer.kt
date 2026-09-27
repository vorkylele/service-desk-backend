package ru.servicedesk.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import ru.servicedesk.repo.EmployeeRepo

/** Хеши паролей в миграциях не хранятся: демонстрационным учётным записям пароль задаётся при первом запуске. */
@Component
class DemoPasswordInitializer(
    private val employees: EmployeeRepo,
    private val encoder: PasswordEncoder,
    @Value("\${servicedesk.demo.default-password:}") private val defaultPassword: String,
) : CommandLineRunner {

    @Transactional
    override fun run(vararg args: String) {
        if (defaultPassword.isBlank()) return
        val hash = encoder.encode(defaultPassword)
        employees.findByPasswordHashIsNull().forEach { it.passwordHash = hash }
    }
}
