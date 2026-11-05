package app.tsuzuku.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test
    fun acceptsExactlyFourDigits() {
        assertTrue(PinSecurity.isValid("0123"))
        assertFalse(PinSecurity.isValid("123"))
        assertFalse(PinSecurity.isValid("12345"))
        assertFalse(PinSecurity.isValid("12a4"))
    }

    @Test
    fun hashMatchesOnlyTheCorrectPinAndSalt() {
        val salt = PinSecurity.newSalt()
        val hash = PinSecurity.hash("4821", salt)

        assertTrue(PinSecurity.matches("4821", salt, hash))
        assertFalse(PinSecurity.matches("4820", salt, hash))
        assertFalse(PinSecurity.matches("4821", "different", hash))
    }

    @Test
    fun saltsProduceDifferentHashes() {
        assertNotEquals(PinSecurity.hash("4821", "salt-a"), PinSecurity.hash("4821", "salt-b"))
    }
}
