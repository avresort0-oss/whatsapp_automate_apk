package com.example

import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * ============================================================================
 * ChatListScanner - WhatsApp এর পুরো Chat List Scan করে Collect করে
 * ============================================================================
 *
 * এই ক্লাসটি AccessibilityService থেকে visible chat list পড়ে,
 * scroll করে আরো chat বের করে, এবং পুরো লিস্ট একটা data structure-এ
 * save করে রাখে যাতে UI-তে দেখানো যায়।
 *
 * Features:
 *  - Visible chat rows parse করা (name, last message, time, read status)
 *  - Duplicate detection (scroll করলে একই chat আবার আসতে পারে)
 *  - Double blue tick detection (read receipt)
 *  - Unread message count detection
 *  - Scroll state tracking
 * ============================================================================
 */
object ChatListScanner {

    /**
     * একটা single chat এর তথ্য।
     */
    data class ScannedChat(
        val id: String,                    // unique ID (contact name + time)
        val contactName: String,           // contact বা group এর নাম
        val lastMessage: String = "",      // শেষ message এর preview
        val timestamp: String = "",        // শেষ message এর time
        val hasDoubleBlueTick: Boolean = false,  // read receipt (আমি পাঠিয়েছি আর সে পড়েছে)
        val hasSingleTick: Boolean = false,      // sent but not delivered
        val hasDoubleGrayTick: Boolean = false,  // delivered but not read
        val isUnread: Boolean = false,           // সে পাঠিয়েছে আমি পড়িনি
        val unreadCount: Int = 0,                // কতটা unread
        val isGroup: Boolean = false,            // group chat কিনা
        val isBusiness: Boolean = false,         // business account কিনা
        val profileColorHex: Long? = null        // profile picture background color
    )

    /**
     * Scan এর status।
     */
    enum class ScanStatus {
        IDLE,
        SCANNING,
        COMPLETED,
        ERROR
    }

    // সব scanned chat গুলো এখানে থাকবে
    private val _scannedChats = MutableStateFlow<List<ScannedChat>>(emptyList())
    val scannedChats: StateFlow<List<ScannedChat>> = _scannedChats.asStateFlow()

    // Scan status
    private val _scanStatus = MutableStateFlow(ScanStatus.IDLE)
    val scanStatus: StateFlow<ScanStatus> = _scanStatus.asStateFlow()

    // Total chat সংখ্যা
    private val _totalChatsFound = MutableStateFlow(0)
    val totalChatsFound: StateFlow<Int> = _totalChatsFound.asStateFlow()

    // এই scan এ কত scroll করা হয়েছে
    private val _scrollCount = MutableStateFlow(0)
    val scrollCount: StateFlow<Int> = _scrollCount.asStateFlow()

    // Track unique contact names (duplicate detection)
    private val scannedContactNames = mutableSetOf<String>()
    private var lastTopContactName: String? = null
    private var sameTopCount = 0

    /**
     * Scan শুরু করার আগে state reset করে।
     */
    fun resetScan() {
        _scannedChats.value = emptyList()
        scannedContactNames.clear()
        _totalChatsFound.value = 0
        _scrollCount.value = 0
        lastTopContactName = null
        sameTopCount = 0
        _scanStatus.value = ScanStatus.SCANNING
    }

    /**
     * বর্তমান visible স্ক্রিন থেকে সব chat row parse করে collect করে।
     *
     * @param rootNode AccessibilityService এর rootInActiveWindow
     * @return নতুন কতটা chat পাওয়া গেছে
     */
    fun parseVisibleChats(rootNode: AccessibilityNodeInfo): Int {
        val newChats = mutableListOf<ScannedChat>()
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        // WhatsApp chat list-এ প্রতিটা row একটা clickable LinearLayout/RelativeLayout
        // যার ভেতরে একটা TextView (contact name) থাকে।
        // আমরা সব clickable row খুঁজব যেগুলোর ভেতরে meaningful text আছে।

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)

            // এই node টা কি একটা chat row?
            val chatInfo = parseChatRow(current)
            if (chatInfo != null) {
                val nameLower = chatInfo.contactName.lowercase(Locale.getDefault())
                if (nameLower.isNotEmpty() && !scannedContactNames.contains(nameLower)) {
                    scannedContactNames.add(nameLower)
                    newChats.add(chatInfo)
                }
            }

