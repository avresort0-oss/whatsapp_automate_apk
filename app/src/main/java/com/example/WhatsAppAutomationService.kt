package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * ============================================================================
 * WhatsAppAutomationService - আপগ্রেডেড ভার্সন
 * ============================================================================
 *
 * নতুন Features:
 *  ✅ Image auto-attach ও send (bug fix)
 *  ✅ BypassManager integration (anti-detection)
 *  ✅ ChatListScanner integration (পুরো chat list scan)
 *  ✅ State machine (image flow এ ধাপে ধাপে advance করার জন্য)
 *  ✅ Random delays ও human-like behavior
 *  ✅ Session limits ও cooldown
 *  ✅ Suspicious activity detection
 * ============================================================================
 */
class WhatsAppAutomationService : AccessibilityService() {

    private lateinit var sharedPreferences: SharedPreferences
    private val completedContacts = mutableSetOf<String>()
    private var isScrolling = false
    private var lastScrollTime = 0L
    private var lastActionTime = 0L

    // Coroutine scope for delays ও async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var automationJob: Job? = null

    // Image flow state machine — কারণ image attach একটানা এক event এ সম্ভব না
    private enum class ImageFlowState {
        IDLE,                   // কোনো image flow নেই
        WAITING_FOR_MENU,       // attachment চাপার পরে bottom sheet এর জন্য অপেক্ষা
        WAITING_FOR_GALLERY,    // gallery option চাপার পরে gallery picker খোলার জন্য অপেক্ষা
        WAITING_FOR_IMAGE_PICK, // gallery তে image select করার পরে preview খোলার জন্য অপেক্ষা
        WAITING_FOR_PREVIEW_SEND // preview স্ক্রিনে caption + send করার জন্য অপেক্ষা
    }
    private var imageFlowState = ImageFlowState.IDLE
    private var currentChatTitle = ""
    private var pendingCaption = ""

