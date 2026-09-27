package ru.servicedesk.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.servicedesk.config.TimeProvider
import ru.servicedesk.domain.AccessGrant
import ru.servicedesk.domain.AccessRole
import ru.servicedesk.domain.Employee
import ru.servicedesk.domain.Ticket
import ru.servicedesk.repo.AccessGrantRepo
import ru.servicedesk.repo.EmployeeRepo
import java.time.LocalDateTime

@Service
@Transactional
class AccessService(
    private val grants: AccessGrantRepo,
    private val employees: EmployeeRepo,
    private val time: TimeProvider,
) {
    fun hasActiveGrant(employeeId: Int, roleId: Int): Boolean =
        grants.findByEmployeeIdAndAccessRoleIdAndRevokedAtIsNull(employeeId, roleId) != null

    fun grant(employee: Employee, role: AccessRole, ticket: Ticket?, by: Employee, now: LocalDateTime): AccessGrant {
        if (hasActiveGrant(employee.id!!, role.id!!)) {
            throw BusinessRuleException("Право «${role.name}» уже действует")
        }
        return grants.save(AccessGrant(employee = employee, accessRole = role, ticket = ticket,
            grantedAt = now, grantedBy = by))
    }

    fun revoke(grantId: Long, byId: Int, reason: String): AccessGrant {
        val now = time.now()
        val grant = grants.findById(grantId).orElseThrow { NotFoundException("Запись о праве не найдена") }
        if (grant.revokedAt != null) throw BusinessRuleException("Право уже отозвано")
        if (reason.isBlank()) throw BusinessRuleException("Необходимо указать основание отзыва")
        return close(grant, employees.getReferenceById(byId), reason, now)
    }

    fun revokeByRole(employee: Employee, role: AccessRole, by: Employee, reason: String, now: LocalDateTime) {
        grants.findByEmployeeIdAndAccessRoleIdAndRevokedAtIsNull(employee.id!!, role.id!!)
            ?.let { close(it, by, reason, now) }
    }

    /** Права отзываются в той же транзакции, что и блокировка: избыточных прав после увольнения не остаётся. */
    fun dismiss(employeeId: Int, byId: Int): Int {
        val now = time.now()
        val employee = employees.findById(employeeId).orElseThrow { NotFoundException("Работник не найден") }
        if (!employee.active) throw BusinessRuleException("Работник уже уволен")
        val by = employees.getReferenceById(byId)
        employee.active = false
        employee.dismissedAt = now
        val active = grants.findByEmployeeIdAndRevokedAtIsNull(employeeId)
        active.forEach { close(it, by, "Прекращение трудовых отношений", now) }
        return active.size
    }

    private fun close(grant: AccessGrant, by: Employee, reason: String, now: LocalDateTime): AccessGrant {
        grant.revokedAt = now
        grant.revokedBy = by
        grant.revokeReason = reason.trim().take(300)
        return grant
    }
}
