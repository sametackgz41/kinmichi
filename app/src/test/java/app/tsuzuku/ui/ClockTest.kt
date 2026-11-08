package app.tsuzuku.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class ClockTest {
    @Test fun turkishUsesADot() {
        assertEquals("23.00", formatClock(23 * 60, Locale.forLanguageTag("tr")))
        assertEquals("07.05", formatClock(7 * 60 + 5, Locale.forLanguageTag("tr-TR")))
    }

    @Test fun otherLanguagesKeepTheColon() {
        assertEquals("23:00", formatClock(23 * 60, Locale.ENGLISH))
        assertEquals("00:00", formatClock(0, Locale.GERMAN))
    }

    @Test fun readsBothSeparators() {
        assertEquals(23 * 60, parseClock("23.00"))
        assertEquals(9 * 60 + 30, parseClock("09:30"))
        assertNull(parseClock("24.00"))
        assertNull(parseClock("abc"))
    }
}
