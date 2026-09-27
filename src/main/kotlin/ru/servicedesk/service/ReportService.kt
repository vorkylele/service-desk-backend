package ru.servicedesk.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate

data class SlaRow(
    val typeCode: String, val typeName: String, val total: Long, val resolved: Long,
    val breached: Long, val avgHours: Double, val maxHours: Double, val withinSlaPercent: Double,
)

data class WorkloadRow(val assignee: String, val groupName: String, val open: Long, val resolved: Long, val breached: Long)

@Service
class ReportService(private val jdbc: JdbcTemplate) {

    fun sla(from: LocalDate, to: LocalDate): List<SlaRow> = jdbc.query(
        """
        SELECT tt.code, tt.name,
               count(*)                                                          AS total,
               count(t.resolved_at)                                              AS resolved,
               count(*) FILTER (WHERE t.sla_breached)                            AS breached,
               coalesce(avg(extract(epoch FROM t.resolved_at - t.created_at)) / 3600, 0) AS avg_hours,
               coalesce(max(extract(epoch FROM t.resolved_at - t.created_at)) / 3600, 0) AS max_hours
        FROM ticket t JOIN ticket_type tt ON tt.id = t.type_id
        WHERE t.created_at >= ? AND t.created_at < ?
        GROUP BY tt.code, tt.name ORDER BY tt.code
        """.trimIndent(),
        { rs, _ ->
            val resolved = rs.getLong("resolved")
            val breached = rs.getLong("breached")
            SlaRow(
                rs.getString("code").trim(), rs.getString("name"), rs.getLong("total"), resolved, breached,
                round1(rs.getDouble("avg_hours")), round1(rs.getDouble("max_hours")),
                if (resolved == 0L) 100.0 else round1(100.0 * (resolved - breached) / resolved),
            )
        },
        from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
    )

    fun workload(from: LocalDate, to: LocalDate): List<WorkloadRow> = jdbc.query(
        """
        SELECT e.full_name, g.name AS group_name,
               count(*) FILTER (WHERE t.status_code IN ('QU', 'WP'))  AS open,
               count(t.resolved_at)                                   AS resolved,
               count(*) FILTER (WHERE t.sla_breached)                 AS breached
        FROM ticket t
             JOIN employee e ON e.id = t.assignee_id
             JOIN support_group g ON g.id = t.support_group_id
        WHERE t.created_at >= ? AND t.created_at < ?
        GROUP BY e.full_name, g.name ORDER BY resolved DESC
        """.trimIndent(),
        { rs, _ -> WorkloadRow(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)) },
        from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
    )

    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}
