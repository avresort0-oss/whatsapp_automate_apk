package com.example

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ============================================================================
 * MainViewModel - আপগ্রেডেড ভার্সন
 * ============================================================================
 *
 * নতুন Features:
 *  ✅ Image URI internal storage-এ persist করা (bug fix)
 *  ✅ Bypass settings গুলো ViewModel থেকে control
 *  ✅ ChatListScanner এর সাথে integration
 *  ✅ Scan-only mode toggle
 * ============================================================================
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        WhatsAppAutomationService.PREFS_NAME, Context.MODE_PRIVATE
    )

    // === Existing State Flows ===
    private val _targetContacts = MutableStateFlow("")
    val targetContacts: StateFlow<String> = _targetContacts.asStateFlow()

    private val _quickReplyMessage = MutableStateFlow("")
    val quickReplyMessage: StateFlow<String> = _quickReplyMessage.asStateFlow()

    private val _automationActive = MutableStateFlow(false)
    val automationActive: StateFlow<Boolean> = _automationActive.asStateFlow()

    private val _humanDelay = MutableStateFlow(2500)
    val humanDelay: StateFlow<Int> = _humanDelay.asStateFlow()

    private val _onlyBlueTicks = MutableStateFlow(false)
    val onlyBlueTicks: StateFlow<Boolean> = _onlyBlueTicks.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled.asStateFlow()

    // === New: Image Settings ===
    private val _imageFlowEnabled = MutableStateFlow(false)
    val imageFlowEnabled: StateFlow<Boolean> = _imageFlowEnabled.asStateFlow()

    private val _imageCaption = MutableStateFlow("")
    val imageCaption: StateFlow<String> = _imageCaption.asStateFlow()

    // === New: Bypass Settings ===
    private val _bypassEnabled = MutableStateFlow(true)
    val bypassEnabled: StateFlow<Boolean> = _bypassEnabled.asStateFlow()

    private val _bypassMinDelay = MutableStateFlow(3000)
    val bypassMinDelay: StateFlow<Int> = _bypassMinDelay.asStateFlow()

    private val _bypassMaxDelay = MutableStateFlow(8000)
    val bypassMaxDelay: StateFlow<Int> = _bypassMaxDelay.asStateFlow()

    private val _bypassTapRandomness = MutableStateFlow(8)
    val bypassTapRandomness: StateFlow<Int> = _bypassTapRandomness.asStateFlow()

    private val _bypassTypingSim = MutableStateFlow(true)
    val bypassTypingSim: StateFlow<Boolean> = _bypassTypingSim.asStateFlow()

    private val _bypassTypingSpeed = MutableStateFlow(38)
    val bypassTypingSpeed: StateFlow<Int> = _bypassTypingSpeed.asStateFlow()

    private val _bypassSessionLimit = MutableStateFlow(25)
    val bypassSessionLimit: StateFlow<Int> = _bypassSessionLimit.asStateFlow()

    private val _bypassNightMode = MutableStateFlow(true)
    val bypassNightMode: StateFlow<Boolean> = _bypassNightMode.asStateFlow()

    private val _bypassRandomPauses = MutableStateFlow(true)
    val bypassRandomPauses: StateFlow<Boolean> = _bypassRandomPauses.asStateFlow()

    private val _bypassReadBeforeReply = MutableStateFlow(true)
    val bypassReadBeforeReply: StateFlow<Boolean> = _bypassReadBeforeReply.asStateFlow()

    private val _bypassMaxPerHour = MutableStateFlow(30)
    val bypassMaxPerHour: StateFlow<Int> = _bypassMaxPerHour.asStateFlow()

    // === New: Scan Settings ===
    private val _scanListOnly = MutableStateFlow(false)
    val scanListOnly: StateFlow<Boolean> = _scanListOnly.asStateFlow()

    // === Simulator States (existing) ===
    private val _isSimulatorRunning = MutableStateFlow(false)
    val isSimulatorRunning: StateFlow<Boolean> = _isSimulatorRunning.asStateFlow()

    private val _simulatorStatus = MutableStateFlow("Idle")
    val simulatorStatus: StateFlow<String> = _simulatorStatus.asStateFlow()

    private val _simulatorScreen = MutableStateFlow(SimulatorScreen.CHAT_LIST)
    val simulatorScreen: StateFlow<SimulatorScreen> = _simulatorScreen.asStateFlow()

    private val _activeChatName = MutableStateFlow("")
    val activeChatName: StateFlow<String> = _activeChatName.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _selectedFilter = MutableStateFlow(ContactFilter.ALL)
    val selectedFilter: StateFlow<ContactFilter> = _selectedFilter.asStateFlow()

    val mockChats = mutableStateListOf<MockChat>()
    val activeChatMessages = mutableStateListOf<MockMessage>()

    // === ChatListScanner state exposed to UI ===
    val scannedChats = ChatListScanner.scannedChats
    val scanStatus = ChatListScanner.scanStatus
    val totalChatsFound = ChatListScanner.totalChatsFound

    enum class SimulatorScreen { CHAT_LIST, CHAT_DETAIL }
    enum class ContactFilter { ALL, READ, UNREAD, BLUE_TICK }

    data class MockChat(
        val id: Int,
        val name: String,
        val lastMessage: String,
        val time: String,
        val colorHex: Long,
        var isMatched: Boolean = false,
        var isCompleted: Boolean = false,
        val lastMessageSentByMe: Boolean = false,
        val hasDoubleBlueTick: Boolean = false,
        val isUnread: Boolean = false,
        val unreadCount: Int = 0
    )

    data class MockMessage(
        val text: String,
        val isSender: Boolean,
        val time: String,
        val imageUri: Uri? = null
    )

    init {
        // === Load existing SharedPreferences ===
        _targetContacts.value = sharedPrefs.getString(
            WhatsAppAutomationService.KEY_TARGET_CONTACTS, "Abir, Jahid, Robin"
        ) ?: "Abir, Jahid, Robin"

        _quickReplyMessage.value = sharedPrefs.getString(
            WhatsAppAutomationService.KEY_QUICK_REPLY,
            "Assalamu Alaikum! This is an automated reply with your selected image."
        ) ?: "Assalamu Alaikum! This is an automated reply with your selected image."

        _automationActive.value = sharedPrefs.getBoolean(
            WhatsAppAutomationService.KEY_AUTOMATION_ACTIVE, false
        )

        _humanDelay.value = sharedPrefs.getInt(
            WhatsAppAutomationService.KEY_HUMAN_DELAY, 2500
        )

        _onlyBlueTicks.value = sharedPrefs.getBoolean(
            WhatsAppAutomationService.KEY_ONLY_BLUE_TICKS, false
        )

        // === Load new image settings ===
        _imageFlowEnabled.value = sharedPrefs.getBoolean(
            ImageAttachmentHelper.KEY_IMAGE_ENABLED, false
        )
        _imageCaption.value = sharedPrefs.getString(
            ImageAttachmentHelper.KEY_IMAGE_CAPTION, ""
        ) ?: ""

        // যদি image path save করা থাকে, সেটা থেকে URI বানাও
        val savedPath = sharedPrefs.getString(ImageAttachmentHelper.KEY_IMAGE_PATH, null)
        if (savedPath != null) {
            _selectedImageUri.value = Uri.fromFile(java.io.File(savedPath))
        }

        // === Load bypass settings ===
        val context = getApplication<Application>()
        _bypassEnabled.value = BypassManager.isBypassEnabled(context)
        _bypassMinDelay.value = BypassManager.getIntSetting(
            context, BypassManager.KEY_MIN_DELAY_MS, BypassManager.Defaults.MIN_DELAY_MS
        )
        _bypassMaxDelay.value = BypassManager.getIntSetting(
            context, BypassManager.KEY_MAX_DELAY_MS, BypassManager.Defaults.MAX_DELAY_MS
        )
        _bypassTapRandomness.value = BypassManager.getIntSetting(
            context, BypassManager.KEY_TAP_RANDOMNESS_DP, BypassManager.Defaults.TAP_RANDOMNESS_DP
        )
        _bypassTypingSim.value = BypassManager.getBooleanSetting(
            context, BypassManager.KEY_TYPING_SIMULATION, BypassManager.Defaults.TYPING_SIMULATION
        )
        _bypassTypingSpeed.value = BypassManager.getIntSetting(
            context, BypassManager.KEY_TYPING_SPEED_WPM, BypassManager.Defaults.TYPING_SPEED_WPM
        )
        _bypassSessionLimit.value = BypassManager.getIntSetting(
            context, BypassManager.KEY_SESSION_LIMIT, BypassManager.Defaults.SESSION_LIMIT
        )
        _bypassNightMode.value = BypassManager.getBooleanSetting(
            context, BypassManager.KEY_NIGHT_MODE, BypassManager.Defaults.NIGHT_MODE
        )
        _bypassRandomPauses.value = BypassManager.getBooleanSetting(
            context, BypassManager.KEY_RANDOM_PAUSES, BypassManager.Defaults.RANDOM_PAUSES
        )
        _bypassReadBeforeReply.value = BypassManager.getBooleanSetting(
            context, BypassManager.KEY_READ_BEFORE_REPLY, BypassManager.Defaults.READ_BEFORE_REPLY
        )
        _bypassMaxPerHour.value = BypassManager.getIntSetting(
            context, BypassManager.KEY_MAX_PER_HOUR, BypassManager.Defaults.MAX_PER_HOUR
        )

        _scanListOnly.value = sharedPrefs.getBoolean(
            WhatsAppAutomationService.KEY_SCAN_LIST_ONLY, false
        )

        resetMockChats()
    }

    // ============================================================================
    //  Existing Methods (kept same)
    // ============================================================================

    fun checkServiceStatus() {
        _isServiceEnabled.value = isAccessibilityServiceEnabled(
            getApplication(),
            WhatsAppAutomationService::class.java
        )
    }

    fun updateTargetContacts(contacts: String) {
        _targetContacts.value = contacts
        sharedPrefs.edit().putString(
            WhatsAppAutomationService.KEY_TARGET_CONTACTS, contacts
        ).apply()
    }

    fun updateQuickReplyMessage(message: String) {
        _quickReplyMessage.value = message
        sharedPrefs.edit().putString(
            WhatsAppAutomationService.KEY_QUICK_REPLY, message
        ).apply()
    }

    fun updateHumanDelay(delayMs: Int) {
        _humanDelay.value = delayMs
        sharedPrefs.edit().putInt(
            WhatsAppAutomationService.KEY_HUMAN_DELAY, delayMs
        ).apply()
    }

    fun updateOnlyBlueTicks(enabled: Boolean) {
        _onlyBlueTicks.value = enabled
        sharedPrefs.edit().putBoolean(
            WhatsAppAutomationService.KEY_ONLY_BLUE_TICKS, enabled
        ).apply()
        if (enabled) {
            AutomationLogManager.log(
                "Smart Double Blue Tick validation activated.",
                AutomationLogManager.LogType.SUCCESS
            )
        }
    }

    fun updateSelectedFilter(filter: ContactFilter) {
        _selectedFilter.value = filter
        AutomationLogManager.log(
            "Filter: ${filter.name}",
            AutomationLogManager.LogType.INFO
        )
    }

    fun toggleAutomation(active: Boolean) {
        _automationActive.value = active
        sharedPrefs.edit().putBoolean(
            WhatsAppAutomationService.KEY_AUTOMATION_ACTIVE, active
        ).apply()

        if (active) {
            // Bypass session শুরু করো
            BypassManager.startSession(getApplication())
            AutomationLogManager.log(
                "🚀 Automation Started! Bypass: ${if (_bypassEnabled.value) "ON" else "OFF"}",
                AutomationLogManager.LogType.SUCCESS
            )
            AutomationLogManager.log("Targets: ${_targetContacts.value}")
            if (_imageFlowEnabled.value) {
                AutomationLogManager.log(
                    "Image flow: ENABLED",
                    AutomationLogManager.LogType.INFO
                )
            }
        } else {
            AutomationLogManager.log(
                "⏹ Automation Stopped.\n${BypassManager.getSessionStats()}",
                AutomationLogManager.LogType.WARNING
            )
        }
    }

    // ============================================================================
    //  New: Image Handling (Bug Fix — URI কে internal file-এ convert করে persist)
    // ============================================================================

    /**
     * ইউজার যেই URI তা গ্যালারি থেকে পেয়েছে, সেটাকে:
     *  1. Internal storage-এ copy করে (permanent access)
     *  2. SharedPreferences-এ path save করে
     *  3. Image flow enable করে
     */
    fun selectImage(uri: Uri?) {
        if (uri == null) {
            clearImage()
            return
        }

        val context = getApplication<Application>()
        val savedPath = ImageAttachmentHelper.copyImageToInternalStorage(context, uri)

        if (savedPath != null) {
            _selectedImageUri.value = Uri.fromFile(java.io.File(savedPath))
            ImageAttachmentHelper.saveImagePath(context, savedPath)
            _imageFlowEnabled.value = true
            sharedPrefs.edit().putBoolean(
                WhatsAppAutomationService.KEY_USE_IMAGE_FLOW, true
            ).apply()

            AutomationLogManager.log(
                "✅ Image saved & flow enabled!",
                AutomationLogManager.LogType.SUCCESS
            )
        } else {
            AutomationLogManager.log(
                "❌ Failed to save image",
                AutomationLogManager.LogType.ERROR
            )
        }
    }

    fun clearImage() {
        _selectedImageUri.value = null
        _imageFlowEnabled.value = false
        ImageAttachmentHelper.clearSavedImage(getApplication())
        sharedPrefs.edit().putBoolean(
            WhatsAppAutomationService.KEY_USE_IMAGE_FLOW, false
        ).apply()
    }

    fun updateImageCaption(caption: String) {
        _imageCaption.value = caption
        sharedPrefs.edit().putString(
            ImageAttachmentHelper.KEY_IMAGE_CAPTION, caption
        ).apply()
    }

    fun updateImageFlowEnabled(enabled: Boolean) {
        _imageFlowEnabled.value = enabled
        sharedPrefs.edit().putBoolean(
            WhatsAppAutomationService.KEY_USE_IMAGE_FLOW, enabled
        ).apply()
        if (enabled && _selectedImageUri.value == null) {
            AutomationLogManager.log(
                "⚠️ Image flow enabled but no image selected",
                AutomationLogManager.LogType.WARNING
            )
        }
    }

    // ============================================================================
    //  New: Bypass Settings Update Methods
    // ============================================================================

    fun updateBypassEnabled(enabled: Boolean) {
        _bypassEnabled.value = enabled
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_BYPASS_ENABLED, enabled
        )
        AutomationLogManager.log(
            "Bypass: ${if (enabled) "ENABLED" else "DISABLED"}",
            if (enabled) AutomationLogManager.LogType.SUCCESS else AutomationLogManager.LogType.WARNING
        )
    }

    fun updateBypassMinDelay(ms: Int) {
        _bypassMinDelay.value = ms
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_MIN_DELAY_MS, ms
        )
    }

    fun updateBypassMaxDelay(ms: Int) {
        _bypassMaxDelay.value = ms
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_MAX_DELAY_MS, ms
        )
    }

    fun updateBypassTapRandomness(dp: Int) {
        _bypassTapRandomness.value = dp
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_TAP_RANDOMNESS_DP, dp
        )
    }

    fun updateBypassTypingSim(enabled: Boolean) {
        _bypassTypingSim.value = enabled
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_TYPING_SIMULATION, enabled
        )
    }

    fun updateBypassTypingSpeed(wpm: Int) {
        _bypassTypingSpeed.value = wpm
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_TYPING_SPEED_WPM, wpm
        )
    }

    fun updateBypassSessionLimit(limit: Int) {
        _bypassSessionLimit.value = limit
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_SESSION_LIMIT, limit
        )
    }

    fun updateBypassNightMode(enabled: Boolean) {
        _bypassNightMode.value = enabled
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_NIGHT_MODE, enabled
        )
    }

    fun updateBypassRandomPauses(enabled: Boolean) {
        _bypassRandomPauses.value = enabled
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_RANDOM_PAUSES, enabled
        )
    }

    fun updateBypassReadBeforeReply(enabled: Boolean) {
        _bypassReadBeforeReply.value = enabled
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_READ_BEFORE_REPLY, enabled
        )
    }

    fun updateBypassMaxPerHour(limit: Int) {
        _bypassMaxPerHour.value = limit
        BypassManager.updateSetting(
            getApplication(), BypassManager.KEY_MAX_PER_HOUR, limit
        )
    }

    fun resetBypassToDefaults() {
        BypassManager.resetToDefaults(getApplication())
        // Reload সব bypass settings
        val context = getApplication<Application>()
        _bypassEnabled.value = BypassManager.isBypassEnabled(context)
        _bypassMinDelay.value = BypassManager.Defaults.MIN_DELAY_MS
        _bypassMaxDelay.value = BypassManager.Defaults.MAX_DELAY_MS
        _bypassTapRandomness.value = BypassManager.Defaults.TAP_RANDOMNESS_DP
        _bypassTypingSim.value = BypassManager.Defaults.TYPING_SIMULATION
        _bypassTypingSpeed.value = BypassManager.Defaults.TYPING_SPEED_WPM
        _bypassSessionLimit.value = BypassManager.Defaults.SESSION_LIMIT
        _bypassNightMode.value = BypassManager.Defaults.NIGHT_MODE
        _bypassRandomPauses.value = BypassManager.Defaults.RANDOM_PAUSES
        _bypassReadBeforeReply.value = BypassManager.Defaults.READ_BEFORE_REPLY
        _bypassMaxPerHour.value = BypassManager.Defaults.MAX_PER_HOUR

        AutomationLogManager.log(
            "Bypass settings reset to defaults",
            AutomationLogManager.LogType.SUCCESS
        )
    }

    // ============================================================================
    //  New: Scan-Only Mode
    // ============================================================================

    fun updateScanListOnly(enabled: Boolean) {
        _scanListOnly.value = enabled
        sharedPrefs.edit().putBoolean(
            WhatsAppAutomationService.KEY_SCAN_LIST_ONLY, enabled
        ).apply()
        if (enabled) {
            ChatListScanner.resetScan()
            AutomationLogManager.log(
                "📋 Scan-only mode started — will collect entire chat list",
                AutomationLogManager.LogType.INFO
            )
        } else {
            ChatListScanner.completeScan()
            AutomationLogManager.log(
                "Scan-only mode stopped. ${ChatListScanner.getScanSummary()}",
                AutomationLogManager.LogType.INFO
            )
        }
    }

    // ============================================================================
    //  Existing: Simulator
    // ============================================================================

    private fun resetMockChats() {
        mockChats.clear()
        mockChats.addAll(
            listOf(
                MockChat(1, "Abir Sheikh", "Kire, ki khobor tore khuje paoa jay na", "04:32 PM", 0xFF0D9488, lastMessageSentByMe = false, hasDoubleBlueTick = false, isUnread = true, unreadCount = 2),
                MockChat(2, "Matiur Rahman", "Are boss, automate app ta check koren", "03:15 PM", 0xFF0284C7, lastMessageSentByMe = false, hasDoubleBlueTick = false, isUnread = true, unreadCount = 1),
                MockChat(3, "Jahidul Hasan", "Image load hocche ki?", "02:10 PM", 0xFF4F46E5, lastMessageSentByMe = true, hasDoubleBlueTick = true, isUnread = false),
                MockChat(4, "Unknown Number", "Who are you?", "Yesterday", 0xFF64748B, lastMessageSentByMe = false, hasDoubleBlueTick = false, isUnread = true, unreadCount = 1),
                MockChat(5, "Robin Mia", "Ami asbo naki bikal belae?", "Yesterday", 0xFFEA580C, lastMessageSentByMe = true, hasDoubleBlueTick = true, isUnread = false),
                MockChat(6, "Amir Hossain", "Taka ta paile janas", "2 days ago", 0xFFD97706, lastMessageSentByMe = true, hasDoubleBlueTick = false, isUnread = false),
                MockChat(7, "Sujon Kanti", "Assalamu Alaikum", "3 days ago", 0xFF059669, lastMessageSentByMe = false, hasDoubleBlueTick = false, isUnread = false)
            )
        )
    }

    fun runAutomationSimulation() {
        if (_isSimulatorRunning.value) return

        viewModelScope.launch {
            _isSimulatorRunning.value = true
            _simulatorScreen.value = SimulatorScreen.CHAT_LIST
            _simulatorStatus.value = "Starting automation (bypass enabled)..."
            resetMockChats()
            delay(1500)

            val targetList = _targetContacts.value.split(",")
                .map { it.trim().lowercase(Locale.getDefault()) }
                .filter { it.isNotEmpty() }

            if (targetList.isEmpty()) {
                _simulatorStatus.value = "No keywords configured!"
                _isSimulatorRunning.value = false
                return@launch
            }

            _simulatorStatus.value = "Scanning chat list..."
            delay(1500)

            // === BYPASS: Human-like scroll simulation ===
            _simulatorStatus.value = "Human-like scrolling (random velocity)..."
            delay(2000)

            for (index in mockChats.indices) {
                val chat = mockChats[index]

                val matchesFilter = when (_selectedFilter.value) {
                    ContactFilter.ALL -> true
                    ContactFilter.READ -> !chat.isUnread
                    ContactFilter.UNREAD -> chat.isUnread
                    ContactFilter.BLUE_TICK -> chat.hasDoubleBlueTick
                }

                if (!matchesFilter) continue

                _simulatorStatus.value = "Checking: '${chat.name}'"
                delay(1200)

                val isMatch = targetList.any { chat.name.lowercase(Locale.getDefault()).contains(it) }
                if (isMatch) {
                    if (_onlyBlueTicks.value && !chat.hasDoubleBlueTick) {
                        mockChats[index] = chat.copy(isMatched = true)
                        _simulatorStatus.value = "SKIPPED: '${chat.name}' — no blue tick"
                        delay(2200)
                        mockChats[index] = chat.copy(isMatched = false)
                        continue
                    }

                    mockChats[index] = chat.copy(isMatched = true)
                    _simulatorStatus.value = "✅ MATCH: '${chat.name}'"
                    delay(1500)

                    _simulatorStatus.value = "Opening chat..."
                    delay(1000)
                    _activeChatName.value = chat.name
                    activeChatMessages.clear()
                    activeChatMessages.addAll(
                        listOf(MockMessage("Kire, ki khobor tore khuje paoa jay na", false, "04:32 PM"))
                    )
                    _simulatorScreen.value = SimulatorScreen.CHAT_DETAIL
                    delay(1500)

                    // === BYPASS: Read-before-reply simulation ===
                    if (_bypassReadBeforeReply.value) {
                        _simulatorStatus.value = "📖 Reading messages..."
                        delay(2500)
                    }

                    // === BYPASS: Typing simulation ===
                    if (_bypassTypingSim.value) {
                        _simulatorStatus.value = "⌨️ Typing (simulating ${_bypassTypingSpeed.value} WPM)..."
                        _isTyping.value = true
                        val typingTime = BypassManager.calculateTypingTime(
                            getApplication(), _quickReplyMessage.value
                        )
                        delay(typingTime)
                        _isTyping.value = false
                    }

                    val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    val replyText = _quickReplyMessage.value

                    activeChatMessages.add(
                        MockMessage(
                            text = replyText,
                            isSender = true,
                            time = currentTime,
                            imageUri = _selectedImageUri.value
                        )
                    )

                    _simulatorStatus.value = "✅ Reply sent!"
                    delay(1500)

                    // === BYPASS: Post-send random delay ===
                    val postDelay = (_bypassMinDelay.value.._bypassMaxDelay.value).random()
                    _simulatorStatus.value = "⏱ Post-send delay: ${postDelay}ms"
                    delay(postDelay.toLong())

                    mockChats[index] = chat.copy(isMatched = false, isCompleted = true, lastMessage = replyText)
                    _simulatorStatus.value = "Returning to chat list..."
                    delay(1500)

                    _simulatorScreen.value = SimulatorScreen.CHAT_LIST
                    delay(1000)
                }
            }

            _simulatorStatus.value = "✅ Automation finished!\n${BypassManager.getSessionStats()}"
            delay(2000)
            _isSimulatorRunning.value = false
            _simulatorStatus.value = "Completed"
        }
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out android.accessibilityservice.AccessibilityService>
    ): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
                as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC
        )
        for (enabledService in enabledServices) {
            val enabledServiceInfo = enabledService.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName == context.packageName &&
                enabledServiceInfo.name == service.name) {
                return true
            }
        }
        return false
    }
}
