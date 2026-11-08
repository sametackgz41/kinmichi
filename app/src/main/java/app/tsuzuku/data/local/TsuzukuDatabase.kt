package app.tsuzuku.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitEntryEntity::class,
        LibraryContentEntity::class,
        ContentSeasonEntity::class,
        ContentNoteEntity::class,
        GoalEntity::class,
        WeeklyReviewEntity::class,
        PlanTaskEntity::class,
        PlanChecklistItemEntity::class,
        PlanTemplateEntity::class,
        CategoryEntity::class,
        FocusSessionEntity::class,
        InboxItemEntity::class,
        HabitGoalLinkEntity::class,
        TaskGoalLinkEntity::class,
        HabitLibraryLinkEntity::class,
        DailyReviewEntity::class,
        PeriodReviewEntity::class,
        LibraryCollectionEntity::class,
        LibraryCollectionItemEntity::class,
        ProjectEntity::class,
        ProjectMilestoneEntity::class,
        ProjectTaskLinkEntity::class,
        ProjectGoalLinkEntity::class,
        HabitLevelEntity::class,
        HabitFeelingEntity::class,
    ],
    version = 24,
    exportSchema = false,
)
abstract class TsuzukuDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun libraryDao(): LibraryDao
    abstract fun goalDao(): GoalDao
    abstract fun backupDao(): BackupDao
    abstract fun planDao(): PlanDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusDao(): FocusDao
    abstract fun inboxDao(): InboxDao
    abstract fun connectionDao(): ConnectionDao
    abstract fun reviewDao(): ReviewDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var instance: TsuzukuDatabase? = null

        fun getInstance(context: Context): TsuzukuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TsuzukuDatabase::class.java,
                    "tsuzuku.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `library_contents` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `status` TEXT NOT NULL, `creator` TEXT NOT NULL, `coverUri` TEXT, `currentUnits` REAL NOT NULL, `totalUnits` REAL NOT NULL, `unit` TEXT NOT NULL, `generalNote` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_contents_status` ON `library_contents` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_contents_category` ON `library_contents` (`category`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `content_seasons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contentId` INTEGER NOT NULL, `number` INTEGER NOT NULL, `title` TEXT NOT NULL, `currentEpisodes` INTEGER NOT NULL, `totalEpisodes` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`contentId`) REFERENCES `library_contents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_seasons_contentId` ON `content_seasons` (`contentId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_content_seasons_contentId_number` ON `content_seasons` (`contentId`, `number`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `content_notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contentId` INTEGER NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`contentId`) REFERENCES `library_contents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_content_notes_contentId` ON `content_notes` (`contentId`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderMinutes` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `reminderRepeatMinutes` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `generalNote` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `currentValue` REAL NOT NULL, `targetValue` REAL NOT NULL, `unit` TEXT NOT NULL, `dueDateEpochDay` INTEGER DEFAULT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status` ON `goals` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_dueDateEpochDay` ON `goals` (`dueDateEpochDay`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `goalPeriod` TEXT NOT NULL DEFAULT 'Daily'")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `periodTarget` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `routineName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `prerequisiteHabitId` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `healthMetric` TEXT NOT NULL DEFAULT 'None'")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weekly_reviews` (`weekStartEpochDay` INTEGER NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`weekStartEpochDay`))",
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `plan_tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `scheduledEpochDay` INTEGER DEFAULT NULL, `dueMinutes` INTEGER DEFAULT NULL, `priority` INTEGER NOT NULL, `estimatedMinutes` INTEGER DEFAULT NULL, `actualMinutes` INTEGER DEFAULT NULL, `recurrence` TEXT NOT NULL, `status` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `carryCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_tasks_scheduledEpochDay` ON `plan_tasks` (`scheduledEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_tasks_status` ON `plan_tasks` (`status`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `plan_checklist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `title` TEXT NOT NULL, `completed` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, FOREIGN KEY(`taskId`) REFERENCES `plan_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_checklist_items_taskId` ON `plan_checklist_items` (`taskId`)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `minimumTarget` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `maximumTarget` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `difficulty` INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `contextLabel` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `intention` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `activeStartEpochDay` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `activeEndEpochDay` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `repeatIntervalDays` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `pausedUntilEpochDay` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `plan_tasks` ADD COLUMN `startMinutes` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `plan_tasks` ADD COLUMN `endMinutes` INTEGER DEFAULT NULL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `plan_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `priority` INTEGER NOT NULL, `estimatedMinutes` INTEGER DEFAULT NULL, `startMinutes` INTEGER DEFAULT NULL, `endMinutes` INTEGER DEFAULT NULL, `recurrence` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `plan_tasks` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `custom_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_custom_categories_name` ON `custom_categories` (`name`)")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `plan_templates` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER, `habitId` INTEGER, `goalId` INTEGER, `mode` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `durationMinutes` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `interrupted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_taskId` ON `focus_sessions` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_habitId` ON `focus_sessions` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_goalId` ON `focus_sessions` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_focus_sessions_startedAt` ON `focus_sessions` (`startedAt`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `inbox_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `text` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inbox_items_status` ON `inbox_items` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inbox_items_createdAt` ON `inbox_items` (`createdAt`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `habit_goal_links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `goalId` INTEGER NOT NULL, `contributionMode` TEXT NOT NULL, `contributionValue` REAL NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_goal_links_habitId` ON `habit_goal_links` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_goal_links_goalId` ON `habit_goal_links` (`goalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_goal_links_habitId_goalId` ON `habit_goal_links` (`habitId`, `goalId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `task_goal_links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `taskId` INTEGER NOT NULL, `goalId` INTEGER NOT NULL, `contributionValue` REAL NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_goal_links_taskId` ON `task_goal_links` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_goal_links_goalId` ON `task_goal_links` (`goalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_goal_links_taskId_goalId` ON `task_goal_links` (`taskId`, `goalId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `habit_library_links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `contentId` INTEGER NOT NULL, `contributionMultiplier` REAL NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_library_links_habitId` ON `habit_library_links` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_library_links_contentId` ON `habit_library_links` (`contentId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_library_links_habitId_contentId` ON `habit_library_links` (`habitId`, `contentId`)")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `rating` REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `startDateEpochDay` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `finishDateEpochDay` INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `reviewNote` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `library_contents` ADD COLUMN `rewatchDesired` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `library_collections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `emoji` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `library_collection_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `collectionId` INTEGER NOT NULL, `contentId` INTEGER NOT NULL, FOREIGN KEY(`collectionId`) REFERENCES `library_collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`contentId`) REFERENCES `library_contents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_collection_items_collectionId` ON `library_collection_items` (`collectionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_library_collection_items_contentId` ON `library_collection_items` (`contentId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_library_collection_items_collectionId_contentId` ON `library_collection_items` (`collectionId`, `contentId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_reviews` (`epochDay` INTEGER PRIMARY KEY NOT NULL, `mood` INTEGER NOT NULL, `energy` INTEGER NOT NULL, `productivity` INTEGER NOT NULL, `stress` INTEGER NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `period_reviews` (`periodType` TEXT NOT NULL, `periodKey` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`periodType`, `periodKey`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_period_reviews_periodType_periodKey` ON `period_reviews` (`periodType`, `periodKey`)")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `emoji` TEXT NOT NULL, `status` TEXT NOT NULL, `targetDateEpochDay` INTEGER, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_targetDateEpochDay` ON `projects` (`targetDateEpochDay`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_milestones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `dueDateEpochDay` INTEGER, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_milestones_projectId` ON `project_milestones` (`projectId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_task_links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `taskId` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_task_links_projectId` ON `project_task_links` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_task_links_taskId` ON `project_task_links` (`taskId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_project_task_links_projectId_taskId` ON `project_task_links` (`projectId`, `taskId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `project_goal_links` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `goalId` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_goal_links_projectId` ON `project_goal_links` (`projectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_goal_links_goalId` ON `project_goal_links` (`goalId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_project_goal_links_projectId_goalId` ON `project_goal_links` (`projectId`, `goalId`)")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `journeyTotal` REAL")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `journeyStart` REAL NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `periodDays` INTEGER NOT NULL DEFAULT 10")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `slots` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `deletedAt` INTEGER")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_feelings` (`habitId` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `feeling` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`habitId`, `localDate`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_feelings_habitId` ON `habit_feelings` (`habitId`)")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `tinyVersion` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `currentLevel` INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_levels` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `habitId` INTEGER NOT NULL, `levelNumber` INTEGER NOT NULL, `name` TEXT NOT NULL, `target` REAL NOT NULL, `minimumTarget` REAL NOT NULL DEFAULT 0.0, `maximumTarget` REAL DEFAULT NULL, `rewardNote` TEXT NOT NULL DEFAULT '', `createdAt` INTEGER NOT NULL, FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_levels_habitId` ON `habit_levels` (`habitId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_levels_habitId_levelNumber` ON `habit_levels` (`habitId`, `levelNumber`)")
            }
        }
    }
}

