package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BrowserDatabase.getDatabase(application, viewModelScope)
    val repository = BrowserRepository(database)

    // Room Flows
    val allTabs: StateFlow<List<WebTab>> = repository.allTabs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<WebBookmark>> = repository.allBookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<WebActivityLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Tab tracking
    val activeTab: StateFlow<WebTab?> = allTabs
        .map { tabs -> tabs.find { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Theme & Styling state
    var selectedAccentGradientsIndex = MutableStateFlow(0) // 0: Classic Aureom (amber-magenta-violet), 1: Cyberpunk (cyan-magenta), 2: Bitcoin Gold

    // Wallet State
    val walletConnected = MutableStateFlow(true)
    val satsBalance = MutableStateFlow(24580200L) // 24.580200 BTC in Sats
    val btcAddress = MutableStateFlow("bc1q9s3h6m8v7ux69pa6qlnwre4dcp0ztsn88f6hde")
    val lightningChannelCount = MutableStateFlow(6)
    val lightningCapacity = MutableStateFlow(8200000L) // 0.082 BTC
    val activePeersCount = MutableStateFlow(32)
    val currentBlockHeight = MutableStateFlow(847924L)

    // UI overlays
    val rightPanelOpen = MutableStateFlow(true) // Open by default on widescreen, toggleable
    val settingsDrawerOpen = MutableStateFlow(false)
    val currentUrlInput = MutableStateFlow("")

    // Transaction payment modal detail
    data class PaymentInvoice(
        val itemTitle: String,
        val recipient: String,
        val invoiceHash: String,
        val amountSats: Long,
        val feeRateSatVb: Int = 18,
        val description: String
    )
    val activePaymentInvoice = MutableStateFlow<PaymentInvoice?>(null)
    val paymentStatus = MutableStateFlow("") // "", "PENDING", "SETTLED"

    // Settings preferences
    val sandboxIsolation = MutableStateFlow("Strict (Encrypted Partition)")
    val zeroKnowledgeDNS = MutableStateFlow("On (Aureom Private Router)")
    val activeChainNetwork = MutableStateFlow("Lightning / Bitcoin Mainnet")
    val secureShields = MutableStateFlow("Strict Tracker Shield")

    // Simulation Live Feed
    private var nodeFeedJob: Job? = null

    init {
        // Collect active tab to update the Address bar URL field
        viewModelScope.launch {
            activeTab.collect { tab ->
                if (tab != null) {
                    currentUrlInput.value = tab.url
                }
            }
        }
        startMempoolNodeSimulation()
    }

    fun startMempoolNodeSimulation() {
        nodeFeedJob?.cancel()
        nodeFeedJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(12000 + Random.nextLong(8000, 15000))
                if (walletConnected.value) {
                    // Random block or transaction log
                    val type = Random.nextInt(4)
                    when (type) {
                        0 -> {
                            currentBlockHeight.value += 1
                            repository.insertLog(WebActivityLog(
                                actionTitle = "New Block Mined",
                                subtitle = "Block #${currentBlockHeight.value} solved. Hash: 0000...fc2",
                                category = "Network",
                                status = "Settled",
                                txHash = "Height: ${currentBlockHeight.value}"
                            ))
                        }
                        1 -> {
                            val fee = Random.nextInt(12, 35)
                            repository.insertLog(WebActivityLog(
                                actionTitle = "Mempool Fee Updated",
                                subtitle = "Median transaction fee adjusted to $fee sat/vB.",
                                category = "Network",
                                status = "Verified",
                                txHash = "Sats/vB: $fee"
                            ))
                        }
                        2 -> {
                            val peerDelta = if (Random.nextBoolean()) 1 else -1
                            activePeersCount.value = (activePeersCount.value + peerDelta).coerceIn(12, 48)
                            repository.insertLog(WebActivityLog(
                                actionTitle = "Peer Topology Mutated",
                                subtitle = "Active network peers: ${activePeersCount.value} connected links",
                                category = "Network",
                                status = "Verified",
                                txHash = "Sync: 100%"
                            ))
                        }
                        3 -> {
                            // Lightning node query rebalanced
                            val capacitySats = Random.nextLong(2000, 15000)
                            repository.insertLog(WebActivityLog(
                                actionTitle = "LN Channel Rebalanced",
                                subtitle = "Rebalanced channel with Node_${Random.nextInt(100)}: +$capacitySats sats",
                                category = "Wallet",
                                status = "Settled",
                                txHash = "Success"
                            ))
                        }
                    }
                }
            }
        }
    }

    // Actions
    fun handleUrlSearchString(url: String) {
        val cleanUrl = url.trim().lowercase()
        if (cleanUrl.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val active = activeTab.value
            if (active != null) {
                // Determine trust indicators based on url
                val emoji = when {
                    cleanUrl.contains("ln-sats-store") || cleanUrl.contains("store") -> "⚡"
                    cleanUrl.contains("auth-id") || cleanUrl.contains("identity") -> "🆔"
                    cleanUrl.contains("node-console") || cleanUrl.contains("node") -> "🖥️"
                    cleanUrl.contains("home") || cleanUrl.contains("portal") -> "🪐"
                    else -> "🌐"
                }
                val isVerified = cleanUrl.startsWith("aureom://") && !cleanUrl.contains("node")
                val trustScore = when {
                    cleanUrl.contains("home") || cleanUrl.contains("auth-id") -> 100
                    cleanUrl.contains("ln-sats-store") -> 98
                    cleanUrl.contains("node") -> 95
                    else -> 45 // normal external web3 site
                }
                val title = when {
                    cleanUrl.contains("home") -> "Aureom Portal"
                    cleanUrl.contains("ln-sats-store") -> "LN-Sats Store"
                    cleanUrl.contains("auth-id") -> "Identity Center"
                    cleanUrl.contains("node") -> "Node Console"
                    else -> "Decentralized Host: $cleanUrl"
                }

                repository.updateTab(active.copy(
                    title = title,
                    url = cleanUrl,
                    iconEmoji = emoji,
                    trustScore = trustScore,
                    isVerified = isVerified,
                    isSecure = cleanUrl.startsWith("aureom://") || cleanUrl.startsWith("https://")
                ))

                // Log navigation
                repository.insertLog(WebActivityLog(
                    actionTitle = "Dispatched Request",
                    subtitle = "Domain localized: $cleanUrl via Aureom DNS Protocol",
                    category = "Browse",
                    status = "Settled",
                    txHash = "GET $cleanUrl"
                ))
            }
        }
    }

    fun setTabSelected(tabId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveTab(tabId)
        }
    }

    fun addNewTab() {
        viewModelScope.launch(Dispatchers.IO) {
            // Uncheck previous active, insert new home tab
            val tabId = repository.insertTab(WebTab(
                title = "Aureom Portal",
                url = "aureom://home",
                isActive = false,
                iconEmoji = "🪐",
                trustScore = 100,
                isVerified = true,
                isSecure = true
            ))
            repository.setActiveTab(tabId.toInt())
            // Log it
            repository.insertLog(WebActivityLog(
                actionTitle = "Created Sandbox Frame",
                subtitle = "Active frame partitioned #$tabId",
                category = "Browse",
                status = "Verified",
                txHash = "Allocated"
            ))
        }
    }

    fun closeTab(tab: WebTab) {
        viewModelScope.launch(Dispatchers.IO) {
            val tabs = allTabs.value
            if (tabs.size <= 1) return@launch // keep at least one tab

            repository.deleteTab(tab)

            // If the deleted tab was active, set another tab active
            if (tab.isActive) {
                val remaining = tabs.filter { it.id != tab.id }
                if (remaining.isNotEmpty()) {
                    repository.setActiveTab(remaining.first().id)
                }
            }

            repository.insertLog(WebActivityLog(
                actionTitle = "Destroyed Frame Sandbox",
                subtitle = "Formally terminated partition ${tab.title}",
                category = "Browse",
                status = "Verified",
                txHash = "Deallocated"
            ))
        }
    }

    fun toggleBookmark(tab: WebTab) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = repository.bookmarkExists(tab.url)
            if (exists) {
                // Delete
                val filtered = bookmarks.value.find { it.url == tab.url }
                if (filtered != null) {
                    repository.deleteBookmark(filtered)
                    repository.insertLog(WebActivityLog(
                        actionTitle = "Removed Bookmark Ref",
                        subtitle = "Freed database index for ${tab.title}",
                        category = "Browse",
                        status = "Warning",
                        txHash = "Drop"
                    ))
                }
            } else {
                // Save
                repository.insertBookmark(WebBookmark(title = tab.title, url = tab.url))
                repository.insertLog(WebActivityLog(
                    actionTitle = "Indexed Web3 Landmark",
                    subtitle = "Cached route: ${tab.title} (${tab.url})",
                    category = "Browse",
                    status = "Verified",
                    txHash = "Bookmarked"
                ))
            }
        }
    }

    // Interactive dApp items functions

    fun triggerPaymentInvoice(title: String, costSats: Long, desc: String) {
        if (!walletConnected.value) return
        val randomInvoiceHash = "lnbc" + costSats + "n1p" + "0".repeat(16) + (Random.nextInt(100000, 999999)) + "d3"
        activePaymentInvoice.value = PaymentInvoice(
            itemTitle = title,
            recipient = "Aureom.ai App Broker (Merchant ID: 41922-A)",
            invoiceHash = randomInvoiceHash,
            amountSats = costSats,
            description = desc
        )
        paymentStatus.value = "PENDING"
    }

    fun confirmPayment(feeRate: Int) {
        val invoice = activePaymentInvoice.value ?: return
        paymentStatus.value = "PROCESSING"
        viewModelScope.launch(Dispatchers.IO) {
            delay(1800) // Beautiful simulated on-chain processing/mining animation
            val totalCost = invoice.amountSats + (feeRate * 125L) // cost + network fee
            if (satsBalance.value >= totalCost) {
                satsBalance.value -= totalCost
                paymentStatus.value = "SETTLED"
                // Record activity log
                repository.insertLog(WebActivityLog(
                    actionTitle = "Lightning Sats Transferred",
                    subtitle = "Successfully settled to ${invoice.itemTitle} (-${invoice.amountSats} Sats)",
                    category = "Wallet",
                    status = "Settled",
                    txHash = invoice.invoiceHash.take(12) + "..."
                ))
            } else {
                paymentStatus.value = "FAIL_INSUFFICIENT"
                repository.insertLog(WebActivityLog(
                    actionTitle = "Payment Aborted",
                    subtitle = "Insufficient sats balance to complete Lightning invoice",
                    category = "Wallet",
                    status = "Warning",
                    txHash = "Rejected"
                ))
            }
        }
    }

    fun cancelActivePayment() {
        activePaymentInvoice.value = null
        paymentStatus.value = ""
    }

    // Decentralized Identity Custom registration
    fun anchorNewDID(customName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog(WebActivityLog(
                actionTitle = "DID Registration Request",
                subtitle = "Compiling ZK key-pair proof for '$customName'",
                category = "Identity",
                status = "Pending",
                txHash = "Anchoring..."
            ))
            delay(2000)
            repository.insertLog(WebActivityLog(
                actionTitle = "Sovereign DID Anchored",
                subtitle = "Root DID did:aureom:$customName anchored to block #${currentBlockHeight.value}",
                category = "Identity",
                status = "Verified",
                txHash = "did:aureom:$customName"
            ))
        }
    }

    fun toggleWalletConnection() {
        walletConnected.value = !walletConnected.value
        viewModelScope.launch(Dispatchers.IO) {
            val statusStr = if (walletConnected.value) "Re-Anchored Hot Sigs" else "Zero-Trust Hot-Signature Keys Severed"
            repository.insertLog(WebActivityLog(
                actionTitle = if (walletConnected.value) "Wallet Port Open" else "Wallet Airground Isolation Mode",
                subtitle = statusStr,
                category = "Wallet",
                status = if (walletConnected.value) "Verified" else "Warning",
                txHash = if (walletConnected.value) "bc1q9s3h" else "Air-Gapped"
            ))
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    override fun onCleared() {
        super.onCleared()
        nodeFeedJob?.cancel()
    }
}
