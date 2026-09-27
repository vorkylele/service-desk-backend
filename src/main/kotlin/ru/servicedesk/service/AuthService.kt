package ru.servicedesk.service

import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import ru.servicedesk.config.SecurityProperties
import ru.servicedesk.domain.Employee
import ru.servicedesk.repo.EmployeeRepo
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AuthService(
    private val employees: EmployeeRepo,
    private val encoder: PasswordEncoder,
    private val jwtEncoder: JwtEncoder,
    private val props: SecurityProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun login(email: String, password: String): Pair<String, Employee> {
        val employee = employees.findByEmailIgnoreCase(email.trim())
        val ok = employee != null && employee.active && employee.passwordHash != null &&
            encoder.matches(password, employee.passwordHash)
        if (!ok) {
            log.warn("Неуспешная попытка входа: {}", email)
            throw BadCredentialsException()
        }
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(employee!!.id.toString())
            .issuedAt(now)
            .expiresAt(now.plus(props.tokenTtlMinutes, ChronoUnit.MINUTES))
            .claim("role", listOf(employee.role.name))
            .build()
        val token = jwtEncoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).tokenValue
        return token to employee
    }
}
