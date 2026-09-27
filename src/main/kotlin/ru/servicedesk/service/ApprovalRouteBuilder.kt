package ru.servicedesk.service

import org.springframework.stereotype.Service
import ru.servicedesk.domain.ApproverKind
import ru.servicedesk.domain.Department
import ru.servicedesk.domain.Employee
import ru.servicedesk.domain.InfoResource
import ru.servicedesk.domain.Role
import ru.servicedesk.repo.EmployeeRepo

/**
 * Маршрут согласования заявки на доступ:
 *   шаг 1 — непосредственный руководитель заявителя;
 *   шаг 2 — владелец информационного ресурса;
 *   шаг 3 — отдел информационной безопасности (только для платёжного контура).
 * Лицо не согласует собственную заявку и не включается в маршрут дважды.
 */
@Service
class ApprovalRouteBuilder(private val employees: EmployeeRepo) {

    fun build(author: Employee, resource: InfoResource?): List<Pair<ApproverKind, Employee>> {
        val route = mutableListOf<Pair<ApproverKind, Employee>>()
        managerOf(author)?.let { route += ApproverKind.MANAGER to it }
        resource?.owner
            ?.takeIf { owner -> owner.id != author.id && route.none { it.second.id == owner.id } }
            ?.let { route += ApproverKind.OWNER to it }
        if (resource?.paymentContour == true) {
            employees.findByRoleAndActiveTrue(Role.SECURITY).minByOrNull { it.id!! }
                ?.takeIf { sec -> route.none { it.second.id == sec.id } }
                ?.let { route += ApproverKind.SECURITY to it }
        }
        return route
    }

    /** Руководитель — глава подразделения заявителя; для самого главы — глава вышестоящего. */
    private fun managerOf(author: Employee): Employee? {
        var dept: Department? = author.department
        while (dept != null) {
            val head = dept.head
            if (head != null && head.id != author.id && head.active) return head
            dept = dept.parent
        }
        return null
    }
}
