package app.tsuzuku.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-protected backups: the JSON backup, compressed, then sealed with AES-256-GCM under a
 * key stretched from the password (PBKDF2, 210 000 rounds). Without the password nobody can open
 * it, and a wrong password or a changed byte is caught rather than giving garbage.
 *
 * File: "KMENC1" | salt (16) | iv (12) | ciphertext with tag.
 */
object BackupCrypto {
    private val MAGIC = "KMENC1".toByteArray(Charsets.US_ASCII)
    private const val ROUNDS = 210_000
    const val MIN_PASSWORD = 8

    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    fun encrypt(json: String, password: CharArray): ByteArray {
        require(password.size >= MIN_PASSWORD)
        val random = SecureRandom()
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val packed = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(json.toByteArray(Charsets.UTF_8)) } }.toByteArray()
        return MAGIC + salt + iv + cipher.doFinal(packed)
    }

    /** The JSON inside, or an exception for a wrong password or a damaged file. */
    fun decrypt(bytes: ByteArray, password: CharArray): String {
        require(isEncrypted(bytes) && bytes.size > MAGIC.size + 28)
        var at = MAGIC.size
        val salt = bytes.copyOfRange(at, at + 16).also { at += 16 }
        val iv = bytes.copyOfRange(at, at + 12).also { at += 12 }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        val packed = cipher.doFinal(bytes, at, bytes.size - at)
        return GZIPInputStream(ByteArrayInputStream(packed)).use { it.readBytes() }.toString(Charsets.UTF_8)
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ROUNDS, 256)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    /** 0 weak, 1 fair, 2 strong: length and a mix of kinds of characters. */
    fun strength(password: String): Int {
        if (password.length < MIN_PASSWORD) return 0
        val kinds = listOf(
            password.any(Char::isLowerCase),
            password.any(Char::isUpperCase),
            password.any(Char::isDigit),
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        return when {
            password.length >= 12 && kinds >= 3 -> 2
            password.length >= 10 || kinds >= 2 -> 1
            else -> 0
        }
    }
}
