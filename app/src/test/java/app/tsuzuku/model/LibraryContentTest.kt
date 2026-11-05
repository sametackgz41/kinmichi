package app.tsuzuku.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryContentTest {
    @Test
    fun progressFractionUsesCurrentAndTotal() {
        val content = sampleContent(current = 120.0, total = 300.0)
        assertEquals(0.4f, content.progressFraction, 0.001f)
    }

    @Test
    fun progressFractionIsClamped() {
        val content = sampleContent(current = 350.0, total = 300.0)
        assertEquals(1f, content.progressFraction, 0.001f)
    }

    @Test
    fun seriesAndAnimeUseSeasons() {
        assertTrue(sampleContent(ContentCategory.Series).usesSeasons)
        assertTrue(sampleContent(ContentCategory.Anime).usesSeasons)
        assertFalse(sampleContent(ContentCategory.Book).usesSeasons)
    }

    @Test
    fun seasonProgressUsesEpisodeCounts() {
        val season = ContentSeason(1, 1, 1, "", 6, 12)
        assertEquals(0.5f, season.progressFraction, 0.001f)
    }

    private fun sampleContent(
        category: ContentCategory = ContentCategory.Book,
        current: Double = 0.0,
        total: Double = 100.0,
    ) = LibraryContent(
        id = 1,
        title = "Test",
        category = category,
        status = ContentStatus.InProgress,
        creator = "",
        coverUri = null,
        currentUnits = current,
        totalUnits = total,
        unit = "sayfa",
        generalNote = "",
        sortOrder = 0,
    )
}
