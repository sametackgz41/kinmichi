package app.tsuzuku.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test
    fun comparesVersionsNumerically() {
        assertTrue(isNewerVersion("2.10.0", "2.9.3"))
        assertTrue(isNewerVersion("v2.6.0", "2.5.0"))
        assertTrue(isNewerVersion("3.0", "2.99.99"))
        assertFalse(isNewerVersion("2.5.0", "2.5.0"))
        assertFalse(isNewerVersion("v2.4.0", "2.5.0"))
        assertFalse(isNewerVersion("2.5.0-beta", "2.5.0"))
    }
}
