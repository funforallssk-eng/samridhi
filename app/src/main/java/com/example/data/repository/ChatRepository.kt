package com.example.data.repository

import android.content.Context
import com.example.data.local.ChatDao
import com.example.data.model.ConversationEntity
import com.example.data.model.GroupMemberEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.example.data.sync.CloudSyncStatus
import com.example.data.sync.FirebaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatDao,
    private val appScope: CoroutineScope,
    private val context: Context? = null
) {
    val syncManager: FirebaseSyncManager? = context?.let { FirebaseSyncManager(it, chatDao) }

    // Real-time typing indicators mapped by conversationId -> typing status string (e.g. "Maya is typing...")
    private val _typingStatus = MutableStateFlow<Map<String, String?>>(emptyMap())
    val typingStatus: StateFlow<Map<String, String?>> = _typingStatus.asStateFlow()

    val syncStatus: StateFlow<CloudSyncStatus>? = syncManager?.syncStatus
    val incomingNotification: StateFlow<Pair<String, String>?>? = syncManager?.incomingNotification

    fun clearNotification() = syncManager?.clearNotification()
    fun configureFirebaseProject(projectId: String, apiKey: String) = syncManager?.configureProject(projectId, apiKey)
    fun startListeningToConversation(convId: String) = syncManager?.startListeningToConversation(convId)
    fun stopListeningToConversation() = syncManager?.stopListeningToConversation()

    fun getCurrentUserFlow(): Flow<UserEntity?> = chatDao.getCurrentUserFlow()
    fun getAllOtherUsersFlow(): Flow<List<UserEntity>> = chatDao.getAllOtherUsers()
    fun getUserByIdFlow(userId: String): Flow<UserEntity?> = chatDao.getUserByIdFlow(userId)
    suspend fun getUserById(userId: String): UserEntity? = chatDao.getUserById(userId)

    fun getAllConversations(): Flow<List<ConversationEntity>> = chatDao.getAllConversations()
    fun getDirectConversations(): Flow<List<ConversationEntity>> = chatDao.getDirectConversations()
    fun getGroupConversations(): Flow<List<ConversationEntity>> = chatDao.getGroupConversations()
    fun getConversationFlow(id: String): Flow<ConversationEntity?> = chatDao.getConversationFlow(id)

    fun getMessagesForConversation(convId: String): Flow<List<MessageEntity>> =
        chatDao.getMessagesForConversation(convId)

    fun getGroupMembers(convId: String): Flow<List<GroupMemberEntity>> =
        chatDao.getGroupMembers(convId)

    fun getGroupMemberUsers(convId: String): Flow<List<UserEntity>> =
        chatDao.getGroupMemberUsers(convId)

    suspend fun initSeedDataIfEmpty() {
        val existingUser = chatDao.getUserByUsername("alex_m")
        if (existingUser == null) {
            seedInitialData()
        }
    }

    private suspend fun seedInitialData() {
        // Seed users
        val me = UserEntity(
            id = "user_me",
            username = "alex_m",
            displayName = "Alex Morgan",
            email = "funforallssk@gmail.com",
            password = "password123",
            bio = "Product Engineer & UI Enthusiast. Loving vibrant gradients and fast mobile apps! 🚀",
            status = "Online ✨",
            isCurrentUser = false,
            gradientIndex = 0,
            googleEmail = "funforallssk@gmail.com",
            isGoogleAuthEnabled = true,
            googleAuthSecret = "JBSWY3DPEHPK3PXP"
        )

        val elena = UserEntity(
            id = "user_elena",
            username = "elena_design",
            displayName = "Elena Rostova",
            email = "elena@samridhi.app",
            password = "password123",
            bio = "Lead UX & Design Systems. Obsessed with dark mode aesthetics and micro-interactions 🎨",
            status = "Designing new spaces",
            isCurrentUser = false,
            gradientIndex = 1
        )

        val liam = UserEntity(
            id = "user_liam",
            username = "liam_v",
            displayName = "Liam Vance",
            email = "liam@samridhi.app",
            password = "password123",
            bio = "Android Architect & Kotlin lover. Audio waves and real-time syncing 🎧",
            status = "In a meeting 🎙️",
            isCurrentUser = false,
            gradientIndex = 2
        )

        val maya = UserEntity(
            id = "user_maya",
            username = "maya_ai",
            displayName = "Maya Chen",
            email = "maya@samridhi.app",
            password = "password123",
            bio = "AI & Machine Learning Engineer. Building intelligent agents and collaborative tools 💡",
            status = "Online",
            isCurrentUser = false,
            gradientIndex = 3
        )

        val marcus = UserEntity(
            id = "user_marcus",
            username = "marcus_b",
            displayName = "Marcus Brody",
            email = "marcus@samridhi.app",
            password = "password123",
            bio = "Cloud Infrastructure & Real-time protocol specialist. High throughput enthusiast ⚡",
            status = "Away",
            isCurrentUser = false,
            gradientIndex = 4
        )

        val devin = UserEntity(
            id = "user_devin",
            username = "devin_c",
            displayName = "Devin Cole",
            email = "devin@samridhi.app",
            password = "password123",
            bio = "Frontend & Creative Developer. WebGL, shaders, and fluid motion 🌊",
            status = "Coding...",
            isCurrentUser = false,
            gradientIndex = 5
        )

        chatDao.insertUsers(listOf(me, elena, liam, maya, marcus, devin))

        val now = System.currentTimeMillis()

        // Conversations
        val convCoreGroup = ConversationEntity(
            id = "group_prism_core",
            name = "🚀 Prism Core Team",
            isGroup = true,
            gradientIndex = 0,
            lastMessage = "Alex: Sounds awesome! Let's ship the *gradient update* today.",
            lastMessageTime = now - 1000 * 60 * 5,
            unreadCount = 2,
            description = "Main product discussion, release planning, and real-time coordination."
        )

        val convDesignGroup = ConversationEntity(
            id = "group_design_hub",
            name = "🎨 Design & Gradients Hub",
            isGroup = true,
            gradientIndex = 1,
            lastMessage = "Elena: Added the new *neon violet* palette for testing!",
            lastMessageTime = now - 1000 * 60 * 35,
            unreadCount = 0,
            description = "Color schemes, dark mode tokens, typography, and fluid animation reviews."
        )

        val convElena = ConversationEntity(
            id = "dm_elena",
            name = "Elena Rostova",
            isGroup = false,
            gradientIndex = 1,
            lastMessage = "Check out this *new gradient spec*! It looks super smooth.",
            lastMessageTime = now - 1000 * 60 * 12,
            unreadCount = 1,
            directUserId = "user_elena"
        )

        val convLiam = ConversationEntity(
            id = "dm_liam",
            name = "Liam Vance",
            isGroup = false,
            gradientIndex = 2,
            lastMessage = "🎙️ Voice note (0:08)",
            lastMessageTime = now - 1000 * 60 * 90,
            unreadCount = 0,
            directUserId = "user_liam"
        )

        val convMaya = ConversationEntity(
            id = "dm_maya",
            name = "Maya Chen",
            isGroup = false,
            gradientIndex = 3,
            lastMessage = "Just uploaded the architecture diagram 📷",
            lastMessageTime = now - 1000 * 60 * 240,
            unreadCount = 0,
            directUserId = "user_maya"
        )

        chatDao.insertConversations(listOf(convCoreGroup, convDesignGroup, convElena, convLiam, convMaya))

        // Group members
        val coreMembers = listOf(
            GroupMemberEntity(conversationId = "group_prism_core", userId = "user_me", role = "Owner"),
            GroupMemberEntity(conversationId = "group_prism_core", userId = "user_elena", role = "Admin"),
            GroupMemberEntity(conversationId = "group_prism_core", userId = "user_liam", role = "Member"),
            GroupMemberEntity(conversationId = "group_prism_core", userId = "user_maya", role = "Member"),
            GroupMemberEntity(conversationId = "group_prism_core", userId = "user_marcus", role = "Member")
        )

        val designMembers = listOf(
            GroupMemberEntity(conversationId = "group_design_hub", userId = "user_elena", role = "Owner"),
            GroupMemberEntity(conversationId = "group_design_hub", userId = "user_me", role = "Admin"),
            GroupMemberEntity(conversationId = "group_design_hub", userId = "user_maya", role = "Member"),
            GroupMemberEntity(conversationId = "group_design_hub", userId = "user_devin", role = "Member")
        )

        chatDao.insertGroupMembers(coreMembers + designMembers)

        // Seed messages for group_prism_core
        val coreMessages = listOf(
            MessageEntity(
                id = "msg_c1",
                conversationId = "group_prism_core",
                senderId = "user_elena",
                senderName = "Elena Rostova",
                senderGradientIndex = 1,
                text = "Hey team! Notice how smooth the *dark mode* transitions are now? ✨",
                timestamp = now - 1000 * 60 * 30,
                isSentByMe = false
            ),
            MessageEntity(
                id = "msg_c2",
                conversationId = "group_prism_core",
                senderId = "user_liam",
                senderName = "Liam Vance",
                senderGradientIndex = 2,
                text = "Yes! And the _voice notes_ with real-time waveforms feel incredibly responsive.",
                timestamp = now - 1000 * 60 * 25,
                isSentByMe = false
            ),
            MessageEntity(
                id = "msg_c3",
                conversationId = "group_prism_core",
                senderId = "user_maya",
                senderName = "Maya Chen",
                senderGradientIndex = 3,
                text = "Also make sure you test the <u>rich text toolbar</u> — *bold*, _italics_, and <u>underline</u> are all working together seamlessly!",
                timestamp = now - 1000 * 60 * 15,
                isSentByMe = false
            ),
            MessageEntity(
                id = "msg_c4",
                conversationId = "group_prism_core",
                senderId = "user_me",
                senderName = "Alex Morgan",
                senderGradientIndex = 0,
                text = "Sounds awesome! Let's ship the *gradient update* today.",
                timestamp = now - 1000 * 60 * 5,
                isSentByMe = true,
                status = "READ"
            )
        )

        // Seed messages for dm_elena
        val elenaMessages = listOf(
            MessageEntity(
                id = "msg_e1",
                conversationId = "dm_elena",
                senderId = "user_elena",
                senderName = "Elena Rostova",
                senderGradientIndex = 1,
                text = "Hey Alex! Can you check the contrast ratios on the dark cards?",
                timestamp = now - 1000 * 60 * 40,
                isSentByMe = false
            ),
            MessageEntity(
                id = "msg_e2",
                conversationId = "dm_elena",
                senderId = "user_me",
                senderName = "Alex Morgan",
                senderGradientIndex = 0,
                text = "Checked it! Contrast is over *4.5:1*, passes WCAG AA comfortably.",
                timestamp = now - 1000 * 60 * 25,
                isSentByMe = true,
                status = "READ"
            ),
            MessageEntity(
                id = "msg_e3",
                conversationId = "dm_elena",
                senderId = "user_elena",
                senderName = "Elena Rostova",
                senderGradientIndex = 1,
                text = "Check out this *new gradient spec*! It looks super smooth.",
                timestamp = now - 1000 * 60 * 12,
                isSentByMe = false
            )
        )

        // Seed messages for dm_liam
        val liamMessages = listOf(
            MessageEntity(
                id = "msg_l1",
                conversationId = "dm_liam",
                senderId = "user_liam",
                senderName = "Liam Vance",
                senderGradientIndex = 2,
                text = "Hey, sent you a quick thought on the audio buffer caching:",
                timestamp = now - 1000 * 60 * 95,
                isSentByMe = false
            ),
            MessageEntity(
                id = "msg_l2",
                conversationId = "dm_liam",
                senderId = "user_liam",
                senderName = "Liam Vance",
                senderGradientIndex = 2,
                text = "Voice message",
                mediaType = "AUDIO",
                audioDurationSec = 8,
                timestamp = now - 1000 * 60 * 90,
                isSentByMe = false
            )
        )

        chatDao.insertMessages(coreMessages + elenaMessages + liamMessages)
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        mediaType: String = "TEXT",
        mediaUri: String? = null,
        audioDurationSec: Int = 0
    ) {
        val currentUser = chatDao.getCurrentUser() ?: return
        val messageId = "msg_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val message = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUser.id,
            senderName = currentUser.displayName,
            senderAvatarUri = currentUser.avatarUri,
            senderGradientIndex = currentUser.gradientIndex,
            text = text,
            mediaType = mediaType,
            mediaUri = mediaUri,
            audioDurationSec = audioDurationSec,
            timestamp = now,
            isSentByMe = true,
            status = "DELIVERED"
        )

        chatDao.insertMessage(message)
        syncManager?.pushMessage(message)

        val preview = when (mediaType) {
            "IMAGE" -> "📷 Photo shared"
            "AUDIO" -> "🎙️ Voice note (${audioDurationSec}s)"
            else -> text
        }

        val conv = chatDao.getConversation(conversationId)
        if (conv != null) {
            chatDao.updateConversation(
                conv.copy(
                    lastMessage = "You: $preview",
                    lastMessageTime = now,
                    unreadCount = 0
                )
            )
        }

        // Trigger simulated real-time response from group or direct user
        triggerSimulatedReply(conversationId, conv, text, mediaType)
    }

    private fun triggerSimulatedReply(
        conversationId: String,
        conv: ConversationEntity?,
        userText: String,
        mediaType: String
    ) {
        appScope.launch(Dispatchers.IO) {
            delay(1200)

            // Determine responder
            val responder: UserEntity? = if (conv?.isGroup == true) {
                val members = chatDao.getGroupMemberUsers(conversationId).firstOrNull() ?: emptyList()
                members.filter { !it.isCurrentUser }.randomOrNull()
            } else {
                conv?.directUserId?.let { chatDao.getUserById(it) }
            }

            if (responder != null) {
                // Show typing indicator
                setTypingStatus(conversationId, "${responder.displayName} is typing...")

                delay(2200)

                // Clear typing indicator
                setTypingStatus(conversationId, null)

                // Generate contextual reply
                val replyText = generateContextualReply(responder.displayName, userText, mediaType, conv?.isGroup == true)
                val replyId = "msg_${UUID.randomUUID()}"
                val replyTime = System.currentTimeMillis()

                val replyMsg = MessageEntity(
                    id = replyId,
                    conversationId = conversationId,
                    senderId = responder.id,
                    senderName = responder.displayName,
                    senderAvatarUri = responder.avatarUri,
                    senderGradientIndex = responder.gradientIndex,
                    text = replyText,
                    mediaType = "TEXT",
                    timestamp = replyTime,
                    isSentByMe = false
                )

                chatDao.insertMessage(replyMsg)

                if (conv != null) {
                    val preview = "${responder.displayName.split(" ").first()}: $replyText"
                    chatDao.updateConversation(
                        conv.copy(
                            lastMessage = preview,
                            lastMessageTime = replyTime
                        )
                    )
                }
            }
        }
    }

    private fun generateContextualReply(
        name: String,
        prompt: String,
        mediaType: String,
        isGroup: Boolean
    ): String {
        val firstName = name.split(" ").first()
        return when (mediaType) {
            "IMAGE" -> {
                val imageReplies = listOf(
                    "This image looks *fantastic*! Love the clarity and color gradients.",
                    "Great shot! The color palette fits our _smooth dark theme_ perfectly 🎨",
                    "Uploaded smoothly! Looking crisp on modern high-DPI displays.",
                    "Nice preview! Let's use this asset in our upcoming showcase."
                )
                imageReplies.random()
            }
            "AUDIO" -> {
                val audioReplies = listOf(
                    "Heard the voice note! Clear audio playback with zero stutter 🎙️",
                    "Great voice message. The *amplitude waveform* looked awesome while playing!",
                    "Got your audio note, totally agree on those points."
                )
                audioReplies.random()
            }
            else -> {
                val lower = prompt.lowercase()
                when {
                    lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                        "Hey Alex! How is the _Prism Chat_ build going on your end?"
                    lower.contains("bold") || lower.contains("format") || lower.contains("rich") ->
                        "The *rich text formatting* is super slick! Supporting *bold*, _italics_, and <u>underline</u> makes messages stand out."
                    lower.contains("group") || lower.contains("team") || lower.contains("space") ->
                        "Groups are great for collaboration. We can invite more teammates right from the group details sheet!"
                    lower.contains("voice") || lower.contains("audio") ->
                        "Voice notes are so convenient! Hold or tap the mic button to record anytime."
                    lower.contains("profile") || lower.contains("avatar") ->
                        "Your profile looks great! Having customizable bios and avatars makes the community feel alive."
                    lower.contains("dark") || lower.contains("gradient") ->
                        "The *colorful gradients* on the deep obsidian background create such a premium vibe."
                    isGroup -> {
                        listOf(
                            "Agreed! Let's keep moving forward with this direction.",
                            "Awesome update! Everyone in the group can see the live sync now 🚀",
                            "Checking this out right now, looks *rock solid*.",
                            "Count me in on this!"
                        ).random()
                    }
                    else -> {
                        listOf(
                            "Thanks for the message! Let me review this right away.",
                            "Totally on board with this! *Let's make it happen* ✨",
                            "Got it! Works seamlessly across both _light_ and _dark_ modes.",
                            "Looks great! Appreciate the quick update, Alex."
                        ).random()
                    }
                }
            }
        }
    }

    private fun setTypingStatus(conversationId: String, status: String?) {
        val current = _typingStatus.value.toMutableMap()
        if (status == null) {
            current.remove(conversationId)
        } else {
            current[conversationId] = status
        }
        _typingStatus.value = current
    }

    // --- Profile Management ---
    suspend fun updateCurrentUserProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUri: String?,
        status: String
    ) {
        val current = chatDao.getCurrentUser() ?: return
        val updated = current.copy(
            username = username.trim(),
            displayName = displayName.trim(),
            bio = bio.trim(),
            avatarUri = avatarUri ?: current.avatarUri,
            status = status.trim()
        )
        chatDao.updateUser(updated)
    }

    // --- Group Management ---
    suspend fun createGroup(
        name: String,
        description: String,
        selectedUserIds: List<String>,
        gradientIndex: Int = 0
    ): String {
        val groupId = "group_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        val currentUser = chatDao.getCurrentUser()

        val group = ConversationEntity(
            id = groupId,
            name = name.trim(),
            isGroup = true,
            gradientIndex = gradientIndex,
            lastMessage = "Space created with ${selectedUserIds.size + 1} members",
            lastMessageTime = now,
            unreadCount = 0,
            description = description.trim().ifEmpty { "A vibrant group space for discussion and sharing." }
        )
        chatDao.insertConversation(group)
        syncManager?.pushConversation(group)

        // Add current user as Owner
        val members = mutableListOf<GroupMemberEntity>()
        if (currentUser != null) {
            members.add(GroupMemberEntity(conversationId = groupId, userId = currentUser.id, role = "Owner"))
        }

        // Add selected users as Members
        for (userId in selectedUserIds) {
            if (currentUser == null || userId != currentUser.id) {
                members.add(GroupMemberEntity(conversationId = groupId, userId = userId, role = "Member"))
            }
        }
        chatDao.insertGroupMembers(members)

        // Initial welcome message
        val welcomeMsg = MessageEntity(
            id = "msg_${UUID.randomUUID()}",
            conversationId = groupId,
            senderId = currentUser?.id ?: "user_me",
            senderName = currentUser?.displayName ?: "Alex Morgan",
            senderGradientIndex = currentUser?.gradientIndex ?: 0,
            text = "Welcome to *$name*! 🚀 Feel free to share messages, images, and voice notes here.",
            timestamp = now,
            isSentByMe = true,
            status = "DELIVERED"
        )
        chatDao.insertMessage(welcomeMsg)
        syncManager?.pushMessage(welcomeMsg)

        return groupId
    }

    suspend fun addGroupMember(conversationId: String, userId: String) {
        chatDao.insertGroupMember(
            GroupMemberEntity(conversationId = conversationId, userId = userId, role = "Member")
        )
        val user = chatDao.getUserById(userId)
        if (user != null) {
            val announcement = MessageEntity(
                id = "msg_${UUID.randomUUID()}",
                conversationId = conversationId,
                senderId = "system",
                senderName = "System",
                senderGradientIndex = 4,
                text = "${user.displayName} joined the space 👋",
                timestamp = System.currentTimeMillis(),
                isSentByMe = false
            )
            chatDao.insertMessage(announcement)
        }
    }

    suspend fun removeGroupMember(conversationId: String, userId: String) {
        chatDao.removeGroupMember(conversationId, userId)
    }

    suspend fun createDirectChatWithUser(user: UserEntity): String {
        // Check if DM exists
        val existingConvs = chatDao.getDirectConversations().firstOrNull() ?: emptyList()
        val found = existingConvs.firstOrNull { it.directUserId == user.id }
        if (found != null) {
            return found.id
        }

        val convId = "dm_${user.id}"
        val now = System.currentTimeMillis()
        val conv = ConversationEntity(
            id = convId,
            name = user.displayName,
            isGroup = false,
            gradientIndex = user.gradientIndex,
            lastMessage = "Started a conversation with ${user.displayName}",
            lastMessageTime = now,
            unreadCount = 0,
            directUserId = user.id
        )
        chatDao.insertConversation(conv)
        syncManager?.pushConversation(conv)
        return convId
    }

    suspend fun updateGoogleAuthStatus(isEnabled: Boolean, googleEmail: String?) {
        val current = chatDao.getCurrentUser() ?: return
        chatDao.updateUser(current.copy(
            isGoogleAuthEnabled = isEnabled,
            googleEmail = googleEmail ?: current.googleEmail
        ))
    }

    fun searchUsersFlow(query: String): Flow<List<UserEntity>> {
        return chatDao.searchUsers(query.trim())
    }

    suspend fun registerUser(
        displayName: String,
        username: String,
        email: String,
        password: String,
        avatarUri: String? = null
    ): Result<UserEntity> {
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim()

        if (cleanName.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        if (cleanUsername.length < 3) return Result.failure(IllegalArgumentException("Username must be at least 3 characters"))
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) return Result.failure(IllegalArgumentException("Please enter a valid email"))
        if (password.length < 4) return Result.failure(IllegalArgumentException("Password must be at least 4 characters"))

        val existingByUsername = chatDao.getUserByUsername(cleanUsername)
        if (existingByUsername != null) {
            return Result.failure(IllegalArgumentException("Username '@$cleanUsername' is already taken"))
        }

        val existingByEmail = chatDao.getUserByEmail(cleanEmail)
        if (existingByEmail != null) {
            return Result.failure(IllegalArgumentException("An account with email '$cleanEmail' already exists"))
        }

        chatDao.clearCurrentUser()

        val newUser = UserEntity(
            id = "user_${UUID.randomUUID()}",
            username = cleanUsername,
            displayName = cleanName,
            email = cleanEmail,
            password = password,
            avatarUri = avatarUri,
            bio = "Hey there! I am using Samridhi Chat ✨",
            status = "Online ✨",
            isCurrentUser = true,
            gradientIndex = kotlin.math.abs(cleanUsername.hashCode()) % 8,
            googleEmail = if (cleanEmail.endsWith("@gmail.com")) cleanEmail else null,
            isGoogleAuthEnabled = true
        )

        chatDao.insertUser(newUser)
        syncManager?.pushUser(newUser)
        return Result.success(newUser)
    }

    suspend fun loginWithPassword(identifier: String, password: String): Result<UserEntity> {
        val cleanIdentifier = identifier.trim().lowercase().removePrefix("@")
        if (cleanIdentifier.isBlank()) return Result.failure(IllegalArgumentException("Please enter your username or email"))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Please enter your password"))

        val user = chatDao.getUserByUsernameOrEmail(cleanIdentifier)
            ?: return Result.failure(IllegalArgumentException("No user found with username or email '$identifier'"))

        if (user.password != null && user.password != password) {
            return Result.failure(IllegalArgumentException("Incorrect password for @${user.username}"))
        }

        chatDao.clearCurrentUser()
        chatDao.setCurrentUser(user.id)
        val updated = user.copy(isCurrentUser = true)
        chatDao.updateUser(updated)
        return Result.success(updated)
    }

    suspend fun loginWithGoogle(
        googleEmail: String,
        displayName: String = "Google User",
        avatarUri: String? = null
    ): Result<UserEntity> {
        val cleanEmail = googleEmail.trim().lowercase()
        val existingUser = chatDao.getUserByEmail(cleanEmail)

        chatDao.clearCurrentUser()

        if (existingUser != null) {
            val updated = existingUser.copy(
                isCurrentUser = true,
                googleEmail = cleanEmail,
                isGoogleAuthEnabled = true
            )
            chatDao.updateUser(updated)
            chatDao.setCurrentUser(updated.id)
            return Result.success(updated)
        } else {
            val usernameFromEmail = cleanEmail.substringBefore("@").replace(".", "_")
            val newUser = UserEntity(
                id = "user_${UUID.randomUUID()}",
                username = usernameFromEmail,
                displayName = displayName.ifBlank { usernameFromEmail.replaceFirstChar { it.uppercase() } },
                email = cleanEmail,
                password = "google_auth_oauth",
                avatarUri = avatarUri,
                bio = "Google Account verified. Real-time messaging on Samridhi 🌟",
                status = "Online ✨",
                isCurrentUser = true,
                gradientIndex = kotlin.math.abs(cleanEmail.hashCode()) % 8,
                googleEmail = cleanEmail,
                isGoogleAuthEnabled = true
            )
            chatDao.insertUser(newUser)
            syncManager?.pushUser(newUser)
            return Result.success(newUser)
        }
    }

    suspend fun logout() {
        chatDao.clearCurrentUser()
    }
}
