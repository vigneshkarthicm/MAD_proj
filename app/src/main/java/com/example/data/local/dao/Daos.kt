package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ClassInfoEntity
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.PeerStudentEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.StudentProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM student_profiles WHERE id = :studentId LIMIT 1")
    fun getProfile(studentId: String): Flow<StudentProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: StudentProfileEntity)

    @Query("SELECT * FROM student_skills WHERE studentId = :studentId ORDER BY level DESC")
    fun getSkills(studentId: String): Flow<List<SkillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkills(skills: List<SkillEntity>)

    @Query("DELETE FROM student_skills WHERE studentId = :studentId")
    suspend fun clearSkills(studentId: String)

    @Query("SELECT * FROM class_info WHERE id = :classId LIMIT 1")
    fun getClassInfo(classId: String): Flow<ClassInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassInfo(classInfo: ClassInfoEntity)
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAchievements(studentId: String): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Query("DELETE FROM achievements WHERE id = :achievementId")
    suspend fun deleteAchievement(achievementId: String)

    @Query("DELETE FROM achievements WHERE studentId = :studentId")
    suspend fun clearAchievements(studentId: String)
}

@Dao
interface DiscussionDao {
    @Query("SELECT * FROM discussions ORDER BY createdAt DESC")
    fun getAllDiscussions(): Flow<List<DiscussionEntity>>

    @Query("SELECT * FROM discussions WHERE id = :discussionId LIMIT 1")
    suspend fun getDiscussionById(discussionId: String): DiscussionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussions(discussions: List<DiscussionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscussion(discussion: DiscussionEntity)

    @Update
    suspend fun updateDiscussion(discussion: DiscussionEntity)

    @Query("UPDATE discussions SET upvotesCount = :count, userHasUpvoted = :hasUpvoted WHERE id = :id")
    suspend fun updateUpvote(id: String, count: Int, hasUpvoted: Boolean)

    @Query("UPDATE discussions SET pollOptionsJson = :optionsJson, userVotedOptionIndex = :optionIndex, visibilityPercentage = :visPercent WHERE id = :id")
    suspend fun updatePollVote(id: String, optionsJson: String, optionIndex: Int, visPercent: Int)

    @Query("DELETE FROM discussions")
    suspend fun clearDiscussions()
}

@Dao
interface ExploreDao {
    @Query("SELECT * FROM peer_students")
    fun getAllPeers(): Flow<List<PeerStudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeers(peers: List<PeerStudentEntity>)

    @Query("DELETE FROM peer_students")
    suspend fun clearPeers()
}
