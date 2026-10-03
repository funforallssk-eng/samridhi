package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.ChatDao
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncConnectionState {
    CONNECTED,
    CONNECTING,
    OFFLINE_LOCAL_MODE,
    ERROR
}

data class CloudSyncStatus(
    val state: SyncConnectionState = SyncConnectionState.CONNECTING,
    val projectId: String = "samridhi-chat-cloud",
    val lastSyncTime: Long = System.currentTimeMillis(),
    val totalSyncedMessages: Int = 0,
    val activeListenersCount: Int = 0,
    val statusMessage: String = "Initializing cloud synchronization..."
)

class FirebaseSyncManager(
    private val context: Context,
    private val chatDao: ChatDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var firestore: FirebaseFirestore? = null

    private val _syncStatus = MutableStateFlow(CloudSyncStatus())
    val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

    private val _incomingNotification = MutableStateFlow<Pair<String, String>?>(null)
    val incomingNotification: StateFlow<Pair<String, String>?> = _incomingNotification.asStateFlow()

    private val activeRegistrations = mutableListOf<ListenerRegistration>()
    private var messageListener: ListenerRegistration? = null
    private var currentActiveConvId: String? = null

    init {
        initFirebase()
    }

    private fun initFirebase(customProjectId: String? = null, customApiKey: String? = null) {
        try {
            val app = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseApp.getInstance()
            } else {
                val projId = customProjectId ?: "samridhi-chat-realtime"
                val apiKey = customApiKey ?: "AIzaSyPrismChatSyncKey969140230481"
                val options = FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.prismchat.vqxrt")
                    .setApiKey(apiKey)
                    .setProjectId(projId)
                    .build()
                FirebaseApp.initializeApp(context, options, "samridhi-chat")
            }

            firestore = FirebaseFirestore.getInstance(app)
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncConnectionState.CONNECTED,
                projectId = app.options.projectId ?: "samridhi-chat-realtime",
                statusMessage = "Connected to Firestore Real-Time Cloud ⚡",
                lastSyncTime = System.currentTimeMillis()
            )
            Log.d("FirebaseSync", "Firebase initialized with project: ${app.options.projectId}")

            startGlobalCloudListeners()
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Cloud Firestore initialization fallback", e)
            _syncStatus.value = _syncStatus.value.copy(
                state = SyncConnectionState.OFFLINE_LOCAL_MODE,
                statusMessage = "Local SQLite Room Mode active (Offline or Cloud standby)"
            )
        }
    }

    fun configureProject(projectId: String, apiKey: String) {
        scope.launch {
            initFirebase(projectId.trim(), apiKey.trim())
        }
    }

    fun clearNotification() {
        _incomingNotification.value = null
    }

    private fun startGlobalCloudListeners() {
        val db = firestore ?: return

        // 1. Users Listener
        try {
            val userReg = db.collection("users")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FirebaseSync", "Error listening to users", error)
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val data = change.document.data
                            val userId = change.document.id
                            scope.launch {
                                val current = chatDao.getUserById(userId)
                                val isCurrent = current?.isCurrentUser ?: false
                                val user = UserEntity(
                                    id = userId,
                                    username = data["username"] as? String ?: "user",
                                    displayName = data["displayName"] as? String ?: "User",
                                    email = data["email"] as? String ?: "",
                                    password = current?.password ?: (data["password"] as? String),
                                    avatarUri = data["avatarUri"] as? String,
                                    bio = data["bio"] as? String ?: "",
                                    status = data["status"] as? String ?: "Online",
                                    isCurrentUser = isCurrent,
                                    gradientIndex = (data["gradientIndex"] as? Long)?.toInt() ?: 0,
                                    googleEmail = data["googleEmail"] as? String,
                                    isGoogleAuthEnabled = data["isGoogleAuthEnabled"] as? Boolean ?: false
                                )
                                chatDao.insertUser(user)
                            }
                        }
                    }
                }
            activeRegistrations.add(userReg)
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Could not attach user listener", e)
        }

        // 2. Conversations Listener
        try {
            val convReg = db.collection("conversations")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FirebaseSync", "Error listening to conversations", error)
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val data = change.document.data
                            val convId = change.document.id
                            scope.launch {
                                val existing = chatDao.getConversation(convId)
                                val conv = ConversationEntity(
                                    id = convId,
                                    name = data["name"] as? String ?: "Conversation",
                                    isGroup = data["isGroup"] as? Boolean ?: false,
                                    gradientIndex = (data["gradientIndex"] as? Long)?.toInt() ?: 0,
                                    lastMessage = data["lastMessage"] as? String ?: "",
                                    lastMessageTime = (data["lastMessageTime"] as? Long) ?: System.currentTimeMillis(),
                                    unreadCount = existing?.unreadCount ?: 0,
                                    description = data["description"] as? String,
                                    avatarUri = data["avatarUri"] as? String,
                                    directUserId = data["directUserId"] as? String
                                )
                                chatDao.insertConversation(conv)
                            }
                        }
                    }
                }
            activeRegistrations.add(convReg)
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Could not attach conversation listener", e)
        }

        _syncStatus.value = _syncStatus.value.copy(
            activeListenersCount = activeRegistrations.size
        )
    }

    fun startListeningToConversation(convId: String) {
        currentActiveConvId = convId
        messageListener?.remove()

        val db = firestore ?: return
        try {
            messageListener = db.collection("conversations")
                .document(convId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FirebaseSync", "Error listening to messages for $convId", error)
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                            val data = change.document.data
                            val msgId = change.document.id
                            val senderId = data["senderId"] as? String ?: ""

                            scope.launch {
                                val currentUser = chatDao.getCurrentUser()
                                val isFromMe = currentUser?.id == senderId

                                val msg = MessageEntity(
                                    id = msgId,
                                    conversationId = convId,
                                    senderId = senderId,
                                    senderName = data["senderName"] as? String ?: "User",
                                    senderAvatarUri = data["senderAvatarUri"] as? String,
                                    senderGradientIndex = (data["senderGradientIndex"] as? Long)?.toInt() ?: 0,
                                    text = data["text"] as? String ?: "",
                                    mediaType = data["mediaType"] as? String ?: "TEXT",
                                    mediaUri = data["mediaUri"] as? String,
                                    audioDurationSec = (data["audioDurationSec"] as? Long)?.toInt() ?: 0,
                                    timestamp = (data["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                    isSentByMe = isFromMe,
                                    status = data["status"] as? String ?: "DELIVERED"
                                )

                                chatDao.insertMessage(msg)

                                if (!isFromMe && change.type == DocumentChange.Type.ADDED) {
                                    _incomingNotification.value = Pair(msg.senderName, msg.text.ifBlank { "Sent an attachment" })
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FirebaseSync", "Could not start message listener", e)
        }
    }

    fun stopListeningToConversation() {
        messageListener?.remove()
        messageListener = null
        currentActiveConvId = null
    }

    fun pushUser(user: UserEntity) {
        val db = firestore ?: return
        val userMap = hashMapOf(
            "username" to user.username,
            "displayName" to user.displayName,
            "email" to user.email,
            "bio" to user.bio,
            "status" to user.status,
            "avatarUri" to (user.avatarUri ?: ""),
            "gradientIndex" to user.gradientIndex,
            "googleEmail" to (user.googleEmail ?: ""),
            "isGoogleAuthEnabled" to user.isGoogleAuthEnabled,
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(user.id)
            .set(userMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FirebaseSync", "User synced: ${user.username}")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseSync", "Failed to sync user", e)
            }
    }

    fun pushConversation(conv: ConversationEntity) {
        val db = firestore ?: return
        val convMap = hashMapOf(
            "name" to conv.name,
            "isGroup" to conv.isGroup,
            "gradientIndex" to conv.gradientIndex,
            "lastMessage" to conv.lastMessage,
            "lastMessageTime" to conv.lastMessageTime,
            "description" to (conv.description ?: ""),
            "avatarUri" to (conv.avatarUri ?: ""),
            "directUserId" to (conv.directUserId ?: ""),
            "updatedAt" to System.currentTimeMillis()
        )

        db.collection("conversations").document(conv.id)
            .set(convMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FirebaseSync", "Conversation synced: ${conv.id}")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseSync", "Failed to sync conversation", e)
            }
    }

    fun pushMessage(message: MessageEntity) {
        val db = firestore ?: return
        val msgMap = hashMapOf(
            "senderId" to message.senderId,
            "senderName" to message.senderName,
            "senderAvatarUri" to (message.senderAvatarUri ?: ""),
            "senderGradientIndex" to message.senderGradientIndex,
            "text" to message.text,
            "mediaType" to message.mediaType,
            "mediaUri" to (message.mediaUri ?: ""),
            "audioDurationSec" to message.audioDurationSec,
            "timestamp" to message.timestamp,
            "status" to message.status
        )

        db.collection("conversations")
            .document(message.conversationId)
            .collection("messages")
            .document(message.id)
            .set(msgMap, SetOptions.merge())
            .addOnSuccessListener {
                _syncStatus.value = _syncStatus.value.copy(
                    totalSyncedMessages = _syncStatus.value.totalSyncedMessages + 1,
                    lastSyncTime = System.currentTimeMillis()
                )
                Log.d("FirebaseSync", "Message pushed: ${message.id}")
            }
            .addOnFailureListener { e ->
                Log.w("FirebaseSync", "Failed to push message", e)
            }

        val updateMap = hashMapOf<String, Any>(
            "lastMessage" to when (message.mediaType) {
                "IMAGE" -> "📷 Photo shared"
                "AUDIO" -> "🎙️ Voice note (${message.audioDurationSec}s)"
                else -> message.text
            },
            "lastMessageTime" to message.timestamp
        )
        db.collection("conversations").document(message.conversationId)
            .set(updateMap, SetOptions.merge())
    }

    fun release() {
        activeRegistrations.forEach { it.remove() }
        activeRegistrations.clear()
        messageListener?.remove()
        messageListener = null
    }
}
