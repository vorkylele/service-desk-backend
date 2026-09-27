package ru.servicedesk.domain

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ApproverKind { MANAGER, OWNER, SECURITY }

/** WAITING — очередь шага ещё не наступила; PENDING — ожидает решения согласующего. */
enum class Decision { WAITING, PENDING, APPROVED, REJECTED }

@Entity
@Table(name = "ticket")
class Ticket(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    var number: String = "",
    @ManyToOne @JoinColumn(name = "type_id") var type: TicketType = TicketType(),
    @ManyToOne @JoinColumn(name = "status_code") var status: TicketStatus = TicketStatus(),
    @ManyToOne @JoinColumn(name = "priority_code") var priority: TicketPriority = TicketPriority(),
    @ManyToOne @JoinColumn(name = "author_id") var author: Employee = Employee(),
    @ManyToOne @JoinColumn(name = "assignee_id") var assignee: Employee? = null,
    @ManyToOne @JoinColumn(name = "support_group_id") var supportGroup: SupportGroup = SupportGroup(),
    var subject: String = "",
    var description: String = "",
    @ManyToOne @JoinColumn(name = "resource_id") var resource: InfoResource? = null,
    @ManyToOne @JoinColumn(name = "access_role_id") var accessRole: AccessRole? = null,
    @Column(name = "equipment_no") var equipmentNo: String? = null,
    @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "due_at") var dueAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "taken_at") var takenAt: LocalDateTime? = null,
    @Column(name = "resolved_at") var resolvedAt: LocalDateTime? = null,
    @Column(name = "closed_at") var closedAt: LocalDateTime? = null,
    var resolution: String? = null,
    @Column(name = "sla_breached") var slaBreached: Boolean = false,
)

@Entity
@Table(name = "approval")
class Approval(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne @JoinColumn(name = "ticket_id") var ticket: Ticket = Ticket(),
    @Column(name = "step_no") var stepNo: Short = 1,
    @ManyToOne @JoinColumn(name = "approver_id") var approver: Employee = Employee(),
    @Enumerated(EnumType.STRING) @Column(name = "approver_kind") var approverKind: ApproverKind = ApproverKind.MANAGER,
    @Enumerated(EnumType.STRING) var decision: Decision = Decision.WAITING,
    var comment: String? = null,
    @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "decided_at") var decidedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "ticket_event")
class TicketEvent(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne @JoinColumn(name = "ticket_id") var ticket: Ticket = Ticket(),
    @ManyToOne @JoinColumn(name = "actor_id") var actor: Employee? = null,
    @Column(name = "event_type") var eventType: String = "",
    @Column(name = "old_status") var oldStatus: String? = null,
    @Column(name = "new_status") var newStatus: String? = null,
    var message: String = "",
    @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "ticket_comment")
class TicketComment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne @JoinColumn(name = "ticket_id") var ticket: Ticket = Ticket(),
    @ManyToOne @JoinColumn(name = "author_id") var author: Employee = Employee(),
    var body: String = "",
    @Column(name = "created_at") var createdAt: LocalDateTime = LocalDateTime.now(),
)

@Entity
@Table(name = "access_grant")
class AccessGrant(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne @JoinColumn(name = "employee_id") var employee: Employee = Employee(),
    @ManyToOne @JoinColumn(name = "access_role_id") var accessRole: AccessRole = AccessRole(),
    @ManyToOne @JoinColumn(name = "ticket_id") var ticket: Ticket? = null,
    @Column(name = "granted_at") var grantedAt: LocalDateTime = LocalDateTime.now(),
    @ManyToOne @JoinColumn(name = "granted_by") var grantedBy: Employee = Employee(),
    @Column(name = "revoked_at") var revokedAt: LocalDateTime? = null,
    @ManyToOne @JoinColumn(name = "revoked_by") var revokedBy: Employee? = null,
    @Column(name = "revoke_reason") var revokeReason: String? = null,
)
