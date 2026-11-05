package app.tsuzuku.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudChunkCodecTest {
    @Test
    fun unicodePayloadRoundTripsAcrossMultipleChunks() {
        val json = "{\"note\":\"Türkçe: ğüşiöç · 日本語 続く 📚\",\"data\":\"${"x".repeat(400_000)}\"}"

        val encoded = CloudChunkCodec.encode(json)

        assertTrue(encoded.chunks.size > 1)
        assertEquals(json.toByteArray().size, encoded.byteCount)
        assertEquals(json, CloudChunkCodec.decode(encoded.chunks, encoded.hash))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tamperedPayloadFailsIntegrityCheck() {
        val encoded = CloudChunkCodec.encode("{\"habits\":[]}")
        val changed = encoded.chunks.map(ByteArray::clone).toMutableList()
        changed.first()[0] = (changed.first()[0].toInt() xor 1).toByte()

        CloudChunkCodec.decode(changed, encoded.hash)
    }
}
