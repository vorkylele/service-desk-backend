package ru.servicedesk.service

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.servicedesk.config.TimeProvider
import ru.servicedesk.domain.*
import ru.servicedesk.repo.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DUE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

data class NewTicket(
    val typeId: Int,
    @field:NotBlank @field:Size(max = 200) val subject: String,
    @field:NotBlank @field:Size(max = 4000) val description: String,
    val priorityCode: Short? = null,
    val resourceId: Int? = null,
    val accessRoleId: Int? = null,
    @field:Size(max = 20) val equipmentNo: String? = null,
)

@Service
@Transactional
class TicketService(
    private val tickets: TicketRepo,
    private val types: TicketTypeRepo,
    private val statuses: TicketStatusRepo,
    private val priorities: TicketPriorityRepo,
    private val accessRoles: AccessRoleRepo,
    private val approvals: ApprovalRepo,
    private val events: TicketEventRepo,
    private val comments: TicketCommentRepo,
    private val employees: EmployeeRepo,
    private val sla: SlaCalculator,
    private val routing: RoutingService,
    private val routeBuilder: ApprovalRouteBuilder,
    private val access: AccessService,
    private val time: TimeProvider,
) {
    fun create(authorId: Int, req: NewTicket): Ticket {
        val now = time.now()
        val author = employee(authorId)
        val type = types.findById(req.typeId).orElseThrow { NotFoundException("Вид заявки не найден") }
        val priority = req.priorityCode?.let { priorities.findById(it).orElse(null) } ?: type.defaultPriority
        val role = if (type.category == Category.ACCESS) accessRole(req, author, type) else null

        val ticket = tickets.save(
            Ticket(
                number = tickets.nextNumber(),
                type = type,
                status = status(TicketStatus.QUEUED),
                priority = priority,
                author = author,
                supportGroup = type.supportGroup,
                subject = req.subject.trim(),
                description = req.description.trim(),
                resource = role?.resource,
                accessRole = role,
                equipmentNo = req.equipmentNo?.trim()?.ifBlank { null },
                createdAt = now,
                dueAt = sla.dueAt(now, type.slaHours, priority.slaFactor),
            )
        )
        log(ticket, author, "CREATED", null, ticket.status.code,
            "Заявка зарегистрирована. Контрольный срок: ${ticket.dueAt.format(DUE_FORMAT)}")

        val route = if (type.needsApproval) routeBuilder.build(author, role?.resource) else emptyList()
        if (route.isEmpty()) routeToSupport(ticket, now) else startApproval(ticket, route, now)
        return ticket
    }

    fun decide(approvalId: Long, approverId: Int, approved: Boolean, comment: String?): Ticket {
        val now = time.now()
        val step = approvals.findById(approvalId).orElseThrow { NotFoundException("Шаг согласования не найден") }
        if (step.approver.id != approverId) throw ForbiddenException("Согласование адресовано другому работнику")
        if (step.decision != Decision.PENDING) throw BusinessRuleException("Решение по этому шагу уже принято")
        if (!approved && comment.isNullOrBlank()) throw BusinessRuleException("При отказе необходимо указать причину")

        step.decision = if (approved) Decision.APPROVED else Decision.REJECTED
        step.comment = comment?.trim()?.ifBlank { null }
        step.decidedAt = now
        val ticket = step.ticket
        val approver = step.approver

        if (!approved) {
            log(ticket, approver, "REJECTED", null, null, "Отказано в согласовании: ${step.comment}")
            changeStatus(ticket, approver, TicketStatus.REJECTED, "Заявка отклонена на шаге согласования ${step.stepNo}")
            ticket.closedAt = now
            return ticket
        }
        log(ticket, approver, "APPROVED", null, null, "Согласовано (шаг ${step.stepNo})")
        val next = approvals.findByTicketIdOrderByStepNo(ticket.id!!).firstOrNull { it.decision == Decision.WAITING }
        if (next != null) {
            next.decision = Decision.PENDING
        } else {
            routeToSupport(ticket, now)
        }
        return ticket
    }

    fun take(ticketId: Long, actorId: Int): Ticket {
        val now = time.now()
        val ticket = ticket(ticketId)
        val actor = supportMember(actorId, ticket)
        if (ticket.status.code != TicketStatus.QUEUED) throw BusinessRuleException("Заявка не находится в очереди")
        ticket.assignee = actor
        ticket.takenAt = now
        changeStatus(ticket, actor, TicketStatus.IN_PROGRESS, "Принята в работу")
        return ticket
    }

    fun resolve(ticketId: Long, actorId: Int, resolution: String): Ticket {
        val now = time.now()
        val ticket = ticket(ticketId)
        val actor = supportMember(actorId, ticket)
        if (ticket.status.code != TicketStatus.IN_PROGRESS) throw BusinessRuleException("Заявка не находится в работе")
        if (resolution.isBlank()) throw BusinessRuleException("Необходимо описать выполненные действия")

        // решение заявки на доступ меняет реестр прав в той же транзакции
        ticket.accessRole?.let { role ->
            when (ticket.type.accessAction) {
                AccessAction.GRANT -> access.grant(ticket.author, role, ticket, actor, now)
                AccessAction.REVOKE -> access.revokeByRole(ticket.author, role, actor, "По заявке № ${ticket.number}", now)
                null -> Unit
            }
        }
        ticket.resolution = resolution.trim()
        ticket.resolvedAt = now
        ticket.slaBreached = now.isAfter(ticket.dueAt)
        changeStatus(ticket, actor, TicketStatus.RESOLVED,
            if (ticket.slaBreached) "Решена с нарушением контрольного срока" else "Решена в пределах контрольного срока")
        return ticket
    }

    fun close(ticketId: Long, actorId: Int): Ticket {
        val now = time.now()
        val ticket = ticket(ticketId)
        val actor = employee(actorId)
        if (ticket.author.id != actorId && actor.role == Role.EMPLOYEE) {
            throw ForbiddenException("Подтвердить решение может только заявитель")
        }
        if (ticket.status.code != TicketStatus.RESOLVED) throw BusinessRuleException("Заявка ещё не решена")
        ticket.closedAt = now
        changeStatus(ticket, actor, TicketStatus.CLOSED, "Решение подтверждено заявителем, заявка закрыта")
        return ticket
    }

    fun comment(ticketId: Long, authorId: Int, body: String): TicketComment {
        val ticket = ticket(ticketId)
        if (body.isBlank()) throw BusinessRuleException("Пустой комментарий")
        return comments.save(TicketComment(ticket = ticket, author = employee(authorId), body = body.trim(),
            createdAt = time.now()))
    }

    @Transactional(readOnly = true)
    fun visibleTo(ticket: Ticket, viewer: Employee): Boolean = when {
        viewer.role != Role.EMPLOYEE -> true
        ticket.author.id == viewer.id -> true
        else -> approvals.findByTicketIdOrderByStepNo(ticket.id!!).any { it.approver.id == viewer.id }
    }

    /** Работать с заявкой может администратор либо исполнитель её группы. */
    fun canWorkOn(ticket: Ticket, employee: Employee): Boolean = employee.role == Role.ADMIN ||
        (employee.role == Role.SUPPORT && employee.supportGroup?.id == ticket.supportGroup.id)

    fun ticket(id: Long): Ticket = tickets.findById(id).orElseThrow { NotFoundException("Заявка не найдена") }

    fun employee(id: Int): Employee = employees.findById(id).orElseThrow { NotFoundException("Работник не найден") }

    /** Роль доступа заявки с проверкой, что запрашиваемое право ещё не выдано, а отзываемое — существует. */
    private fun accessRole(req: NewTicket, author: Employee, type: TicketType): AccessRole {
        val role = accessRoles.findById(req.accessRoleId ?: throw BusinessRuleException("Не указана роль доступа"))
            .orElseThrow { NotFoundException("Роль доступа не найдена") }
        if (req.resourceId != null && req.resourceId != role.resource.id) {
            throw BusinessRuleException("Роль не принадлежит выбранному информационному ресурсу")
        }
        val has = access.hasActiveGrant(author.id!!, role.id!!)
        if (type.accessAction == AccessAction.GRANT && has) {
            throw BusinessRuleException("Право «${role.name}» в системе «${role.resource.name}» уже предоставлено")
        }
        if (type.accessAction == AccessAction.REVOKE && !has) {
            throw BusinessRuleException("Право «${role.name}» у заявителя отсутствует — отзывать нечего")
        }
        return role
    }

    private fun startApproval(ticket: Ticket, route: List<Pair<ApproverKind, Employee>>, now: LocalDateTime) {
        route.forEachIndexed { i, (kind, approver) ->
            approvals.save(
                Approval(
                    ticket = ticket, stepNo = (i + 1).toShort(), approver = approver, approverKind = kind,
                    decision = if (i == 0) Decision.PENDING else Decision.WAITING, createdAt = now,
                )
            )
        }
        changeStatus(ticket, null, TicketStatus.ON_APPROVAL,
            "Направлена на согласование: " + route.joinToString(" → ") { it.second.fullName })
    }

    private fun routeToSupport(ticket: Ticket, now: LocalDateTime) {
        val assignee = routing.pickAssignee(ticket.supportGroup)
        ticket.assignee = assignee
        val message = "Направлена в группу «${ticket.supportGroup.name}»" +
            (assignee?.let { ", исполнитель: ${it.fullName}" } ?: ", исполнитель не назначен")
        if (ticket.status.code == TicketStatus.QUEUED) {
            log(ticket, null, "ROUTED", null, null, message)
        } else {
            changeStatus(ticket, null, TicketStatus.QUEUED, message)
        }
    }

    private fun supportMember(actorId: Int, ticket: Ticket): Employee {
        val actor = employee(actorId)
        if (!canWorkOn(ticket, actor)) throw ForbiddenException("Заявка относится к другой группе исполнителей")
        return actor
    }

    private fun status(code: String): TicketStatus =
        statuses.findById(code).orElseThrow { IllegalStateException("Нет статуса $code") }

    private fun changeStatus(ticket: Ticket, actor: Employee?, newCode: String, message: String) {
        val old = ticket.status.code
        ticket.status = status(newCode)
        log(ticket, actor, "STATUS", old, newCode, message)
    }

    private fun log(ticket: Ticket, actor: Employee?, type: String, old: String?, new: String?, message: String) {
        events.save(TicketEvent(ticket = ticket, actor = actor, eventType = type,
            oldStatus = old, newStatus = new, message = message.take(500), createdAt = time.now()))
    }
}
