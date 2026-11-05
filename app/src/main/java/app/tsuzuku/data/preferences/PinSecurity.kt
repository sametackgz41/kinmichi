package app.tsuzuku.data.preferences

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private val validPin = Regex("\\d{4}")

    fun isValid(pin: String): Boolean = validPin.matches(pin)

    fun newSalt(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hash(pin: String, salt: String): String {
        val saltBytes = "tsuzuku-pin-v1:$salt".toByteArray(StandardCharsets.UTF_8)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, 120_000, 256)
        return try {
            Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    fun matches(pin: String, salt: String, expectedHash: String): Boolean {
        if (!isValid(pin) || salt.isBlank() || expectedHash.isBlank()) return false
        return java.security.MessageDigest.isEqual(
            hash(pin, salt).toByteArray(StandardCharsets.UTF_8),
            expectedHash.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
