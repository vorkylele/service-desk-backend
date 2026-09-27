package ru.servicedesk.web

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.servicedesk.config.TimeProvider
import ru.servicedesk.domain.Decision
import ru.servicedesk.repo.ApprovalRepo
import ru.servicedesk.service.TicketService

data class DecisionRequest(val approved: Boolean, @field:Size(max = 500) val comment: String? = null)

@RestController
@RequestMapping("/api/approvals")
class ApprovalController(
    private val service: TicketService,
    private val approvals: ApprovalRepo,
    private val time: TimeProvider,
) {
    @GetMapping
    fun pending(@AuthenticationPrincipal jwt: Jwt): List<ApprovalDto> =
        approvals.findByApproverIdAndDecisionOrderByCreatedAt(jwt.employeeId, Decision.PENDING).map { it.toDto() }

    @PostMapping("/{id}/decision")
    fun decide(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long, @Valid @RequestBody req: DecisionRequest): TicketDto =
        service.decide(id, jwt.employeeId, req.approved, req.comment).toDto(time.now())
}
