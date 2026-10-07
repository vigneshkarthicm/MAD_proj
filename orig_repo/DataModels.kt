package com.example.skilldiscovery

// --- Data Models ---
data class AchievementItem(
    val title: String,
    val date: String,
    val category: String,
    val skills: String,
    val description: String,
    val imageUri: String? = null
)

data class DiscussionItem(
    val id: Int,
    val author: String,
    val handle: String,
    val title: String,
    val content: String,
    val tags: List<String>,
    val visibility: String,
    val priority: String, // "High" or "Normal"
    val votes: Int,
    val timeAgo: String,
    val poll: DiscussionPoll? = null,
    val upvoted: Boolean = false,
    val downvoted: Boolean = false
)

data class DiscussionPollOption(
    val label: String,
    val votes: Int
)

data class DiscussionPoll(
    val question: String,
    val options: List<DiscussionPollOption>,
    val selectedOptionIndex: Int? = null
)

data class ClassmateItem(
    val name: String,
    val role: String,
    val skillLevel: Int,
    val skills: List<String>,
    val year: String,
    val imageChar: String
)

// --- Navigation States ---
enum class AppScreen {
    AUTHENTICATION,
    CLASS_CODE,
    MAIN_DASHBOARD
}

enum class MainTab(val displayName: String) {
    PROFILE("Profile"),
    ACHIEVE("Achieve"),
    DISCUSS("Discuss"),
    EXPLORE("Explore"),
    MORE("More")
}
