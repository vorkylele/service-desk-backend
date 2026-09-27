package ru.servicedesk.service

import org.springframework.stereotype.Service
import ru.servicedesk.domain.Employee
import ru.servicedesk.domain.SupportGroup
import ru.servicedesk.repo.EmployeeRepo
import ru.servicedesk.repo.TicketRepo

/**
 * Автоматическая маршрутизация: группа исполнителей определяется видом заявки,
 * исполнитель — наименее загруженный действующий участник группы.
 */
@Service
class RoutingService(
    private val employees: EmployeeRepo,
    private val tickets: TicketRepo,
) {
    fun pickAssignee(group: SupportGroup): Employee? =
        employees.findBySupportGroupIdAndActiveTrue(group.id!!)
            .minWithOrNull(compareBy<Employee>({ tickets.countOpenByAssignee(it.id!!) }, { it.id }))
}
