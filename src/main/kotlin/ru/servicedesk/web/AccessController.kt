package ru.servicedesk.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.servicedesk.repo.AccessGrantRepo
import ru.servicedesk.repo.EmployeeRepo
import ru.servicedesk.service.AccessService

data class RevokeRequest(@field:NotBlank @field:Size(max = 300) val reason: String)

@RestController
@RequestMapping("/api")
class AccessController(
    private val access: AccessService,
    private val grants: AccessGrantRepo,
    private val employees: EmployeeRepo,
) {
    @GetMapping("/access-grants/my")
    fun my(@AuthenticationPrincipal jwt: Jwt): List<GrantDto> =
        grants.findByEmployeeIdOrderByGrantedAtDesc(jwt.employeeId).map { it.toDto() }

    @GetMapping("/access-grants")
    @PreAuthorize("hasAnyRole('SUPPORT','SECURITY','ADMIN')")
    fun registry(@RequestParam(required = false) employeeId: Int?): List<GrantDto> =
        (if (employeeId != null) grants.findByEmployeeIdOrderByGrantedAtDesc(employeeId)
         else grants.findAllByOrderByGrantedAtDesc()).map { it.toDto() }

    @PostMapping("/access-grants/{id}/revoke")
    @PreAuthorize("hasAnyRole('SECURITY','ADMIN')")
    fun revoke(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long, @Valid @RequestBody req: RevokeRequest): GrantDto =
        access.revoke(id, jwt.employeeId, req.reason).toDto()

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('SUPPORT','SECURITY','ADMIN')")
    fun employees(): List<EmployeeDto> = employees.findAllByOrderByFullName().map { it.toDto() }

    @PostMapping("/employees/{id}/dismiss")
    @PreAuthorize("hasRole('ADMIN')")
    fun dismiss(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Int): DismissResult =
        DismissResult(access.dismiss(id, jwt.employeeId))
}
