package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AchievementEntity
import com.example.data.local.entity.ClassInfoEntity
import com.example.data.local.entity.DiscussionEntity
import com.example.data.local.entity.PeerStudentEntity
import com.example.data.local.entity.SkillEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.remote.NetworkMonitor
import com.example.data.remote.model.SkillDto
import com.example.data.repository.SessionManager
import com.example.data.repository.SkillDiscoveryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

data class SkillGraphLink(
    val source: String,
    val target: String,
    val weight: Int
)

data class ProfileMetrics(
    val weeklyConsistency: Int = 0,
    val weeklyActivity: List<Pair<String, Int>> = emptyList(),
    val skillGraph: List<SkillGraphLink> = emptyList()
)

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class SkillDiscoveryViewModel(
    private val repository: SkillDiscoveryRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isCurrentlyOnline())

    val lastSyncTime: StateFlow<Long> = sessionManager.lastUpdatedTime

    val currentUserId: StateFlow<String> = sessionManager.currentUserId
    val currentClassCode: StateFlow<String> = sessionManager.currentClassCode
    val apiBaseUrl: StateFlow<String> = sessionManager.apiBaseUrl
    val isSandboxBackend: StateFlow<Boolean> = sessionManager.isSandboxBackend
    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedIn

    private val _profileMetrics = MutableStateFlow(ProfileMetrics())
    val profileMetrics: StateFlow<ProfileMetrics> = _profileMetrics.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val profile: StateFlow<StudentProfileEntity?> = currentUserId
        .flatMapLatest { userId -> repository.getProfileStream(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val skills: StateFlow<List<SkillEntity>> = currentUserId
        .flatMapLatest { userId -> repository.getSkillsStream(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val achievements: StateFlow<List<AchievementEntity>> = currentUserId
        .flatMapLatest { userId -> repository.getAchievementsStream(userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discussions: StateFlow<List<DiscussionEntity>> = repository.getDiscussionsStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val peers: StateFlow<List<PeerStudentEntity>> = repository.getPeersStream()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classInfo: StateFlow<ClassInfoEntity?> = repository.getClassInfoStream("cls_cs2026")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var syncJob: Job? = null
    private val pendingDiscussionActions = mutableSetOf<String>()

    private val _isExtractingSkills = MutableStateFlow(false)
    val isExtractingSkills: StateFlow<Boolean> = _isExtractingSkills.asStateFlow()

    private val _extractedSkillsPreview = MutableStateFlow<List<SkillDto>>(emptyList())
    val extractedSkillsPreview: StateFlow<List<SkillDto>> = _extractedSkillsPreview.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedDiscussionTag = MutableStateFlow("All")
    val selectedDiscussionTag: StateFlow<String> = _selectedDiscussionTag.asStateFlow()

    private val _selectedTimeFilter = MutableStateFlow("All")
    val selectedTimeFilter: StateFlow<String> = _selectedTimeFilter.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    init {
        if (sessionManager.isLoggedIn.value) {
            viewModelScope.launch {
                repository.initializeSeedDataIfEmpty()
                refreshAll()
            }
        }
    }

    fun refreshAll() {
        if (!sessionManager.isLoggedIn.value) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                withTimeout(20_000) {
                    val profileResult = repository.syncProfile()
                    val achievementsResult = repository.syncAchievements()
                    val metricsResult = repository.syncMetrics()
                    profileResult.exceptionOrNull()?.let { throw it }
                    achievementsResult.exceptionOrNull()?.let { throw it }
                    metricsResult.onSuccess { metrics ->
                        _profileMetrics.value = ProfileMetrics(
                            weeklyConsistency = metrics.weeklyConsistency,
                            weeklyActivity = metrics.weeklyActivity.map { it.date to it.points },
                            skillGraph = metrics.skillGraph.edges.map { edge ->
                                SkillGraphLink(edge.source, edge.target, edge.weight)
                            }
                        )
                    }.onFailure { throw it }
                    repository.syncDiscussions()
                    repository.searchPeers(null)
                }
                _uiEvents.emit(UiMessage.Success("Backend sync completed"))
            } catch (e: TimeoutCancellationException) {
                _uiEvents.emit(UiMessage.Error("Backend connection timed out. Check the phone and PC are on the same Wi-Fi."))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "server unreachable"
                if (message.contains("401")) {
                    sessionManager.clearSession()
                    repository.clearSessionCache()
                    _profileMetrics.value = ProfileMetrics()
                    _authError.value = "Session expired. Please log in again."
                } else {
                    _uiEvents.emit(UiMessage.Error("Backend sync failed: $message"))
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            val result = repository.login(email, password)
            if (result.isSuccess) {
                refreshAll()
                onSuccess()
            } else {
                _authError.value = result.exceptionOrNull()?.message ?: "Login failed"
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _profileMetrics.value = ProfileMetrics()
        viewModelScope.launch { repository.clearSessionCache() }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            repository.searchPeers(query.ifBlank { null })
        }
    }

    fun onDiscussionTagSelected(tag: String) {
        _selectedDiscussionTag.value = tag
        viewModelScope.launch {
            repository.syncDiscussions(if (tag == "All") null else tag)
        }
    }

    fun onTimeFilterSelected(filter: String) {
        _selectedTimeFilter.value = filter
    }

    fun extractSkillsPreview(text: String) {
        if (text.isBlank()) {
            _extractedSkillsPreview.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isExtractingSkills.value = true
            val result = repository.extractSkills(text)
            _isExtractingSkills.value = false
            result.onSuccess { extracted ->
                _extractedSkillsPreview.value = extracted
            }.onFailure { error ->
                _uiEvents.emit(UiMessage.Error(error.message ?: "Could not extract skills"))
            }
        }
    }

    fun clearExtractedSkills() {
        _extractedSkillsPreview.value = emptyList()
    }

    fun addAchievement(
        title: String,
        description: String,
        date: String,
        category: String,
        proofUrl: String,
        visibility: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createAchievement(
                title = title,
                description = description,
                date = date,
                category = category,
                proofUrl = proofUrl,
                visibility = visibility
            )
            _isLoading.value = false
            result.onSuccess {
                _extractedSkillsPreview.value = emptyList()
                _uiEvents.emit(UiMessage.Success("Achievement added and skills extracted!"))
                refreshAll()
                onSuccess()
            }.onFailure { error ->
                _uiEvents.emit(UiMessage.Error(error.message ?: "Failed to add achievement"))
            }
        }
    }

    fun upvoteDiscussion(discussionId: String) {
        if (!pendingDiscussionActions.add("upvote:$discussionId")) return
        viewModelScope.launch {
            try {
                val result = repository.upvoteDiscussion(discussionId)
                result.onFailure { error ->
                    _uiEvents.emit(UiMessage.Error(error.message ?: "Failed to upvote"))
                }
            } finally {
                pendingDiscussionActions.remove("upvote:$discussionId")
            }
        }
    }

    fun voteInDiscussionPoll(discussionId: String, optionIndex: Int) {
        if (!pendingDiscussionActions.add("vote:$discussionId")) return
        viewModelScope.launch {
            try {
                val result = repository.voteDiscussion(discussionId, optionIndex)
                result.onFailure { error ->
                    _uiEvents.emit(UiMessage.Error(error.message ?: "Failed to cast vote"))
                }
            } finally {
                pendingDiscussionActions.remove("vote:$discussionId")
            }
        }
    }

    fun clearDiscussionPollVote(discussionId: String) {
        if (!pendingDiscussionActions.add("vote:$discussionId")) return
        viewModelScope.launch {
            try {
                val result = repository.clearDiscussionVote(discussionId)
                result.onFailure { error ->
                    _uiEvents.emit(UiMessage.Error(error.message ?: "Failed to clear vote"))
                }
            } finally {
                pendingDiscussionActions.remove("vote:$discussionId")
            }
        }
    }

    fun createDiscussion(
        title: String,
        description: String,
        tags: List<String>,
        pollQuestion: String,
        pollOptions: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.createDiscussion(
                title = title,
                description = description,
                tags = tags,
                pollQuestion = pollQuestion,
                pollOptions = pollOptions
            )
            _isLoading.value = false
            result.onSuccess {
                _uiEvents.emit(UiMessage.Success("Discussion published successfully"))
                onSuccess()
            }.onFailure { error ->
                _uiEvents.emit(UiMessage.Error(error.message ?: "Failed to publish discussion"))
            }
        }
    }

    fun joinClass(classCode: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.joinClass(classCode)
            _isLoading.value = false
            result.onSuccess {
                _uiEvents.emit(UiMessage.Success("Successfully joined ${it.name}"))
                onSuccess()
            }.onFailure { error ->
                _uiEvents.emit(UiMessage.Error(error.message ?: "Failed to join class"))
            }
        }
    }

    fun updateServerSettings(baseUrl: String, isSandbox: Boolean) {
        sessionManager.setApiBaseUrl(baseUrl)
        sessionManager.setSandboxBackend(isSandbox)
        repository.refreshApiService()
        viewModelScope.launch {
            _uiEvents.emit(UiMessage.Success("Backend configuration updated"))
            refreshAll()
        }
    }

    fun switchStudentProfile(userId: String) {
        sessionManager.switchUser(userId)
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncProfile()
                repository.syncAchievements()
                _uiEvents.emit(UiMessage.Success("Loaded backend profile for $userId"))
            } catch (e: Exception) {
                _uiEvents.emit(UiMessage.Error(e.message ?: "Failed to load profile"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _uiEvents.emit(UiMessage.Success("Local Room cache reset successfully"))
        }
    }
}

class SkillDiscoveryViewModelFactory(
    private val repository: SkillDiscoveryRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkillDiscoveryViewModel::class.java)) {
            return SkillDiscoveryViewModel(repository, sessionManager, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
