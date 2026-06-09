package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WebTabDao {
    @Query("SELECT * FROM web_tabs")
    fun getAllTabsFlow(): Flow<List<WebTab>>

    @Query("SELECT * FROM web_tabs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTab(): WebTab?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: WebTab): Long

    @Update
    suspend fun updateTab(tab: WebTab)

    @Delete
    suspend fun deleteTab(tab: WebTab)

    @Query("DELETE FROM web_tabs")
    suspend fun deleteAllTabs()

    @Transaction
    suspend fun setActiveTab(tabId: Int) {
        // Deactivate all, then set this one active
        val tabs = getRawAll()
        for (t in tabs) {
            if (t.id == tabId) {
                updateTab(t.copy(isActive = true))
            } else if (t.isActive) {
                updateTab(t.copy(isActive = false))
            }
        }
    }

    @Query("SELECT * FROM web_tabs")
    suspend fun getRawAll(): List<WebTab>
}

@Dao
interface WebBookmarkDao {
    @Query("SELECT * FROM web_bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): Flow<List<WebBookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: WebBookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: WebBookmark)

    @Query("SELECT EXISTS(SELECT 1 FROM web_bookmarks WHERE url = :url)")
    suspend fun bookmarkExists(url: String): Boolean
}

@Dao
interface WebActivityLogDao {
    @Query("SELECT * FROM web_activity_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<WebActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WebActivityLog)

    @Query("DELETE FROM web_activity_logs")
    suspend fun clearAllLogs()
}
