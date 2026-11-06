package app.tsuzuku.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WatchTodayTest {
    private val json = """
        {"date":"2026-11-06","habits":[
          {"id":1,"name":"Read","emoji":"📖","color":-16776961,"done":true,"detail":"","fraction":1},
          {"id":2,"name":"Water","emoji":"💧","color":-16776961,"done":false,"detail":"3/8 glasses","fraction":0.375}
        ]}
    """.trimIndent()

    @Test
    fun parsesThePhonePayload() {
        val today = WatchToday.parse(json)
        assertEquals(2, today.habits.size)
        assertEquals(1, today.doneCount)
        assertEquals("3/8 glasses", today.habits[1].detail)
        assertTrue(today.isCurrent(LocalDate.of(2026, 11, 6)))
        assertFalse(today.isCurrent(LocalDate.of(2026, 11, 7)))
    }

    @Test
    fun brokenPayloadIsEmpty() {
        assertEquals(WatchToday.Empty, WatchToday.parse("{nope"))
        assertEquals(WatchToday.Empty, WatchToday.parse(null))
    }
}
