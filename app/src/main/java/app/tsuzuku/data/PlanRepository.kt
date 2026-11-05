package app.tsuzuku.data

import app.tsuzuku.data.local.PlanChecklistItemEntity
import app.tsuzuku.data.local.PlanDao
import app.tsuzuku.data.local.PlanTaskEntity
import app.tsuzuku.data.local.PlanTemplateEntity
import app.tsuzuku.model.PlanChecklistItem
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.PlanTaskDraft
import app.tsuzuku.model.PlanTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

class PlanRepository(private val dao: PlanDao) {
    fun observeTemplates(): Flow<List<PlanTemplate>> = dao.observeTemplates().map { templates ->
        templates.map { PlanTemplate(it.id, it.title, it.note, it.priority, it.estimatedMinutes, it.startMinutes, it.endMinutes, it.recurrence, it.tags) }
    }
    fun observeTasks(): Flow<List<PlanTask>> = combine(dao.observeTasks(), dao.observeChecklist()) { tasks, items ->
        val byTask = items.groupBy(PlanChecklistItemEntity::taskId)
        tasks.map { task ->
            PlanTask(
                id = task.id, title = task.title, note = task.note,
                scheduledEpochDay = task.scheduledEpochDay, dueMinutes = task.dueMinutes,
                priority = task.priority, estimatedMinutes = task.estimatedMinutes,
                actualMinutes = task.actualMinutes, recurrence = task.recurrence,
                completed = task.status == "Completed", sortOrder = task.sortOrder,
                carryCount = task.carryCount,
                checklist = byTask[task.id].orEmpty().map { PlanChecklistItem(it.id, it.title, it.completed) },
                startMinutes = task.startMinutes, endMinutes = task.endMinutes,
                tags = task.tags,
            )
        }
    }

    suspend fun create(draft: PlanTaskDraft): Long {
        val now = System.currentTimeMillis()
        return dao.insertTask(PlanTaskEntity(
            title = draft.title.trim(), note = draft.note.trim(), scheduledEpochDay = draft.scheduledEpochDay,
            dueMinutes = draft.dueMinutes, priority = draft.priority, estimatedMinutes = draft.estimatedMinutes,
            recurrence = draft.recurrence, createdAt = now, updatedAt = now,
            startMinutes = draft.startMinutes, endMinutes = draft.endMinutes,
            tags = normalizePlanTags(draft.tags),
        ))
    }

    suspend fun update(id: Long, draft: PlanTaskDraft) {
        val current = dao.getTask(id) ?: return
        dao.updateTask(current.copy(
            title = draft.title.trim(), note = draft.note.trim(), scheduledEpochDay = draft.scheduledEpochDay,
            dueMinutes = draft.dueMinutes, priority = draft.priority.coerceIn(1, 3), estimatedMinutes = draft.estimatedMinutes,
            recurrence = draft.recurrence, startMinutes = draft.startMinutes, endMinutes = draft.endMinutes,
            tags = normalizePlanTags(draft.tags), updatedAt = System.currentTimeMillis(),
        ))
    }

