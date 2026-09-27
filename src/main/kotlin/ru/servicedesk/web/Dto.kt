package ru.servicedesk.web

import ru.servicedesk.domain.*
import java.time.LocalDateTime

data class EmployeeDto(val id: Int, val personnelNo: String, val fullName: String, val email: String,
                       val position: String, val department: String, val role: String,
                       val supportGroup: String?, val active: Boolean)

fun Employee.toDto() = EmployeeDto(id!!, personnelNo.trim(), fullName, email, position, department.name,
    role.name, supportGroup?.name, active)

data class TicketTypeDto(val id: Int, val code: String, val name: String, val category: String,
                         val description: String?, val slaHours: Int, val defaultPriority: Short,
                         val needsApproval: Boolean, val containsPd: Boolean, val supportGroup: String)

fun TicketType.toDto() = TicketTypeDto(id!!, code.trim(), name, category.name, description, slaHours,
    defaultPriority.code, needsApproval, containsPd, supportGroup.name)

data class PriorityDto(val code: Short, val name: String, val slaFactor: Double)

fun TicketPriority.toDto() = PriorityDto(code, name, slaFactor.toDouble())

data class ResourceDto(val id: Int, val code: String, val name: String, val owner: String, val paymentContour: Boolean)

fun InfoResource.toDto() = ResourceDto(id!!, code.trim(), name, owner.fullName, paymentContour)

data class AccessRoleDto(val id: Int, val code: String, val name: String, val description: String?)

fun AccessRole.toDto() = AccessRoleDto(id!!, code.trim(), name, description)

data class TicketDto(
    val id: Long, val number: String, val typeCode: String, val typeName: String, val category: String,
    val statusCode: String, val statusName: String, val priorityCode: Short, val priorityName: String,
    val author: String, val authorDepartment: String, val assignee: String?, val supportGroup: String,
    val subject: String, val description: String, val resource: String?, val accessRole: String?,
    val equipmentNo: String?, val createdAt: LocalDateTime, val dueAt: LocalDateTime,
    val takenAt: LocalDateTime?, val resolvedAt: LocalDateTime?, val closedAt: LocalDateTime?,
    val resolution: String?, val slaBreached: Boolean, val overdue: Boolean,
)

fun Ticket.toDto(now: LocalDateTime) = TicketDto(
    id!!, number.trim(), type.code.trim(), type.name, type.category.name, status.code.trim(), status.name,
    priority.code, priority.name, author.fullName, author.department.name, assignee?.fullName,
    supportGroup.name, subject, description, resource?.name, accessRole?.name, equipmentNo,
    createdAt, dueAt, takenAt, resolvedAt, closedAt, resolution, slaBreached,
    overdue = resolvedAt == null && !status.isFinal && now.isAfter(dueAt),
)

data class ApprovalDto(val id: Long, val ticketId: Long, val ticketNumber: String, val subject: String,
                       val author: String, val resource: String?, val accessRole: String?,
                       val stepNo: Short, val approver: String, val kind: String, val decision: String,
                       val comment: String?, val createdAt: LocalDateTime, val decidedAt: LocalDateTime?)

fun Approval.toDto() = ApprovalDto(id!!, ticket.id!!, ticket.number.trim(), ticket.subject, ticket.author.fullName,
    ticket.resource?.name, ticket.accessRole?.name, stepNo, approver.fullName, approverKind.name,
    decision.name, comment, createdAt, decidedAt)

data class EventDto(val actor: String, val type: String, val oldStatus: String?, val newStatus: String?,
                    val message: String, val createdAt: LocalDateTime)

fun TicketEvent.toDto() = EventDto(actor?.fullName ?: "Система", eventType, oldStatus?.trim(), newStatus?.trim(),
    message, createdAt)

data class CommentDto(val author: String, val body: String, val createdAt: LocalDateTime)

fun TicketComment.toDto() = CommentDto(author.fullName, body, createdAt)

data class TicketDetailsDto(val ticket: TicketDto, val approvals: List<ApprovalDto>,
                            val events: List<EventDto>, val comments: List<CommentDto>,
                            val canTake: Boolean, val canResolve: Boolean, val canClose: Boolean)

data class GrantDto(val id: Long, val employee: String, val personnelNo: String, val department: String,
                    val resource: String, val role: String, val ticketNumber: String?,
                    val grantedAt: LocalDateTime, val grantedBy: String,
                    val revokedAt: LocalDateTime?, val revokedBy: String?, val revokeReason: String?)

fun AccessGrant.toDto() = GrantDto(id!!, employee.fullName, employee.personnelNo.trim(), employee.department.name,
    accessRole.resource.name, accessRole.name, ticket?.number?.trim(), grantedAt, grantedBy.fullName,
    revokedAt, revokedBy?.fullName, revokeReason)

data class DismissResult(val revokedGrants: Int)