    companion object {
        const val PREFS_NAME = "WhatsAppAutomatorPrefs"
        const val KEY_TARGET_CONTACTS = "target_contacts"
        const val KEY_QUICK_REPLY = "quick_reply"
        const val KEY_AUTOMATION_ACTIVE = "automation_active"
        const val KEY_HUMAN_DELAY = "human_delay"
        const val KEY_ONLY_BLUE_TICKS = "only_blue_ticks"

        // নতুন keys
        const val KEY_SCAN_LIST_ONLY = "scan_list_only"  // শুধু scan করবে, message পাঠাবে না
        const val KEY_USE_BYPASS = "use_bypass"
        const val KEY_USE_IMAGE_FLOW = "use_image_flow"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        AutomationLogManager.log(
            "Accessibility Service Connected! (v2)",
            AutomationLogManager.LogType.SUCCESS
        )
        AutomationLogManager.log("Targeting WhatsApp package: com.whatsapp")
        AutomationLogManager.log("Bypass: ${if (BypassManager.isBypassEnabled(this)) "ENABLED" else "DISABLED"}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val isActive = sharedPreferences.getBoolean(KEY_AUTOMATION_ACTIVE, false)
        if (!isActive) return

        // Verify package name is WhatsApp
        val packageName = event.packageName?.toString() ?: ""
        if (packageName != "com.whatsapp") return

        val rootNode = rootInActiveWindow ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                processAutomation(rootNode)
            }
        }
    }

    override fun onInterrupt() {
        AutomationLogManager.log("Service Interrupted!", AutomationLogManager.LogType.ERROR)
    }

    override fun onDestroy() {
        super.onDestroy()
        automationJob?.cancel()
        AutomationLogManager.log("Service Destroyed. Session stats:\n${BypassManager.getSessionStats()}")
    }

    // ============================================================================
    //  Main Automation Logic
    // ============================================================================

    private fun processAutomation(rootNode: AccessibilityNodeInfo) {
        // Rate limiting: খুব দ্রুত event এ আবার process না করা
        val now = System.currentTimeMillis()
        if (now - lastActionTime < 300) return
        lastActionTime = now

        // Suspicious activity check
        val (isSuspicious, reason) = BypassManager.detectSuspiciousActivity()
        if (isSuspicious) {
            AutomationLogManager.log(
                "⚠️ Suspicious activity detected: $reason — Pausing 60s",
                AutomationLogManager.LogType.WARNING
            )
            serviceScope.launch {
                delay(60000)
            }
            return
        }

        // Image flow state machine — যদি আমরা image attach এর মাঝখানে থাকি
        if (imageFlowState != ImageFlowState.IDLE) {
            handleImageFlowState(rootNode)
            return
        }

        // Session limit check
        if (!BypassManager.canSendMore(this)) {
            AutomationLogManager.log(
                "Session limit reached. Stats:\n${BypassManager.getSessionStats()}",
                AutomationLogManager.LogType.WARNING
            )
            // Cooldown period নাও
            serviceScope.launch {
                val cooldown = BypassManager.getCooldownPeriod(this@WhatsAppAutomationService)
                if (cooldown > 0) {
                    AutomationLogManager.log("Cooldown: ${cooldown / 1000}s")
                    delay(cooldown)
                }
            }
            return
        }

        // Read preferences
        val targetString = sharedPreferences.getString(KEY_TARGET_CONTACTS, "") ?: ""
        val targets = targetString.split(",")
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.isNotEmpty() }
        val replyMessage = sharedPreferences.getString(KEY_QUICK_REPLY, "") ?: "Hello!"
        val scanListOnly = sharedPreferences.getBoolean(KEY_SCAN_LIST_ONLY, false)
        val useImageFlow = sharedPreferences.getBoolean(KEY_USE_IMAGE_FLOW, false) &&
                ImageAttachmentHelper.isImageEnabled(this)

        // যদি শুধু scan mode চালু থাকে
        if (scanListOnly) {
            handleScanOnlyMode(rootNode)
            return
        }

        if (targets.isEmpty()) {
            AutomationLogManager.log(
                "No target contacts set in settings.",
                AutomationLogManager.LogType.WARNING
            )
            return
        }

        // 1. চ্যাট স্ক্রিনে আছি কিনা চেক করো
        val chatInputNode = findChatInput(rootNode)
        if (chatInputNode != null) {
            handleChatScreen(rootNode, chatInputNode, replyMessage, useImageFlow)
            return
        }

        // 2. Contact list এ আছি — target খুঁজে ক্লিক করো
        handleContactList(rootNode, targets)
    }

    // ============================================================================
    //  Scan-Only Mode (শুধু chat list collect করবে, message পাঠাবে না)
    // ============================================================================

    private fun handleScanOnlyMode(rootNode: AccessibilityNodeInfo) {
        val contactListContainer = findWhatsAppContactList(rootNode)
        if (contactListContainer == null) {
            AutomationLogManager.log(
                "Contact list container not found",
                AutomationLogManager.LogType.WARNING
            )
            return
        }

        val newCount = ChatListScanner.parseVisibleChats(rootNode)
        if (newCount > 0) {
            AutomationLogManager.log(
                "Found $newCount new chats. Total: ${ChatListScanner.totalChatsFound.value}",
                AutomationLogManager.LogType.INFO
            )
        }

        // Scroll করো (যদি list এর শেষ না হয়ে থাকে)
        if (!ChatListScanner.hasReachedEndOfList()) {
            val currentTime = System.currentTimeMillis()
            val delay = BypassManager.getIntSetting(
                this, BypassManager.KEY_MIN_DELAY_MS, BypassManager.Defaults.MIN_DELAY_MS
            ).toLong()
            if (currentTime - lastScrollTime > delay && !isScrolling) {
                lastScrollTime = currentTime
                ChatListScanner.trackScroll()
                scrollMainListHumanLike()
            }
        } else {
            AutomationLogManager.log(
                "Reached end of list!\n${ChatListScanner.getScanSummary()}",
                AutomationLogManager.LogType.SUCCESS
            )
            ChatListScanner.completeScan()
            // Scan mode বন্ধ করো
            sharedPreferences.edit().putBoolean(KEY_SCAN_LIST_ONLY, false).apply()
        }
    }

    // ============================================================================
    //  Chat Screen Handling (message/image send)
    // ============================================================================

    private fun handleChatScreen(
        rootNode: AccessibilityNodeInfo,
        chatInputNode: AccessibilityNodeInfo,
        replyMessage: String,
        useImageFlow: Boolean
    ) {
        val chatTitle = getChatTitle(rootNode)
        AutomationLogManager.log(
            "Inside Chat Screen: '$chatTitle'",
            AutomationLogManager.LogType.INFO
        )

        // যদি এই contact কে আগেই process করা হয়ে থাকে — skip
        val titleLower = chatTitle.lowercase(Locale.getDefault())
        if (titleLower.isNotEmpty() && completedContacts.contains(titleLower)) {
            AutomationLogManager.log(
                "'$chatTitle' already processed. Going back.",
                AutomationLogManager.LogType.INFO
            )
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }

        currentChatTitle = chatTitle
        pendingCaption = replyMessage

        serviceScope.launch {
            // === BYPASS: Read-before-reply simulation ===
            BypassManager.simulateReading(this@WhatsAppAutomationService, messageCount = 3)

            if (useImageFlow) {
                // Image flow শুরু করো
                AutomationLogManager.log(
                    "Starting image attachment flow...",
                    AutomationLogManager.LogType.INFO
                )
                imageFlowState = ImageFlowState.WAITING_FOR_MENU

                // Attachment button ক্লিক করো
                val attachmentClicked = clickAttachmentButton(rootNode)
                if (!attachmentClicked) {
                    AutomationLogManager.log(
                        "Attachment button not found. Sending text only.",
                        AutomationLogManager.LogType.WARNING
                    )
                    sendTextOnly(chatInputNode, replyMessage, chatTitle)
                    imageFlowState = ImageFlowState.IDLE
                }
                // next event এ handleImageFlowState ডাকা হবে
            } else {
                // Text only flow
                sendTextOnly(chatInputNode, replyMessage, chatTitle)
            }
        }
    }

    /**
     * Text-only message পাঠায় — bypass logic সহ।
     */
    private suspend fun sendTextOnly(
        chatInputNode: AccessibilityNodeInfo,
        replyMessage: String,
        chatTitle: String
    ) {
        // === BYPASS: Typing simulation (WhatsApp "typing..." indicator দেখাবে) ===
        BypassManager.simulateTypingPause(this, replyMessage)

        // Text set করো
        val isSuccessText = fillTextMessage(chatInputNode, replyMessage)
        if (isSuccessText) {
            // === BYPASS: Random delay before send (যেন মানুষ যেন ভাবছে) ===
            val preSendDelay = BypassManager.getHumanDelay(this) / 3
            delay(preSendDelay)

            val rootNode = rootInActiveWindow ?: return
            val sendButton = findSendButton(rootNode)
            if (sendButton != null) {
                sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                AutomationLogManager.log(
                    "✅ Message sent to '$chatTitle'!",
                    AutomationLogManager.LogType.SUCCESS
                )

                // Mark as completed
                if (chatTitle.isNotEmpty()) {
                    completedContacts.add(chatTitle.lowercase(Locale.getDefault()))
                }
                BypassManager.onMessageSent(this)

                // === BYPASS: Post-send delay ===
                val postSendDelay = BypassManager.getHumanDelay(this)
                delay(postSendDelay)

                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                AutomationLogManager.log(
                    "Send button not found!",
                    AutomationLogManager.LogType.ERROR
                )
                BypassManager.onMessageFailed()
            }
        }
    }

    // ============================================================================
    //  Image Flow State Machine
    // ============================================================================

    private fun handleImageFlowState(rootNode: AccessibilityNodeInfo) {
        serviceScope.launch {
            when (imageFlowState) {
                ImageFlowState.WAITING_FOR_MENU -> {
                    // Bottom sheet খোলার পরে Gallery option খুঁজে ক্লিক করো
                    delay(800) // bottom sheet animation এর জন্য অপেক্ষা

                    val galleryClicked = ImageAttachmentHelper.clickGalleryOption(rootNode)
                    if (galleryClicked) {
                        AutomationLogManager.log(
                            "Gallery option clicked",
                            AutomationLogManager.LogType.SUCCESS
                        )
                        imageFlowState = ImageFlowState.WAITING_FOR_GALLERY
                        delay(1000) // gallery picker খোলার জন্য
                    } else {
                        AutomationLogManager.log(
                            "Gallery option not found. Cancelling image flow.",
                            AutomationLogManager.LogType.ERROR
                        )
                        imageFlowState = ImageFlowState.IDLE
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }

                ImageFlowState.WAITING_FOR_GALLERY -> {
                    // Gallery picker এ recent image select করো
                    val imageClicked = ImageAttachmentHelper.clickMostRecentImage(rootNode)
                    if (imageClicked) {
                        AutomationLogManager.log(
                            "Image selected from gallery",
                            AutomationLogManager.LogType.SUCCESS
                        )
                        imageFlowState = ImageFlowState.WAITING_FOR_IMAGE_PICK
                        delay(1500) // image preview খোলার জন্য
                    } else {
                        AutomationLogManager.log(
                            "Could not select image. Backing out.",
                            AutomationLogManager.LogType.ERROR
                        )
                        imageFlowState = ImageFlowState.IDLE
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        delay(500)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                }

                ImageFlowState.WAITING_FOR_IMAGE_PICK -> {
                    // Preview স্ক্রিনে caption বসাও ও send করো
                    val sent = ImageAttachmentHelper.fillCaptionAndSend(rootNode, pendingCaption)
                    if (sent) {
                        AutomationLogManager.log(
                            "✅ Image + caption sent to '$currentChatTitle'!",
                            AutomationLogManager.LogType.SUCCESS
                        )

                        if (currentChatTitle.isNotEmpty()) {
                            completedContacts.add(currentChatTitle.lowercase(Locale.getDefault()))
                        }
                        BypassManager.onMessageSent(this@WhatsAppAutomationService)

                        // === BYPASS: Post-send delay ===
                        val postSendDelay = BypassManager.getHumanDelay(this@WhatsAppAutomationService)
                        delay(postSendDelay)

                        // Chat list এ ফিরে যাও
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        delay(500)
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    } else {
                        AutomationLogManager.log(
                            "Failed to send image. Backing out.",
                            AutomationLogManager.LogType.ERROR
                        )
                        BypassManager.onMessageFailed()
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }
                    imageFlowState = ImageFlowState.IDLE
                }

                ImageFlowState.WAITING_FOR_PREVIEW_SEND -> {
                    // এটা WAITING_FOR_IMAGE_PICK এর সাথে একই — fallback
                    imageFlowState = ImageFlowState.WAITING_FOR_IMAGE_PICK
                }

                ImageFlowState.IDLE -> {
                    // কিছু করার নেই
                }
            }
        }
    }

    // ============================================================================
    //  Contact List Handling
    // ============================================================================

    private fun handleContactList(rootNode: AccessibilityNodeInfo, targets: List<String>) {
        val contactListContainer = findWhatsAppContactList(rootNode)
        if (contactListContainer != null) {
            val listClass = contactListContainer.className?.toString() ?: "list container"
            AutomationLogManager.log(
                "Detected contact list: $listClass",
                AutomationLogManager.LogType.INFO
            )
        }

        // যদি শুধু scan চলছে থাকে, চলতে দাও
        val scanListOnly = sharedPreferences.getBoolean(KEY_SCAN_LIST_ONLY, false)
        if (scanListOnly) return

        val currentTime = System.currentTimeMillis()
        val delay = BypassManager.getIntSetting(
            this, BypassManager.KEY_MIN_DELAY_MS, BypassManager.Defaults.MIN_DELAY_MS
        ).toLong()

        // Target খুঁজে ক্লিক করো
        val matchedContactNode = findTargetContactNode(rootNode, targets)
        if (matchedContactNode != null) {
            val contactName = matchedContactNode.text?.toString() ?: "Target"
            AutomationLogManager.log(
                "✅ Target found: '$contactName'",
                AutomationLogManager.LogType.SUCCESS
            )

            val clicked = clickNode(matchedContactNode)
            if (clicked) {
                AutomationLogManager.log(
                    "Opening chat for '$contactName'...",
                    AutomationLogManager.LogType.INFO
                )

                // === BYPASS: Pre-click random delay ===
                serviceScope.launch {
                    val preClickDelay = (500..1500).random().toLong()
                    delay(preClickDelay)
                }
            } else {
                AutomationLogManager.log(
                    "Failed to click contact node.",
                    AutomationLogManager.LogType.WARNING
                )
            }
        } else {
            // Target পাওয়া যায়নি — scroll করো
            if (currentTime - lastScrollTime > delay && !isScrolling) {
                lastScrollTime = currentTime
                scrollMainListHumanLike()
            }
        }
    }

    // ============================================================================
    //  Helper: Attachment Button Click (Image Flow Step 1)
    // ============================================================================

    private fun clickAttachmentButton(rootNode: AccessibilityNodeInfo): Boolean {
        val keywords = listOf(
            "attach", "attachment", "clip", "paperclip",
            "আটকান", "সংযুক্ত", "সংযুক্ত করুন", "ক্লিপ", "টানুন"
        )

        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""

            for (keyword in keywords) {
                if (desc.contains(keyword) && current.isClickable) {
                    AutomationLogManager.log(
                        "Attachment button found, clicking...",
                        AutomationLogManager.LogType.INFO
                    )
                    return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return false
    }

    // ============================================================================
    //  Helper: Node Finding (existing, with bypass tweaks)
    // ============================================================================

    private fun findWhatsAppContactList(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val className = current.className?.toString() ?: ""
            if (current.isScrollable && (className.contains("RecyclerView") ||
                        className.contains("ListView") || className.contains("ScrollView"))) {
                return current
            }
            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return null
    }

    private fun findChatInput(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current.className?.toString() == "android.widget.EditText") {
                return current
            }
            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return null
    }

    private fun getChatTitle(node: AccessibilityNodeInfo): String {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val id = current.viewIdResourceName ?: ""
            if (id.contains("conversation_contact_name") || id.contains("contact_name")) {
                val text = current.text?.toString() ?: ""
                if (text.isNotEmpty()) return text
            }
            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return ""
    }

    private fun fillTextMessage(inputNode: AccessibilityNodeInfo, message: String): Boolean {
        val arguments = Bundle()
        arguments.putCharSequence(
            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
            message
        )
        return inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val id = current.viewIdResourceName ?: ""

            if (desc == "send" || desc.contains("পাঠান") || id.contains("send")) {
                if (current.isClickable) return current
            }

            // Fallback: clickable ImageButton
            if (current.className?.toString() == "android.widget.ImageButton" && current.isClickable) {
                if (id.contains("send") || desc.contains("send") || desc.contains("পাঠান")) {
                    return current
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return null
    }

    private fun hasDoubleBlueTick(containerNode: AccessibilityNodeInfo): Boolean {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(containerNode)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val id = current.viewIdResourceName?.lowercase(Locale.getDefault()) ?: ""

            if (desc.contains("read") || desc.contains("seen") ||
                desc.contains("পঠিত") || desc.contains("blue") || desc.contains("double check")) {
                return true
            }

            if (id.contains("status") && (desc.contains("read") || desc.contains("seen") ||
                        desc.contains("পঠিত"))) {
                return true
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return false
    }

    private fun findTargetContactNode(node: AccessibilityNodeInfo, targets: List<String>): AccessibilityNodeInfo? {
        val onlyBlueTicks = sharedPreferences.getBoolean(KEY_ONLY_BLUE_TICKS, false)
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)

            val text = current.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
            if (text.isNotEmpty()) {
                for (target in targets) {
                    if (text.contains(target) && !completedContacts.contains(text)) {
                        var clickTarget: AccessibilityNodeInfo? = current
                        while (clickTarget != null && !clickTarget.isClickable) {
                            clickTarget = clickTarget.parent
                        }
                        if (clickTarget != null) {
                            if (onlyBlueTicks) {
                                val row = findChatRowContainer(current)
                                if (row != null && !hasDoubleBlueTick(row)) {
                                    AutomationLogManager.log(
                                        "Skip '$text': no double blue tick",
                                        AutomationLogManager.LogType.WARNING
                                    )
                                    continue
                                }
                            }
                            return current
                        }
                    }
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) queue.add(child)
            }
        }
        return null
    }

    private fun findChatRowContainer(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            val className = current.className?.toString() ?: ""
            if (current.isClickable && (className.contains("RelativeLayout") ||
                        className.contains("ViewGroup") || className.contains("LinearLayout"))) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun clickNode(node: AccessibilityNodeInfo): Boolean {
        var tempNode: AccessibilityNodeInfo? = node
        while (tempNode != null) {
            if (tempNode.isClickable) {
                return tempNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            tempNode = tempNode.parent
        }
        return false
    }

    // ============================================================================
    //  Human-Like Scroll (with BypassManager randomization)
    // ============================================================================

    private fun scrollMainListHumanLike() {
        AutomationLogManager.log(
            "Scrolling (human-like)...",
            AutomationLogManager.LogType.INFO
        )
        isScrolling = true

        val swipePath = Path()
        val resources = resources
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        // === BYPASS: Random scroll pattern ===
        val (duration, startYPercent, endYPercent) = BypassManager.getScrollPattern(this)

        // Random X offset (যেন প্রতিবার আলাদা জায়গায় swipe করি)
        val startX = (width / 2f) + ((-30..30).random())
        val startY = height * startYPercent + ((-20..20).random())
        val endX = startX + ((-15..15).random())
        val endY = height * endYPercent + ((-20..20).random())

        swipePath.moveTo(startX, startY)
        swipePath.lineTo(endX, endY)

        val gestureBuilder = GestureDescription.Builder()
        val stroke = GestureDescription.StrokeDescription(swipePath, 50, duration)
        gestureBuilder.addStroke(stroke)

        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                isScrolling = false
                AutomationLogManager.log(
                    "Scroll completed",
                    AutomationLogManager.LogType.INFO
                )
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                isScrolling = false
                AutomationLogManager.log(
                    "Scroll cancelled",
                    AutomationLogManager.LogType.WARNING
                )
            }
        }, null)
    }
}
