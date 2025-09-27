package com.example.recordatoriomodelo2.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val firebaseUid: String, // UID de Firebase como clave primaria
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: String,
    val lastLoginAt: String
)