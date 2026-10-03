package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.MessageEntity
import com.example.data.model.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class FirebaseStatus {
    object Unconfigured : FirebaseStatus()
    object Initializing : FirebaseStatus()
    data class Connected(
        val isAuthSignedIn: Boolean,
        val userEmail: String?,
        val firestoreActive: Boolean
    ) : FirebaseStatus()
    data class Error(val message: String) : FirebaseStatus()
}

class FirebaseManager(private val context: Context) {
    private val TAG = "FirebaseManager"

    private val _status = MutableStateFlow<FirebaseStatus>(FirebaseStatus.Unconfigured)
    val status: StateFlow<FirebaseStatus> = _status.asStateFlow()

    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var activeMessageListener: ListenerRegistration? = null

    init {
        initialize()
    }

    private fun initialize() {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isNotEmpty()) {
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                updateStatus()
                Log.d(TAG, "Firebase successfully initialized with default app")
            } else {
                _status.value = FirebaseStatus.Unconfigured
                Log.i(TAG, "Firebase not configured yet: waiting for google-services.json or Firebase project setup")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped or pending setup: ${e.message}")
            _status.value = FirebaseStatus.Unconfigured
        }
    }

    private fun updateStatus() {
        val currentAuth = auth
        val currentFirestore = firestore
        if (currentAuth != null && currentFirestore != null) {
            val user = currentAuth.currentUser
            _status.value = FirebaseStatus.Connected(
                isAuthSignedIn = user != null,
                userEmail = user?.email,
                firestoreActive = true
            )
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            return@withContext Result.failure(
                IllegalStateException("Firebase Auth is pending project setup. Connect Firebase in AI Studio to enable cloud auth.")
            )
        }

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user
            updateStatus()
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No FirebaseUser returned after sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserToFirestore(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not yet connected")
        )

        try {
            val userMap = hashMapOf(
                "id" to user.id,
                "username" to user.username,
                "displayName" to user.displayName,
                "email" to user.email,
                "bio" to user.bio,
                "status" to user.status,
                "gradientIndex" to user.gradientIndex,
                "avatarUri" to (user.avatarUri ?: ""),
                "googleEmail" to (user.googleEmail ?: ""),
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection("users").document(user.id)
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncMessageToFirestore(message: MessageEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext Result.failure(
            IllegalStateException("Firestore is not yet connected")
        )

        try {
            val messageMap = hashMapOf(
                "id" to message.id,
                "conversationId" to message.conversationId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderGradientIndex" to message.senderGradientIndex,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "mediaType" to message.mediaType,
                "mediaUri" to (message.mediaUri ?: ""),
                "audioDurationSec" to message.audioDurationSec
            )

            db.collection("conversations")
                .document(message.conversationId)
                .collection("messages")
                .document(message.id)
                .set(messageMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun startListeningToFirestoreMessages(
        conversationId: String,
        onNewMessageReceived: (MessageEntity) -> Unit
    ) {
        activeMessageListener?.remove()
        val db = firestore ?: return

        activeMessageListener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                for (doc in snapshot.documents) {
                    val id = doc.getString("id") ?: doc.id
                    val text = doc.getString("text") ?: ""
                    val senderId = doc.getString("senderId") ?: ""
                    val senderName = doc.getString("senderName") ?: "Unknown"
                    val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    val mediaType = doc.getString("mediaType") ?: "TEXT"
                    val mediaUri = doc.getString("mediaUri")
                    val duration = doc.getLong("audioDurationSec")?.toInt() ?: 0

                    val msg = MessageEntity(
                        id = id,
                        conversationId = conversationId,
                        senderId = senderId,
                        senderName = senderName,
                        senderGradientIndex = doc.getLong("senderGradientIndex")?.toInt() ?: 0,
                        text = text,
                        timestamp = timestamp,
                        isSentByMe = false,
                        mediaType = mediaType,
                        mediaUri = mediaUri,
                        audioDurationSec = duration
                    )
                    onNewMessageReceived(msg)
                }
            }
    }

    fun stopListening() {
        activeMessageListener?.remove()
        activeMessageListener = null
    }

    fun signOut() {
        auth?.signOut()
        updateStatus()
    }
}
