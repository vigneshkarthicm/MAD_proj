package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "class_code") val classCode: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "token") val token: String,
    @Json(name = "user") val user: UserProfileDto
)

@JsonClass(generateAdapter = true)
data class MetricsResponse(
    @Json(name = "skills") val skills: List<SkillDto> = emptyList(),
    @Json(name = "skill_graph") val skillGraph: SkillGraphDto = SkillGraphDto(),
    @Json(name = "weekly_consistency") val weeklyConsistency: Int = 0,
    @Json(name = "weekly_activity") val weeklyActivity: List<ActivityPointDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SkillGraphDto(
    @Json(name = "nodes") val nodes: List<SkillDto> = emptyList(),
    @Json(name = "edges") val edges: List<SkillGraphEdgeDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SkillGraphEdgeDto(
    @Json(name = "source") val source: String,
    @Json(name = "target") val target: String,
    @Json(name = "weight") val weight: Int = 1
)

@JsonClass(generateAdapter = true)
data class ActivityPointDto(
    @Json(name = "date") val date: String,
    @Json(name = "points") val points: Int
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "student_id") val studentId: String = "",
    @Json(name = "class_id") val classId: String,
    @Json(name = "class_name") val className: String,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "contact_number") val contactNumber: String = "9876543210",
    @Json(name = "skills") val skills: List<SkillDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SkillDto(
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: String = "General",
    @Json(name = "level") val level: Int = 50, // 0 - 100
    @Json(name = "proficiency") val proficiency: String = "Intermediate"
)

@JsonClass(generateAdapter = true)
data class AchievementRequest(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "date") val date: String,
    @Json(name = "category") val category: String,
    @Json(name = "proof_url") val proofUrl: String = "",
    @Json(name = "visibility") val visibility: String = "all" // "all", "user", "groups"
)

@JsonClass(generateAdapter = true)
data class AchievementResponse(
    @Json(name = "id") val id: String,
    @Json(name = "student_id") val studentId: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "date") val date: String,
    @Json(name = "category") val category: String,
    @Json(name = "proof_url") val proofUrl: String = "",
    @Json(name = "extracted_skills") val extractedSkills: List<SkillDto> = emptyList(),
    @Json(name = "visibility") val visibility: String = "all",
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class SkillExtractRequest(
    @Json(name = "achievement_text") val achievementText: String
)

@JsonClass(generateAdapter = true)
data class SkillExtractResponse(
    @Json(name = "skills") val skills: List<SkillDto>
)

@JsonClass(generateAdapter = true)
data class PeerStudentDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "class_id") val classId: String,
    @Json(name = "class_name") val className: String,
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "bio") val bio: String = "",
    @Json(name = "skills") val skills: List<String> = emptyList(),
    @Json(name = "match_score") val matchScore: Int = 85
)

@JsonClass(generateAdapter = true)
data class PollOptionDto(
    @Json(name = "text") val text: String,
    @Json(name = "votes") val votes: Int = 0
)

@JsonClass(generateAdapter = true)
data class CreateDiscussionRequest(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "tags") val tags: List<String>,
    @Json(name = "poll_question") val pollQuestion: String = "",
    @Json(name = "poll_options") val pollOptions: List<String> = emptyList(),
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "visibility") val visibility: String = "class"
)

@JsonClass(generateAdapter = true)
data class DiscussionDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "author_id") val authorId: String,
    @Json(name = "author_name") val authorName: String,
    @Json(name = "author_avatar") val authorAvatar: String = "",
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "upvotes_count") val upvotesCount: Int = 0,
    @Json(name = "user_has_upvoted") val userHasUpvoted: Boolean = false,
    @Json(name = "poll_question") val pollQuestion: String = "",
    @Json(name = "poll_options") val pollOptions: List<PollOptionDto> = emptyList(),
    @Json(name = "user_voted_option_index") val userVotedOptionIndex: Int = -1,
    @Json(name = "visibility_percentage") val visibilityPercentage: Int = 65,
    @Json(name = "class_id") val classId: String = "",
    @Json(name = "visibility") val visibility: String = "class"
)

@JsonClass(generateAdapter = true)
data class UpvoteResponse(
    @Json(name = "discussion_id") val discussionId: String,
    @Json(name = "upvotes_count") val upvotesCount: Int,
    @Json(name = "user_has_upvoted") val userHasUpvoted: Boolean
)

@JsonClass(generateAdapter = true)
data class VoteRequest(
    @Json(name = "option_index") val optionIndex: Int
)

@JsonClass(generateAdapter = true)
data class VoteResponse(
    @Json(name = "discussion_id") val discussionId: String,
    @Json(name = "poll_options") val pollOptions: List<PollOptionDto>,
    @Json(name = "user_voted_option_index") val userVotedOptionIndex: Int,
    @Json(name = "visibility_percentage") val visibilityPercentage: Int
)

@JsonClass(generateAdapter = true)
data class JoinClassRequest(
    @Json(name = "class_code") val classCode: String
)

@JsonClass(generateAdapter = true)
data class ClassResponse(
    @Json(name = "id") val id: String,
    @Json(name = "class_code") val classCode: String,
    @Json(name = "name") val name: String,
    @Json(name = "department") val department: String,
    @Json(name = "institution") val institution: String,
    @Json(name = "total_members") val totalMembers: Int
)

@JsonClass(generateAdapter = true)
data class ApiErrorResponse(
    @Json(name = "error") val error: ErrorDetail?
)

@JsonClass(generateAdapter = true)
data class ErrorDetail(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String
)
