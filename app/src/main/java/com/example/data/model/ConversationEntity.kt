package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isGroup: Boolean = false,
    val avatarUri: String? = null,
    val gradientIndex: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val description: String? = null,
    val directUserId: String? = null // if isGroup == false, id of the other user
)
