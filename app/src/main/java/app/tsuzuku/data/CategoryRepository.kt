package app.tsuzuku.data

import app.tsuzuku.data.local.CategoryDao
import app.tsuzuku.data.local.CategoryEntity

class CategoryRepository(private val dao: CategoryDao) {
    fun observeAll() = dao.observeAll()
    suspend fun create(name: String, emoji: String, colorArgb: Long) {
        val clean = name.trim().take(32)
        if (clean.isNotBlank()) dao.insert(CategoryEntity(name = clean, emoji = emoji.ifBlank { "◆" }.take(4), colorArgb = colorArgb, createdAt = System.currentTimeMillis()))
    }
    suspend fun delete(item: CategoryEntity) = dao.deleteAndDetach(item)
}
