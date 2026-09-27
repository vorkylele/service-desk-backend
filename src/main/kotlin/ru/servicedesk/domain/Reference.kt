package ru.servicedesk.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

enum class Role { EMPLOYEE, SUPPORT, SECURITY, ADMIN }

enum class Category { INCIDENT, ACCESS }

/** Действие с правом доступа, которое выполняется при решении заявки. */
enum class AccessAction { GRANT, REVOKE }

@Entity
@Table(name = "department")
class Department(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    var code: String = "",
    var name: String = "",
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") var parent: Department? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "head_id") var head: Employee? = null,
)

@Entity
@Table(name = "support_group")
class SupportGroup(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    var code: String = "",
    var name: String = "",
)

@Entity
@Table(name = "employee")
class Employee(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    @Column(name = "personnel_no") var personnelNo: String = "",
    @Column(name = "full_name") var fullName: String = "",
    var email: String = "",
    var position: String = "",
    @ManyToOne @JoinColumn(name = "department_id") var department: Department = Department(),
    @ManyToOne @JoinColumn(name = "support_group_id") var supportGroup: SupportGroup? = null,
    @Enumerated(EnumType.STRING) var role: Role = Role.EMPLOYEE,
    @Column(name = "password_hash") var passwordHash: String? = null,
    var active: Boolean = true,
    @Column(name = "dismissed_at") var dismissedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "ticket_status")
class TicketStatus(
    @Id var code: String = "",
    var name: String = "",
    @Column(name = "is_final") var isFinal: Boolean = false,
) {
    companion object {
        const val ON_APPROVAL = "AP"
        const val QUEUED = "QU"
        const val IN_PROGRESS = "WP"
        const val RESOLVED = "RS"
        const val CLOSED = "CL"
        const val REJECTED = "RJ"
    }
}

@Entity
@Table(name = "ticket_priority")
class TicketPriority(
    @Id var code: Short = 3,
    var name: String = "",
    @Column(name = "sla_factor") var slaFactor: BigDecimal = BigDecimal.ONE,
)

@Entity
@Table(name = "ticket_type")
class TicketType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    var code: String = "",
    var name: String = "",
    @Enumerated(EnumType.STRING) var category: Category = Category.INCIDENT,
    var description: String? = null,
    @Column(name = "sla_hours") var slaHours: Int = 8,
    @ManyToOne @JoinColumn(name = "default_priority") var defaultPriority: TicketPriority = TicketPriority(),
    @Column(name = "needs_approval") var needsApproval: Boolean = false,
    @Column(name = "contains_pd") var containsPd: Boolean = false,
    @Enumerated(EnumType.STRING) @Column(name = "access_action") var accessAction: AccessAction? = null,
    @ManyToOne @JoinColumn(name = "support_group_id") var supportGroup: SupportGroup = SupportGroup(),
)

@Entity
@Table(name = "info_resource")
class InfoResource(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    var code: String = "",
    var name: String = "",
    @ManyToOne @JoinColumn(name = "owner_id") var owner: Employee = Employee(),
    @Column(name = "payment_contour") var paymentContour: Boolean = false,
)

@Entity
@Table(name = "access_role")
class AccessRole(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Int? = null,
    @ManyToOne @JoinColumn(name = "resource_id") var resource: InfoResource = InfoResource(),
    var code: String = "",
    var name: String = "",
    var description: String? = null,
)
