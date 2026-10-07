package com.example.data.remote

import com.example.data.remote.model.AchievementRequest
import com.example.data.remote.model.AchievementResponse
import com.example.data.remote.model.AuthResponse
import com.example.data.remote.model.ClassResponse
import com.example.data.remote.model.CreateDiscussionRequest
import com.example.data.remote.model.DiscussionDto
import com.example.data.remote.model.JoinClassRequest
import com.example.data.remote.model.LoginRequest
import com.example.data.remote.model.PeerStudentDto
import com.example.data.remote.model.RegisterRequest
import com.example.data.remote.model.SkillExtractRequest
import com.example.data.remote.model.SkillExtractResponse
import com.example.data.remote.model.UpvoteResponse
import com.example.data.remote.model.UserProfileDto
import com.example.data.remote.model.VoteRequest
import com.example.data.remote.model.VoteResponse
import com.example.data.remote.model.MetricsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SkillDiscoveryApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<UserProfileDto>

    @GET("users/me/metrics")
    suspend fun getMetrics(
        @Header("Authorization") token: String
    ): Response<MetricsResponse>

    @GET("users/{id}")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
        @Path("id") userId: String
    ): Response<UserProfileDto>

    @GET("classes/{id}")
    suspend fun getClassInfo(
        @Header("Authorization") token: String,
        @Path("id") classId: String
    ): Response<ClassResponse>

    @POST("classes/join")
    suspend fun joinClass(
        @Header("Authorization") token: String,
        @Body request: JoinClassRequest
    ): Response<ClassResponse>

    @GET("achievements")
    suspend fun getAchievements(
        @Header("Authorization") token: String,
        @Query("student_id") studentId: String? = null
    ): Response<List<AchievementResponse>>

    @POST("achievements")
    suspend fun createAchievement(
        @Header("Authorization") token: String,
        @Body request: AchievementRequest
    ): Response<AchievementResponse>

    @POST("skills/extract")
    suspend fun extractSkills(
        @Header("Authorization") token: String,
        @Body request: SkillExtractRequest
    ): Response<SkillExtractResponse>

    @GET("explore/students")
    suspend fun searchStudents(
        @Header("Authorization") token: String,
        @Query("query") query: String? = null,
        @Query("skill") skill: String? = null,
        @Query("class_id") classId: String? = null
    ): Response<List<PeerStudentDto>>

    @GET("discussions")
    suspend fun getDiscussions(
        @Header("Authorization") token: String,
        @Query("tag") tag: String? = null,
        @Query("time") time: String? = null,
        @Query("class_id") classId: String? = null
    ): Response<List<DiscussionDto>>

    @POST("discussions")
    suspend fun createDiscussion(
        @Header("Authorization") token: String,
        @Body request: CreateDiscussionRequest
    ): Response<DiscussionDto>

    @POST("discussions/{id}/upvote")
    suspend fun upvoteDiscussion(
        @Header("Authorization") token: String,
        @Path("id") discussionId: String
    ): Response<UpvoteResponse>

    @POST("discussions/{id}/vote")
    suspend fun voteDiscussion(
        @Header("Authorization") token: String,
        @Path("id") discussionId: String,
        @Body request: VoteRequest
    ): Response<VoteResponse>

    @DELETE("discussions/{id}/vote")
    suspend fun clearDiscussionVote(
        @Header("Authorization") token: String,
        @Path("id") discussionId: String
    ): Response<VoteResponse>
}
