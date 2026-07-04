# WhatsApp Automator — Bypass ও Image Flow Integration Guide

এই গাইডটি আপনার বর্তমান WhatsApp Automator প্রজেক্টে নতুন bypass/anti-detection features ও image auto-send flow যোগ করার জন্য।

---

## 📦 যা যা ফাইল দেওয়া হয়েছে

```
com/example/
├── BypassManager.kt              ← নতুন (anti-detection logic)
├── ChatListScanner.kt            ← নতুন (পুরো chat list scan)
├── ImageAttachmentHelper.kt      ← নতুন (image attach bug fix)
├── WhatsAppAutomationService.kt  ← আপগ্রেডেড (replace করুন)
├── MainViewModel.kt              ← আপগ্রেডেড (replace করুন)
└── BypassSettingsPanel.kt        ← নতুন (UI components)
```

---

## 🔧 Installation Steps

### Step 1: ফাইল গুলো copy করুন

আপনার প্রজেক্টের `app/src/main/java/com/example/` ফোল্ডারে এই ৬টা ফাইল রাখুন।

- যেগুলো "নতুন" বলা আছে — সেগুলো directly paste করুন
- যেগুলো "আপগ্রেডেড" — পুরোনো ফাইল replace করুন

### Step 2: AndroidManifest.xml এ Permissions যোগ করুন

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- নতুন: WhatsApp media folder access এর জন্য -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />
    <!-- Android 13+ এর জন্য -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- নতুন: Network state check এর জন্য (BypassManager) -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application ...>
        ...
    </application>
</manifest>
```

### Step 3: FileProvider যোগ করুন (Image share এর জন্য)

`app/src/main/res/xml/file_paths.xml` ফাইল তৈরি করুন:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <files-path name="quick_reply_images" path="quick_reply_images/" />
    <external-path name="whatsapp_media" path="WhatsApp/Media/" />
</paths>
```

`AndroidManifest.xml` এ `<application>` tag এর ভেতরে যোগ করুন:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### Step 4: MainActivity.kt তে নতুন UI যোগ করুন

`MainActivity.kt` এর `SettingsAndLogsTab` ফাংশনে যান। `LazyColumn` এর ভেতরে যেখানে অন্যান্য `item { ... }` আছে, সেখানে যোগ করুন:

```kotlin
// ফাইলের উপরে import যোগ করুন:
import com.example.BypassSettingsPanel
import com.example.ChatListScannerPanel

// SettingsAndLogsTab এর LazyColumn এ:
item {
    BypassSettingsPanel(viewModel = viewModel)
}

item {
    ChatListScannerPanel(viewModel = viewModel)
}
```

### Step 5: build.gradle.kts এ নিশ্চিত করুন যে এই dependencies আছে

```kotlin
dependencies {
    // এগুলো ইতিমধ্যে থাকা উচিত, তবে verify করুন
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

### Step 6: Build ও Run

Android Studio তে **Build > Make Project** করুন। কোনো error না থাকলে APK build হবে।

---

## 🎯 নতুন Features এর তালিকা

### ১. Bypass / Anti-Detection (BypassManager.kt)

| Feature | কী করে | WhatsApp কীভাবে detect করে তা এড়ায় |
|---------|--------|--------------------------------|
| Random Delays | ৩-৮ সেকেন্ড random delay | Fixed timing pattern এড়ায় |
| Tap Randomness | ±৮ dp offset | Exact center tap এড়ায় |
| Typing Simulation | "typing..." indicator pause | Instant text fill এড়ায় |
| Read-Before-Reply | Reply এর আগে reading pause | Instant reply এড়ায় |
| Random Long Pauses | ১৫% সম্ভাবনায় ৫-১৫s pause | Constant rate এড়ায় |
| Night Mode | রাতে delay দ্বিগুণ | Unusual hours activity এড়ায় |
| Session Limit | সর্বোচ্চ ২৫ message/session | Bulk send detection এড়ায় |
| Hourly Limit | সর্বোচ্চ ৩০ message/hour | Rate limit এড়ায় |
| Error Backoff | Error হলে exponential delay | Spam মনে হওয়া এড়ায় |
| Scroll Variation | Random scroll velocity | Constant scroll pattern এড়ায় |
| Suspicious Detector | পরপর ৫ error হলে pause | Block detection আগেই থামায় |
| Network Awareness | Slow network এ বেশি delay | Natural behavior simulation |
| Battery Awareness | Low battery তে বেশি delay | Natural behavior simulation |

### ২. Image Auto-Send Flow (ImageAttachmentHelper.kt)

**Bug fix:** আগে image select করা গেলেও service পাঠাত না। এখন পুরো flow কাজ করে।

**Flow:**
1. ইউজার গ্যালারি থেকে ছবি select করে
2. অ্যাপ internal storage এ image copy করে (permanent access)
3. WhatsApp চ্যাট খুললে attachment (📎) button খুঁজে ক্লিক করে
4. "Gallery" option খুঁজে ক্লিক করে
5. Most recent image খুঁজে ক্লিক করে
6. Preview স্ক্রিনে caption বসায়
7. Send বাটন চাপে

### ৩. Chat List Scanner (ChatListScanner.kt)

WhatsApp এর পুরো chat list scan করে অ্যাপে দেখায়:
- সব contact ও group এর নাম
- Last message preview
- Timestamp
- Read status (single tick / double gray / double blue)
- Unread badge ও count
- Group/Business account detection

**ব্যবহার:** Settings এ "Chat List Scanner" toggle অন করুন, তারপর WhatsApp খুলুন।

---

## 🚀 ব্যবহার করার নিয়ম

### সম্পূর্ণ Workflow

```
১. অ্যাপ খুলুন
২. Settings ট্যাবে যান

