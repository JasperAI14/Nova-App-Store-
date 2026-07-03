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
    val scanResult: String = "Clean (0/72 engines flagged)", // VirusTotal simulated scanning result
    val screenshotsCsv: String = "",
    val uploadedByEmail: String = "",
    val isDownloaded: Boolean = false,
    val installedVersion: String = "",
    val packageName: String = "",
    val versionCode: Int = 1,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val permissionsCsv: String = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE",
    val shortDescription: String = "",
    val supportWebsite: String = "",
    val privacyPolicy: String = "",
    val tagsCsv: String = "",
    val releaseNotes: String = "",
    val publishDate: Long = System.currentTimeMillis(),
    val keepOlderVersions: Boolean = false,
    val historyVersionsJson: String = "[]",
    val aiAutoRepliesEnabled: Boolean = false,
    val aiReviewTone: String = "Friendly",
    val aiTrainingExamplesCsv: String = "",
    val aiReviewBeforePosting: Boolean = true,
    val isReported: Boolean = false,
    val reportReason: String = ""
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: String,
    val authorName: String,
    val rating: Int,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis(),
    val developerReply: String? = null,
    val aiProposedReply: String? = null,
    val isReported: Boolean = false,
    val reportedReason: String? = null
)

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val base64Data: String,
    val providerUsed: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String = ""
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val avatarUrl: String,
    val isPremium: Boolean = false,
    val sharesCount: Int = 0,
    val bonusGenerations: Int = 0,
    val dailyGenerationsUsed: Int = 0,
    val lastResetTimestamp: Long = System.currentTimeMillis(),
    val promoCode: String = "",
    val dailyUploadsUsed: Int = 0
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

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: String,
    val userEmail: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val referrerEmail: String,
    val referredEmail: String,
    val timestamp: Long = System.currentTimeMillis()
)


