package ru.servicedesk.repo

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.servicedesk.domain.*

interface EmployeeRepo : JpaRepository<Employee, Int> {
    fun findByEmailIgnoreCase(email: String): Employee?
    fun findByRoleAndActiveTrue(role: Role): List<Employee>
    fun findBySupportGroupIdAndActiveTrue(groupId: Int): List<Employee>
    fun findByPasswordHashIsNull(): List<Employee>
    fun findAllByOrderByFullName(): List<Employee>
}

interface TicketStatusRepo : JpaRepository<TicketStatus, String>

interface TicketPriorityRepo : JpaRepository<TicketPriority, Short> {
    fun findAllByOrderByCode(): List<TicketPriority>
}

interface TicketTypeRepo : JpaRepository<TicketType, Int> {
    fun findAllByOrderByCode(): List<TicketType>
}

interface InfoResourceRepo : JpaRepository<InfoResource, Int> {
    fun findAllByOrderByCode(): List<InfoResource>
}

interface AccessRoleRepo : JpaRepository<AccessRole, Int> {
    fun findByResourceIdOrderByName(resourceId: Int): List<AccessRole>
}

interface TicketRepo : JpaRepository<Ticket, Long> {
    fun findByAuthorIdOrderByCreatedAtDesc(authorId: Int): List<Ticket>

    @Query("select t from Ticket t where t.supportGroup.id = :groupId and t.status.code in ('QU','WP') order by t.dueAt")
    fun findQueue(groupId: Int): List<Ticket>

    @Query("select t from Ticket t where t.status.code in ('QU','WP') order by t.dueAt")
    fun findAllOpen(): List<Ticket>

    fun findTop200ByOrderByCreatedAtDesc(): List<Ticket>

    /** Число незавершённых заявок у исполнителя — основа балансировки нагрузки. */
    @Query("select count(t) from Ticket t where t.assignee.id = :employeeId and t.status.code in ('QU','WP')")
    fun countOpenByAssignee(employeeId: Int): Long

    @Query(value = "select lpad(nextval('ticket_number_seq')::text, 8, '0')", nativeQuery = true)
    fun nextNumber(): String
}

interface ApprovalRepo : JpaRepository<Approval, Long> {
    fun findByTicketIdOrderByStepNo(ticketId: Long): List<Approval>
    fun findByApproverIdAndDecisionOrderByCreatedAt(approverId: Int, decision: Decision): List<Approval>
}

interface TicketEventRepo : JpaRepository<TicketEvent, Long> {
    fun findByTicketIdOrderByCreatedAtAscIdAsc(ticketId: Long): List<TicketEvent>
}

interface TicketCommentRepo : JpaRepository<TicketComment, Long> {
    fun findByTicketIdOrderByCreatedAt(ticketId: Long): List<TicketComment>
}

interface AccessGrantRepo : JpaRepository<AccessGrant, Long> {
    fun findByEmployeeIdOrderByGrantedAtDesc(employeeId: Int): List<AccessGrant>
    fun findAllByOrderByGrantedAtDesc(): List<AccessGrant>
    fun findByEmployeeIdAndRevokedAtIsNull(employeeId: Int): List<AccessGrant>
    fun findByEmployeeIdAndAccessRoleIdAndRevokedAtIsNull(employeeId: Int, accessRoleId: Int): AccessGrant?
}
