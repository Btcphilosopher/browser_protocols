package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [WebTab::class, WebBookmark::class, WebActivityLog::class], version = 1, exportSchema = false)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun webTabDao(): WebTabDao
    abstract fun webBookmarkDao(): WebBookmarkDao
    abstract fun webActivityLogDao(): WebActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "web3_browser_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDb(database)
                }
            }
        }

        suspend fun populateDb(db: BrowserDatabase) {
            val tabDao = db.webTabDao()
            val bookmarkDao = db.webBookmarkDao()
            val logDao = db.webActivityLogDao()

            // Root Tab default list
            tabDao.insertTab(WebTab(title = "Aureom Portal", url = "aureom://home", isActive = true, iconEmoji = "🪐", trustScore = 100, isVerified = true, isSecure = true))
            tabDao.insertTab(WebTab(title = "LN-Sats Store", url = "aureom://ln-sats-store", isActive = false, iconEmoji = "⚡", trustScore = 98, isVerified = true, isSecure = true))
            tabDao.insertTab(WebTab(title = "Identity Center", url = "aureom://auth-id", isActive = false, iconEmoji = "🆔", trustScore = 100, isVerified = true, isSecure = true))
            tabDao.insertTab(WebTab(title = "Node Console", url = "aureom://node-console", isActive = false, iconEmoji = "🖥️", trustScore = 95, isVerified = false, isSecure = true))

            // Bookmark default list
            bookmarkDao.insertBookmark(WebBookmark(title = "Aureom.ai Official Portal", url = "aureom://home"))
            bookmarkDao.insertBookmark(WebBookmark(title = "Lightning Network Storefront", url = "aureom://ln-sats-store"))
            bookmarkDao.insertBookmark(WebBookmark(title = "MemPool Live", url = "https://mempool.space"))
            bookmarkDao.insertBookmark(WebBookmark(title = "Bitcoiner Base Guide", url = "aureom://node-console"))

            // Log entries default layout
            logDao.insertLog(WebActivityLog(
                actionTitle = "Web3 Sandbox Initialized",
                subtitle = "Active memory partition encrypted with AES-256",
                category = "Network",
                status = "Verified",
                txHash = "Sandbox Active"
            ))
            logDao.insertLog(WebActivityLog(
                actionTitle = "Peer-to-Peer Node Connected",
                subtitle = "Established link to 31 Bitcoin core nodes",
                category = "Network",
                status = "Settled",
                txHash = "Block #847,921"
            ))
            logDao.insertLog(WebActivityLog(
                actionTitle = "Aureom DID Verification",
                subtitle = "Signature matches master DID record 'tom@aureom'",
                category = "Identity",
                status = "Verified",
                txHash = "bc1q9s3h...88f"
            ))
            logDao.insertLog(WebActivityLog(
                actionTitle = "Wallet Address Synced",
                subtitle = "Derivation path m/84'/0'/0' loaded successfully",
                category = "Wallet",
                status = "Settled",
                txHash = "Verified"
            ))
        }
    }
}
