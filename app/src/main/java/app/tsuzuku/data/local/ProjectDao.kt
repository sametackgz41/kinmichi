package app.tsuzuku.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY sortOrder ASC, createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("SELECT * FROM project_milestones WHERE projectId = :projectId ORDER BY sortOrder ASC, createdAt ASC")
    fun observeMilestones(projectId: Long): Flow<List<ProjectMilestoneEntity>>

    @Query("SELECT * FROM project_milestones WHERE projectId = :projectId ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getMilestones(projectId: Long): List<ProjectMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: ProjectMilestoneEntity): Long

    @Query("SELECT * FROM project_milestones WHERE id = :id")
    suspend fun getMilestone(id: Long): ProjectMilestoneEntity?

    @Query("UPDATE project_milestones SET completed = :completed WHERE id = :id")
    suspend fun setMilestoneCompleted(id: Long, completed: Boolean)

    @Update
    suspend fun updateMilestone(milestone: ProjectMilestoneEntity)

    @Query("DELETE FROM project_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: Long)

    @Query("SELECT * FROM project_task_links WHERE projectId = :projectId")
    fun observeTaskLinks(projectId: Long): Flow<List<ProjectTaskLinkEntity>>

    @Query("SELECT * FROM project_task_links")
    fun observeAllTaskLinks(): Flow<List<ProjectTaskLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskLink(link: ProjectTaskLinkEntity): Long

    @Query("DELETE FROM project_task_links WHERE projectId = :projectId AND taskId = :taskId")
    suspend fun deleteTaskLink(projectId: Long, taskId: Long)

    @Query("SELECT * FROM project_goal_links WHERE projectId = :projectId")
    fun observeGoalLinks(projectId: Long): Flow<List<ProjectGoalLinkEntity>>

    @Query("SELECT * FROM project_goal_links")
    fun observeAllGoalLinks(): Flow<List<ProjectGoalLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoalLink(link: ProjectGoalLinkEntity): Long

    @Query("DELETE FROM project_goal_links WHERE projectId = :projectId AND goalId = :goalId")
    suspend fun deleteGoalLink(projectId: Long, goalId: Long)
}