    suspend fun toggle(task: PlanTask) {
        val now = System.currentTimeMillis()
        dao.setStatus(task.id, if (task.completed) "Active" else "Completed", now)
        if (!task.completed && task.recurrence != "None") {
            val base = task.scheduledEpochDay?.let(LocalDate::ofEpochDay) ?: LocalDate.now()
            val next = when (task.recurrence) {
                "Daily" -> base.plusDays(1)
                "Weekdays" -> generateSequence(base.plusDays(1)) { it.plusDays(1) }.first { it.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
                "Weekly" -> base.plusWeeks(1)
                "Monthly" -> base.plusMonths(1)
                else -> null
            }
            if (next != null) dao.insertTask(PlanTaskEntity(
                title = task.title, note = task.note, scheduledEpochDay = next.toEpochDay(), dueMinutes = task.dueMinutes,
                priority = task.priority, estimatedMinutes = task.estimatedMinutes, recurrence = task.recurrence,
                sortOrder = task.sortOrder, startMinutes = task.startMinutes, endMinutes = task.endMinutes,
                tags = task.tags,
                createdAt = now, updatedAt = now,
            ))
        }
    }
    suspend fun move(id: Long, day: Long?) = dao.moveTask(id, day, System.currentTimeMillis())
    suspend fun delete(id: Long) = dao.deleteTask(id)
    suspend fun setActualMinutes(id: Long, minutes: Int?) = dao.updateActualMinutes(id, minutes?.coerceIn(0, 100_000), System.currentTimeMillis())
    suspend fun addActualMinutes(id: Long, minutes: Int) {
        val current = dao.getTask(id) ?: return
        val updated = (current.actualMinutes ?: 0) + minutes
        dao.updateActualMinutes(id, updated.coerceIn(0, 100_000), System.currentTimeMillis())
    }
    suspend fun addTags(tasks: List<PlanTask>, value: String) {
        val incoming = normalizePlanTags(value).split(',').filter(String::isNotBlank)
        if (incoming.isEmpty()) return
        val now = System.currentTimeMillis()
        tasks.forEach { task -> dao.updateTags(task.id, normalizePlanTags(task.tags.split(',').plus(incoming).joinToString(",")), now) }
    }
    suspend fun duplicate(task: PlanTask): Long {
        val newId = create(PlanTaskDraft(
            title = "${task.title} · 2", note = task.note, scheduledEpochDay = task.scheduledEpochDay,
            dueMinutes = task.dueMinutes, priority = task.priority, estimatedMinutes = task.estimatedMinutes,
            recurrence = task.recurrence, startMinutes = task.startMinutes, endMinutes = task.endMinutes,
            tags = task.tags,
        ))
        dao.getChecklist(task.id).forEach { item ->
            dao.insertChecklistItem(item.copy(id = 0, taskId = newId, completed = false))
        }
        return newId
    }
    suspend fun reorder(id: Long, direction: Int) {
        val tasks = dao.getActiveTasks()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = (index + direction).coerceIn(tasks.indices)
        if (target == index) return
        val reordered = tasks.toMutableList().apply { add(target, removeAt(index)) }
        val now = System.currentTimeMillis()
        reordered.forEachIndexed { order, task -> dao.updateSortOrder(task.id, order, now) }
    }
    suspend fun addChecklistItem(taskId: Long, title: String) {
        if (title.isNotBlank()) dao.insertChecklistItem(PlanChecklistItemEntity(taskId = taskId, title = title.trim()))
    }
    suspend fun toggleChecklistItem(id: Long) = dao.toggleChecklistItem(id)
    suspend fun deleteChecklistItem(id: Long) = dao.deleteChecklistItem(id)
    suspend fun saveTemplate(draft: PlanTaskDraft) {
        if (draft.title.isBlank()) return
        dao.insertTemplate(PlanTemplateEntity(
            title = draft.title.trim(), note = draft.note.trim(), priority = draft.priority,
            estimatedMinutes = draft.estimatedMinutes, startMinutes = draft.startMinutes, endMinutes = draft.endMinutes,
            recurrence = draft.recurrence, tags = normalizePlanTags(draft.tags), createdAt = System.currentTimeMillis(),
        ))
    }
    suspend fun createFromTemplate(template: PlanTemplate, day: Long = LocalDate.now().toEpochDay()) = create(PlanTaskDraft(
        title = template.title, note = template.note, scheduledEpochDay = day, priority = template.priority,
        estimatedMinutes = template.estimatedMinutes, recurrence = template.recurrence,
        startMinutes = template.startMinutes, endMinutes = template.endMinutes, tags = template.tags,
    ))
    suspend fun deleteTemplate(id: Long) = dao.deleteTemplate(id)
}

private fun normalizePlanTags(value: String): String = value.split(',', ';', '#').map(String::trim).filter(String::isNotBlank).distinct().joinToString(",")
