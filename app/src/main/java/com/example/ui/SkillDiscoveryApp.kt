package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AchievementItem
import com.example.model.AppScreen
import com.example.model.ClassmateItem
import com.example.model.DiscussionItem
import com.example.model.DiscussionPoll
import com.example.model.DiscussionPollOption
import com.example.model.MainTab
import com.example.ui.achievements.AchievementsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.ClassCodeScreen
import com.example.ui.components.DiscoveryScaffold
import com.example.ui.discussion.DiscussionScreen
import com.example.ui.explore.ExploreScreen
import com.example.ui.more.MoreScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.viewmodel.SkillDiscoveryViewModel
import org.json.JSONArray

private fun syncedAchievement(entity: com.example.data.local.entity.AchievementEntity) = AchievementItem(
    title = entity.title,
    date = entity.date,
    category = entity.category,
    skills = entity.extractedSkillsJson.ifBlank { "General" },
    description = entity.description,
    imageUri = entity.proofUrl.ifBlank { null }
)

private fun syncedDiscussion(entity: com.example.data.local.entity.DiscussionEntity): DiscussionItem {
    val options = JSONArray(entity.pollOptionsJson).let { json ->
        (0 until json.length()).map { index ->
            val option = json.getJSONObject(index)
            DiscussionPollOption(option.optString("text"), option.optInt("votes"))
        }
    }
    return DiscussionItem(
        id = entity.id.hashCode(),
        backendId = entity.id,
        author = entity.authorName,
        handle = "@${entity.authorId}",
        title = entity.title,
        content = entity.description,
        tags = entity.tagsJson.split(",").filter { it.isNotBlank() },
        visibility = if (entity.visibility == "all") "@all" else "@class",
        priority = "Normal",
        votes = entity.upvotesCount,
        timeAgo = "recent",
        poll = entity.pollQuestion.takeIf { it.isNotBlank() }?.let {
            DiscussionPoll(it, options, entity.userVotedOptionIndex.takeIf { index -> index >= 0 })
        },
        upvoted = entity.userHasUpvoted
    )
}

