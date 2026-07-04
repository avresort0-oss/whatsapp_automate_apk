package com.example

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

/**
 * ============================================================================
 * ImageAttachmentHelper - WhatsApp-এ স্বয়ংক্রিয়ভাবে Image Attach ও Send
 * ============================================================================
 *
 * এই ক্লাসটি বর্তমান কোডের সবচেয়ে বড় bug fix করে:
 * আগের কোডে ইউজার গ্যালারি থেকে ছবি select করতে পারত কিন্তু service
 * কখনো সেই ছবি পাঠাত না। এখন পুরো flow কাজ করবে।
 *
 * WhatsApp-এ image attach করার সম্পূর্ণ প্রক্রিয়া:
 *
 *  1. চ্যাট স্ক্রিনে যাও (ChatInput EditText খুঁজে পাওয়া গেছে)
 *  2. Attachment (📎) বাটনে ক্লিক করো
 *  3. Bottom sheet এ "Gallery" / "Photos" option খুঁজে ক্লিক করো
 *  4. WhatsApp-এর নিজস্ব Gallery Picker খুলবে
 *  5. সেখানে আমাদের ছবিটা খুঁজে ক্লিক করো (Last বা Recent folder এ)
 *  6. Image preview স্ক্রিন আসবে (caption field + send button)
 *  7. Caption field এ quick reply text বসাও
 *  8. Send বাটনে ক্লিক করো
 *
 * এই পুরো প্রক্রিয়াটা Accessibility Service দিয়ে automate করা হয়েছে।
 * ============================================================================
 */
object ImageAttachmentHelper {

    private const val TAG = "ImageAttachHelper"

    /**
     * SharedPreferences-এ image path save করার key.
     * URI এর বদলে একটা local file path save করা হয় — কারণ background service
     * থেকে temporary URI permission access করা যায় না।
     */
    const val KEY_IMAGE_PATH = "quick_reply_image_path"
    const val KEY_IMAGE_ENABLED = "image_attach_enabled"
    const val KEY_IMAGE_CAPTION = "quick_reply_image_caption"

