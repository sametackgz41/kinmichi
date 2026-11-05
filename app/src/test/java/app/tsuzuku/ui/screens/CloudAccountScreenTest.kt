package app.tsuzuku.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudAccountScreenTest {
    @Test
    fun googleDisplayNameUsesFirstNameForGreeting() {
        assertEquals("Abdulsamet", preferredGoogleName("  Abdulsamet Açıkgöz  "))
        assertEquals("", preferredGoogleName("   "))
    }
}