@Composable
fun SkillDiscoveryApp(
    modifier: Modifier = Modifier,
    viewModel: SkillDiscoveryViewModel? = null
) {
    val context = LocalContext.current
    val pkg = context.packageName

    // Observe backend-synced reactive state from ViewModel
    val profile = viewModel?.profile?.collectAsStateWithLifecycle()?.value
    val skills = viewModel?.skills?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val syncedAchievements = viewModel?.achievements?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val syncedDiscussions = viewModel?.discussions?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val isOnline = viewModel?.isOnline?.collectAsStateWithLifecycle()?.value ?: true
    val lastSyncTime = viewModel?.lastSyncTime?.collectAsStateWithLifecycle()?.value ?: System.currentTimeMillis()
    val apiBaseUrl = viewModel?.apiBaseUrl?.collectAsStateWithLifecycle()?.value ?: "https://api.skilldiscovery.edu/v1"
    val isSandboxBackend = viewModel?.isSandboxBackend?.collectAsStateWithLifecycle()?.value ?: true
    val isLoading = viewModel?.isLoading?.collectAsStateWithLifecycle()?.value ?: false
    val currentUserId = viewModel?.currentUserId?.collectAsStateWithLifecycle()?.value ?: "std_vinay"
    val isLoggedIn = viewModel?.isLoggedIn?.collectAsStateWithLifecycle()?.value ?: false
    val authError = viewModel?.authError?.collectAsStateWithLifecycle()?.value
    val profileMetrics = viewModel?.profileMetrics?.collectAsStateWithLifecycle()?.value

    val activeStudentName = profile?.name ?: "Vinay Kumar"
    val activeStudentRole = profile?.className ?: "CSE - III Year"
    val activeOverallLevel = if (skills.isNotEmpty()) skills.map { it.level }.average().toInt() else 82
    val activeTechnicalSkills = if (skills.isNotEmpty()) {
        skills.map { it.name to it.level }
    } else {
        listOf("Python Backend" to 80, "Flutter Apps" to 70, "Machine Learning" to 60)
    }
    val activeRadarMetrics = remember(skills) {
        if (skills.isNotEmpty()) {
            mapOf(
                "AI" to (skills.find { it.name.contains("ML", ignoreCase = true) || it.name.contains("AI", ignoreCase = true) || it.name.contains("NLP", ignoreCase = true) }?.level?.div(100f) ?: 0.65f),
                "Coding" to (skills.find { it.name.contains("Python", ignoreCase = true) || it.name.contains("C++", ignoreCase = true) }?.level?.div(100f) ?: 0.85f),
                "Comm" to 0.78f,
                "Lead" to 0.72f,
                "Proj" to (skills.find { it.name.contains("Flutter", ignoreCase = true) || it.name.contains("IoT", ignoreCase = true) || it.name.contains("FastAPI", ignoreCase = true) }?.level?.div(100f) ?: 0.88f)
            )
        } else {
            mapOf("AI" to 0.6f, "Coding" to 0.85f, "Comm" to 0.75f, "Lead" to 0.7f, "Proj" to 0.9f)
        }
    }

    var currentScreen by rememberSaveable { mutableStateOf(if (isLoggedIn) AppScreen.MAIN_DASHBOARD else AppScreen.AUTHENTICATION) }
    var currentTab by rememberSaveable { mutableStateOf(MainTab.PROFILE) }
    var classCode by rememberSaveable { mutableStateOf("CSE2026") }

    // Dialog trigger states managed at root scaffold
    var showAddAchievementDialog by remember { mutableStateOf(false) }
    var showAddPollDialog by remember { mutableStateOf(false) }
    var showDiscussionFilters by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn) {
        currentScreen = if (isLoggedIn) AppScreen.MAIN_DASHBOARD else AppScreen.AUTHENTICATION
    }

    // Persistent State for Classmates
    val classmates = remember {
        listOf(
            ClassmateItem("Arun", "ML Engineer", 75, listOf("Python", "Deep Learning", "TensorFlow"), "3rd", "A"),
            ClassmateItem("Priya", "Flutter Developer", 80, listOf("Flutter", "Dart", "Firebase"), "3rd", "P"),
            ClassmateItem("Rahul", "IoT Developer", 70, listOf("ESP32", "Python", "Embedded C"), "3rd", "R"),
            ClassmateItem("Kiran", "UI/UX Designer", 85, listOf("Figma", "Design Systems", "Prototyping"), "2nd", "K"),
            ClassmateItem("Meera", "Software Engineer", 68, listOf("Solidity", "Web3", "Ethereum"), "4th", "M")
        )
    }

    // Persistent State for Achievements
    val achievements = remember {
        mutableStateListOf(
            AchievementItem(
                title = "Machine Learning Certificate",
                date = "12 July 2026",
                category = "Certification",
                skills = "Python, Deep Learning",
                description = "Completed CNN training and optimization on Edge TPU.",
                imageUri = "android.resource://$pkg/drawable/ml_cert_placeholder"
            ),
            AchievementItem(
                title = "College Event Android App",
                date = "05 June 2026",
                category = "Project",
                skills = "Flutter, Dart",
                description = "Published the college companion app. Managed global states using Clean Architecture.",
                imageUri = "android.resource://$pkg/drawable/android_app_placeholder"
            ),
            AchievementItem(
                title = "Smart IoT Irrigation System",
                date = "20 May 2026",
                category = "Project",
                skills = "ESP32, MicroPython, IoT",
                description = "Automated farm watering using soil moisture feedback loops.",
                imageUri = "android.resource://$pkg/drawable/iot_irrigation_placeholder"
            )
        )
    }
    val backendAchievements = syncedAchievements.map(::syncedAchievement)
    val visibleAchievements = (backendAchievements.filter { !it.imageUri.isNullOrBlank() } +
        achievements.filter { !it.imageUri.isNullOrBlank() })
        .distinctBy { it.title }
        .take(3)

    // Persistent State for Discussions
    val discussions = remember {
        mutableStateListOf(
            DiscussionItem(
                id = 1,
                author = "Arun",
                handle = "@arun_ml",
                title = "Hackathon Team: Need ML Dev",
                content = "Looking for someone experienced with TensorFlow Lite for the upcoming Smart India Hackathon.",
                tags = listOf("AI", "Projects"),
                visibility = "@all",
                priority = "High",
                votes = 45,
                timeAgo = "2h ago"
            ),
            DiscussionItem(
                id = 2,
                author = "Priya",
                handle = "@priya_dev",
                title = "Flutter State Management Debate",
                content = "Should we adopt Bloc or Riverpod for the department companion app rewrite? Any experiences?",
                tags = listOf("Projects", "Help"),
                visibility = "@all",
                priority = "Normal",
                votes = 12,
                timeAgo = "5h ago"
            ),
            DiscussionItem(
                id = 3,
                author = "Vinay Kumar",
                handle = "@vinay_k",
                title = "Study Group Revision Poll",
                content = "Please vote for the most convenient time slot for our peer algorithm review session this Friday.",
                tags = listOf("Events", "Poll"),
                visibility = "@all",
                priority = "Normal",
                votes = 31,
                timeAgo = "1d ago",
                poll = DiscussionPoll(
                    question = "Friday Algorithm Review Window",
                    options = listOf(
                        DiscussionPollOption("6:00 PM - 7:00 PM", 14),
                        DiscussionPollOption("7:00 PM - 8:00 PM", 9),
                        DiscussionPollOption("8:00 PM - 9:00 PM", 5)
                    ),
                    selectedOptionIndex = null
                )
            )
        )
    }
    val visibleDiscussions = remember(syncedDiscussions, discussions.toList()) {
        val syncedItems = syncedDiscussions.map(::syncedDiscussion)
        if (syncedItems.isEmpty()) {
            discussions.toList()
        } else {
            val syncedBackendIds = syncedItems.mapNotNull { it.backendId }.toSet()
            val syncedTitles = syncedItems.map { it.title }.toSet()

            val extraLocalItems = discussions.filter { local ->
                (local.backendId == null || !syncedBackendIds.contains(local.backendId)) &&
                    !syncedTitles.contains(local.title)
            }

            extraLocalItems + syncedItems
        }
    }

    when (currentScreen) {
        AppScreen.AUTHENTICATION -> {
            AuthScreen(
                onLogin = { email, password ->
                    viewModel?.login(email, password) { currentScreen = AppScreen.MAIN_DASHBOARD }
                },
                isLoading = isLoading,
                errorMessage = authError,
                modifier = modifier
            )
        }
        AppScreen.CLASS_CODE -> {
            ClassCodeScreen(
                initialCode = classCode,
                onJoinSuccess = { enteredCode ->
                    classCode = enteredCode
                    currentScreen = AppScreen.MAIN_DASHBOARD
                },
                modifier = modifier
            )
        }
        AppScreen.MAIN_DASHBOARD -> {
            DiscoveryScaffold(
                selectedTab = currentTab,
                classCode = classCode,
                onTabSelected = { currentTab = it },
                onAddAchievementClick = { showAddAchievementDialog = true },
                onAddPollClick = { showAddPollDialog = true },
                onFilterDiscussionClick = { showDiscussionFilters = true }
            ) { innerPadding ->
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        MainTab.PROFILE -> {
                            ProfileScreen(
                                classmates = classmates,
                                studentName = activeStudentName,
                                studentRole = activeStudentRole,
                                contactNumber = profile?.contactNumber ?: "9876543210",
                                overallLevel = activeOverallLevel,
                                projectCount = achievements.count { it.category.contains("Project", ignoreCase = true) }.coerceAtLeast(12),
                                achievementCount = achievements.size.coerceAtLeast(15),
                                technicalSkills = activeTechnicalSkills,
                                radarMetrics = activeRadarMetrics,
                                weeklyActivity = profileMetrics?.weeklyActivity.orEmpty(),
                                skillGraph = profileMetrics?.skillGraph.orEmpty(),
                                weeklyConsistency = profileMetrics?.weeklyConsistency ?: 0
                            )
                        }
                        MainTab.ACHIEVE -> {
                            AchievementsScreen(
                                achievements = visibleAchievements,
                                showAddDialog = showAddAchievementDialog,
                                onDismissAddDialog = { showAddAchievementDialog = false },
                                onAddAchievement = { newAchievement ->
                                    if (viewModel != null) {
                                        viewModel.addAchievement(
                                            title = newAchievement.title,
                                            description = newAchievement.description,
                                            date = newAchievement.date,
                                            category = newAchievement.category,
                                            proofUrl = newAchievement.imageUri.orEmpty(),
                                            visibility = "all",
                                            onSuccess = { viewModel.refreshAll() }
                                        )
                                    } else {
                                        achievements.add(0, newAchievement)
                                    }
                                }
                            )
                        }
                        MainTab.DISCUSS -> {
                            DiscussionScreen(
                                discussions = visibleDiscussions,
                                showAddPoll = showAddPollDialog,
                                showFilters = showDiscussionFilters,
                                onDismissAddPoll = { showAddPollDialog = false },
                                onDismissFilters = { showDiscussionFilters = false },
                                onAddDiscussion = { newDiscussion ->
                                    val poll = newDiscussion.poll
                                    discussions.add(0, newDiscussion)
                                    if (viewModel != null) {
                                        viewModel.createDiscussion(
                                            title = newDiscussion.title,
                                            description = newDiscussion.content,
                                            tags = newDiscussion.tags,
                                            pollQuestion = poll?.question.orEmpty(),
                                            pollOptions = poll?.options?.map { it.label }.orEmpty(),
                                            onSuccess = { viewModel.refreshAll() }
                                        )
                                    }
                                },
                                onUpvote = { item ->
                                    item.backendId?.let { viewModel?.upvoteDiscussion(it) }
                                    if (item.backendId == null) {
                                        val index = discussions.indexOfFirst { it.id == item.id }
                                        if (index != -1) {
                                            val newUpvoted = !item.upvoted
                                            val newVotes = item.votes + (if (newUpvoted) 1 else -1) + (if (item.downvoted) 1 else 0)
                                            discussions[index] = item.copy(
                                                votes = newVotes.coerceAtLeast(0),
                                                upvoted = newUpvoted,
                                                downvoted = false
                                            )
                                        }
                                    }
                                },
                                onDownvote = { item ->
                                    if (item.backendId == null) {
                                        val index = discussions.indexOfFirst { it.id == item.id }
                                        if (index != -1) {
                                        val newDownvoted = !item.downvoted
                                        val newVotes = item.votes - (if (newDownvoted) 1 else -1) - (if (item.upvoted) 1 else 0)
                                        discussions[index] = item.copy(
                                            votes = newVotes.coerceAtLeast(0),
                                            downvoted = newDownvoted,
                                            upvoted = false
                                        )
                                        }
                                    }
                                },
                                onPollVote = { discussionId, optionIndex ->
                                    val itemIndex = visibleDiscussions.indexOfFirst { it.id == discussionId }
                                    if (itemIndex != -1) {
                                        val item = visibleDiscussions[itemIndex]
                                        item.backendId?.let { viewModel?.voteInDiscussionPoll(it, optionIndex) }
                                        if (item.backendId == null) item.poll?.let { poll ->
                                            val currentSelection = poll.selectedOptionIndex
                                            val updatedOptions = poll.options.mapIndexed { idx, opt ->
                                                when {
                                                    currentSelection == idx && idx != optionIndex ->
                                                        opt.copy(votes = (opt.votes - 1).coerceAtLeast(0))
                                                    currentSelection != optionIndex && idx == optionIndex ->
                                                        opt.copy(votes = opt.votes + 1)
                                                    else -> opt
                                                }
                                            }
                                            val updatedPoll = poll.copy(
                                                options = updatedOptions,
                                                selectedOptionIndex = optionIndex
                                            )
                                            discussions[itemIndex] = item.copy(poll = updatedPoll)
                                        }
                                    }
                                },
                                onClearPollVote = { discussionId ->
                                    val item = visibleDiscussions.firstOrNull { it.id == discussionId }
                                    item?.backendId?.let { viewModel?.clearDiscussionPollVote(it) }
                                    if (item?.backendId == null) {
                                        val index = discussions.indexOfFirst { it.id == discussionId }
                                        if (index != -1) discussions[index] = discussions[index].copy(
                                            poll = discussions[index].poll?.copy(selectedOptionIndex = null)
                                        )
                                    }
                                }
                            )
                        }
                        MainTab.EXPLORE -> {
                            ExploreScreen(classmates = classmates)
                        }
                        MainTab.MORE -> {
                            MoreScreen(
                                classCode = classCode,
                                onLogout = { viewModel?.logout() },
                                apiBaseUrl = apiBaseUrl,
                                isSandboxBackend = isSandboxBackend,
                                isOnline = isOnline,
                                lastSyncTime = lastSyncTime,
                                isLoading = isLoading,
                                currentUserId = currentUserId,
                                onUpdateServerSettings = { url, sandbox ->
                                    viewModel?.updateServerSettings(url, sandbox)
                                },
                                onSyncNow = {
                                    viewModel?.refreshAll()
                                },
                                onSwitchProfile = { id ->
                                    viewModel?.switchStudentProfile(id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
