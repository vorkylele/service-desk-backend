package ru.servicedesk.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.servicedesk.config.TimeProvider
import ru.servicedesk.domain.Role
import ru.servicedesk.domain.TicketStatus
import ru.servicedesk.repo.ApprovalRepo
import ru.servicedesk.repo.TicketCommentRepo
import ru.servicedesk.repo.TicketEventRepo
import ru.servicedesk.repo.TicketRepo
import ru.servicedesk.service.ForbiddenException
import ru.servicedesk.service.NewTicket
import ru.servicedesk.service.TicketService

data class ResolveRequest(@field:NotBlank @field:Size(max = 2000) val resolution: String)

data class CommentRequest(@field:NotBlank @field:Size(max = 2000) val body: String)

@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val service: TicketService,
    private val tickets: TicketRepo,
    private val approvals: ApprovalRepo,
    private val events: TicketEventRepo,
    private val comments: TicketCommentRepo,
    private val time: TimeProvider,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody req: NewTicket): TicketDto =
        service.create(jwt.employeeId, req).toDto(time.now())

    /** scope: my — заявки автора; queue — очередь группы исполнителя; all — журнал (не для рядовых работников). */
    @GetMapping
    fun list(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "my") scope: String): List<TicketDto> {
        val viewer = service.employee(jwt.employeeId)
        val found = when (scope) {
            "queue" -> when (viewer.role) {
                Role.SUPPORT -> tickets.findQueue(viewer.supportGroup?.id ?: -1)
                Role.ADMIN, Role.SECURITY -> tickets.findAllOpen()
                Role.EMPLOYEE -> throw ForbiddenException("Очередь доступна только исполнителям")
            }
            "all" -> if (viewer.role == Role.EMPLOYEE) throw ForbiddenException("Журнал заявок недоступен")
                     else tickets.findTop200ByOrderByCreatedAtDesc()
            else -> tickets.findByAuthorIdOrderByCreatedAtDesc(viewer.id!!)
        }
        return found.map { it.toDto(time.now()) }
    }

    @GetMapping("/{id}")
    fun details(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long): TicketDetailsDto {
        val viewer = service.employee(jwt.employeeId)
        val ticket = service.ticket(id)
        if (!service.visibleTo(ticket, viewer)) throw ForbiddenException("Нет доступа к заявке")
        val canWork = service.canWorkOn(ticket, viewer)
        return TicketDetailsDto(
            ticket.toDto(time.now()),
            approvals.findByTicketIdOrderByStepNo(id).map { it.toDto() },
            events.findByTicketIdOrderByCreatedAtAscIdAsc(id).map { it.toDto() },
            comments.findByTicketIdOrderByCreatedAt(id).map { it.toDto() },
            canTake = canWork && ticket.status.code == TicketStatus.QUEUED,
            canResolve = canWork && ticket.status.code == TicketStatus.IN_PROGRESS,
            canClose = ticket.author.id == viewer.id && ticket.status.code == TicketStatus.RESOLVED,
        )
    }

    @PostMapping("/{id}/take")
    fun take(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long): TicketDto =
        service.take(id, jwt.employeeId).toDto(time.now())

    @PostMapping("/{id}/resolve")
    fun resolve(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long, @Valid @RequestBody req: ResolveRequest): TicketDto =
        service.resolve(id, jwt.employeeId, req.resolution).toDto(time.now())

    @PostMapping("/{id}/close")
    fun close(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long): TicketDto =
        service.close(id, jwt.employeeId).toDto(time.now())

    @PostMapping("/{id}/comments")
    fun comment(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long, @Valid @RequestBody req: CommentRequest): CommentDto =
        service.comment(id, jwt.employeeId, req.body).toDto()
}
