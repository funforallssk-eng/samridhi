package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceManager
import com.example.data.local.ChatDatabase
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ChatDatabase.getDatabase(application)
    private val repository = ChatRepository(database.chatDao(), viewModelScope, application.applicationContext)
    val voiceManager = VoiceManager(application)

    // Dark mode state - smooth dark mode default
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Active conversation
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    // Home tab: 0 = Direct Chats, 1 = Spaces (Groups)
    private val _selectedHomeTab = MutableStateFlow(0)
    val selectedHomeTab: StateFlow<Int> = _selectedHomeTab.asStateFlow()

    fun setHomeTab(tabIndex: Int) {
        _selectedHomeTab.value = tabIndex
    }

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // User profile modal (to view any user's profile: Alex, Elena, Maya, etc.)
    private val _viewingUserProfile = MutableStateFlow<UserEntity?>(null)
    val viewingUserProfile: StateFlow<UserEntity?> = _viewingUserProfile.asStateFlow()

    fun showUserProfile(user: UserEntity?) {
        _viewingUserProfile.value = user
    }

    // Edit own profile dialog
    private val _showEditProfileDialog = MutableStateFlow(false)
    val showEditProfileDialog: StateFlow<Boolean> = _showEditProfileDialog.asStateFlow()

    fun setShowEditProfileDialog(show: Boolean) {
        _showEditProfileDialog.value = show
    }

    // Create group dialog
    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()

    fun setShowCreateGroupDialog(show: Boolean) {
        _showCreateGroupDialog.value = show
    }

    // Group info / members dialog
    private val _showGroupInfoDialog = MutableStateFlow(false)
    val showGroupInfoDialog: StateFlow<Boolean> = _showGroupInfoDialog.asStateFlow()

    fun setShowGroupInfoDialog(show: Boolean) {
        _showGroupInfoDialog.value = show
    }

    // Fullscreen image viewer
    private val _fullscreenImageUri = MutableStateFlow<String?>(null)
    val fullscreenImageUri: StateFlow<String?> = _fullscreenImageUri.asStateFlow()

    fun openFullscreenImage(uri: String?) {
        _fullscreenImageUri.value = uri
    }

    // Google Authenticator & Account dialog
    private val _showGoogleAuthDialog = MutableStateFlow(false)
    val showGoogleAuthDialog: StateFlow<Boolean> = _showGoogleAuthDialog.asStateFlow()

    fun setShowGoogleAuthDialog(show: Boolean) {
        _showGoogleAuthDialog.value = show
    }

    // Find Users dialog
    private val _showFindUsersDialog = MutableStateFlow(false)
    val showFindUsersDialog: StateFlow<Boolean> = _showFindUsersDialog.asStateFlow()

    fun setShowFindUsersDialog(show: Boolean) {
        _showFindUsersDialog.value = show
    }

    // Firebase Cloud Sync dialog
    private val _showFirebaseSyncDialog = MutableStateFlow(false)
    val showFirebaseSyncDialog: StateFlow<Boolean> = _showFirebaseSyncDialog.asStateFlow()

    fun setShowFirebaseSyncDialog(show: Boolean) {
        _showFirebaseSyncDialog.value = show
    }

    // Cloud Real-Time status and notifications
    val syncStatus = repository.syncStatus ?: MutableStateFlow(com.example.data.sync.CloudSyncStatus()).asStateFlow()
    val incomingNotification = repository.incomingNotification ?: MutableStateFlow(null).asStateFlow()

    fun clearIncomingNotification() {
        repository.clearNotification()
    }

    fun configureFirebase(projectId: String, apiKey: String) {
        repository.configureFirebaseProject(projectId, apiKey)
    }

    // Repository state flows
    val currentUser: StateFlow<UserEntity?> = repository.getCurrentUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val otherUsers: StateFlow<List<UserEntity>> = repository.getAllOtherUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val directConversations: StateFlow<List<ConversationEntity>> = repository.getDirectConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupConversations: StateFlow<List<ConversationEntity>> = repository.getGroupConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val typingStatus: StateFlow<Map<String, String?>> = repository.typingStatus

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeConversation: StateFlow<ConversationEntity?> = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getConversationFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<List<MessageEntity>> = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getMessagesForConversation(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGroupMemberUsers: StateFlow<List<UserEntity>> = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getGroupMemberUsers(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initSeedDataIfEmpty()
        }
    }

    fun selectConversation(conversationId: String?) {
        voiceManager.stopPlayback()
        _activeConversationId.value = conversationId
        if (conversationId != null) {
            repository.startListeningToConversation(conversationId)
        } else {
            repository.stopListeningToConversation()
        }
    }

    fun sendMessage(text: String) {
        val convId = _activeConversationId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(conversationId = convId, text = text.trim())
        }
    }

    fun sendImage(imageUri: String) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = "",
                mediaType = "IMAGE",
                mediaUri = imageUri
            )
        }
    }

    fun sendVoiceNote(audioUri: String, durationSec: Int) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = "",
                mediaType = "AUDIO",
                mediaUri = audioUri,
                audioDurationSec = durationSec
            )
        }
    }

    fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUri: String?,
        status: String
    ) {
        viewModelScope.launch {
            repository.updateCurrentUserProfile(
                username = username,
                displayName = displayName,
                bio = bio,
                avatarUri = avatarUri,
                status = status
            )
            _showEditProfileDialog.value = false
        }
    }

    fun updateGoogleAuth(isEnabled: Boolean, googleEmail: String?) {
        viewModelScope.launch {
            repository.updateGoogleAuthStatus(isEnabled, googleEmail)
        }
    }

    fun createGroup(
        name: String,
        description: String,
        selectedUserIds: List<String>,
        gradientIndex: Int
    ) {
        viewModelScope.launch {
            val groupId = repository.createGroup(
                name = name,
                description = description,
                selectedUserIds = selectedUserIds,
                gradientIndex = gradientIndex
            )
            _showCreateGroupDialog.value = false
            selectConversation(groupId)
        }
    }

    fun addMemberToActiveGroup(userId: String) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.addGroupMember(convId, userId)
        }
    }

    fun startDirectChat(user: UserEntity) {
        viewModelScope.launch {
            val convId = repository.createDirectChatWithUser(user)
            _viewingUserProfile.value = null
            _showFindUsersDialog.value = false
            selectConversation(convId)
        }
    }

    fun loginWithPassword(identifier: String, password: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.loginWithPassword(identifier, password)
            if (result.isSuccess) {
                onResult(null)
            } else {
                onResult(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun registerUser(
        displayName: String,
        username: String,
        email: String,
        password: String,
        avatarUri: String?,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(displayName, username, email, password, avatarUri)
            if (result.isSuccess) {
                onResult(null)
            } else {
                onResult(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun loginWithGoogle(googleEmail: String, displayName: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.loginWithGoogle(googleEmail, displayName)
            if (result.isSuccess) {
                onResult(null)
            } else {
                onResult(result.exceptionOrNull()?.message ?: "Google Sign-In failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _activeConversationId.value = null
            _viewingUserProfile.value = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
