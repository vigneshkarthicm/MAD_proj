package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AchievementDao
import com.example.data.local.dao.DiscussionDao
import com.example.data.local.dao.ExploreDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ClassInfoEntity
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.PeerStudentEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.StudentProfileEntity

@Database(
    entities = [
        StudentProfileEntity::class,
        SkillEntity::class,
        AchievementEntity::class,
        DiscussionEntity::class,
        PeerStudentEntity::class,
        ClassInfoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SkillDiscoveryDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun achievementDao(): AchievementDao
    abstract fun discussionDao(): DiscussionDao
    abstract fun exploreDao(): ExploreDao

    companion object {
        @Volatile
        private var INSTANCE: SkillDiscoveryDatabase? = null

        fun getDatabase(context: Context): SkillDiscoveryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SkillDiscoveryDatabase::class.java,
                    "skill_discovery_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE discussions ADD COLUMN visibility TEXT NOT NULL DEFAULT 'class'")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE student_profiles ADD COLUMN contactNumber TEXT NOT NULL DEFAULT '9876543210'")
            }
        }
    }
}
