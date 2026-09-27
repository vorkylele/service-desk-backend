package ru.servicedesk.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

@ConfigurationProperties(prefix = "servicedesk.security")
data class SecurityProperties(val jwtSecret: String, val tokenTtlMinutes: Long = 480)

/**
 * Защита REST-интерфейса: сеансы на сервере не хранятся, каждый запрос
 * подтверждается подписанным токеном (HMAC-SHA256); полномочия берутся из роли работника.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(private val props: SecurityProperties) {

    private val key get() = SecretKeySpec(props.jwtSecret.toByteArray(), "HmacSHA256")

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }                       // токен передаётся в заголовке, cookie не используются
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
            it.requestMatchers("/api/**").authenticated()
            it.anyRequest().permitAll()
        }
        .oauth2ResourceServer { rs -> rs.jwt { it.jwtAuthenticationConverter(authoritiesConverter()) } }
        .build()

    @Bean
    fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(key))

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(10)

    private fun authoritiesConverter() = JwtAuthenticationConverter().apply {
        setJwtGrantedAuthoritiesConverter(JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("role")
            setAuthorityPrefix("ROLE_")
        })
    }
}
