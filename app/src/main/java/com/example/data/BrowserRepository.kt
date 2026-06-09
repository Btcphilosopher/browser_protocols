package com.example.data

import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val database: BrowserDatabase) {
    val tabDao = database.webTabDao()
    val bookmarkDao = database.webBookmarkDao()
    val logDao = database.webActivityLogDao()

    val allTabs: Flow<List<WebTab>> = tabDao.getAllTabsFlow()
    val allBookmarks: Flow<List<WebBookmark>> = bookmarkDao.getAllBookmarksFlow()
    val allLogs: Flow<List<WebActivityLog>> = logDao.getAllLogsFlow()

    suspend fun getActiveTab(): WebTab? = tabDao.getActiveTab()

    suspend fun insertTab(tab: WebTab): Long = tabDao.insertTab(tab)

    suspend fun updateTab(tab: WebTab) = tabDao.updateTab(tab)

    suspend fun deleteTab(tab: WebTab) = tabDao.deleteTab(tab)

    suspend fun setActiveTab(tabId: Int) = tabDao.setActiveTab(tabId)

    suspend fun insertBookmark(bookmark: WebBookmark) = bookmarkDao.insertBookmark(bookmark)

    suspend fun deleteBookmark(bookmark: WebBookmark) = bookmarkDao.deleteBookmark(bookmark)

    suspend fun bookmarkExists(url: String): Boolean = bookmarkDao.bookmarkExists(url)

    suspend fun insertLog(log: WebActivityLog) = logDao.insertLog(log)

    suspend fun clearLogs() = logDao.clearAllLogs()
}
