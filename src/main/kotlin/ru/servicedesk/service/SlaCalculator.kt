package ru.servicedesk.service

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@ConfigurationProperties(prefix = "servicedesk.sla")
data class SlaProperties(
    val workDayStart: LocalTime = LocalTime.of(9, 0),
    val workDayEnd: LocalTime = LocalTime.of(18, 0),
)

/**
 * Расчёт контрольного срока исполнения заявки в рабочем времени.
 *
 * Норматив вида заявки задаётся в рабочих часах и корректируется множителем приоритета.
 * Рабочим считается время с понедельника по пятницу в пределах рабочего дня; обращение,
 * поступившее в нерабочее время, начинает отсчёт срока с начала ближайшего рабочего дня.
 */
@Component
class SlaCalculator(private val props: SlaProperties) {

    /** Норматив в минутах с учётом приоритета: часы вида заявки × множитель приоритета. */
    fun normMinutes(slaHours: Int, priorityFactor: BigDecimal): Long =
        BigDecimal(slaHours * 60L).multiply(priorityFactor)
            .setScale(0, RoundingMode.HALF_UP).toLong().coerceAtLeast(15)

    fun dueAt(createdAt: LocalDateTime, slaHours: Int, priorityFactor: BigDecimal): LocalDateTime {
        var remaining = normMinutes(slaHours, priorityFactor)
        // секунды отбрасываются: срок исчисляется в целых минутах
        var cursor = alignToWorkTime(createdAt.truncatedTo(ChronoUnit.MINUTES))
        while (remaining > 0) {
            val dayEnd = cursor.toLocalDate().atTime(props.workDayEnd)
            val available = Duration.between(cursor, dayEnd).toMinutes()
            if (remaining <= available) {
                return cursor.plusMinutes(remaining)
            }
            remaining -= available
            cursor = alignToWorkTime(dayEnd.plusMinutes(1))
        }
        return cursor
    }

    fun workMinutesBetween(from: LocalDateTime, to: LocalDateTime): Long {
        if (!to.isAfter(from)) return 0
        var total = 0L
        var cursor = alignToWorkTime(from)
        while (cursor.isBefore(to)) {
            val dayEnd = cursor.toLocalDate().atTime(props.workDayEnd)
            val sliceEnd = if (to.isBefore(dayEnd)) to else dayEnd
            total += Duration.between(cursor, sliceEnd).toMinutes().coerceAtLeast(0)
            cursor = alignToWorkTime(dayEnd.plusMinutes(1))
        }
        return total
    }

    fun alignToWorkTime(moment: LocalDateTime): LocalDateTime {
        var m = moment
        if (m.toLocalTime().isBefore(props.workDayStart)) {
            m = m.toLocalDate().atTime(props.workDayStart)
        } else if (!m.toLocalTime().isBefore(props.workDayEnd)) {
            m = m.toLocalDate().plusDays(1).atTime(props.workDayStart)
        }
        while (m.dayOfWeek == DayOfWeek.SATURDAY || m.dayOfWeek == DayOfWeek.SUNDAY) {
            m = m.toLocalDate().plusDays(1).atTime(props.workDayStart)
        }
        return m
    }
}
