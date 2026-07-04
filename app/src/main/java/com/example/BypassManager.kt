package com.example

import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Random
import kotlin.math.abs

/**
 * ============================================================================
 * BypassManager - WhatsApp Bot Detection Bypass ও Anti-Detection Logic
 * ============================================================================
 *
 * এই ক্লাসটি WhatsApp-এর bot detection system এড়াতে বিভিন্ন human-like
 * behavior simulate করে। WhatsApp সাধারণত নিচের প্যাটার্ন গুলো detect করে:
 *
 *  1. Fixed timing (সবসময় ২.৫ সেকেন্ড পর পর action)
 *  2. Exact tap coordinates (সবসময় center-এ tap)
 *  3. Instant text fill (character-by-character typing না)
 *  4. Constant scroll velocity
 *  5. No "typing..." indicator pause
 *  6. Too many messages per session
 *  7. Activity during unusual hours (যেমন রাত ৩টায়)
 *  8. Perfect sequential behavior (কোনো random pause না)
 *
 * BypassManager এই সব পয়েন্ট cover করে।
 * ============================================================================
 */
object BypassManager {

    private const val PREFS_NAME = "WhatsAppAutomatorBypassPrefs"

    // ====== Session State Tracking ======
    private var sessionMessageCount = 0
    private var sessionStartTime = 0L
    private var lastMessageTime = 0L
    private var consecutiveErrors = 0
    private val random = Random()

    // ====== Bypass Settings Keys ======
    const val KEY_BYPASS_ENABLED = "bypass_enabled"
    const val KEY_MIN_DELAY_MS = "bypass_min_delay_ms"
    const val KEY_MAX_DELAY_MS = "bypass_max_delay_ms"
    const val KEY_TAP_RANDOMNESS_DP = "bypass_tap_randomness_dp"
    const val KEY_TYPING_SIMULATION = "bypass_typing_simulation"
    const val KEY_TYPING_SPEED_WPM = "bypass_typing_speed_wpm"
    const val KEY_SESSION_LIMIT = "bypass_session_limit"
    const val KEY_NIGHT_MODE = "bypass_night_mode"
    const val KEY_RANDOM_PAUSES = "bypass_random_pauses"
    const val KEY_BACKOFF_ON_ERROR = "bypass_backoff_on_error"
    const val KEY_READ_BEFORE_REPLY = "bypass_read_before_reply"
    const val KEY_MAX_PER_HOUR = "bypass_max_per_hour"
    private const val KEY_LAST_SESSION_DATE = "bypass_last_session_date"
    private const val KEY_HOURLY_COUNT = "bypass_hourly_count"

    /**
     * Default ভ্যালু গুলো — WhatsApp-এর detection threshold এর নিচে রাখা হয়েছে।
     */
    object Defaults {
        const val BYPASS_ENABLED = true
        const val MIN_DELAY_MS = 3000       // ৩ সেকেন্ড
        const val MAX_DELAY_MS = 8000       // ৮ সেকেন্ড
        const val TAP_RANDOMNESS_DP = 8     // ±৮ dp offset
        const val TYPING_SIMULATION = true
        const val TYPING_SPEED_WPM = 38     // মানুষের average typing speed
        const val SESSION_LIMIT = 25        // এক session-এ সর্বোচ্চ ২৫ message
        const val NIGHT_MODE = true         // রাতে কম message
        const val RANDOM_PAUSES = true      // মাঝে মাঝে long pause
        const val BACKOFF_ON_ERROR = true   // error হলে delay বাড়ানো
        const val READ_BEFORE_REPLY = true  // reply করার আগে chat পড়ার simulation
        const val MAX_PER_HOUR = 30         // প্রতি ঘণ্টায় সর্বোচ্চ ৩০
    }