৩. Bypass Settings:
   - "Bypass / Anti-Detection" toggle অন করুন
   - প্রয়োজনে delay range, typing speed ইত্যাদি adjust করুন
   - ডিফল্ট ভ্যালু নিরাপদ, সেগুলোই রাখতে পারেন

৪. Automation Settings:
   - Target Contacts লিখুন (যেমন: Abir, Jahid)
   - Quick Reply Message লিখুন
   - Image চাইলে select করুন (গ্যালারি থেকে)
   - "Use Image Flow" toggle অন করুন (image পাঠাতে চাইলে)
   - "Double Blue Tick Filter" অন করুন (শুধু read receipt ওয়ালাদের পাঠাতে)

৫. Accessibility Service Enable করুন

৬. Automation toggle অন করুন

৭. WhatsApp খুলুন (ম্যানুয়ালি)
   - চ্যাট লিস্টে যান
   - অ্যাপ স্বয়ংক্রিয়ভাবে কাজ শুরু করবে
```

### শুধু Chat List Scan করতে চাইলে

```
১. "Chat List Scanner" toggle অন করুন
২. WhatsApp খুলুন
৩. অ্যাপ scroll করে সব chat collect করবে
৪. অ্যাপে ফিরে গিয়ে দেখুন — পুরো লিস্ট সাথে status দেখাবে
```

---

## ⚠️ গুরুত্বপূর্ণ সতর্কতা

### ১. WhatsApp Account Ban Risk

এই অ্যাপটি WhatsApp এর Terms of Service লঙ্ঘন করে। যদিও bypass settings detection risk কমায়, **১০০% নিরাপদ নয়**।

**Risk কমানোর উপায়:**
- Session limit ২৫ এর বেশি রাখবেন না
- Hourly limit ৩০ এর বেশি রাখবেন না
- Night mode অন রাখুন
- প্রতিদিন সর্বোচ্চ ১০০ message পাঠাবেন
- একই message বারবার পাঠাবেন না (প্রতিটা contact কে আলাদা message)
- Bypass settings সবসময় অন রাখুন

### ২. Privacy ও Security

Accessibility Service একবার enable করলে অ্যাপ আপনার:
- সব WhatsApp message পড়তে পারে
- যেকোনো button ক্লিক করতে পারে
- OTP, ব্যাংক message সব access করতে পারে

**শুধু নিজের ফোনে ব্যবহার করুন, অন্যের ফোনে নয়।**

### ৩. Image Flow Limitations

Image auto-send flow সব ফোনে নিখুঁত কাজ নাও করতে পারে কারণ:
- বিভিন্ন ফোনে WhatsApp এর UI আলাদা
- বিভিন্ন ফোনে Gallery picker আলাদা
- WhatsApp যেকোনো সময় UI আপডেট করতে পারে

**Test করার উপায়:** প্রথমে শুধু ১ জন target contact দিয়ে test করুন।

### ৪. Battery Optimization

Xiaomi, Oppo, Vivo ফোনে aggressive battery management service kill করে দেয়।
- Settings > Battery > WhatsApp Automator > "No restrictions"
- Auto-start permission অন করুন

---

## 🐛 Troubleshooting

### Problem: Image flow কাজ করছে না

**Solution:**
1. Image টা সঠিকভাবে select হয়েছে কিনা চেক করুন (Settings এ image preview দেখাবে)
2. "Use Image Flow" toggle অন আছে কিনা দেখুন
3. Log চেক করুন — "Attachment button not found" লেখা থাকলে WhatsApp ভার্সন আলাদা
4. WhatsApp এর সর্বনিম্ন ভার্সন ব্যবহার করে দেখুন

### Problem: Service কাজ করছে না

**Solution:**
1. Accessibility Service enabled কিনা verify করুন
2. Battery optimization বন্ধ করুন
3. Phone restart করুন
4. App reinstall করুন

### Problem: Bypass সত্ত্বেও account ban হয়ে গেছে

**Solution:**
- ৭২ ঘণ্টা অপেক্ষা করুন (সাধারণত temporary ban)
- এরপর আরো conservative settings দিয়ে চেষ্টা করুন:
  - Session limit: ১০
  - Hourly limit: ১৫
  - Min delay: ৫০০০ms
  - Max delay: ১২০০০ms

---

## 📞 Support

কোনো সমস্যা হলে:
1. Log চেক করুন (Settings ট্যাবের নিচে Live Logs সেকশন)
2. Session stats চেক করুন (বন্ধ করার সময় দেখাবে)
3. প্রতিটা step এ কী হচ্ছে তা log এ লেখা থাকে

---

## 🎓 Technical Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    User Interface                       │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────┐    │
│  │  Settings   │  │  Bypass     │  │  Chat List   │    │
│  │  Panel      │  │  Settings   │  │  Scanner UI  │    │
│  └─────────────┘  └─────────────┘  └──────────────┘    │
└─────────────────────┬───────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│                   MainViewModel                         │
│  (State management, SharedPreferences bridge)           │
└─────────────────────┬───────────────────────────────────┘
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ BypassManager│ │ ChatList     │ │ ImageAttach  │
│              │ │ Scanner      │ │ Helper       │
│ Anti-detect  │ │ Parses chat  │ │ Image flow   │
│ logic        │ │ list nodes   │ │ state machine│
└──────────────┘ └──────────────┘ └──────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────┐
│              WhatsAppAutomationService                  │
│         (AccessibilityService — root)                   │
│                                                         │
│  Reads WhatsApp UI → Bypass delays → Sends message     │
└─────────────────────────────────────────────────────────┘
                      │
                      ▼
              WhatsApp App (com.whatsapp)
```

---

সফল অটোমেশন! 🚀
