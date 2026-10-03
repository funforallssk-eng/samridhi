package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUri: String? = null,
    val senderGradientIndex: Int = 0,
    val text: String = "",
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "AUDIO"
    val mediaUri: String? = null,
    val audioDurationSec: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isSentByMe: Boolean = false,
    val status: String = "DELIVERED" // "SENDING", "SENT", "DELIVERED", "READ"
)