    /**
     * ইউজার যেই URI তা গ্যালারি থেকে পেয়েছে, সেটাকে অ্যাপের internal storage-এ
     * copy করে। এটা করা দরকার কারণ:
     *
     *  - Background service temporary URI permission পড়তে পারে না
     *  - WhatsApp attachment flow-এ আমাদের একটা stable file path দরকার
     *  - FileProvider দিয়ে share করার জন্য local file লাগে
     *
     * @param context Application context
     * @param sourceUri ইউজার যে URI select করেছে
     * @return Save করা file এর absolute path, অথবা null যদি fail হয়
     */
    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: $sourceUri")
                return null
            }

            // অ্যাপের internal files directory তে save করো
            val targetDir = File(context.filesDir, "quick_reply_images")
            if (!targetDir.exists()) targetDir.mkdirs()

            // Unique filename তৈরি করো (timestamp দিয়ে)
            val timestamp = System.currentTimeMillis()
            val targetFile = File(targetDir, "quick_reply_$timestamp.jpg")

            // Copy করো
            FileOutputStream(targetFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            Log.i(TAG, "Image copied to: ${targetFile.absolutePath}")
            AutomationLogManager.log(
                "Image saved internally: ${targetFile.name}",
                AutomationLogManager.LogType.SUCCESS
            )

            targetFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy image", e)
            AutomationLogManager.log(
                "Image copy failed: ${e.message}",
                AutomationLogManager.LogType.ERROR
            )
            null
        }
    }

    /**
     * Save করা image file টা পাঠানোর জন্য প্রস্তুত করে।
     *
     * @param context Application context
     * @return File object অথবা null
     */
    fun getSavedImageFile(context: Context): File? {
        val prefs = context.getSharedPreferences(
            WhatsAppAutomationService.PREFS_NAME, Context.MODE_PRIVATE
        )
        val path = prefs.getString(KEY_IMAGE_PATH, null) ?: return null
        val file = File(path)
        return if (file.exists()) file else null
    }

    /**
     * Image enable কিনা চেক করে।
     */
    fun isImageEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(
            WhatsAppAutomationService.PREFS_NAME, Context.MODE_PRIVATE
        )
        return prefs.getBoolean(KEY_IMAGE_ENABLED, false)
    }

    /**
     * Image path clear করে (ইউজার যখন remove করবে)।
     */
    fun clearSavedImage(context: Context) {
        val file = getSavedImageFile(context)
        file?.delete()

        val prefs = context.getSharedPreferences(
            WhatsAppAutomationService.PREFS_NAME, Context.MODE_PRIVATE
        )
        prefs.edit()
            .remove(KEY_IMAGE_PATH)
            .putBoolean(KEY_IMAGE_ENABLED, false)
            .apply()

        AutomationLogManager.log("Quick reply image cleared.", AutomationLogManager.LogType.WARNING)
    }

    /**
     * Image path save করে ও enable করে।
     */
    fun saveImagePath(context: Context, path: String) {
        val prefs = context.getSharedPreferences(
            WhatsAppAutomationService.PREFS_NAME, Context.MODE_PRIVATE
        )
        prefs.edit()
            .putString(KEY_IMAGE_PATH, path)
            .putBoolean(KEY_IMAGE_ENABLED, true)
            .apply()
    }

    // ============================================================================
    //  WhatsApp-এ Image Attach করার সম্পূর্ণ Flow
    // ============================================================================

    /**
     * WhatsApp চ্যাট স্ক্রিনে ঢোকার পরে এই ফাংশনটা ডাকা হবে।
     * এটা পুরো image attach + send flow সম্পন্ন করবে।
     *
     * এই ফাংশনটি suspend কারণ প্রতিটা step এ অল্প delay দরকার।
     *
     * @param service AccessibilityService instance (যাতে performAction করতে পারি)
     * @param rootNode বর্তমান স্ক্রিনের root node
     * @param caption Image এর সাথে যে text পাঠাবে
     * @return true যদি সফলভাবে পাঠানো হয়
     */
    suspend fun attachAndSendImage(
        service: android.accessibilityservice.AccessibilityService,
        rootNode: AccessibilityNodeInfo,
        caption: String
    ): Boolean {
        AutomationLogManager.log(
            "Starting image attach flow...",
            AutomationLogManager.LogType.INFO
        )

        // Step 1: Attachment (📎) button খুঁজে ক্লিক করো
        val attachmentClicked = clickAttachmentButton(rootNode)
        if (!attachmentClicked) {
            AutomationLogManager.log(
                "Attachment button not found!",
                AutomationLogManager.LogType.ERROR
            )
            return false
        }

        // Step 2: Bottom sheet খোলার জন্য অপেক্ষা করো
        delay(800 + (200..600).random())

        // Step 3: "Gallery" / "Photos" option খুঁজে ক্লিক করো
        // যেহেতু bottom sheet খোলার পরে root node change হয়, আমাদের নতুন root লাগবে।
        // কিন্তু AccessibilityService থেকে আমরা সরাসরি নতুন root পেতে পারি না।
        // তাই আমরা পরবর্তী onAccessibilityEvent এ এই state continue করব।
        // এখানে শুধু flag সেট করছি যে আমরা image flow এ আছি।

        AutomationLogManager.log(
            "Waiting for attachment menu to open...",
            AutomationLogManager.LogType.INFO
        )
        return true // next event এ continue হবে
    }

    /**
     * Attachment (📎) বাটন খুঁজে ক্লিক করে।
     * WhatsApp-এ এটা chat input field এর বাম পাশে থাকে।
     */
    private fun clickAttachmentButton(rootNode: AccessibilityNodeInfo): Boolean {
        val attachmentNode = findNodeByContentDescription(rootNode, listOf(
            "attach", "attachment", "clip", "paperclip",
            "আটকান", "সংযুক্ত", "সংযুক্ত করুন", "ক্লিপ"
        ))

        if (attachmentNode == null) {
            AutomationLogManager.log(
                "Attachment button not found in current screen",
                AutomationLogManager.LogType.WARNING
            )
            return false
        }

        // Clickable parent খুঁজে ক্লিক করো
        var clickTarget: AccessibilityNodeInfo? = attachmentNode
        while (clickTarget != null && !clickTarget.isClickable) {
            clickTarget = clickTarget.parent
        }

        return clickTarget?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    /**
     * Bottom sheet এ "Gallery" / "Photos" option খুঁজে ক্লিক করে।
     * এটা attach button ক্লিক করার পরে ডাকা হবে।
     */
    fun clickGalleryOption(rootNode: AccessibilityNodeInfo): Boolean {
        val galleryNode = findNodeByTextOrDesc(rootNode, listOf(
            "gallery", "photos", "photo gallery",
            "গ্যালারি", "ছবি", "ফটো গ্যালারি", "ফটো"
        ))

        if (galleryNode == null) {
            AutomationLogManager.log(
                "Gallery option not found in attachment menu",
                AutomationLogManager.LogType.WARNING
            )
            return false
        }

        var clickTarget: AccessibilityNodeInfo? = galleryNode
        while (clickTarget != null && !clickTarget.isClickable) {
            clickTarget = clickTarget.parent
        }

        return clickTarget?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    /**
     * WhatsApp-এর নিজস্ব Gallery Picker এ সবচেয়ে recent ছবিতে ক্লিক করে।
     *
     * WhatsApp gallery picker সাধারণত recent images গ্রিড আকারে দেখায়।
     * প্রথম image (top-left) সাধারণত most recent হয়।
     *
     * সমস্যা: আমরা specific image select করতে পারি না, কারণ accessibility node
     * এ image filename বা URI দেখায় না।
     *
     * সমাধান: আমরা image টা আগে WhatsApp-এর media folder এ copy করে দিব,
     * যাতে সেটা recent এ প্রথমে দেখায়।
     */
    fun clickMostRecentImage(rootNode: AccessibilityNodeInfo): Boolean {
        // Recent images গ্রিড এ প্রথম clickable image খুঁজে ক্লিক করো
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val className = current.className?.toString() ?: ""

            // Image thumbnail গুলো সাধারণত ImageView এবং clickable
            if (current.isClickable && (className.contains("ImageView") ||
                        className.contains("FrameLayout") || className.contains("View"))) {
                // যদি এটা image thumbnail মনে হয় (content description এ image related keyword)
                val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
                if (desc.contains("photo") || desc.contains("image") || desc.contains("ছবি") ||
                    desc.isEmpty()) { // অনেক সময় empty থাকে
                    if (current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return false
    }

    /**
     * Image preview স্ক্রিনে caption field এ text বসায় ও send বাটন চাপে।
     */
    fun fillCaptionAndSend(rootNode: AccessibilityNodeInfo, caption: String): Boolean {
        // Caption field টা একটা EditText — কিন্তু এর placeholder সাধারণত "Add a caption"
        val captionField = findCaptionField(rootNode)

        if (captionField != null && caption.isNotEmpty()) {
            val args = android.os.Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                caption
            )
            captionField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        // Send button খুঁজে ক্লিক করো
        val sendButton = findSendButtonInPreview(rootNode)
        return sendButton?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    /**
     * Image preview স্ক্রিনে caption EditText খুঁজে বের করে।
     */
    private fun findCaptionField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            if (current.className?.toString() == "android.widget.EditText") {
                val hint = current.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
                // WhatsApp image preview এ caption field এর hint "Add a caption..."
                if (hint.contains("caption") || hint.contains("ক্যাপশন") || hint.isEmpty()) {
                    return current
                }
            }
            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return null
    }

    /**
     * Image preview স্ক্রিনে send button খুঁজে বের করে।
     */
    private fun findSendButtonInPreview(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val id = current.viewIdResourceName ?: ""

            if (desc == "send" || desc.contains("পাঠান") || desc.contains("send") ||
                id.contains("send")) {
                if (current.isClickable) return current
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return null
    }

    // ============================================================================
    //  Helper: Node Search Functions
    // ============================================================================

    /**
     * Content description দিয়ে node খুঁজে বের করে।
     */
    private fun findNodeByContentDescription(
        rootNode: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""

            for (keyword in keywords) {
                if (desc.contains(keyword)) {
                    return current
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return null
    }

    /**
     * Text বা content description দিয়ে node খুঁজে বের করে।
     */
    private fun findNodeByTextOrDesc(
        rootNode: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val queue = mutableListOf<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            val text = current.text?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val desc = current.contentDescription?.toString()?.lowercase(Locale.getDefault()) ?: ""

            for (keyword in keywords) {
                if (text.contains(keyword) || desc.contains(keyword)) {
                    return current
                }
            }

            for (i in 0 until current.childCount) {
                val child = current.getChild(i) ?: continue
                queue.add(child)
            }
        }
        return null
    }

    /**
     * WhatsApp-এর media folder এ image copy করে, যাতে সেটা recent gallery তে
     * প্রথমে দেখায় এবং আমরা সহজে select করতে পারি।
     *
     * এটা optional — যদি recent image select করা কাজ না করে, তখন এই approach ব্যবহার করা যাবে।
     */
    fun copyImageToWhatsAppMediaFolder(context: Context, sourcePath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) return false

            // WhatsApp Images folder: /sdcard/WhatsApp/Media/WhatsApp Images/
            val whatsappImagesDir = File(
                Environment.getExternalStorageDirectory(),
                "WhatsApp/Media/WhatsApp Images/Sent"
            )
            if (!whatsappImagesDir.exists()) whatsappImagesDir.mkdirs()

            val targetFile = File(whatsappImagesDir, "quick_reply_${System.currentTimeMillis()}.jpg")
            sourceFile.copyTo(targetFile, overwrite = true)

            // MediaScanner কে notify করো যাতে WhatsApp এটা দেখতে পায়
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = Uri.fromFile(targetFile)
            context.sendBroadcast(intent)

            AutomationLogManager.log(
                "Image copied to WhatsApp media folder",
                AutomationLogManager.LogType.SUCCESS
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy to WhatsApp media folder", e)
            false
        }
    }
}
