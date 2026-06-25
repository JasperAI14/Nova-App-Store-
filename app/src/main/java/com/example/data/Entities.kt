package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val logoUrl: String, // can be a local drawable name (e.g. "ic_launcher_foreground") or a URL
    val category: String,
    val isGame: Boolean,
    val version: String,
    val developer: String,
    val downloads: Int,
    val rating: Float,
    val isInstalled: Boolean,
    val status: String, // "Approved", "Pending", "Rejected"
    val sizeMb: Float,
    val apkFileName: String,
    val isUserUploaded: Boolean = false,
    val scanResult: String = "Clean (0/72 engines flagged)" // VirusTotal simulated scanning result
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val base64Data: String,
    val providerUsed: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_session")
data class UserSessionEntity(
    @PrimaryKey val id: Int = 1,
    val isLoggedIn: Boolean = false,
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val isPremium: Boolean = false,
    val sharesCount: Int = 0,
    val bonusGenerations: Int = 0,
    val dailyGenerationsUsed: Int = 0,
    val lastResetTimestamp: Long = System.currentTimeMillis(),
    val promoCode: String = "",
    val dailyUploadsUsed: Int = 0
)
