package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "web_tabs")
data class WebTab(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val isActive: Boolean,
    val iconEmoji: String,
    val trustScore: Int,
    val isVerified: Boolean,
    val isSecure: Boolean
)

@Entity(tableName = "web_bookmarks")
data class WebBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "web_activity_logs")
data class WebActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionTitle: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String, // "Wallet", "Identity", "Network", "Browse"
    val status: String,    // "Settled", "Pending", "Verified", "Warning"
    val txHash: String
)
