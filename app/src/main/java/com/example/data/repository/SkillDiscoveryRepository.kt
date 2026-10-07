package com.example.data.repository

import com.example.data.local.SkillDiscoveryDatabase
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ClassInfoEntity
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.PeerStudentEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.SkillDiscoveryApiService
import com.example.data.remote.model.AchievementRequest
import com.example.data.remote.model.CreateDiscussionRequest
import com.example.data.remote.model.JoinClassRequest
import com.example.data.remote.model.LoginRequest
import com.example.data.remote.model.RegisterRequest
import com.example.data.remote.model.SkillDto
import com.example.data.remote.model.SkillExtractRequest
import com.example.data.remote.model.VoteRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class OfflineActionException(message: String = "You're offline. This action requires an Internet connection.") : Exception(message)

class SkillDiscoveryRepository(
    private val database: SkillDiscoveryDatabase,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private var apiService: SkillDiscoveryApiService = ApiClient.createService(sessionManager)

    fun refreshApiService() {
        apiService = ApiClient.createService(sessionManager)
    }

    suspend fun login(email: String, password: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val response = apiService.login(LoginRequest(email.trim(), password))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                sessionManager.setSession(auth.token, auth.user.id, auth.user.classId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Login failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val studentDao = database.studentDao()
    private val achievementDao = database.achievementDao()
    private val discussionDao = database.discussionDao()
    private val exploreDao = database.exploreDao()

    // Reactive streams from local Room cache
    fun getProfileStream(studentId: String): Flow<StudentProfileEntity?> = studentDao.getProfile(studentId)
    fun getSkillsStream(studentId: String): Flow<List<SkillEntity>> = studentDao.getSkills(studentId)
    fun getAchievementsStream(studentId: String): Flow<List<AchievementEntity>> = achievementDao.getAchievements(studentId)
    fun getDiscussionsStream(): Flow<List<DiscussionEntity>> = discussionDao.getAllDiscussions()
    fun getPeersStream(): Flow<List<PeerStudentEntity>> = exploreDao.getAllPeers()
    fun getClassInfoStream(classId: String): Flow<ClassInfoEntity?> = studentDao.getClassInfo(classId)

    suspend fun initializeSeedDataIfEmpty() = withContext(ioDispatcher) {
        val currentUserId = sessionManager.currentUserId.value
        val existingProfile = studentDao.getProfile(currentUserId).firstOrNull()
        if (existingProfile == null) {
            // Seed initial student profile
            val initialProfile = StudentProfileEntity(
                id = currentUserId,
                name = "Alex Rivera",
                email = "alex.rivera@student.edu",
                studentId = "STU-2026-042",
                classId = "cls_cs2026",
                className = "CS & Artificial Intelligence Honours",
                avatarUrl = "",
                bio = "Computer Science undergraduate specializing in Embedded AI and Distributed Systems. Working on edge ML inference and collaborative study groups.",
                contactNumber = "9876543210",
                lastUpdated = System.currentTimeMillis()
            )
            studentDao.insertProfile(initialProfile)

            // Seed initial skills
            val initialSkills = listOf(
                SkillEntity("sk_1", currentUserId, "Python", "Programming", 92, "Expert"),
                SkillEntity("sk_2", currentUserId, "Machine Learning", "AI & Data Science", 85, "Advanced"),
                SkillEntity("sk_3", currentUserId, "ESP32 & IoT", "Hardware & Embedded", 88, "Advanced"),
                SkillEntity("sk_4", currentUserId, "Embedded C++", "Programming", 78, "Intermediate"),
                SkillEntity("sk_5", currentUserId, "Jetpack Compose", "Mobile Development", 80, "Advanced"),
                SkillEntity("sk_6", currentUserId, "FastAPI & REST", "Backend & Cloud", 84, "Advanced")
            )
            studentDao.insertSkills(initialSkills)

            // Seed class info
            val initialClass = ClassInfoEntity(
                id = "cls_cs2026",
                classCode = "CS-2026-AI",
                name = "Computer Science & AI Honours",
                department = "School of Computing",
                institution = "Institute of Technology",
                totalMembers = 42
            )
            studentDao.insertClassInfo(initialClass)

            // Seed initial achievements with extracted skills
            val initialAchievements = listOf(
                AchievementEntity(
                    id = "ach_001",
                    studentId = currentUserId,
                    title = "ESP32 Autonomous Environmental Sensor Node",
                    description = "Designed and deployed an edge IoT sensing device measuring humidity, temperature, and CO2, streaming encrypted telemetry over MQTT.",
                    date = "2026-08-15",
                    category = "Embedded & IoT",
                    proofUrl = "https://github.com/vigneshkarthicm/SkillDiscovery/esp32-node",
                    extractedSkillsJson = "ESP32, IoT Protocols (MQTT), Hardware Development, Embedded C++",
                    visibility = "all",
                    timestamp = System.currentTimeMillis() - 86400000L * 14
                ),
                AchievementEntity(
                    id = "ach_002",
                    studentId = currentUserId,
                    title = "Distributed Skill Matching Engine",
                    description = "Built a vector-based student skill discovery algorithm in Python using cosine similarity to recommend study teammates and project collaborators.",
                    date = "2026-09-02",
                    category = "AI & Machine Learning",
                    proofUrl = "https://github.com/vigneshkarthicm/SkillDiscovery",
                    extractedSkillsJson = "Python, Machine Learning, Vector Embeddings, Cosine Similarity",
                    visibility = "all",
                    timestamp = System.currentTimeMillis() - 86400000L * 5
                ),
                AchievementEntity(
                    id = "ach_003",
                    studentId = currentUserId,
                    title = "High-Throughput FastAPI Microservice",
                    description = "Implemented asynchronous REST endpoints supporting real-time collaborative discussion voting and skill taxonomy persistence.",
                    date = "2026-09-10",
                    category = "Backend Systems",
                    proofUrl = "https://gitlab.com/projects/skill-service-api",
                    extractedSkillsJson = "FastAPI, REST API Design, PostgreSQL, Asynchronous I/O",
                    visibility = "groups",
                    timestamp = System.currentTimeMillis() - 86400000L * 2
                )
            )
            achievementDao.insertAchievements(initialAchievements)

            // Seed initial discussions
            val initialDiscussions = listOf(
                DiscussionEntity(
                    id = "disc_101",
                    title = "Edge AI vs Cloud Inference for Capstone IoT Sensors",
                    description = "We are designing an on-device anomaly detection system. Should we deploy quantized TFLite on ESP32/Jetson Nano, or transmit batch telemetry to FastAPI?",
                    authorId = "std_user_003",
                    authorName = "Maya Chen",
                    authorAvatar = "",
                    tagsJson = "AI,IoT",
                    createdAt = System.currentTimeMillis() - 3600000L * 4,
                    upvotesCount = 18,
                    userHasUpvoted = true,
                    pollQuestion = "Which architecture best balances latency and power consumption?",
                    pollOptionsJson = JSONArray().apply {
                        put(JSONObject().apply { put("text", "Quantized TFLite on Edge (Jetson Nano)"); put("votes", 14) })
                        put(JSONObject().apply { put("text", "ESP32 Telemetry + Central FastAPI"); put("votes", 8) })
                        put(JSONObject().apply { put("text", "Hybrid: Local rule filters + Cloud ML"); put("votes", 11) })
                    }.toString(),
                    userVotedOptionIndex = 0,
                    visibilityPercentage = 82,
                    classId = "CS-2026-AI"
                ),
                DiscussionEntity(
                    id = "disc_102",
                    title = "Inter-Class Hackathon Team Formation & Skill Stacks",
                    description = "Preparing for the National Collegiate Tech Challenge. Let's align teams to ensure every squad pairs a backend engineer with mobile and ML developers.",
                    authorId = "std_user_004",
                    authorName = "Liam O'Connor",
                    authorAvatar = "",
                    tagsJson = "Events,Mobile,Web",
                    createdAt = System.currentTimeMillis() - 86400000L * 1,
                    upvotesCount = 24,
                    userHasUpvoted = false,
                    pollQuestion = "What is your primary contribution preference for the hackathon?",
                    pollOptionsJson = JSONArray().apply {
                        put(JSONObject().apply { put("text", "Mobile & UI (Compose)"); put("votes", 12) })
                        put(JSONObject().apply { put("text", "Backend & APIs (FastAPI)"); put("votes", 15) })
                        put(JSONObject().apply { put("text", "AI/ML Models & Analytics"); put("votes", 18) })
                        put(JSONObject().apply { put("text", "Hardware / Embedded Prototyping"); put("votes", 7) })
                    }.toString(),
                    userVotedOptionIndex = -1,
                    visibilityPercentage = 91,
                    classId = "CS-2026-AI"
                ),
                DiscussionEntity(
                    id = "disc_103",
                    title = "Standardizing Dataset Formatting for Peer Skill Matching",
                    description = "Proposing an open JSON schema for skill taxonomy classification across department labs. We need consensus on category labels and proficiency level scoring.",
                    authorId = "std_user_005",
                    authorName = "Amina Patel",
                    authorAvatar = "",
                    tagsJson = "AI,Web",
                    createdAt = System.currentTimeMillis() - 86400000L * 3,
                    upvotesCount = 11,
                    userHasUpvoted = false,
                    pollQuestion = "Adopt O*NET standard taxonomy or customized departmental skills?",
                    pollOptionsJson = JSONArray().apply {
                        put(JSONObject().apply { put("text", "O*NET Standard Competency Taxonomy"); put("votes", 9) })
                        put(JSONObject().apply { put("text", "Custom Department Hierarchy"); put("votes", 13) })
                    }.toString(),
                    userVotedOptionIndex = -1,
                    visibilityPercentage = 64,
                    classId = "CS-2026-AI"
                )
            )
            discussionDao.insertDiscussions(initialDiscussions)

            // Seed initial peers
            val initialPeers = listOf(
                PeerStudentEntity("peer_101", "Maya Chen", "maya.chen@student.edu", "cls_cs2026", "CS & AI Honours", "", "Passionate about Computer Vision & Edge AI.", "AI, PyTorch, Computer Vision, Python", 95),
                PeerStudentEntity("peer_102", "Liam O'Connor", "liam.oconnor@student.edu", "cls_cs2026", "CS & AI Honours", "", "Embedded systems engineer focusing on IoT sensor networks.", "IoT, ESP32, Embedded Systems, C++", 91),
                PeerStudentEntity("peer_103", "Amina Patel", "amina.patel@student.edu", "cls_cs2026", "CS & AI Honours", "", "Android enthusiast crafting accessible Jetpack Compose apps.", "Jetpack Compose, Kotlin, Android, Mobile", 88),
                PeerStudentEntity("peer_104", "Carlos Ramirez", "carlos.ramirez@student.edu", "cls_cs2026", "CS & AI Honours", "", "Cloud & backend engineer specializing in high-throughput APIs.", "FastAPI, PostgreSQL, Docker, Cloud", 84),
                PeerStudentEntity("peer_105", "Elena Rostova", "elena.rostova@student.edu", "cls_cs2026", "CS & AI Honours", "", "Data scientist modeling statistical distributions.", "Data Science, Machine Learning, Statistics, R", 82),
                PeerStudentEntity("peer_106", "Marcus Vance", "marcus.vance@student.edu", "cls_cs2026", "CS & AI Honours", "", "Security researcher exploring network vulnerabilities.", "Cybersecurity, Network Analysis, Rust, Linux", 79)
            )
            exploreDao.insertPeers(initialPeers)

            sessionManager.updateLastSyncTime()
        }
    }

    // Refresh profile from network and update Room cache
    suspend fun syncProfile(): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("Offline: Loading cached profile"))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val userDto = response.body()!!
                val profileEntity = StudentProfileEntity(
                    id = userDto.id,
                    name = userDto.name,
                    email = userDto.email,
                    studentId = userDto.studentId,
                    classId = userDto.classId,
                    className = userDto.className,
                    avatarUrl = userDto.avatarUrl,
                    bio = userDto.bio,
                    contactNumber = userDto.contactNumber.ifBlank { "9876543210" },
                    lastUpdated = System.currentTimeMillis()
                )
                studentDao.insertProfile(profileEntity)

                if (userDto.skills.isNotEmpty()) {
                    studentDao.clearSkills(userDto.id)
                    val skillEntities = userDto.skills.mapIndexed { index, s ->
                        SkillEntity(
                            id = "sk_${userDto.id}_$index",
                            studentId = userDto.id,
                            name = s.name,
                            category = s.category,
                            level = s.level,
                            proficiency = s.proficiency,
                            verified = true
                        )
                    }
                    studentDao.insertSkills(skillEntities)
                }

                sessionManager.updateLastSyncTime()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncMetrics(): Result<com.example.data.remote.model.MetricsResponse> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("Offline: Loading cached metrics"))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.getMetrics("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val metrics = response.body()!!
                studentDao.clearSkills(sessionManager.currentUserId.value)
                studentDao.insertSkills(metrics.skills.mapIndexed { index, skill ->
                    SkillEntity(
                        id = "sk_${sessionManager.currentUserId.value}_$index",
                        studentId = sessionManager.currentUserId.value,
                        name = skill.name,
                        category = skill.category,
                        level = skill.level,
                        proficiency = skill.proficiency,
                        verified = true
                    )
                })
                sessionManager.updateLastSyncTime()
                Result.success(metrics)
            } else {
                Result.failure(Exception("Failed to fetch metrics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync achievements from backend and update Room cache
    suspend fun syncAchievements(): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("Offline: Loading cached achievements"))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.getAchievements("Bearer $token", sessionManager.currentUserId.value)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                achievementDao.clearAchievements(sessionManager.currentUserId.value)
                val entities = list.map { ach ->
                    val skillsStr = ach.extractedSkills.joinToString(", ") { it.name }
                    AchievementEntity(
                        id = ach.id,
                        studentId = ach.studentId,
                        title = ach.title,
                        description = ach.description,
                        date = ach.date,
                        category = ach.category,
                        proofUrl = ach.proofUrl,
                        extractedSkillsJson = skillsStr,
                        visibility = ach.visibility,
                        timestamp = ach.createdAt
                    )
                }
                achievementDao.insertAchievements(entities)
                sessionManager.updateLastSyncTime()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch achievements: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Extract skills via backend AI service
    suspend fun extractSkills(achievementText: String): Result<List<SkillDto>> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("Skill extraction requires an Internet connection."))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.extractSkills("Bearer $token", SkillExtractRequest(achievementText))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.skills)
            } else {
                Result.failure(Exception("AI Extraction failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Create achievement (disallowed offline per Section 29)
    suspend fun createAchievement(
        title: String,
        description: String,
        date: String,
        category: String,
        proofUrl: String,
        visibility: String
    ): Result<AchievementEntity> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("You're offline. This action requires an Internet connection."))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val request = AchievementRequest(
                title = title,
                description = description,
                date = date,
                category = category,
                proofUrl = proofUrl,
                visibility = visibility
            )
            val response = apiService.createAchievement("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val ach = response.body()!!
                val skillsStr = ach.extractedSkills.joinToString(", ") { it.name }
                val entity = AchievementEntity(
                    id = ach.id,
                    studentId = ach.studentId,
                    title = ach.title,
                    description = ach.description,
                    date = ach.date,
                    category = ach.category,
                    proofUrl = ach.proofUrl,
                    extractedSkillsJson = skillsStr,
                    visibility = ach.visibility,
                    timestamp = ach.createdAt
                )
                // Cache to Room
                achievementDao.insertAchievement(entity)

                // Also update local skills in Room from extracted skills
                if (ach.extractedSkills.isNotEmpty()) {
                    val newSkills = ach.extractedSkills.mapIndexed { idx, s ->
                        SkillEntity(
                            id = "sk_${entity.id}_$idx",
                            studentId = entity.studentId,
                            name = s.name,
                            category = s.category,
                            level = s.level,
                            proficiency = s.proficiency,
                            verified = true
                        )
                    }
                    studentDao.insertSkills(newSkills)
                }

                sessionManager.updateLastSyncTime()
                Result.success(entity)
            } else {
                Result.failure(Exception("Failed to create achievement: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync discussions
    suspend fun syncDiscussions(tag: String? = null): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("Offline: Loading cached discussions"))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.getDiscussions("Bearer $token", tag = tag)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                val entities = list.map { d ->
                    val optionsJson = JSONArray().apply {
                        d.pollOptions.forEach { opt ->
                            put(JSONObject().apply {
                                put("text", opt.text)
                                put("votes", opt.votes)
                            })
                        }
                    }.toString()

                    DiscussionEntity(
                        id = d.id,
                        title = d.title,
                        description = d.description,
                        authorId = d.authorId,
                        authorName = d.authorName,
                        authorAvatar = d.authorAvatar,
                        tagsJson = d.tags.joinToString(","),
                        createdAt = d.createdAt,
                        upvotesCount = d.upvotesCount,
                        userHasUpvoted = d.userHasUpvoted,
                        pollQuestion = d.pollQuestion,
                        pollOptionsJson = optionsJson,
                        userVotedOptionIndex = d.userVotedOptionIndex,
                        visibilityPercentage = d.visibilityPercentage,
                        classId = d.classId,
                        visibility = d.visibility
                    )
                }
                discussionDao.insertDiscussions(entities)
                sessionManager.updateLastSyncTime()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch discussions: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upvote discussion with instant optimistic UI update
    suspend fun upvoteDiscussion(discussionId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("You're offline. This action requires an Internet connection."))
        }

        val existingEntity = discussionDao.getDiscussionById(discussionId)
        val origUpvoted = existingEntity?.userHasUpvoted ?: false
        val origCount = existingEntity?.upvotesCount ?: 0

        val optUpvoted = !origUpvoted
        val optCount = if (optUpvoted) origCount + 1 else (origCount - 1).coerceAtLeast(0)

        // Optimistic UI update in Room
        if (existingEntity != null) {
            discussionDao.updateUpvote(discussionId, optCount, optUpvoted)
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.upvoteDiscussion("Bearer $token", discussionId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                discussionDao.updateUpvote(body.discussionId, body.upvotesCount, body.userHasUpvoted)
                Result.success(Unit)
            } else {
                if (existingEntity != null) {
                    discussionDao.updateUpvote(discussionId, origCount, origUpvoted)
                }
                Result.failure(Exception("Failed to submit upvote: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (existingEntity != null) {
                discussionDao.updateUpvote(discussionId, origCount, origUpvoted)
            }
            Result.failure(e)
        }
    }

    // Vote in collaborative discussion poll with instant optimistic UI update
    suspend fun voteDiscussion(discussionId: String, optionIndex: Int): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("You're offline. This action requires an Internet connection."))
        }

        val existingEntity = discussionDao.getDiscussionById(discussionId)

        // Optimistically update poll options in Room
        if (existingEntity != null) {
            val origJson = JSONArray(existingEntity.pollOptionsJson)
            val updatedJson = JSONArray()
            val prevIndex = existingEntity.userVotedOptionIndex
            for (i in 0 until origJson.length()) {
                val obj = origJson.getJSONObject(i)
                var votes = obj.optInt("votes", 0)
                if (prevIndex == i && i != optionIndex) votes = (votes - 1).coerceAtLeast(0)
                if (prevIndex != optionIndex && i == optionIndex) votes += 1
                updatedJson.put(JSONObject().apply {
                    put("text", obj.optString("text"))
                    put("votes", votes)
                })
            }
            discussionDao.updatePollVote(discussionId, updatedJson.toString(), optionIndex, existingEntity.visibilityPercentage)
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.voteDiscussion("Bearer $token", discussionId, VoteRequest(optionIndex))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val optionsJson = JSONArray().apply {
                    body.pollOptions.forEach { opt ->
                        put(JSONObject().apply {
                            put("text", opt.text)
                            put("votes", opt.votes)
                        })
                    }
                }.toString()

                discussionDao.updatePollVote(
                    body.discussionId,
                    optionsJson,
                    body.userVotedOptionIndex,
                    body.visibilityPercentage
                )
                Result.success(Unit)
            } else {
                if (existingEntity != null) {
                    discussionDao.updatePollVote(discussionId, existingEntity.pollOptionsJson, existingEntity.userVotedOptionIndex, existingEntity.visibilityPercentage)
                }
                Result.failure(Exception("Failed to submit vote: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (existingEntity != null) {
                discussionDao.updatePollVote(discussionId, existingEntity.pollOptionsJson, existingEntity.userVotedOptionIndex, existingEntity.visibilityPercentage)
            }
            Result.failure(e)
        }
    }

    suspend fun clearDiscussionVote(discussionId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("You're offline. This action requires an Internet connection."))
        }

        val existingEntity = discussionDao.getDiscussionById(discussionId)
        if (existingEntity != null && existingEntity.userVotedOptionIndex >= 0) {
            val origJson = JSONArray(existingEntity.pollOptionsJson)
            val updatedJson = JSONArray()
            val prevIndex = existingEntity.userVotedOptionIndex
            for (i in 0 until origJson.length()) {
                val obj = origJson.getJSONObject(i)
                var votes = obj.optInt("votes", 0)
                if (prevIndex == i) votes = (votes - 1).coerceAtLeast(0)
                updatedJson.put(JSONObject().apply {
                    put("text", obj.optString("text"))
                    put("votes", votes)
                })
            }
            discussionDao.updatePollVote(discussionId, updatedJson.toString(), -1, existingEntity.visibilityPercentage)
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.clearDiscussionVote("Bearer $token", discussionId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val optionsJson = JSONArray().apply {
                    body.pollOptions.forEach { opt ->
                        put(JSONObject().apply {
                            put("text", opt.text)
                            put("votes", opt.votes)
                        })
                    }
                }.toString()
                discussionDao.updatePollVote(
                    body.discussionId,
                    optionsJson,
                    body.userVotedOptionIndex,
                    body.visibilityPercentage
                )
                Result.success(Unit)
            } else {
                if (existingEntity != null) {
                    discussionDao.updatePollVote(discussionId, existingEntity.pollOptionsJson, existingEntity.userVotedOptionIndex, existingEntity.visibilityPercentage)
                }
                Result.failure(Exception("Failed to clear vote: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (existingEntity != null) {
                discussionDao.updatePollVote(discussionId, existingEntity.pollOptionsJson, existingEntity.userVotedOptionIndex, existingEntity.visibilityPercentage)
            }
            Result.failure(e)
        }
    }

    // Create discussion (disallowed offline)
    suspend fun createDiscussion(
        title: String,
        description: String,
        tags: List<String>,
        pollQuestion: String,
        pollOptions: List<String>
    ): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("You're offline. This action requires an Internet connection."))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val request = CreateDiscussionRequest(
                title = title,
                description = description,
                tags = tags,
                pollQuestion = pollQuestion,
                pollOptions = pollOptions,
                classId = ""
            )
            val response = apiService.createDiscussion("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                val d = response.body()!!
                val optionsJson = JSONArray().apply {
                    d.pollOptions.forEach { opt ->
                        put(JSONObject().apply {
                            put("text", opt.text)
                            put("votes", opt.votes)
                        })
                    }
                }.toString()

                val entity = DiscussionEntity(
                    id = d.id,
                    title = d.title,
                    description = d.description,
                    authorId = d.authorId,
                    authorName = d.authorName,
                    authorAvatar = d.authorAvatar,
                    tagsJson = d.tags.joinToString(","),
                    createdAt = d.createdAt,
                    upvotesCount = d.upvotesCount,
                    userHasUpvoted = d.userHasUpvoted,
                    pollQuestion = d.pollQuestion,
                    pollOptionsJson = optionsJson,
                    userVotedOptionIndex = d.userVotedOptionIndex,
                    visibilityPercentage = d.visibilityPercentage,
                    classId = d.classId,
                    visibility = d.visibility
                )
                discussionDao.insertDiscussion(entity)
                sessionManager.updateLastSyncTime()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to create discussion: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search students for skill discovery
    suspend fun searchPeers(query: String?): Result<Unit> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("Offline: Loading cached peers"))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.searchStudents("Bearer $token", query = query)
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!
                val entities = list.map { peer ->
                    PeerStudentEntity(
                        id = peer.id,
                        name = peer.name,
                        email = peer.email,
                        classId = peer.classId,
                        className = peer.className,
                        avatarUrl = peer.avatarUrl,
                        bio = peer.bio,
                        skillsJson = peer.skills.joinToString(", "),
                        matchScore = peer.matchScore
                    )
                }
                exploreDao.insertPeers(entities)
                sessionManager.updateLastSyncTime()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to search peers: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Join class with code
    suspend fun joinClass(classCode: String): Result<ClassInfoEntity> = withContext(ioDispatcher) {
        if (!networkMonitor.isCurrentlyOnline() && !sessionManager.isSandboxBackend.value) {
            return@withContext Result.failure(OfflineActionException("You're offline. This action requires an Internet connection."))
        }

        try {
            val token = sessionManager.authToken.value ?: ""
            val response = apiService.joinClass("Bearer $token", JoinClassRequest(classCode))
            if (response.isSuccessful && response.body() != null) {
                val cls = response.body()!!
                val entity = ClassInfoEntity(
                    id = cls.id,
                    classCode = cls.classCode,
                    name = cls.name,
                    department = cls.department,
                    institution = cls.institution,
                    totalMembers = cls.totalMembers
                )
                studentDao.insertClassInfo(entity)
                sessionManager.setClassCode(classCode)
                Result.success(entity)
            } else {
                Result.failure(Exception("Failed to join class: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Clear local cache for testing
    suspend fun clearCache() = withContext(ioDispatcher) {
        database.clearAllTables()
        initializeSeedDataIfEmpty()
    }

    suspend fun clearSessionCache() = withContext(ioDispatcher) {
        database.clearAllTables()
    }
}
