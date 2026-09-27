package ru.servicedesk.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.servicedesk.repo.EmployeeRepo
import ru.servicedesk.service.AuthService
import ru.servicedesk.service.NotFoundException

data class LoginRequest(@field:NotBlank val email: String, @field:NotBlank val password: String)

data class LoginResponse(val token: String, val user: EmployeeDto)

@RestController
@RequestMapping("/api")
class AuthController(private val auth: AuthService, private val employees: EmployeeRepo) {

    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse {
        val (token, employee) = auth.login(req.email, req.password)
        return LoginResponse(token, employee.toDto())
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): EmployeeDto =
        employees.findById(jwt.employeeId).orElseThrow { NotFoundException("Работник не найден") }.toDto()
}
