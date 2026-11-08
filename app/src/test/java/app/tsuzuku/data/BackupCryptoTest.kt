package app.tsuzuku.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    private val json = """{"habits":[{"name":"Kitap oku 📖 çğüşıö"}]}"""

    @Test
    fun opensWithTheRightPassword() {
        val sealed = BackupCrypto.encrypt(json, "doğru-parola".toCharArray())
        assertTrue(BackupCrypto.isEncrypted(sealed))
        assertEquals(json, BackupCrypto.decrypt(sealed, "doğru-parola".toCharArray()))
    }

    @Test(expected = Exception::class)
    fun refusesAWrongPassword() {
        val sealed = BackupCrypto.encrypt(json, "doğru-parola".toCharArray())
        BackupCrypto.decrypt(sealed, "yanlış-parola".toCharArray())
    }

    @Test(expected = Exception::class)
    fun noticesAChangedByte() {
        val sealed = BackupCrypto.encrypt(json, "doğru-parola".toCharArray())
        sealed[sealed.size - 5] = (sealed[sealed.size - 5] + 1).toByte()
        BackupCrypto.decrypt(sealed, "doğru-parola".toCharArray())
    }

    @Test
    fun plainBackupsAreNotTakenForSealedOnes() {
        assertFalse(BackupCrypto.isEncrypted(json.toByteArray()))
    }

    @Test
    fun strengthGrowsWithLengthAndVariety() {
        assertEquals(0, BackupCrypto.strength("kısa"))
        assertEquals(2, BackupCrypto.strength("Uzun-Parola-2026"))
    }
}
