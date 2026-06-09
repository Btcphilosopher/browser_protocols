package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val tabs by viewModel.allTabs.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val walletConnected by viewModel.walletConnected.collectAsState()
    val satsBalance by viewModel.satsBalance.collectAsState()
    val activePeers by viewModel.activePeersCount.collectAsState()
    val blockHeight by viewModel.currentBlockHeight.collectAsState()
    val rightPanelOpen by viewModel.rightPanelOpen.collectAsState()
    val settingsDrawerOpen by viewModel.settingsDrawerOpen.collectAsState()
    val urlInput by viewModel.currentUrlInput.collectAsState()
    val activePaymentInvoice by viewModel.activePaymentInvoice.collectAsState()
    val paymentStatus by viewModel.paymentStatus.collectAsState()
    val accentIndex by viewModel.selectedAccentGradientsIndex.collectAsState()

    // Determine layout scale based on orientation
    val configuration = LocalConfiguration.current
    val isWidescreen = configuration.screenWidthDp >= 800

    // Accent Gradient setups
    val gradients = listOf(
        listOf(AureomGold, AmberGlow, NeonMagenta, VioletLuminous), // Classic Aureom
        listOf(HighTechCyan, NeonMagenta, VioletLuminous),         // Cyberpunk Neon
        listOf(AureomGold, AmberGlow, Color(0xFFF39C12))            // Bitcoin Gold
    )
    val activeGradient = gradients[accentIndex.coerceIn(0, gradients.lastIndex)]
    val brandGradientBrush = Brush.horizontalGradient(activeGradient)

    // Layout configuration
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AmbientDarkBg)
            .drawBehind {
                val width = size.width
                val height = size.height

                // Radial blurred glow spots from Design HTML
                // top 1/4 left 1/2: bg-violet-600/10 rounded-full blur-[100px]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VioletLuminous.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(width * 0.5f, height * 0.25f),
                        radius = maxOf(width, height) * 0.4f
                    ),
                    center = Offset(width * 0.5f, height * 0.25f),
                    radius = maxOf(width, height) * 0.4f
                )

                // bottom 1/4 right: bg-cyan-600/10 rounded-full blur-[80px]
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(HighTechCyan.copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(width * 0.9f, height * 0.75f),
                        radius = maxOf(width, height) * 0.35f
                    ),
                    center = Offset(width * 0.9f, height * 0.75f),
                    radius = maxOf(width, height) * 0.35f
                )

                // Subtle dotted minimalist grid from HTML: white 1px circle, transparent 1px, size 24px, opacity 3%
                val dotSpacing = 24.dp.toPx()
                val dotRadius = 1.dp.toPx()
                var x = 0f
                while (x < width) {
                    var y = 0f
                    while (y < height) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.04f),
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += dotSpacing
                    }
                    x += dotSpacing
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP BROWSER ADDRESS AND CONTROL BAR
            BrowserTopNavigation(
                viewModel = viewModel,
                tabs = tabs,
                activeTab = activeTab,
                urlInput = urlInput,
                walletConnected = walletConnected,
                satsBalance = satsBalance,
                blockHeight = blockHeight,
                brandGradientBrush = brandGradientBrush,
                isWidescreen = isWidescreen
            )

            // Dynamic Tabs bar inside navigation if mobile or widescreen
            BrowserTabsHeader(
                tabs = tabs,
                activeTab = activeTab,
                onTabSelect = { viewModel.setTabSelected(it.id) },
                onAddTab = { viewModel.addNewTab() },
                onCloseTab = { viewModel.closeTab(it) },
                activeGradient = activeGradient
            )

            // MAIN WORKSPACE (SPLIT SECTION FOR WIDESCREEN OR STANDARD COMPACT VIEW)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // LEFT dAPP EMBEDDED ENGINE WORKSPACE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (activeTab != null) {
                        AnimatedContent(
                            targetState = activeTab!!.url,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith
                                        fadeOut(animationSpec = tween(250))
                            },
                            label = "TabContentTransition"
                        ) { targetUrl ->
                            when {
                                targetUrl.contains("home") || targetUrl.isEmpty() -> {
                                    AureomPortalHomepage(
                                        viewModel = viewModel,
                                        activeGradient = activeGradient,
                                        brandGradientBrush = brandGradientBrush,
                                        onNavigate = { viewModel.handleUrlSearchString(it) }
                                    )
                                }
                                targetUrl.contains("ln-sats-store") -> {
                                    LNSatsStore(
                                        viewModel = viewModel,
                                        activeGradient = activeGradient,
                                        walletConnected = walletConnected,
                                        onPayInvoice = { title, cost, desc ->
                                            viewModel.triggerPaymentInvoice(title, cost, desc)
                                        }
                                    )
                                }
                                targetUrl.contains("auth-id") -> {
                                    AureomIdentityCenter(
                                        viewModel = viewModel,
                                        activeGradient = activeGradient,
                                        brandGradientBrush = brandGradientBrush
                                    )
                                }
                                targetUrl.contains("node-console") -> {
                                    NodeConsoleCenter(
                                        viewModel = viewModel,
                                        activeGradient = activeGradient,
                                        peersCount = activePeers,
                                        blockHeight = blockHeight
                                    )
                                }
                                else -> {
                                    // Custom URL loaded sandbox interface
                                    ExternalWeb3Sandbox(
                                        url = targetUrl,
                                        tab = activeTab!!,
                                        activeGradient = activeGradient
                                    )
                                }
                            }
                        }
                    } else {
                        // Empty states placeholder
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AureomGold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Booting Web.3.0 Sandbox Container...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // RIGHT ACTIVITY SIDEBAR (REACTIVE PIPELINE STREAM OR CONDITIONAL MOBILE DRAWERS)
                AnimatedVisibility(
                    visible = rightPanelOpen && isWidescreen,
                    enter = slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                    ) + fadeIn(),
                    exit = slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(200)
                    ) + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .padding(end = 12.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Web3ActivitySidebar(
                            logs = activityLogs,
                            activeGradient = activeGradient,
                            peersCount = activePeers,
                            blockHeight = blockHeight,
                            onClearLogs = { viewModel.clearAllLogs() }
                        )
                    }
                }
            }

            // -------------------------------------------------------------------------
            // DESIGN THEME: CLEAN MINIMALISM STATUS DOCK / FOOTER
            // -------------------------------------------------------------------------
            Surface(
                color = Color.Black.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Very subtle upper glass border
                        drawLine(
                            color = BorderGlassWhite,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1f
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left Info
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "NETWORK STATUS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.3f),
                                letterSpacing = 2.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "UPTIME: 99.9%",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = HyperEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                )
                                Text(
                                    text = "SYNC: 1.2s",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Right Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Toggle Menu button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x0EFFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                    .clickable {
                                        viewModel.rightPanelOpen.value = !viewModel.rightPanelOpen.value
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "☰",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Circular ADD tab button
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(brandGradientBrush)
                                    .clickable {
                                        viewModel.addNewTab()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Tab Sandbox",
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Progress loading bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color(0x0FFFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.66f)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(AureomGold, HighTechCyan)))
                        )
                    }
                }
            }
        }

        // 2. LIGHTNING ON-CHAIN TRANSACTION CONFIRMATION OVERLAY
        AnimatedVisibility(
            visible = activePaymentInvoice != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            activePaymentInvoice?.let { invoice ->
                PaymentConfirmationDialog(
                    invoice = invoice,
                    status = paymentStatus,
                    activeGradient = activeGradient,
                    blockHeight = blockHeight,
                    onConfirm = { fee -> viewModel.confirmPayment(fee) },
                    onDismiss = { viewModel.cancelActivePayment() }
                )
            }
        }

        // 3. SETTINGS & PRIVACY DRAWER
        AnimatedVisibility(
            visible = settingsDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            SettingsPrivacyDrawer(
                viewModel = viewModel,
                accentIndex = accentIndex,
                onClose = { viewModel.settingsDrawerOpen.value = false }
            )
        }

        // FLOATING RESPONSIVE TOGGLE FOR SIDEBAR (ON COMPACT MOBILE SCREENS OR COLLAPSED)
        if (!isWidescreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { viewModel.rightPanelOpen.value = !viewModel.rightPanelOpen.value },
                    containerColor = PanelBlack,
                    contentColor = AureomGold,
                    modifier = Modifier
                        .testTag("mobile_feed_fab")
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(HighTechCyan, NeonMagenta)),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Toggle ledger live stream"
                    )
                }
            }

            // Slide out bottom activity panel for compact viewports
            AnimatedVisibility(
                visible = rightPanelOpen && !isWidescreen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlassOverlay)
                        .clickable { viewModel.rightPanelOpen.value = false },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.7f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(AmbientDarkBg)
                            .border(
                                1.dp,
                                BorderGlassWhite,
                                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .clickable(enabled = false) {}
                    ) {
                        Web3ActivitySidebar(
                            logs = activityLogs,
                            activeGradient = activeGradient,
                            peersCount = activePeers,
                            blockHeight = blockHeight,
                            onClearLogs = { viewModel.clearAllLogs() }
                        )

                        // Close button for mobile sheet
                        IconButton(
                            onClick = { viewModel.rightPanelOpen.value = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TOP BAR COMPOSABLE
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BrowserTopNavigation(
    viewModel: BrowserViewModel,
    tabs: List<WebTab>,
    activeTab: WebTab?,
    urlInput: String,
    walletConnected: Boolean,
    satsBalance: Long,
    blockHeight: Long,
    brandGradientBrush: Brush,
    isWidescreen: Boolean
) {
    val coroutineScope = rememberCoroutineScope()

    Surface(
        color = PanelBlack.copy(alpha = 0.85f),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = BorderGlassWhite,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .shadow(6.dp, ambientColor = Color.Black, spotColor = Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // SANS AND SECURE INDICATORS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // System logo
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10111A))
                            .border(1.dp, brandGradientBrush, CircleShape)
                            .clickable { viewModel.settingsDrawerOpen.value = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A3",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            style = TextStyle(brush = brandGradientBrush)
                        )
                    }

                    if (isWidescreen) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AUREOM.ai",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (walletConnected) HyperEmerald else AmberGlow)
                                )
                            }
                            Text(
                                text = "Web.3.0 Client v1.1",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // CENTRAL HOVERING CAPSULE ADDRESS BAR
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                        .height(36.dp) // Sleeker minimalist height
                        .clip(CircleShape)
                        .background(Color(0x0DFFFFFF)) // Glassmorphic (White/5)
                        .border(
                            0.5.dp,
                            if (activeTab?.isVerified == true) HyperEmerald.copy(alpha = 0.4f) else Color(0x11FFFFFF),
                            CircleShape
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shield Trust Icon
                    Icon(
                        imageVector = if (activeTab?.isVerified == true) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = "Trust status",
                        tint = if (activeTab?.isVerified == true) HyperEmerald else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Address input
                    BasicTextFieldWithoutLabel(
                        value = urlInput,
                        onValueChange = { viewModel.currentUrlInput.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("address_url_input"),
                        textColor = if (activeTab?.isVerified == true) HyperEmerald else TextPrimary,
                        fontFamily = HighTechMonospace,
                        fontSize = 13.sp,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            viewModel.handleUrlSearchString(urlInput)
                        })
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Bookmark star toggle
                    if (activeTab != null) {
                        val isBookmarked = viewModel.bookmarks.collectAsState().value.any { it.url == activeTab.url }
                        IconButton(
                            onClick = { viewModel.toggleBookmark(activeTab) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Star else Icons.Default.Star,
                                contentDescription = "Bookmark Star",
                                tint = if (isBookmarked) AureomGold else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Reload/Navigate Button
                    IconButton(
                        onClick = { viewModel.handleUrlSearchString(urlInput) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Request",
                            tint = HighTechCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // INTEGRATED WALLET STATUS INDICATION
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Balance status
                    Surface(
                        color = Color(0xFF10121D),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .clickable { viewModel.toggleWalletConnection() }
                            .border(
                                width = 1.dp,
                                color = if (walletConnected) AureomGold.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow, // Fallback for Bitcoin icon
                                contentDescription = "Sats",
                                tint = if (walletConnected) AureomGold else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (walletConnected) {
                                    val formatted = String.format("%,d", satsBalance)
                                    "$formatted SATS"
                                } else {
                                    "Wallet AirGapped"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 11.sp,
                                color = if (walletConnected) TextPrimary else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Menu Settings trigger
                    IconButton(
                        onClick = { viewModel.settingsDrawerOpen.value = !viewModel.settingsDrawerOpen.value },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF10121D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings configuration",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CUSTOM TAB SYSTEM HEADER WITH ANIMATED ACTIVE STATE
// -----------------------------------------------------------------------------
@Composable
fun BrowserTabsHeader(
    tabs: List<WebTab>,
    activeTab: WebTab?,
    onTabSelect: (WebTab) -> Unit,
    onAddTab: () -> Unit,
    onCloseTab: (WebTab) -> Unit,
    activeGradient: List<Color>
) {
    ScrollableRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF050505))
            .drawBehind {
                drawLine(
                    color = BorderGlassWhite,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .padding(vertical = 4.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val isActive = tab.isActive
            val backgroundGradient = if (isActive) {
                Brush.horizontalGradient(listOf(Color(0x1BFFFFFF), Color(0x15FFFFFF))) // White/10
            } else {
                Brush.horizontalGradient(listOf(Color(0x0AFFFFFF), Color(0x0AFFFFFF))) // White/4
            }
            val borderGradient = if (isActive) {
                Brush.horizontalGradient(listOf(Color(0x4DFFFFFF), Color(0x33FFFFFF))) // White/30
            } else {
                Brush.horizontalGradient(listOf(Color(0x0DFFFFFF), Color(0x0DFFFFFF))) // White/5
            }

            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundGradient)
                    .border(
                        0.5.dp, // Super-thin border
                        borderGradient,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onTabSelect(tab) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("tab_${tab.id}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = tab.iconEmoji, fontSize = 13.sp)

                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) TextPrimary else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp)
                    )

                    if (tabs.size > 1) {
                        IconButton(
                            onClick = { onCloseTab(tab) },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = if (isActive) TextPrimary.copy(alpha = 0.6f) else TextMuted,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Add Tab Button
        IconButton(
            onClick = { onAddTab() },
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF10121D))
                .border(0.5.dp, BorderGlassWhite, CircleShape)
                .testTag("add_tab_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add workspace sandbox",
                tint = HighTechCyan,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// Helper wrapper for Row that allows scrolling of tabs
@Composable
fun ScrollableRow(
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = verticalAlignment,
        horizontalArrangement = Arrangement.Start,
        content = content
    )
}

// -----------------------------------------------------------------------------
// BASIC TEXT FIELD SANS LABEL (FOR EMBEDDED ADDRESS INPUTS)
// -----------------------------------------------------------------------------
@Composable
fun BasicTextFieldWithoutLabel(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimary,
    fontFamily: FontFamily = FontFamily.SansSerif,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = TextStyle(
            color = textColor,
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal
        ),
        cursorBrush = Brush.verticalGradient(listOf(textColor, textColor)),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true
    )
}

// -----------------------------------------------------------------------------
// TAB PAGE: AUREOM PORTAL HOMEPAGE (`aureom://home`)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AureomPortalHomepage(
    viewModel: BrowserViewModel,
    activeGradient: List<Color>,
    brandGradientBrush: Brush,
    onNavigate: (String) -> Unit
) {
    var searchStr by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "PortalGlowAnimation")
    val currentAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Angle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Aureom Glowing Elegant Brand Mark from Design HTML
        Box(
            modifier = Modifier
                .padding(top = 24.dp, bottom = 12.dp)
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AureomGold, NeonMagenta, HighTechCyan)
                    )
                )
                .padding(1.5.dp) // border thickness
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF050505)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(AureomGold, Color.White)
                        )
                    )
                )
            }
        }

        // Search the Web3 URL input
        Column(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Web3 Sovereign Operating Framework",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Fully sandboxed, on-chain resolved domains, native Lightning routing.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Main input
            OutlinedTextField(
                value = searchStr,
                onValueChange = { searchStr = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_search_input"),
                placeholder = { Text("Search the web3.0 (e.g. ln-sats-store)", color = TextMuted) },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = { onNavigate(searchStr) },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1BFFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Go",
                            tint = TextPrimary
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = Color(0x4DFFFFFF), // White/30
                    unfocusedBorderColor = Color(0x1AFFFFFF), // White/10
                    focusedContainerColor = Color(0x0DFFFFFF), // White/5
                    unfocusedContainerColor = Color(0x0DFFFFFF) // White/5
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Live stats grid - Uptime, Speed, Volumes
        Column(
            modifier = Modifier.fillMaxWidth(0.91f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "NODE TELEMETRY & METRICS STATS",
                style = MaterialTheme.typography.labelSmall,
                color = HighTechCyan,
                letterSpacing = 1.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3
            ) {
                // Stats Card 1
                MiniTelemetryCard(
                    title = "Block Settlement Speed",
                    value = "⚡ Direct Lightning / 10m On-Chain",
                    metric = "1.8 ms local avg",
                    activeGradient = activeGradient,
                    modifier = Modifier.weight(1f)
                )

                // Stats Card 2
                MiniTelemetryCard(
                    title = "Sovereign Domains",
                    value = "158,409 verified keys",
                    metric = "Zero-Knowledge DNS",
                    activeGradient = activeGradient,
                    modifier = Modifier.weight(1f)
                )

                // Stats Card 3
                MiniTelemetryCard(
                    title = "System Sandbox Integrity",
                    value = "AES-GCM Partitioned",
                    metric = "Maximum Isolation",
                    activeGradient = activeGradient,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Web3 Shortcuts Grid
        Column(
            modifier = Modifier.fillMaxWidth(0.91f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SECURE NATIVE SYSTEM WEB3 APPLICATIONS",
                style = MaterialTheme.typography.labelSmall,
                color = AureomGold,
                letterSpacing = 1.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                // Card 1: Wallet Launcher
                CategoryShortcutCard(
                    title = "Wallet Ledger",
                    description = "Manage Bitcoin hot keys, check Lightning balances, view historic derivation chains.",
                    emoji = "🔑",
                    statusColor = HyperEmerald,
                    statusText = "Hot Sigs Active",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("aureom://ln-sats-store") }
                )

                // Card 2: LNSats Store
                CategoryShortcutCard(
                    title = "Sats Marketplace",
                    description = "Browse secure dApps, buy network priority extensions, and settle invoices with lightning speeds.",
                    emoji = "⚡",
                    statusColor = AureomGold,
                    statusText = "Settlement Ready",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("aureom://ln-sats-store") }
                )

                // Card 3: Identity Center
                CategoryShortcutCard(
                    title = "Aureom Sovereign ID",
                    description = "Generate did:aureom decentralized keys, lock secure scopes, and authenticate seamlessly.",
                    emoji = "🆔",
                    statusColor = HighTechCyan,
                    statusText = "Verified Key: tom@aureom",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("aureom://auth-id") }
                )

                // Card 4: Node Console
                CategoryShortcutCard(
                    title = "Node Console",
                    description = "Monitor peer nodes synchronization, explore mempool transaction densities, analyze local stats.",
                    emoji = "🖥️",
                    statusColor = AmberGlow,
                    statusText = "Sync Active: 100%",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate("aureom://node-console") }
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun MiniTelemetryCard(
    title: String,
    value: String,
    metric: String,
    activeGradient: List<Color>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF)) // Glassmorphic (White/5)
            .border(0.5.dp, Color(0x11FFFFFF), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title.uppercase(), fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
            Text(text = value, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(activeGradient))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = metric, fontSize = 9.sp, color = TextMuted)
            }
        }
    }
}

@Composable
fun CategoryShortcutCard(
    title: String,
    description: String,
    emoji: String,
    statusColor: Color,
    statusText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0DFFFFFF)) // Glassmorphic (White/5)
            .border(0.5.dp, Color(0x11FFFFFF), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = emoji, fontSize = 18.sp)
                    Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 8.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// -----------------------------------------------------------------------------
// TAB PAGE: LN-SATS STORE (`aureom://ln-sats-store`)
// -----------------------------------------------------------------------------
@Composable
fun LNSatsStore(
    viewModel: BrowserViewModel,
    activeGradient: List<Color>,
    walletConnected: Boolean,
    onPayInvoice: (String, Long, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Trust and Verification Header
        Surface(
            color = Color(0x3B10B981),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, HyperEmerald.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HyperEmerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Verified Merchant", tint = HyperEmerald)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "Verified Web3 Merchant", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HyperEmerald)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0C1412))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(text = "Score 99", fontSize = 8.sp, color = HyperEmerald, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        text = "Merchant ID: e98c392f... LN multisig keys audited by Sovereign Labs",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Store description
        Text(
            text = "Aureom Network Extensions",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "Authorize directly using your hot cryptographic credentials. Secure sandbox execution isolation ensures absolute security.",
            fontSize = 12.sp,
            color = TextSecondary
        )

        // Products Catalog Cards
        val products = listOf(
            Triple("Aureom Sentinel Shield Protocol", 25000L, "Encrypted private metadata tunneling service valid for 365 days."),
            Triple("Global Satellite Peer Transit Route", 100000L, "Enable direct-to-orbit priority block broadcasting route links."),
            Triple("Cosmic Obsidian Theme Pack", 5000L, "Unlocks the premium Rolls-Royce level hyperdark theme.")
        )

        products.forEach { (name, cost, desc) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PanelBlack)
                    .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Lightning Micro-invoice", fontSize = 10.sp, color = TextSecondary)
                        }

                        // Cost pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B150A))
                                .border(0.5.dp, AureomGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format("%,d Sats", cost),
                                fontSize = 12.sp,
                                color = AureomGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Text(text = desc, fontSize = 11.sp, color = TextSecondary)

                    Divider(color = BorderGlassWhite, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HyperEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Instant 2ms settlement", fontSize = 9.sp, color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                onPayInvoice(name, cost, desc)
                            },
                            enabled = walletConnected,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AureomGold,
                                contentColor = Color.Black,
                                disabledContainerColor = TextMuted,
                                disabledContentColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Settle via LN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB PAGE: SOVEREIGN IDENTITY CENTER (`aureom://auth-id`)
// -----------------------------------------------------------------------------
@Composable
fun AureomIdentityCenter(
    viewModel: BrowserViewModel,
    activeGradient: List<Color>,
    brandGradientBrush: Brush
) {
    var registerNameInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Aureom Decentralized ID Authority",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        // DID Badge card mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(activeGradient.map { it.copy(alpha = 0.08f) }))
                .border(
                    1.dp,
                    Brush.linearGradient(activeGradient),
                    RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "SOVEREIGN WEBAUTH CARD",
                            fontSize = 9.sp,
                            color = HighTechCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "tom@aureom",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "did:aureom:bc1q9s3h6m8v7ux69pa6ql",
                            fontSize = 11.sp,
                            fontFamily = HighTechMonospace,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, BorderGlassWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🆔", fontSize = 16.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "SECURE ANCHOR HEIGHT", fontSize = 8.sp, color = TextSecondary)
                        Text(text = "#847,921 block consensus", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "ZKP KEY SIGNATURES", fontSize = 8.sp, color = TextSecondary)
                        Text(text = "Ed25519 Active Verified", fontSize = 11.sp, color = HyperEmerald, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Anchor custom domain section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBlack)
                .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Anchor Sovereign Web3 Domain", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    text = "Claim your peer did:aureom alias. This registers your Secp256k1 public keys on the Bitcoin ledger.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = registerNameInput,
                    onValueChange = { registerNameInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("did_input"),
                    placeholder = { Text("e.g. satoshi", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = HighTechCyan,
                        unfocusedBorderColor = BorderGlassWhite,
                        focusedContainerColor = Color(0xFF07080B),
                        unfocusedContainerColor = Color(0xFF07080B)
                    )
                )

                Button(
                    onClick = {
                        if (registerNameInput.isNotEmpty()) {
                            viewModel.anchorNewDID(registerNameInput)
                            registerNameInput = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = HighTechCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Anchor Peer Alias", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB PAGE: NODE CONSOLE (`aureom://node-console`)
// -----------------------------------------------------------------------------
@Composable
fun NodeConsoleCenter(
    viewModel: BrowserViewModel,
    activeGradient: List<Color>,
    peersCount: Int,
    blockHeight: Long
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Bitcoin Direct Node Console",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        // Node metrics panels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelBlack)
                    .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "Sync status", fontSize = 8.sp, color = TextSecondary)
                    Text(text = "100.00% synchronized", fontSize = 12.sp, color = HyperEmerald, fontWeight = FontWeight.Bold)
                    Text(text = "341.2 GB local DB size", fontSize = 9.sp, color = TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelBlack)
                    .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "Consensus Block", fontSize = 8.sp, color = TextSecondary)
                    Text(text = "#$blockHeight height", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(text = "Median time past synced", fontSize = 9.sp, color = TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PanelBlack)
                    .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = "Active Peers Link", fontSize = 8.sp, color = TextSecondary)
                    Text(text = "$peersCount connected", fontSize = 12.sp, color = HighTechCyan, fontWeight = FontWeight.Bold)
                    Text(text = "Aureom satellite mesh sync", fontSize = 9.sp, color = TextSecondary)
                }
            }
        }

        // Live drew Mempool fee speed chart in Compose Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PanelBlack)
                .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mempool Transaction Fee Dynamics",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    // Grid guides
                    val lines = 4
                    for (i in 1..lines) {
                        val hY = height * (i.toFloat() / lines)
                        drawLine(
                            color = BorderGlassWhite.copy(alpha = 0.08f),
                            start = Offset(0f, hY),
                            end = Offset(width, hY),
                            strokeWidth = 1f
                        )
                    }

                    // Draw interactive neon wavy mountain
                    val path = Path().apply {
                        moveTo(0f, height * 0.7f)
                        var prevX = 0f
                        var prevY = height * 0.7f

                        for (i in 1..10) {
                            val x = width * (i.toFloat() / 10f)
                            // deterministic wave values
                            val wave = sin((i * 0.9) + 4.2).toFloat()
                            val y = height * 0.5f + (wave * height * 0.28f)
                            lineTo(x, y)
                        }
                    }

                    val gradientBrush = Brush.verticalGradient(
                        colors = listOf(HighTechCyan.copy(alpha = 0.35f), Color.Transparent)
                    )

                    drawPath(
                        path = path,
                        brush = gradientBrush
                    )

                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(activeGradient),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )

                    // Draw metric dots
                    drawCircle(
                        color = AureomGold,
                        center = Offset(width * 0.4f, height * 0.35f),
                        radius = 5.5f
                    )
                    drawCircle(
                        color = HighTechCyan,
                        center = Offset(width * 0.7f, height * 0.65f),
                        radius = 5.5f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Block #847,910", fontSize = 8.sp, color = TextSecondary)
                    Text(text = "Now (18 sat/vB avg fee)", fontSize = 8.sp, color = HighTechCyan, fontWeight = FontWeight.Bold)
                    Text(text = "Block #847,924", fontSize = 8.sp, color = TextSecondary)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TAB PAGE: CUSTOM SECURE SANDBOX FOR EXTERNAL SITES
// -----------------------------------------------------------------------------
@Composable
fun ExternalWeb3Sandbox(
    url: String,
    tab: WebTab,
    activeGradient: List<Color>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(PanelBlack)
            .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color(0xFF191A23))
                .border(1.dp, Brush.linearGradient(activeGradient), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = tab.iconEmoji, fontSize = 28.sp)
        }

        Text(
            text = tab.title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Surface(
            color = Color(0x1FEEAE22),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.border(0.5.dp, AureomGold.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "🛡️ Sandbox Isolated Container Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AureomGold)
            }
        }

        Text(
            text = "Connected via $url securely. Metadata, cookies, and network hooks are completely isolated in a dedicated Web.3.0 sandbox frame. Outbound RPC requests require hot key signatures.",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Divider(color = BorderGlassWhite, thickness = 0.5.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Security Tier", fontSize = 9.sp, color = TextSecondary)
                Text(text = "ZKP Isolated", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Host DNS Status", fontSize = 9.sp, color = TextSecondary)
                Text(text = "Tor routed", fontSize = 13.sp, color = HighTechCyan, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -----------------------------------------------------------------------------
// RIGHT WORKSPACE: LIVE BLOCK INTERACTION STREAM & LEDGER PANEL
// -----------------------------------------------------------------------------
@Composable
fun Web3ActivitySidebar(
    logs: List<WebActivityLog>,
    activeGradient: List<Color>,
    peersCount: Int,
    blockHeight: Long,
    onClearLogs: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        modifier = Modifier
            .fillMaxSize()
            .border(0.5.dp, BorderGlassWhite, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEB3 LEDGER CORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Synchronized at block #$blockHeight",
                        fontSize = 9.sp,
                        color = TextSecondary
                    )
                }

                IconButton(onClick = onClearLogs, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear trace log", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Short Telemetry Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF07080B))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "PEERS STATS", fontSize = 8.sp, color = TextSecondary)
                    Text(text = "$peersCount connected", fontSize = 11.sp, color = HighTechCyan, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "MEMPOOL CAPACITY", fontSize = 8.sp, color = TextSecondary)
                    Text(text = "18 sat/vB median", fontSize = 11.sp, color = AureomGold, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lazy Column activity log list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    val badgeColor = when (log.status) {
                        "Settled" -> HyperEmerald
                        "Verified" -> HighTechCyan
                        "Pending" -> AmberGlow
                        else -> CoralAlert
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0A0B10))
                            .border(0.5.dp, BorderGlassWhite.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = when(log.category) {
                                            "Wallet" -> "🔑"
                                            "Identity" -> "🆔"
                                            "Network" -> "🖥️"
                                            else -> "🪐"
                                        },
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = log.actionTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = log.status,
                                        fontSize = 7.sp,
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Text(
                                text = log.subtitle,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                lineHeight = 13.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = log.txHash,
                                    fontSize = 8.sp,
                                    fontFamily = HighTechMonospace,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    fontSize = 7.sp,
                                    fontFamily = HighTechMonospace,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PREMIUM DIALOG: LIGHTNING INDEPENDENT PAYMENT MODAL (SWIPE TO CONFIRM)
// -----------------------------------------------------------------------------
@Composable
fun PaymentConfirmationDialog(
    invoice: BrowserViewModel.PaymentInvoice,
    status: String,
    activeGradient: List<Color>,
    blockHeight: Long,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var feeSelection by remember { mutableStateOf(18) } // standard fee

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable { if (status != "PROCESSING") onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PanelBlack),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 440.dp)
                .clickable(enabled = false) {}
                .border(1.dp, Brush.linearGradient(activeGradient), RoundedCornerShape(16.dp))
                .shadow(16.dp, ambientColor = HighTechCyan, spotColor = HighTechCyan),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡", fontSize = 18.sp)
                        Text(text = "Sovereign LN Settle Gate", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AureomGold)
                    }

                    if (status != "PROCESSING" && status != "SETTLED") {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                        }
                    }
                }

                Divider(color = BorderGlassWhite, thickness = 0.5.dp)

                // Main amount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "AMOUNT REQUESTED", fontSize = 9.sp, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = String.format("%,d Sats", invoice.amountSats),
                        style = MaterialTheme.typography.displayLarge.copy(
                            brush = Brush.horizontalGradient(activeGradient),
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = invoice.itemTitle,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Recipient list details
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF07080B))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DetailItemRow("Destination", invoice.recipient)
                        DetailItemRow("Auth Hash", invoice.invoiceHash.take(16) + "...")
                        DetailItemRow("Isolated Mode", "Partition sandboxed")
                    }
                }

                // Status controller
                when (status) {
                    "PENDING" -> {
                        // Fee Selector
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Select Network Mining Speed", fontSize = 10.sp, color = TextSecondary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(8 to "Slow", 18 to "Standard", 35 to "Speedy").forEach { (fee, tag) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (feeSelection == fee) Color(0xFF1B150A) else Color(0xFF10111A))
                                            .border(
                                                1.dp,
                                                if (feeSelection == fee) AureomGold else BorderGlassWhite,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { feeSelection = fee }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = "$fee sat/vB", fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { onConfirm(feeSelection) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("confirm_payment_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AureomGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "SWIPE TO AUTHORIZE SIGNATURE", fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    "PROCESSING" -> {
                        CircularProgressIndicator(color = HighTechCyan, modifier = Modifier.size(32.dp))
                        Text(text = "Broadcasting ZKP Keys to Bitcoin nodes...", fontSize = 11.sp, color = HighTechCyan)
                    }
                    "SETTLED" -> {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HyperEmerald.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Settled", tint = HyperEmerald, modifier = Modifier.size(32.dp))
                        }
                        Text(text = "Lightning Invoice Settled!", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HyperEmerald)
                        Text(text = "Signature broadcast completed in block #${blockHeight}", fontSize = 11.sp, color = TextSecondary)

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = HyperEmerald, contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Complete", fontWeight = FontWeight.Bold)
                        }
                    }
                    "FAIL_INSUFFICIENT" -> {
                        Text(text = "Transfer Aborted: Insufficient Balance", fontSize = 12.sp, color = CoralAlert, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = CoralAlert)
                        ) {
                            Text(text = "Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 9.sp, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(text = value, fontSize = 9.sp, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// -----------------------------------------------------------------------------
// SETTINGS INTEGRATION DRAWER
// -----------------------------------------------------------------------------
@Composable
fun SettingsPrivacyDrawer(
    viewModel: BrowserViewModel,
    accentIndex: Int,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sandbox by viewModel.sandboxIsolation.collectAsState()
    val mockDns by viewModel.zeroKnowledgeDNS.collectAsState()
    val activeChain by viewModel.activeChainNetwork.collectAsState()
    val shields by viewModel.secureShields.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .background(AmbientDarkBg)
                .drawBehind {
                    drawLine(
                        color = BorderGlassWhite,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 2f
                    )
                }
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Sovereign Settings", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Divider(color = BorderGlassWhite, thickness = 0.5.dp)

                Text(
                    text = "AUREOM BRAND VISUAL IDENTITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = AureomGold,
                    letterSpacing = 1.sp
                )

                // Visual Gradient toggler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Classical", "Cyberpunk", "Sovereign").forEachIndexed { index, name ->
                        val isSelected = accentIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF1B150A) else Color(0xFF10121D))
                                .border(
                                    1.dp,
                                    if (isSelected) AureomGold else BorderGlassWhite,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.selectedAccentGradientsIndex.value = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) AureomGold else TextSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "PRIVACY & DECENTRALIZATION ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = HighTechCyan,
                    letterSpacing = 1.sp
                )

                // Preference selector configurations
                PreferenceToggleRow(
                    label = "Strict Container Isolation",
                    desc = "Isolate cookies and browser states in AES memory partitions",
                    value = sandbox,
                    onSelect = { viewModel.sandboxIsolation.value = it },
                    options = listOf("Standard Sandbox", "Strict (Encrypted Partition)")
                )

                PreferenceToggleRow(
                    label = "Zero-Knowledge DNS Routing",
                    desc = "Tunnel all searches over private network router rails",
                    value = mockDns,
                    onSelect = { viewModel.zeroKnowledgeDNS.value = it },
                    options = listOf("Off (Local Provider)", "On (Aureom Private Router)")
                )

                PreferenceToggleRow(
                    label = "Core Consensus Network",
                    desc = "Active network channel",
                    value = activeChain,
                    onSelect = { viewModel.activeChainNetwork.value = it },
                    options = listOf("Bitcoin Mainnet", "Lightning / Bitcoin Mainnet", "Regtest / Signet")
                )

                PreferenceToggleRow(
                    label = "Metadata Shield Protector",
                    desc = "Secured fingerprint masking",
                    value = shields,
                    onSelect = { viewModel.secureShields.value = it },
                    options = listOf("Standard Shield", "Strict Tracker Shield")
                )
            }
        }
    }
}

@Composable
fun PreferenceToggleRow(
    label: String,
    desc: String,
    value: String,
    onSelect: (String) -> Unit,
    options: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = desc, fontSize = 9.sp, color = TextSecondary, lineHeight = 12.sp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1015))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { opt ->
                val isSelected = opt == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PanelBlack else Color.Transparent)
                        .clickable { onSelect(opt) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) AureomGold else TextSecondary
                    )
                }
            }
        }
    }
}
