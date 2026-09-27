package ru.servicedesk.web

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.servicedesk.repo.AccessRoleRepo
import ru.servicedesk.repo.InfoResourceRepo
import ru.servicedesk.repo.TicketPriorityRepo
import ru.servicedesk.repo.TicketTypeRepo

@RestController
@RequestMapping("/api")
class ReferenceController(
    private val types: TicketTypeRepo,
    private val priorities: TicketPriorityRepo,
    private val resources: InfoResourceRepo,
    private val roles: AccessRoleRepo,
) {
    @GetMapping("/catalog")
    fun catalog(): List<TicketTypeDto> = types.findAllByOrderByCode().map { it.toDto() }

    @GetMapping("/priorities")
    fun priorities(): List<PriorityDto> = priorities.findAllByOrderByCode().map { it.toDto() }

    @GetMapping("/resources")
    fun resources(): List<ResourceDto> = resources.findAllByOrderByCode().map { it.toDto() }

    @GetMapping("/resources/{id}/roles")
    fun roles(@PathVariable id: Int): List<AccessRoleDto> = roles.findByResourceIdOrderByName(id).map { it.toDto() }
}
