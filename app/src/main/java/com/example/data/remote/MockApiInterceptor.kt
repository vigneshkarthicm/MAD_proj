package com.example.data.remote

import com.example.data.repository.SessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class MockApiInterceptor(private val sessionManager: SessionManager) : okhttp3.Interceptor {

    private val sandboxUpvotes = mutableMapOf<String, Boolean>()
    private val sandboxDiscussions = mutableListOf<JSONObject>()

    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
        val request = chain.request()

        // If sandbox backend is disabled, attempt the real network request
        if (!sessionManager.isSandboxBackend.value) {
            return try {
                chain.proceed(request)
            } catch (e: Exception) {
                // If real network fails, rethrow so the repository handles offline state
                throw e
            }
        }

        // Otherwise handle requests locally in Sandbox mode according to API contract
        val path = request.url.encodedPath
        val method = request.method

        return when {
            path.endsWith("auth/login") && method == "POST" -> {
                val mockUser = createMockUserJson("std_user_001", "Alex Rivera", "alex.rivera@student.edu", "CS-2026-AI")
                val responseJson = JSONObject().apply {
                    put("token", "bearer_jwt_token_alex_2026")
                    put("user", mockUser)
                }.toString()
                createSuccessResponse(request, responseJson)
            }

            path.endsWith("auth/register") && method == "POST" -> {
                val mockUser = createMockUserJson("std_user_002", "New Student", "student@university.edu", "CS-2026-AI")
                val responseJson = JSONObject().apply {
                    put("token", "bearer_jwt_token_new_user_2026")
                    put("user", mockUser)
                }.toString()
                createSuccessResponse(request, responseJson)
            }

            path.contains("users/me") || (path.contains("users/") && method == "GET") -> {
                val currentId = sessionManager.currentUserId.value
                val userJson = createMockUserJson(
                    id = currentId,
                    name = when (currentId) {
                        "std_alex" -> "Alex Rivera"
                        "std_elena" -> "Elena Rostova"
                        else -> "Vinay Kumar"
                    },
                    email = when (currentId) {
                        "std_alex" -> "alex.rivera@student.edu"
                        "std_elena" -> "elena.rostova@student.edu"
                        else -> "vinay.k@student.edu"
                    },
                    classCode = sessionManager.currentClassCode.value
                ).toString()
                createSuccessResponse(request, userJson)
            }

            path.contains("classes/") && method == "GET" -> {
                val classJson = JSONObject().apply {
                    put("id", "cls_cs2026")
                    put("class_code", sessionManager.currentClassCode.value)
                    put("name", "Computer Science & AI Honours")
                    put("department", "School of Computing")
                    put("institution", "Institute of Technology")
                    put("total_members", 42)
                }.toString()
                createSuccessResponse(request, classJson)
            }

            path.endsWith("classes/join") && method == "POST" -> {
                val classJson = JSONObject().apply {
                    put("id", "cls_joined")
                    put("class_code", sessionManager.currentClassCode.value)
                    put("name", "Class Community ${sessionManager.currentClassCode.value}")
                    put("department", "Faculty of Engineering")
                    put("institution", "State University")
                    put("total_members", 38)
                }.toString()
                createSuccessResponse(request, classJson)
            }

            path.endsWith("achievements") && method == "GET" -> {
                val list = createMockAchievementsJson()
                createSuccessResponse(request, list.toString())
            }

            path.endsWith("achievements") && method == "POST" -> {
                val bodyStr = request.bodyToString()
                val parsed = if (bodyStr.isNotEmpty()) JSONObject(bodyStr) else JSONObject()
                val title = parsed.optString("title", "New Achievement")
                val desc = parsed.optString("description", "")
                val category = parsed.optString("category", "Projects")
                val visibility = parsed.optString("visibility", "all")
                val proofUrl = parsed.optString("proof_url", "")

                val extractedSkills = extractSkillsFromText(title + " " + desc)

                val responseObj = JSONObject().apply {
                    put("id", "ach_" + UUID.randomUUID().toString().take(8))
                    put("student_id", sessionManager.currentUserId.value)
                    put("title", title)
                    put("description", desc)
                    put("date", "2026-09-19")
                    put("category", category)
                    put("proof_url", proofUrl)
                    put("visibility", visibility)
                    put("created_at", System.currentTimeMillis())
                    val skillsArray = JSONArray()
                    extractedSkills.forEach { skill ->
                        skillsArray.put(JSONObject().apply {
                            put("name", skill.first)
                            put("category", skill.second)
                            put("level", skill.third)
                            put("proficiency", if (skill.third >= 80) "Advanced" else "Intermediate")
                        })
                    }
                    put("extracted_skills", skillsArray)
                }
                createSuccessResponse(request, responseObj.toString())
            }

            path.endsWith("skills/extract") && method == "POST" -> {
                val bodyStr = request.bodyToString()
                val parsed = if (bodyStr.isNotEmpty()) JSONObject(bodyStr) else JSONObject()
                val text = parsed.optString("achievement_text", "")
                val extracted = extractSkillsFromText(text)
                val skillsArray = JSONArray()
                extracted.forEach { skill ->
                    skillsArray.put(JSONObject().apply {
                        put("name", skill.first)
                        put("category", skill.second)
                        put("level", skill.third)
                        put("proficiency", if (skill.third >= 80) "Advanced" else "Intermediate")
                    })
                }
                val responseObj = JSONObject().apply {
                    put("skills", skillsArray)
                }
                createSuccessResponse(request, responseObj.toString())
            }

            path.endsWith("explore/students") && method == "GET" -> {
                val query = request.url.queryParameter("query") ?: request.url.queryParameter("skill") ?: ""
                val peers = createMockPeersJson(query)
                createSuccessResponse(request, peers.toString())
            }

            path.endsWith("discussions") && method == "GET" -> {
                val tag = request.url.queryParameter("tag")
                val mockList = createMockDiscussionsJson(tag)
                val combined = JSONArray()

                sandboxDiscussions.filter { d ->
                    if (tag.isNullOrBlank() || tag == "All") true else {
                        val tagsArr = d.optJSONArray("tags") ?: JSONArray()
                        var match = false
                        for (i in 0 until tagsArr.length()) {
                            if (tagsArr.optString(i).equals(tag, ignoreCase = true)) match = true
                        }
                        match
                    }
                }.forEach { combined.put(it) }

                for (i in 0 until mockList.length()) {
                    combined.put(mockList.getJSONObject(i))
                }

                createSuccessResponse(request, combined.toString())
            }

            path.endsWith("discussions") && method == "POST" -> {
                val bodyStr = request.bodyToString()
                val parsed = if (bodyStr.isNotEmpty()) JSONObject(bodyStr) else JSONObject()
                val title = parsed.optString("title", "Untitled Discussion")
                val desc = parsed.optString("description", "")
                val tags = parsed.optJSONArray("tags") ?: JSONArray()
                val question = parsed.optString("poll_question", "")
                val optionsArray = parsed.optJSONArray("poll_options") ?: JSONArray()
                val visibility = parsed.optString("visibility", "class")

                val pollOptions = JSONArray()
                for (i in 0 until optionsArray.length()) {
                    pollOptions.put(JSONObject().apply {
                        put("text", optionsArray.getString(i))
                        put("votes", 0)
                    })
                }

                val discussion = JSONObject().apply {
                    put("id", "disc_" + UUID.randomUUID().toString().take(8))
                    put("title", title)
                    put("description", desc)
                    put("author_id", sessionManager.currentUserId.value)
                    put("author_name", "Alex Rivera")
                    put("author_avatar", "")
                    put("tags", tags)
                    put("created_at", System.currentTimeMillis())
                    put("upvotes_count", 0)
                    put("user_has_upvoted", false)
                    put("poll_question", question)
                    put("poll_options", pollOptions)
                    put("user_voted_option_index", -1)
                    put("visibility_percentage", 100)
                    put("class_id", sessionManager.currentClassCode.value)
                    put("visibility", visibility)
                }

                sandboxDiscussions.add(0, discussion)
                createSuccessResponse(request, discussion.toString())
            }

            path.contains("/upvote") && method == "POST" -> {
                val discId = path.substringBefore("/upvote").substringAfterLast("/")
                val targetItem = sandboxDiscussions.find { it.optString("id") == discId }
                val hasUpvoted = !(sandboxUpvotes[discId] ?: false)
                sandboxUpvotes[discId] = hasUpvoted

                val newCount = if (targetItem != null) {
                    val current = targetItem.optInt("upvotes_count", 0)
                    val next = if (hasUpvoted) current + 1 else (current - 1).coerceAtLeast(0)
                    targetItem.put("upvotes_count", next)
                    targetItem.put("user_has_upvoted", hasUpvoted)
                    next
                } else {
                    if (hasUpvoted) 19 else 18
                }

                val responseObj = JSONObject().apply {
                    put("discussion_id", discId)
                    put("upvotes_count", newCount)
                    put("user_has_upvoted", hasUpvoted)
                }
                createSuccessResponse(request, responseObj.toString())
            }

            path.contains("/vote") && method == "DELETE" -> {
                val discId = path.substringBefore("/vote").substringAfterLast("/")
                val sandboxItem = sandboxDiscussions.find { it.optString("id") == discId }
                
                val responseObj = JSONObject().apply {
                    put("discussion_id", discId)
                    if (sandboxItem != null) {
                        val prevIndex = sandboxItem.optInt("user_voted_option_index", -1)
                        val opts = sandboxItem.optJSONArray("poll_options") ?: JSONArray()
                        val updatedOptions = JSONArray()
                        for (i in 0 until opts.length()) {
                            val opt = opts.getJSONObject(i)
                            var votes = opt.optInt("votes", 0)
                            if (prevIndex == i) votes = (votes - 1).coerceAtLeast(0)
                            updatedOptions.put(JSONObject().apply {
                                put("text", opt.optString("text"))
                                put("votes", votes)
                            })
                        }
                        sandboxItem.put("poll_options", updatedOptions)
                        sandboxItem.put("user_voted_option_index", -1)
                        put("poll_options", updatedOptions)
                        put("visibility_percentage", sandboxItem.optInt("visibility_percentage", 100))
                    } else {
                        put("poll_options", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Option 1: PyTorch / Jetson Nano"); put("votes", 13) })
                            put(JSONObject().apply { put("text", "Option 2: TensorFlow Lite / ESP32"); put("votes", 8) })
                            put(JSONObject().apply { put("text", "Option 3: ONNX Web Runtime"); put("votes", 11) })
                        })
                        put("visibility_percentage", 82)
                    }
                    put("user_voted_option_index", -1)
                }
                createSuccessResponse(request, responseObj.toString())
            }

            path.contains("/vote") && method == "POST" -> {
                val discId = path.substringBefore("/vote").substringAfterLast("/")
                val bodyStr = request.bodyToString()
                val parsed = if (bodyStr.isNotEmpty()) JSONObject(bodyStr) else JSONObject()
                val votedIndex = parsed.optInt("option_index", 0)
                val sandboxItem = sandboxDiscussions.find { it.optString("id") == discId }

                val responseObj = JSONObject().apply {
                    put("discussion_id", discId)
                    if (sandboxItem != null) {
                        val prevIndex = sandboxItem.optInt("user_voted_option_index", -1)
                        val opts = sandboxItem.optJSONArray("poll_options") ?: JSONArray()
                        val updatedOptions = JSONArray()
                        for (i in 0 until opts.length()) {
                            val opt = opts.getJSONObject(i)
                            var votes = opt.optInt("votes", 0)
                            if (prevIndex == i && i != votedIndex) votes = (votes - 1).coerceAtLeast(0)
                            if (prevIndex != votedIndex && i == votedIndex) votes += 1
                            updatedOptions.put(JSONObject().apply {
                                put("text", opt.optString("text"))
                                put("votes", votes)
                            })
                        }
                        sandboxItem.put("poll_options", updatedOptions)
                        sandboxItem.put("user_voted_option_index", votedIndex)
                        put("poll_options", updatedOptions)
                        put("visibility_percentage", sandboxItem.optInt("visibility_percentage", 100))
                    } else {
                        val opts = JSONArray().apply {
                            put(JSONObject().apply { put("text", "Option 1: PyTorch / Jetson Nano"); put("votes", if (votedIndex == 0) 15 else 14) })
                            put(JSONObject().apply { put("text", "Option 2: TensorFlow Lite / ESP32"); put("votes", if (votedIndex == 1) 9 else 8) })
                            put(JSONObject().apply { put("text", "Option 3: ONNX Web Runtime"); put("votes", if (votedIndex == 2) 5 else 4) })
                        }
                        put("poll_options", opts)
                        put("visibility_percentage", 82)
                    }
                    put("user_voted_option_index", votedIndex)
                }
                createSuccessResponse(request, responseObj.toString())
            }

            else -> {
                chain.proceed(request)
            }
        }
    }

    private fun createSuccessResponse(request: Request, jsonString: String): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(jsonString.toResponseBody("application/json".toMediaType()))
            .build()
    }

    private fun Request.bodyToString(): String {
        return try {
            val copy = this.newBuilder().build()
            val buffer = okio.Buffer()
            copy.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            ""
        }
    }

    // AI skill extraction from text
    private fun extractSkillsFromText(text: String): List<Triple<String, String, Int>> {
        val lower = text.lowercase()
        val skills = mutableListOf<Triple<String, String, Int>>()

        if (lower.contains("esp32") || lower.contains("iot") || lower.contains("arduino") || lower.contains("sensor")) {
            skills.add(Triple("IoT & Sensor Architecture", "Hardware & Embedded", 85))
            skills.add(Triple("ESP32", "Hardware & Embedded", 80))
            skills.add(Triple("Embedded C/C++", "Programming", 75))
        }
        if (lower.contains("python") || lower.contains("machine learning") || lower.contains("ml") || lower.contains("model")) {
            skills.add(Triple("Python", "Programming", 90))
            skills.add(Triple("Machine Learning", "AI & Data Science", 85))
            skills.add(Triple("Scikit-Learn / PyTorch", "AI & Data Science", 78))
        }
        if (lower.contains("android") || lower.contains("kotlin") || lower.contains("compose") || lower.contains("mobile")) {
            skills.add(Triple("Android Jetpack Compose", "Mobile", 88))
            skills.add(Triple("Kotlin Coroutines", "Programming", 82))
        }
        if (lower.contains("web") || lower.contains("react") || lower.contains("fastapi") || lower.contains("api") || lower.contains("rest")) {
            skills.add(Triple("REST API Design", "Backend & Cloud", 84))
            skills.add(Triple("FastAPI", "Backend & Cloud", 80))
        }
        if (lower.contains("database") || lower.contains("sql") || lower.contains("room") || lower.contains("postgres")) {
            skills.add(Triple("Database Optimization", "Data Engineering", 82))
        }

        if (skills.isEmpty()) {
            skills.add(Triple("Problem Solving", "Core Competency", 75))
            skills.add(Triple("Technical Documentation", "Communication", 70))
        }

        return skills
    }

    private fun createMockUserJson(id: String, name: String, email: String, classCode: String): JSONObject {
        val (className, bio, skillsList) = when (id) {
            "std_alex" -> Triple(
                "CS & AI Honours - IV Year",
                "Computer Science undergraduate specializing in Embedded AI and Distributed Systems.",
                listOf(
                    Triple("Python", "Programming", 92),
                    Triple("Machine Learning", "AI & Data Science", 85),
                    Triple("ESP32 & IoT", "Hardware & Embedded", 88),
                    Triple("Embedded C++", "Programming", 78),
                    Triple("Jetpack Compose", "Mobile Development", 80),
                    Triple("FastAPI & REST", "Backend & Cloud", 84)
                )
            )
            "std_elena" -> Triple(
                "Data Science & NLP - II Year",
                "Data scientist and NLP researcher exploring multi-modal generative language models.",
                listOf(
                    Triple("NLP & Transformers", "AI & Data Science", 90),
                    Triple("PyTorch", "AI & Data Science", 88),
                    Triple("Statistics & R", "Data Science", 82),
                    Triple("SQL & BigQuery", "Data Engineering", 85),
                    Triple("Data Modeling", "Analytics", 84)
                )
            )
            else -> Triple(
                "CSE - III Year",
                "Machine learning and mobile application engineering student. Building collaborative peer networks.",
                listOf(
                    Triple("Python Backend", "Backend Systems", 80),
                    Triple("Flutter Apps", "Mobile Development", 70),
                    Triple("Machine Learning", "AI & Data Science", 60),
                    Triple("Clean Architecture", "Engineering", 85)
                )
            )
        }

        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("email", email)
            put("student_id", if (id == "std_alex") "STU-2026-042" else if (id == "std_elena") "STU-2026-089" else "STU-2026-015")
            put("class_id", "cls_cs2026")
            put("class_name", className)
            put("avatar_url", "")
            put("bio", bio)
            put("contact_number", if (id == "std_alex") "9876543210" else if (id == "std_elena") "9123456789" else "9988776655")
            val skillsArray = JSONArray().apply {
                skillsList.forEach { s ->
                    put(JSONObject().apply {
                        put("name", s.first)
                        put("category", s.second)
                        put("level", s.third)
                        put("proficiency", if (s.third >= 85) "Expert" else if (s.third >= 75) "Advanced" else "Intermediate")
                    })
                }
            }
            put("skills", skillsArray)
        }
    }

    private fun createMockAchievementsJson(): JSONArray {
        return JSONArray().apply {
            put(JSONObject().apply {
                put("id", "ach_001")
                put("student_id", sessionManager.currentUserId.value)
                put("title", "ESP32 Autonomous Environmental Sensor Node")
                put("description", "Designed and deployed an edge IoT sensing device measuring humidity, temperature, and CO2, streaming encrypted telemetry over MQTT.")
                put("date", "2026-08-15")
                put("category", "Embedded & IoT")
                put("proof_url", "https://github.com/vigneshkarthicm/SkillDiscovery/esp32-node")
                put("visibility", "all")
                put("created_at", System.currentTimeMillis() - 86400000L * 14)
                put("extracted_skills", JSONArray().apply {
                    put(JSONObject().apply { put("name", "ESP32"); put("category", "Embedded"); put("level", 88); put("proficiency", "Advanced") })
                    put(JSONObject().apply { put("name", "IoT Protocols (MQTT)"); put("category", "Networking"); put("level", 85); put("proficiency", "Advanced") })
                    put(JSONObject().apply { put("name", "Hardware Development"); put("category", "Hardware"); put("level", 78); put("proficiency", "Intermediate") })
                })
            })
            put(JSONObject().apply {
                put("id", "ach_002")
                put("student_id", sessionManager.currentUserId.value)
                put("title", "Distributed Skill Matching Engine")
                put("description", "Built a vector-based student skill discovery algorithm in Python using cosine similarity to recommend study teammates and project collaborators.")
                put("date", "2026-09-02")
                put("category", "AI & Machine Learning")
                put("proof_url", "https://github.com/vigneshkarthicm/SkillDiscovery")
                put("visibility", "all")
                put("created_at", System.currentTimeMillis() - 86400000L * 5)
                put("extracted_skills", JSONArray().apply {
                    put(JSONObject().apply { put("name", "Python"); put("category", "Programming"); put("level", 92); put("proficiency", "Expert") })
                    put(JSONObject().apply { put("name", "Machine Learning"); put("category", "AI"); put("level", 85); put("proficiency", "Advanced") })
                    put(JSONObject().apply { put("name", "Vector Embeddings"); put("category", "AI"); put("level", 80); put("proficiency", "Advanced") })
                })
            })
            put(JSONObject().apply {
                put("id", "ach_003")
                put("student_id", sessionManager.currentUserId.value)
                put("title", "High-Throughput FastAPI Microservice")
                put("description", "Implemented asynchronous REST endpoints supporting real-time collaborative discussion voting and skill taxonomy persistence.")
                put("date", "2026-09-10")
                put("category", "Backend Systems")
                put("proof_url", "https://gitlab.com/projects/skill-service-api")
                put("visibility", "groups")
                put("created_at", System.currentTimeMillis() - 86400000L * 2)
                put("extracted_skills", JSONArray().apply {
                    put(JSONObject().apply { put("name", "FastAPI"); put("category", "Backend"); put("level", 84); put("proficiency", "Advanced") })
                    put(JSONObject().apply { put("name", "PostgreSQL / SQLite"); put("category", "Databases"); put("level", 80); put("proficiency", "Intermediate") })
                })
            })
        }
    }

    private fun createMockPeersJson(query: String): JSONArray {
        val allPeers = listOf(
            Triple("Maya Chen", "AI, PyTorch, Computer Vision, Python", 95),
            Triple("Liam O'Connor", "IoT, ESP32, Embedded Systems, C++", 91),
            Triple("Amina Patel", "Jetpack Compose, Kotlin, Android, Mobile", 88),
            Triple("Carlos Ramirez", "FastAPI, PostgreSQL, Docker, Cloud", 84),
            Triple("Elena Rostova", "Data Science, Machine Learning, Statistics, R", 82),
            Triple("Marcus Vance", "Cybersecurity, Network Analysis, Rust, Linux", 79)
        )

        val filtered = if (query.isBlank()) {
            allPeers
        } else {
            val qLower = query.lowercase().replace("+", " ")
            val terms = qLower.split(" ", ",").filter { it.isNotBlank() }
            allPeers.filter { peer ->
                terms.any { term -> peer.second.lowercase().contains(term) || peer.first.lowercase().contains(term) }
            }.ifEmpty { allPeers }
        }

        val array = JSONArray()
        filtered.forEachIndexed { index, peer ->
            array.put(JSONObject().apply {
                put("id", "peer_${index + 101}")
                put("name", peer.first)
                put("email", "${peer.first.lowercase().replace(" ", ".")}@student.edu")
                put("class_id", "cls_cs2026")
                put("class_name", "CS & Artificial Intelligence Honours")
                put("avatar_url", "")
                put("bio", "Passionate about collaborative open source projects and peer mentoring.")
                put("skills", JSONArray(peer.second.split(", ")))
                put("match_score", peer.third)
            })
        }
        return array
    }

    private fun createMockDiscussionsJson(tag: String?): JSONArray {
        val array = JSONArray()

        val item1 = JSONObject().apply {
            put("id", "disc_101")
            put("title", "Edge AI vs Cloud Inference for Capstone IoT Sensors")
            put("description", "We are designing an on-device anomaly detection system. Should we deploy quantized TFLite on ESP32/Jetson Nano, or transmit batch telemetry to FastAPI?")
            put("author_id", "std_user_003")
            put("author_name", "Maya Chen")
            put("author_avatar", "")
            put("tags", JSONArray().apply { put("AI"); put("IoT") })
            put("created_at", System.currentTimeMillis() - 3600000L * 4) // 4 hours ago
            put("upvotes_count", 18)
            put("user_has_upvoted", true)
            put("poll_question", "Which architecture best balances latency and power consumption?")
            put("poll_options", JSONArray().apply {
                put(JSONObject().apply { put("text", "Quantized TFLite on Edge (Jetson Nano)"); put("votes", 14) })
                put(JSONObject().apply { put("text", "ESP32 Telemetry + Central FastAPI"); put("votes", 8) })
                put(JSONObject().apply { put("text", "Hybrid: Local rule filters + Cloud ML"); put("votes", 11) })
            })
            put("user_voted_option_index", 0)
            put("visibility_percentage", 82)
            put("class_id", "CS-2026-AI")
        }

        val item2 = JSONObject().apply {
            put("id", "disc_102")
            put("title", "Inter-Class Hackathon Team Formation & Skill Stacks")
            put("description", "Preparing for the National Collegiate Tech Challenge. Let's align teams to ensure every squad pairs a backend engineer with mobile and ML developers.")
            put("author_id", "std_user_004")
            put("author_name", "Liam O'Connor")
            put("author_avatar", "")
            put("tags", JSONArray().apply { put("Events"); put("Mobile"); put("Web") })
            put("created_at", System.currentTimeMillis() - 86400000L * 1) // yesterday
            put("upvotes_count", 24)
            put("user_has_upvoted", false)
            put("poll_question", "What is your primary contribution preference for the hackathon?")
            put("poll_options", JSONArray().apply {
                put(JSONObject().apply { put("text", "Mobile & UI (Compose)"); put("votes", 12) })
                put(JSONObject().apply { put("text", "Backend & APIs (FastAPI)"); put("votes", 15) })
                put(JSONObject().apply { put("text", "AI/ML Models & Analytics"); put("votes", 18) })
                put(JSONObject().apply { put("text", "Hardware / Embedded Prototyping"); put("votes", 7) })
            })
            put("user_voted_option_index", -1)
            put("visibility_percentage", 91)
            put("class_id", "CS-2026-AI")
        }

        val item3 = JSONObject().apply {
            put("id", "disc_103")
            put("title", "Standardizing Dataset Formatting for Peer Skill Matching")
            put("description", "Proposing an open JSON schema for skill taxonomy classification across department labs. We need consensus on category labels and proficiency level scoring.")
            put("author_id", "std_user_005")
            put("author_name", "Amina Patel")
            put("author_avatar", "")
            put("tags", JSONArray().apply { put("AI"); put("Web") })
            put("created_at", System.currentTimeMillis() - 86400000L * 3)
            put("upvotes_count", 11)
            put("user_has_upvoted", false)
            put("poll_question", "Adopt O*NET standard taxonomy or customized departmental skills?")
            put("poll_options", JSONArray().apply {
                put(JSONObject().apply { put("text", "O*NET Standard Competency Taxonomy"); put("votes", 9) })
                put(JSONObject().apply { put("text", "Custom Department Hierarchy"); put("votes", 13) })
            })
            put("user_voted_option_index", -1)
            put("visibility_percentage", 64)
            put("class_id", "CS-2026-AI")
        }

        val allItems = listOf(item1, item2, item3)
        if (tag.isNullOrBlank() || tag == "All") {
            allItems.forEach { array.put(it) }
        } else {
            allItems.filter { item ->
                val tags = item.getJSONArray("tags")
                var match = false
                for (i in 0 until tags.length()) {
                    if (tags.getString(i).equals(tag, ignoreCase = true)) match = true
                }
                match
            }.forEach { array.put(it) }
        }

        return array
    }
}
