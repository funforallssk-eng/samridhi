package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = false),
        Index(value = ["email"], unique = false)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val email: String = "",
    val password: String? = null,
    val avatarUri: String? = null,
    val bio: String = "",
    val status: String = "Online",
    val isCurrentUser: Boolean = false,
    val gradientIndex: Int = 0,
    val googleEmail: String? = null,
    val isGoogleAuthEnabled: Boolean = false,
    val googleAuthSecret: String = "JBSWY3DPEHPK3PXP"
)
