package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profiles")
data class StudentProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val studentId: String,
    val classId: String,
    val className: String,
    val avatarUrl: String,
    val bio: String,
    val contactNumber: String = "9876543210",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "student_skills")
data class SkillEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val name: String,
    val category: String,
    val level: Int, // 0 - 100
    val proficiency: String, // Beginner, Intermediate, Advanced, Expert
    val verified: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    val proofUrl: String,
    val extractedSkillsJson: String, // Comma-separated or JSON list of extracted skills
    val visibility: String, // "all", "user", "groups"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "discussions")
data class DiscussionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val tagsJson: String, // e.g. "AI,IoT"
    val createdAt: Long,
    val upvotesCount: Int,
    val userHasUpvoted: Boolean,
    val pollQuestion: String,
    val pollOptionsJson: String, // JSON format of option strings and vote counts
    val userVotedOptionIndex: Int = -1, // -1 means no vote cast
    val visibilityPercentage: Int, // Calculated engagement percentage by backend
    val classId: String,
    val visibility: String = "class"
)

@Entity(tableName = "peer_students")
data class PeerStudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val classId: String,
    val className: String,
    val avatarUrl: String,
    val bio: String,
    val skillsJson: String, // Comma separated list of skills
    val matchScore: Int = 0
)

@Entity(tableName = "class_info")
data class ClassInfoEntity(
    @PrimaryKey val id: String,
    val classCode: String,
    val name: String,
    val department: String,
    val institution: String,
    val totalMembers: Int
)
