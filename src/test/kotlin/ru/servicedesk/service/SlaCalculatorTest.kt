package ru.servicedesk.service

import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SlaCalculatorTest {
    private val sla = SlaCalculator(SlaProperties())
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 9, day, h, m)   // 21.09.2026 — понедельник

    @Test
    fun `срок в пределах рабочего дня`() =
        assertEquals(at(21, 14), sla.dueAt(at(21, 10), 4, BigDecimal.ONE))

    @Test
    fun `срок переходит на следующий рабочий день`() =
        assertEquals(at(22, 11), sla.dueAt(at(21, 15), 5, BigDecimal.ONE))

    @Test
    fun `обращение в нерабочее время отсчитывается с начала рабочего дня`() =
        assertEquals(at(22, 11), sla.dueAt(at(21, 20, 30), 2, BigDecimal.ONE))

    @Test
    fun `выходные дни в срок не включаются`() =
        assertEquals(at(28, 12), sla.dueAt(at(25, 16), 5, BigDecimal.ONE))            // пт 16:00 + 5 ч = пн 12:00

    @Test
    fun `обращение в субботу отсчитывается с понедельника`() =
        assertEquals(at(28, 10), sla.dueAt(at(26, 12), 1, BigDecimal.ONE))

    @Test
    fun `критический приоритет сокращает норматив вчетверо`() =
        assertEquals(at(21, 12), sla.dueAt(at(21, 10), 8, BigDecimal("0.25")))

    @Test
    fun `низкий приоритет увеличивает норматив в полтора раза`() =
        assertEquals(at(22, 13), sla.dueAt(at(21, 10), 8, BigDecimal("1.50")))         // 12 ч: 8 в пн + 4 во вт

    @Test
    fun `норматив в 16 часов завершается на второй рабочий день`() =
        assertEquals(at(22, 17), sla.dueAt(at(21, 10), 16, BigDecimal.ONE))           // 8 ч в пн + 8 ч во вт

    @Test
    fun `затраченное рабочее время не учитывает ночь`() =
        assertEquals(180, sla.workMinutesBetween(at(21, 17), at(22, 11)))

    @Test
    fun `затраченное рабочее время не учитывает выходные`() =
        assertEquals(120, sla.workMinutesBetween(at(25, 17), at(28, 10)))
}
