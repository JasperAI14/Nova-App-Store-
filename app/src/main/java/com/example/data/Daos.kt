package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY rating DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE id = :id")
    suspend fun getAppById(id: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllApps(apps: List<AppEntity>)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET isInstalled = :isInstalled WHERE id = :id")
    suspend fun updateAppInstallStatus(id: String, isInstalled: Boolean)

    @Query("UPDATE apps SET downloads = downloads + 1 WHERE id = :id")
    suspend fun incrementDownloads(id: String)

    @Query("DELETE FROM apps WHERE id = :id")
    suspend fun deleteAppById(id: String)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE appId = :appId ORDER BY timestamp DESC")
    fun getReviewsForApp(appId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews WHERE appId = :appId ORDER BY timestamp DESC")
    suspend fun getReviewsForAppDirect(appId: String): List<ReviewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}

@Dao
interface GeneratedImageDao {
    @Query("SELECT * FROM generated_images WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getAllImages(email: String): Flow<List<GeneratedImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: GeneratedImageEntity)

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteImageById(id: Int)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles WHERE email = :email")
    suspend fun getProfileByEmail(email: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles")
    fun getAllProfilesFlow(): Flow<List<UserProfileEntity>>
}

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_session WHERE id = 1")
    fun getUserSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_session WHERE id = 1")
    suspend fun getUserSessionDirect(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserSession(session: UserSessionEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getBookmarksForUser(email: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE appId = :appId AND userEmail = :email")
    suspend fun deleteBookmark(appId: String, email: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE appId = :appId AND userEmail = :email LIMIT 1)")
    suspend fun isBookmarked(appId: String, email: String): Boolean
}

@Dao
interface ReferralDao {
    @Query("SELECT * FROM referrals ORDER BY timestamp DESC")
    fun getAllReferralsFlow(): Flow<List<ReferralEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: ReferralEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM referrals WHERE referredEmail = :email LIMIT 1)")
    suspend fun isReferred(email: String): Boolean

    @Query("SELECT COUNT(*) FROM referrals")
    suspend fun getReferralCount(): Int
}