    /**
     * SharedPreferences instance নিয়ে আসে।
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Bypass enabled কিনা চেক করে।
     */
    fun isBypassEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BYPASS_ENABLED, Defaults.BYPASS_ENABLED)
    }

    /**
     * একটা নির্দিষ্ট setting এর ভ্যালু পড়ে।
     */
    fun getIntSetting(context: Context, key: String, default: Int): Int {
        return getPrefs(context).getInt(key, default)
    }

    fun getBooleanSetting(context: Context, key: String, default: Boolean): Boolean {
        return getPrefs(context).getBoolean(key, default)
    }

    /**
     * Setting update করে।
     */
    fun updateSetting(context: Context, key: String, value: Any) {
        val editor = getPrefs(context).edit()
        when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is String -> editor.putString(key, value)
        }
        editor.apply()
    }

    // ============================================================================
    //  ১. Random Delay Generation (Fixed timing detection bypass)
    // ============================================================================

    /**
     * Human-like random delay তৈরি করে।
     *
     * - Base delay: min ও max এর মধ্যে random
     * - Probability boost: মাঝে মাঝে আরো বেশি delay (যেমন মানুষ যখন ভাবে)
     * - Last message time থেকে elapsed time consider করে
     * - Consecutive error থাকলে delay exponentially বাড়ে (backoff)
     */
    suspend fun getHumanDelay(context: Context): Long {
        if (!isBypassEnabled(context)) {
            // Bypass বন্ধ থাকলে fixed delay ব্যবহার করো
            return 2500L
        }

        val minDelay = getIntSetting(context, KEY_MIN_DELAY_MS, Defaults.MIN_DELAY_MS)
        val maxDelay = getIntSetting(context, KEY_MAX_DELAY_MS, Defaults.MAX_DELAY_MS)
        val randomPauses = getBooleanSetting(context, KEY_RANDOM_PAUSES, Defaults.RANDOM_PAUSES)
        val backoffOnError = getBooleanSetting(context, KEY_BACKOFF_ON_ERROR, Defaults.BACKOFF_ON_ERROR)

        // Base random delay
        var delay = (minDelay + random.nextInt(maxDelay - minDelay)).toLong()

        // ১৫% সম্ভাবনায় long pause (৫-১৫ সেকেন্ড) — যেন মানুষ যেন দেখছে বা ভাবছে
        if (randomPauses && random.nextInt(100) < 15) {
            delay += 5000 + random.nextInt(10000)
        }

        // Error backoff — পরপর error হলে delay বাড়াও
        if (backoffOnError && consecutiveErrors > 0) {
            val backoffMultiplier = (1 shl consecutiveErrors.coerceAtMost(4)).toLong() // 2, 4, 8, 16
            delay *= backoffMultiplier
        }

        // Night mode — রাত ১১টা থেকে সকাল ৬টা পর্যন্ত delay দ্বিগুণ
        if (getBooleanSetting(context, KEY_NIGHT_MODE, Defaults.NIGHT_MODE)) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hour >= 23 || hour < 6) {
                delay *= 2
            }
        }

        return delay
    }

    // ============================================================================
    //  ২. Random Tap Coordinates (Exact tap detection bypass)
    // ============================================================================

    /**
     * একটা node-এর center coordinate এ ছোট random offset যোগ করে।
     *
     * মানুষ কখনো exact center-এ tap করে না — সবসময় একটু offset থাকে।
     * এই offset ছাড়া WhatsApp easily detect করতে পারে যে এটা bot।
     *
     * @return Pair<Float, Float> = (offsetX, offsetY) in pixels
     */
    fun getTapOffset(context: Context): Pair<Float, Float> {
        if (!isBypassEnabled(context)) {
            return 0f to 0f
        }

        val randomnessDp = getIntSetting(context, KEY_TAP_RANDOMNESS_DP, Defaults.TAP_RANDOMNESS_DP)
        val density = context.resources.displayMetrics.density
        val randomnessPx = randomnessDp * density

        val offsetX = (random.nextFloat() * 2 - 1) * randomnessPx
        val offsetY = (random.nextFloat() * 2 - 1) * randomnessPx

        return offsetX to offsetY
    }

    // ============================================================================
    //  ३. Typing Simulation ("typing..." indicator pause ও character-by-character)
    // ============================================================================

    /**
     * একটা message টাইপ করতে কত সময় লাগবে সেটা calculate করে।
     *
     * - Average typing speed: 38 WPM (words per minute)
     * - প্রতি character-এ 50-150ms random delay
     * - Space ও punctuation-এ সামান্য বেশি delay (যেন ভাবছে)
     * - WhatsApp "typing..." indicator অন্তত ১ সেকেন্ড দেখায়
     *
     * @param message যে message টাইপ করবে
     * @return মোট typing time মিলিসেকেন্ডে
     */
    fun calculateTypingTime(context: Context, message: String): Long {
        if (!isBypassEnabled(context)) return 100L

        val typingSim = getBooleanSetting(context, KEY_TYPING_SIMULATION, Defaults.TYPING_SIMULATION)
        if (!typingSim) return 200L

        val wpm = getIntSetting(context, KEY_TYPING_SPEED_WPM, Defaults.TYPING_SPEED_WPM)
        val avgCharPerMinute = wpm * 5 // average word length ৫
        val baseMsPerChar = 60000L / avgCharPerMinute

        var totalMs = 0L
        for (char in message) {
            // প্রতি character-এ ±40% random variation
            val variation = 0.6 + random.nextDouble() * 0.8
            totalMs += (baseMsPerChar * variation).toLong()

            // Space, comma, period এর পর সামান্য বেশি pause
            if (char == ' ' || char == ',' || char == '.' || char == '?' || char == '!') {
                totalMs += 100 + random.nextInt(300)
            }
        }

        // WhatsApp "typing..." indicator অন্তত ১ সেকেন্ড দেখায় — সেটা ensure করো
        return maxOf(totalMs, 1000L)
    }

    /**
     * Message টাইপ করার simulation — character-by-character pause দিয়ে।
     * এটা সরাসরি EditText-এ text set না করে ধীরে ধীরে append করে।
     */
    suspend fun simulateTypingPause(context: Context, message: String) {
        val totalTypingTime = calculateTypingTime(context, message)
        // আসলে character-by-character set করা সম্ভব না accessibility API দিয়ে দ্রুত,
        // তাই শুধু pause দিচ্ছি যাতে WhatsApp "typing..." indicator দেখায়।
        delay(totalTypingTime)
    }

    // ============================================================================
    //  ४. Read-Before-Reply Simulation (Natural reading behavior)
    // ============================================================================

    /**
     * Reply করার আগে chat "পড়ার" simulation করে।
     *
     * মানুষ যখন কোনো chat খোলে, সে আগে message পড়ে, তারপর reply করে।
     * Bot সরাসরি message পাঠায় — এটা detect করা সহজ।
     *
     * @param messageCount চ্যাটে কয়টা message আছে (approximate)
     */
    suspend fun simulateReading(context: Context, messageCount: Int = 3) {
        if (!isBypassEnabled(context)) return

        val readBeforeReply = getBooleanSetting(context, KEY_READ_BEFORE_REPLY, Defaults.READ_BEFORE_REPLY)
        if (!readBeforeReply) return

        // প্রতি message পড়তে গড়ে ৮০০ms-২৫০০ms লাগে
        val perMessageMs = 800 + random.nextInt(1700)
        val totalReadTime = perMessageMs * messageCount.coerceAtMost(5)

        // পুরো read time এর চেয়ে সামান্য কম (মানুষ সব message পড়ে না)
        val actualDelay = (totalReadTime * 0.7).toLong()
        delay(actualDelay)
    }

    // ============================================================================
    //  ५. Session Limits (Rate limiting bypass)
    // ============================================================================

    /**
     * একটা নতুন session শুরু করে — counter reset।
     */
    fun startSession(context: Context) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            .format(Calendar.getInstance().time)

        val prefs = getPrefs(context)
        val lastDate = prefs.getString(KEY_LAST_SESSION_DATE, "")
        val lastHourlyReset = prefs.getLong(KEY_HOURLY_COUNT + "_time", 0)
        val now = System.currentTimeMillis()

        // নতুন দিন হলে daily reset
        if (lastDate != today) {
            prefs.edit()
                .putString(KEY_LAST_SESSION_DATE, today)
                .putInt(KEY_HOURLY_COUNT, 0)
                .putLong(KEY_HOURLY_COUNT + "_time", now)
                .apply()
        }
        // ১ ঘণ্টা পার হলে hourly reset
        else if (now - lastHourlyReset > 3600000) {
            prefs.edit()
                .putInt(KEY_HOURLY_COUNT, 0)
                .putLong(KEY_HOURLY_COUNT + "_time", now)
                .apply()
        }

        sessionMessageCount = 0
        sessionStartTime = now
        lastMessageTime = 0L
        consecutiveErrors = 0
    }

    /**
     * আরও message পাঠানো যাবে কিনা চেক করে।
     *
     * - Session limit পার হলে false
     * - Hourly limit পার হলে false
     * - Consecutive error বেশি হলে false (সম্ভবত WhatsApp সন্দেহ করছে)
     */
    fun canSendMore(context: Context): Boolean {
        val sessionLimit = getIntSetting(context, KEY_SESSION_LIMIT, Defaults.SESSION_LIMIT)
        val hourlyLimit = getIntSetting(context, KEY_MAX_PER_HOUR, Defaults.MAX_PER_HOUR)

        if (sessionMessageCount >= sessionLimit) return false

        val hourlyCount = getPrefs(context).getInt(KEY_HOURLY_COUNT, 0)
        if (hourlyCount >= hourlyLimit) return false

        // পরপর ৫টা error হলে থেমে যাও — সম্ভবত WhatsApp block করছে
        if (consecutiveErrors >= 5) return false

        return true
    }

    /**
     * একটা message সফলভাবে পাঠানো হয়েছে — counter increment।
     */
    fun onMessageSent(context: Context) {
        sessionMessageCount++
        lastMessageTime = System.currentTimeMillis()
        consecutiveErrors = 0

        val prefs = getPrefs(context)
        val currentHourly = prefs.getInt(KEY_HOURLY_COUNT, 0)
        prefs.edit().putInt(KEY_HOURLY_COUNT, currentHourly + 1).apply()
    }

    /**
     * একটা message পাঠাতে fail হয়েছে — error counter increment।
     */
    fun onMessageFailed() {
        consecutiveErrors++
    }

    /**
     * এই session-এ কতটা message পাঠানো হয়েছে।
     */
    fun getSessionMessageCount(): Int = sessionMessageCount

    /**
     * Session কতক্ষণ চলছে।
     */
    fun getSessionDurationMs(): Long {
        return if (sessionStartTime > 0) System.currentTimeMillis() - sessionStartTime else 0
    }

    // ============================================================================
    //  ६. Cooldown Period (Suspicious activity হলে long pause)
    // ============================================================================

    /**
     * যদি WhatsApp সন্দেহ করে বা অনেক message পাঠানো হয়ে যায়,
     * তাহলে একটা বড় cooldown period নিতে হবে।
     *
     * @return কত মিলিসেকেন্ড অপেক্ষা করতে হবে
     */
    suspend fun getCooldownPeriod(context: Context): Long {
        val sessionLimit = getIntSetting(context, KEY_SESSION_LIMIT, Defaults.SESSION_LIMIT)

        return when {
            // Session-এর ৮০% শেষ হলে ৫-১০ মিনিট break
            sessionMessageCount >= sessionLimit * 0.8 -> {
                (5 * 60 * 1000L) + random.nextInt(5 * 60 * 1000)
            }
            // পরপর ३টা error হলে ১০ মিনিট break
            consecutiveErrors >= 3 -> 10 * 60 * 1000L
            // Hourly limit-এর কাছে হলে ३০ মিনিট break
            else -> {
                val hourlyCount = getPrefs(context).getInt(KEY_HOURLY_COUNT, 0)
                val hourlyLimit = getIntSetting(context, KEY_MAX_PER_HOUR, Defaults.MAX_PER_HOUR)
                if (hourlyCount >= hourlyLimit * 0.9) {
                    30 * 60 * 1000L
                } else {
                    0L
                }
            }
        }
    }

    // ============================================================================
    //  ७. Device Fingerprint Randomization (Hardware signature variation)
    // ============================================================================

    /**
     * WhatsApp কিছু hardware signature দিয়ে bot detect করতে পারে।
     * Accessibility service থেকে এটা directly bypass করা কঠিন,
     * কিন্তু behavior pattern randomize করা যায়।
     */
    fun getDeviceFingerprintHash(): String {
        val fingerprint = listOf(
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.RELEASE,
            Settings.Secure.ANDROID_ID
        ).joinToString("-")
        return fingerprint.hashCode().toString(16)
    }

    // ============================================================================
    //  ८. Scroll Pattern Variation (Constant scroll velocity bypass)
    // ============================================================================

    /**
     * প্রতিবার scroll করার সময় আলাদা velocity ও duration ব্যবহার করে।
     *
     * @return Pair<durationMs, startYPercentage, endYPercentage>
     */
    fun getScrollPattern(context: Context): Triple<Long, Float, Float> {
        if (!isBypassEnabled(context)) {
            return Triple(450L, 0.75f, 0.35f)
        }

        // Duration: ३५०-७००ms random
        val duration = 350L + random.nextInt(350)

        // Start Y: ०.७०-०.८५ random
        val startY = 0.70f + random.nextFloat() * 0.15f

        // End Y: ०.२५-०.४५ random (start থেকে অন্তত ०.३ diff)
        val endY = 0.25f + random.nextFloat() * 0.20f

        return Triple(duration, startY, endY)
    }

    // ============================================================================
    //  ९. Network Quality Awareness
    // ============================================================================

    /**
     * Slow network-এ আরো বেশি delay দিতে হবে — মানুষ slow হলে ধৈর্য ধরে।
     */
    fun getNetworkDelayMultiplier(context: Context): Float {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val connectionInfo = wifiManager?.connectionInfo
            val linkSpeed = connectionInfo?.linkSpeed ?: 0

            when {
                linkSpeed == 0 -> 1.5f  // No WiFi (mobile data — slower)
                linkSpeed < 10 -> 1.3f  // Slow WiFi
                linkSpeed < 50 -> 1.1f  // Medium
                else -> 1.0f            // Fast
            }
        } catch (e: Exception) {
            1.2f
        }
    }

    /**
     * Battery low থাকলে আরো slow হবে (battery saving mode)।
     */
    fun getBatteryDelayMultiplier(context: Context): Float {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            when {
                level < 15 -> 1.4f  // Critical battery
                level < 30 -> 1.2f  // Low battery
                else -> 1.0f
            }
        } catch (e: Exception) {
            1.0f
        }
    }

    // ============================================================================
    //  १०. Suspicious Activity Detector
    // ============================================================================

    /**
     * WhatsApp কি সন্দেহ করছে সেটা detect করার চেষ্টা করে।
     * যদি কোনো সন্দেহজনক pattern ধরা পড়ে, সাথে সাথে session থামানো উচিত।
     *
     * @return Pair<isSuspicious, reason>
     */
    fun detectSuspiciousActivity(): Pair<Boolean, String> {
        // १. পরপর অনেক error
        if (consecutiveErrors >= 5) {
            return true to "পরপর ५টা error — সম্ভবত WhatsApp rate limit করছে"
        }

        // २. খুব দ্রুত message পাঠানো হচ্ছে
        val now = System.currentTimeMillis()
        if (lastMessageTime > 0 && now - lastMessageTime < 500) {
            return true to "Message খুব দ্রুত পাঠানো হচ্ছে (<500ms gap)"
        }

        // ३. খুব বেশি message short time-এ
        if (sessionMessageCount > 10) {
            val sessionDurationMin = (now - sessionStartTime) / 60000.0
            if (sessionDurationMin < 2) {
                return true to "२ মিনিটে १०+ message — suspicious rate"
            }
        }

        return false to ""
    }

    /**
     * পুরো session stats একটা readable string-এ দেয়।
     */
    fun getSessionStats(): String {
        val durationMin = getSessionDurationMs() / 60000.0
        return buildString {
            append("📊 Session Stats:\n")
            append("• Messages sent: $sessionMessageCount\n")
            append("• Session duration: ${"%.1f".format(durationMin)} min\n")
            append("• Consecutive errors: $consecutiveErrors\n")
            append("• Device FP: ${getDeviceFingerprintHash()}\n")
        }
    }

    /**
     * সব bypass settings একসাথে reset করে default এ।
     */
    fun resetToDefaults(context: Context) {
        val editor = getPrefs(context).edit()
        editor.putBoolean(KEY_BYPASS_ENABLED, Defaults.BYPASS_ENABLED)
        editor.putInt(KEY_MIN_DELAY_MS, Defaults.MIN_DELAY_MS)
        editor.putInt(KEY_MAX_DELAY_MS, Defaults.MAX_DELAY_MS)
        editor.putInt(KEY_TAP_RANDOMNESS_DP, Defaults.TAP_RANDOMNESS_DP)
        editor.putBoolean(KEY_TYPING_SIMULATION, Defaults.TYPING_SIMULATION)
        editor.putInt(KEY_TYPING_SPEED_WPM, Defaults.TYPING_SPEED_WPM)
        editor.putInt(KEY_SESSION_LIMIT, Defaults.SESSION_LIMIT)
        editor.putBoolean(KEY_NIGHT_MODE, Defaults.NIGHT_MODE)
        editor.putBoolean(KEY_RANDOM_PAUSES, Defaults.RANDOM_PAUSES)
        editor.putBoolean(KEY_BACKOFF_ON_ERROR, Defaults.BACKOFF_ON_ERROR)
        editor.putBoolean(KEY_READ_BEFORE_REPLY, Defaults.READ_BEFORE_REPLY)
        editor.putInt(KEY_MAX_PER_HOUR, Defaults.MAX_PER_HOUR)
        editor.apply()
    }
}
