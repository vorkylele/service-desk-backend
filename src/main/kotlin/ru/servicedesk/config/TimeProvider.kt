package ru.servicedesk.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/**
 * Единый источник текущего времени.
 *
 * На стенде тестирования часы можно сдвинуть, чтобы сценарии, зависящие от рабочего времени,
 * воспроизводились одинаково в любой момент запуска:
 *   servicedesk.clock.start-at       — момент, с которого часы начинают идти после старта приложения;
 *   servicedesk.clock.offset-minutes — постоянное смещение относительно системных часов.
 */
@Component
class TimeProvider(
    @Value("\${servicedesk.clock.start-at:}") startAt: String,
    @Value("\${servicedesk.clock.offset-minutes:0}") offsetMinutes: Long,
) {
    private val clock: Clock = Clock.offset(
        Clock.systemDefaultZone(),
        if (startAt.isBlank()) Duration.ofMinutes(offsetMinutes)
        else Duration.between(LocalDateTime.now(), LocalDateTime.parse(startAt)),
    )

    fun now(): LocalDateTime = LocalDateTime.now(clock).withNano(0)
}
