package app.tsuzuku.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiSearchTest {
    private fun find(q: String) = searchEmoji(q).map { it.emoji }

    @Test fun turkishAndEnglishWordsFindTheSameEmoji() {
        assertTrue("💧" in find("su"))
        assertTrue("💧" in find("water"))
        assertTrue("📖" in find("kitap"))
        assertTrue("📖" in find("book"))
    }

    @Test fun ignoresCaseAndTurkishLetters() {
        assertTrue("🏃" in find("KOŞU"))
        assertTrue("🏃" in find("kosu"))
    }

    @Test fun blankQueryFindsNothing() {
        assertTrue(find("  ").isEmpty())
    }
}