            // Children traverse
            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }

        if (newChats.isNotEmpty()) {
            val currentList = _scannedChats.value.toMutableList()
            currentList.addAll(newChats)
            _scannedChats.value = currentList
            _totalChatsFound.value = currentList.size
        }

        return newChats.size
    }

    /**
     * একটা node থেকে chat row এর তথ্য parse করার চেষ্টা করে।
     *
     * WhatsApp chat row structure (approximate):
     * LinearLayout (clickable)
     *   ├── ImageView (profile picture)
     *   ├── LinearLayout
     *   │   ├── TextView (contact name)        ← এটা খুঁজছি
     *   │   ├── TextView (last message preview)
     *   │   └── ...
     *   └── LinearLayout (right side)
     *       ├── TextView (timestamp)
     *       └── ImageView (tick status / unread badge)
     */
    private fun parseChatRow(node: AccessibilityNodeInfo): ScannedChat? {
        // শুধু clickable node গুলোই chat row হতে পারে
        if (!node.isClickable) return null

        val className = node.className?.toString() ?: ""
        // WhatsApp row গুলো সাধারণত LinearLayout বা RelativeLayout
        if (!className.contains("LinearLayout") &&
            !className.contains("RelativeLayout") &&
            !className.contains("ViewGroup")) {
            return null
        }

        // এই row এর ভেতরে সব text collect করো
        val texts = mutableListOf<String>()
        val contentDescs = mutableListOf<String>()
        collectTextsAndDescs(node, texts, contentDescs)

        if (texts.isEmpty()) return null

        // প্রথম meaningful text টা contact name (সাধারণত > ২ character)
        val contactName = texts.firstOrNull {
            it.length > 2 &&
            !isTimeString(it) &&
            !isMessagePreviewKeyword(it)
        } ?: return null

        // Group chat detect (যদি নামের সাথে group icon থাকে)
        val isGroup = contentDescs.any {
            it.lowercase().contains("group") || it.contains("গ্রুপ")
        }

        // Business account detect
        val isBusiness = contentDescs.any {
            it.lowercase().contains("business") || it.contains("ব্যবসা")
        }

        // Time string খোঁজা (সাধারণত "12:34 PM", "Yesterday", "Today" ইত্যাদি)
        val timestamp = texts.firstOrNull { isTimeString(it) } ?: ""

        // Last message preview (সাধারণত দ্বিতীয় meaningful text)
        val lastMessage = texts.firstOrNull {
            it.length > 2 &&
            it != contactName &&
            !isTimeString(it)
        } ?: ""

        // Tick status detect
        val tickStatus = detectTickStatus(node, contentDescs)

        // Unread badge detect
        val unreadInfo = detectUnreadBadge(node, contentDescs)

        return ScannedChat(
            id = "${contactName}_$timestamp",
            contactName = contactName,
            lastMessage = lastMessage,
            timestamp = timestamp,
            hasDoubleBlueTick = tickStatus.hasDoubleBlueTick,
            hasSingleTick = tickStatus.hasSingleTick,
            hasDoubleGrayTick = tickStatus.hasDoubleGrayTick,
            isUnread = unreadInfo.isUnread,
            unreadCount = unreadInfo.count,
            isGroup = isGroup,
            isBusiness = isBusiness
        )
    }

    /**
     * একটা node tree থেকে সব text ও contentDescription collect করে।
     */
    private fun collectTextsAndDescs(
        node: AccessibilityNodeInfo,
        texts: MutableList<String>,
        contentDescs: MutableList<String>
    ) {
        node.text?.toString()?.let {
            if (it.isNotEmpty()) texts.add(it)
        }
        node.contentDescription?.toString()?.let {
            if (it.isNotEmpty()) contentDescs.add(it)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTextsAndDescs(child, texts, contentDescs)
        }
    }

    /**
     * একটা string time কিনা চেক করে।
     */
    private fun isTimeString(s: String): Boolean {
        val timePattern = Regex("""^\d{1,2}:\d{2}\s*(AM|PM|am|pm)?$""")
        val keywords = listOf("Yesterday", "Today", "now", "just now", "জানুয়ারি", "ফেব্রুয়ারি",
            "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর",
            "নভেম্বর", "ডিসেম্বর", "ago", "দিন আগে", "কাল", "আজ")
        return timePattern.matches(s) || keywords.any { s.contains(it, ignoreCase = true) }
    }

    /**
     * Message preview keyword কিনা চেক করে (যেমন "Photo", "Video", "Voice message" ইত্যাদি)।
     */
    private fun isMessagePreviewKeyword(s: String): Boolean {
        val keywords = listOf("Photo", "Video", "Voice message", "Audio", "Document",
            "Sticker", "GIF", "Location", "Contact", "টেক্সট", "ছবি", "ভিডিও")
        return keywords.any { s.contains(it, ignoreCase = true) }
    }

    /**
     * Tick status detect করে।
     */
    private data class TickStatus(
        val hasSingleTick: Boolean = false,
        val hasDoubleGrayTick: Boolean = false,
        val hasDoubleBlueTick: Boolean = false
    )

    private fun detectTickStatus(node: AccessibilityNodeInfo, contentDescs: List<String>): TickStatus {
        // WhatsApp-এ tick গুলোর contentDescription সাধারণত:
        // - "Sent" / "পাঠানো হয়েছে" (single tick)
        // - "Delivered" / "পৌঁছে গেছে" (double gray tick)
        // - "Read" / "পঠিত" / "Seen" (double blue tick)

        for (desc in contentDescs) {
            val lower = desc.lowercase(Locale.getDefault())
            when {
                lower.contains("read") || lower.contains("seen") ||
                    lower.contains("পঠিত") || lower.contains("পড়া হয়েছে") -> {
                    return TickStatus(hasDoubleBlueTick = true)
                }
                lower.contains("delivered") || lower.contains("পৌঁছে গেছে") ||
                    lower.contains("পাঠানো হয়েছে") -> {
                    return TickStatus(hasDoubleGrayTick = true)
                }
                lower.contains("sent") || lower.contains("পাঠানো হয়েছে") -> {
                    return TickStatus(hasSingleTick = true)
                }
            }
        }
        return TickStatus()
    }

    /**
     * Unread badge detect করে।
     */
    private data class UnreadInfo(
        val isUnread: Boolean = false,
        val count: Int = 0
    )

    private fun detectUnreadBadge(node: AccessibilityNodeInfo, contentDescs: List<String>): UnreadInfo {
        // Unread badge সাধারণত একটা circular green background এর উপর number
        // contentDescription: "5 unread messages" / "৫টি অপঠিত বার্তা"

        for (desc in contentDescs) {
            val lower = desc.lowercase(Locale.getDefault())
            if (lower.contains("unread") || lower.contains("অপঠিত") || lower.contains("new")) {
                // Number extract করার চেষ্টা
                val number = Regex("""\d+""").find(desc)?.value?.toIntOrNull() ?: 1
                return UnreadInfo(isUnread = true, count = number)
            }
        }
        return UnreadInfo()
    }

    /**
     * Scroll করার পরে list-এর শেষ হয়ে গেছে কিনা চেক করে।
     *
     * যদি scroll করার পরেও top-এ একই contact থাকে, তার মানে list শেষ।
     */
    fun hasReachedEndOfList(): Boolean {
        return sameTopCount >= 3
    }

    /**
     * বর্তমান top contact ট্র্যাক করে — scroll করার আগে ডাকা হবে।
     */
    fun trackScroll() {
        val currentTop = _scannedChats.value.firstOrNull()?.contactName
        if (currentTop == lastTopContactName) {
            sameTopCount++
        } else {
            sameTopCount = 0
            lastTopContactName = currentTop
        }
        _scrollCount.value = _scrollCount.value + 1
    }

    /**
     * Scan সম্পূর্ণ হয়েছে চিহ্নিত করে।
     */
    fun completeScan() {
        _scanStatus.value = ScanStatus.COMPLETED
    }

    /**
     * Scan-এ error হয়েছে।
     */
    fun errorScan() {
        _scanStatus.value = ScanStatus.ERROR
    }

    /**
     * শুধুমাত্র double blue tick থাকা chat গুলো filter করে দেয়।
     */
    fun getChatsWithDoubleBlueTick(): List<ScannedChat> {
        return _scannedChats.value.filter { it.hasDoubleBlueTick }
    }

    /**
     * শুধুমাত্র unread chat গুলো filter করে দেয়।
     */
    fun getUnreadChats(): List<ScannedChat> {
        return _scannedChats.value.filter { it.isUnread }
    }

    /**
     * Scanned chat গুলোর summary text তৈরি করে।
     */
    fun getScanSummary(): String {
        val all = _scannedChats.value
        val read = all.count { it.hasDoubleBlueTick }
        val unread = all.count { it.isUnread }
        val groups = all.count { it.isGroup }
        val business = all.count { it.isBusiness }

        return buildString {
            appendLine("📋 Scan Summary:")
            appendLine("• Total chats: ${all.size}")
            appendLine("• Read (blue tick): $read")
            appendLine("• Unread: $unread")
            appendLine("• Groups: $groups")
            appendLine("• Business: $business")
            appendLine("• Scrolls: ${_scrollCount.value}")
        }
    }
}
