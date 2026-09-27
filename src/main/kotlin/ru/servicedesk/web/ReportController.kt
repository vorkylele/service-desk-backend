package ru.servicedesk.web

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.servicedesk.service.ReportService
import ru.servicedesk.service.SlaRow
import ru.servicedesk.service.WorkloadRow
import java.time.LocalDate

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('SUPPORT','SECURITY','ADMIN')")
class ReportController(private val reports: ReportService) {

    @GetMapping("/sla")
    fun sla(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<SlaRow> = reports.sla(from, to)

    @GetMapping("/workload")
    fun workload(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): List<WorkloadRow> = reports.workload(from, to)
}
