# 🚀 VS Code দিয়ে APK Build করার সহজ গাইড

এই গাইড অনুযায়ী করলে **৩০ মিনিটের মধ্যে** আপনার APK প্রস্তুত হবে। Android Studio এর কোনো দরকার নেই!

---

## 📋 যা যা দরকার (একসাথে download করুন)

| # | Software | Size | Download Link |
|---|----------|------|---------------|
| 1 | JDK 17 (Temurin) | ~180 MB | https://adoptium.net/temurin/releases/?version=17 |
| 2 | Android SDK Command-line Tools | ~150 MB | https://developer.android.com/studio#command-line-tools-only |
| 3 | VS Code | ~90 MB | https://code.visualstudio.com/ |
| 4 | (Optional) VS Code Extension Pack | ~5 MB | VS Code এর ভেতরে install করবেন |

**মোট Download:** ~425 MB (Android Studio-র চেয়ে ১০ গুণ কম!)

---

## 🪟 Windows এর জন্য Step-by-Step

### Step 1: JDK 17 Install করুন (৫ মিনিট)

1. https://adoptium.net/temurin/releases/?version=17 খুলুন
2. **Operating System:** Windows
3. **Architecture:** x64 (যদি 32-bit হয় তবে x86)
4. **Version:** 17 - LTS
5. `.msi` ফাইল download করুন
6. সেটি double-click করে install করুন
7. Install করার সময় **"Set JAVA_HOME variable"** অপশনটি **অন করে দিন**
8. **"Add to PATH"** অপশনটিও **অন করে দিন**

**Verify করুন:**
- `Win + R` চাপুন, `cmd` লিখে Enter চাপুন
- লিখুন: `java -version`
- নিচের মত output দেখাবে:
  ```
  openjdk version "17.0.x" 2023-xx-xx
  OpenJDK Runtime Environment Temurin-17.0.x
  ```

---

### Step 2: Android SDK Command-line Tools Install করুন (১০ মিনিট)

1. https://developer.android.com/studio#command-line-tools-only খুলুন
2. নিচে স্ক্রল করে **"Command line tools only"** সেকশন খুঁজুন
3. Windows এর জন্য `.zip` ফাইল download করুন
4. ZIP টি extract করুন এই লোকেশনে: `C:\Android\Sdk\cmdline-tools\`
   - ফোল্ডার structure এমন হবে:
     ```
     C:\Android\Sdk\cmdline-tools\latest\bin\
     C:\Android\Sdk\cmdline-tools\latest\lib\
     ```
   - খেয়াল রাখবেন: `latest` নামের ফোল্ডার থাকতে হবে (যেটা download করেছেন সেটার ভেতরের ফাইল গুলো এই `latest` ফোল্ডারে রাখুন)

5. **Environment Variable** সেট করুন:
   - `Win + S` চেপে "Environment Variables" খুঁজুন
   - "Edit the system environment variables" এ ক্লিক করুন
   - "Environment Variables..." বাটনে ক্লিক করুন
   - "System variables" সেকশনে "New..." ক্লিক করুন
   - **Variable name:** `ANDROID_HOME`
   - **Variable value:** `C:\Android\Sdk`
   - OK করে বের হন

6. PC একবার Restart করুন

---

### Step 3: SDK Packages Install করুন (৫ মিনিট)

1. Command Prompt খুলুন (`Win + R` → `cmd`)
2. নিচের command গুলো একটা একটা করে চালান:

```bash
# sdkmanager এ যান
cd C:\Android\Sdk\cmdline-tools\latest\bin

# প্রয়োজনীয় packages install করুন
sdkmanager "platform-tools" "platforms;android-34"
sdkmanager "build-tools;34.0.0"

# License accept করুন (y চাপতে থাকুন)
sdkmanager --licenses
```

---

### Step 4: VS Code Install করুন (৩ মিনিট)

1. https://code.visualstudio.com/ থেকে VS Code download করুন
2. Install করুন (সব default অপশনে রাখুন)

---

### Step 5: Project Setup করুন (২ মিনিট)

1. `WhatsApp_automata_FINAL.zip` ফাইলটি extract করুন যেকোনো জায়গায়
   - যেমন: `C:\Projects\WhatsApp_automata-main\`

2. `local.properties.template` ফাইলটি copy করে `local.properties` নামে rename করুন
   - ফাইলটি text editor দিয়ে খুলুন
   - `PASTE_YOUR_SDK_PATH_HERE` এর জায়গায় আপনার SDK path বসান:
   ```
   sdk.dir=C\:\\Android\\Sdk
   ```
   - খেয়াল রাখবেন Windows এ backslash দুইবার দিতে হবে (`\\`)

3. VS Code খুলুন
4. **File > Open Folder** ক্লিক করুন
5. `WhatsApp_automata-main` ফোল্ডারটি select করুন

---

### Step 6: APK Build করুন (৫-১৫ মিনিট)

#### সহজ উপায় (Recommended):
1. VS Code এ প্রজেক্ট খোলা অবস্থায়
2. `build_scripts` ফোল্ডারে যান
3. `build_apk_windows.bat` ফাইলটিতে double-click করুন
4. ৫-১৫ মিনিট অপেক্ষা করুন (প্রথমবার বেশি সময় লাগে)
5. Build হলে একটা ফোল্ডার খুলবে — সেখানে `app-debug.apk` থাকবে

#### VS Code Terminal দিয়ে:
1. VS Code এ `Ctrl + ` (backtick) চাপুন — Terminal খুলবে
2. নিচের command লিখুন:
   ```bash
   gradlew.bat assembleDebug
   ```
3. Enter চাপুন, অপেক্ষা করুন

---

### Step 7: APK খুঁজে বের করুন

Build successful হলে APK পাবেন এখানে:
```
WhatsApp_automata-main\app\build\outputs\apk\debug\app-debug.apk
```

এই ফাইলটি:
1. Copy করুন
2. Android ফোনে paste করুন (USB cable / Bluetooth / Google Drive)
3. ফোনে সেই ফাইলে tap করে install করুন
4. "Install from unknown sources" permission দিন

---

## 🍎 Mac এর জন্য Step-by-Step

### Step 1: JDK 17 Install করুন

Terminal খুলুন (Cmd + Space → "Terminal") এবং লিখুন:
```bash
brew install openjdk@17
sudo ln -sfn $(brew --prefix)/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

Verify: `java -version`

### Step 2: Android SDK Install করুন

```bash
# Android SDK এর জন্য ফোল্ডার তৈরি করুন
mkdir -p ~/Library/Android/sdk/cmdline-tools

# Command-line tools download করুন
cd ~/Downloads
curl -O https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip
unzip commandlinetools-mac-11076708_latest.zip
mv cmdline-tools ~/Library/Android/sdk/cmdline-tools/latest

# Environment variable
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.zshrc
source ~/.zshrc
```

### Step 3: SDK Packages Install করুন

```bash
sdkmanager "platform-tools" "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager --licenses
```

### Step 4: VS Code Install করুন

https://code.visualstudio.com/ থেকে download ও install করুন

### Step 5: Project Setup

1. ZIP extract করুন
2. `local.properties.template` কে `local.properties` এ rename করুন
3. ফাইলটি খুলে SDK path বসান:
   ```
   sdk.dir=/Users/YourUsername/Library/Android/sdk
   ```
4. VS Code দিয়ে project খুলুন

### Step 6: Build করুন

Terminal খুলে লিখুন:
```bash
cd build_scripts
chmod +x build_apk_mac_linux.sh
./build_apk_mac_linux.sh
```

অথবা সরাসরি:
```bash
./gradlew assembleDebug
```

---

## 🐧 Linux এর জন্য Step-by-Step

### Step 1: JDK 17 Install করুন

```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version
```

### Step 2: Android SDK Install করুন

```bash
mkdir -p ~/Android/sdk/cmdline-tools
cd ~/Downloads
curl -O https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools ~/Android/sdk/cmdline-tools/latest

echo 'export ANDROID_HOME=$HOME/Android/sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

### Step 3: SDK Packages

```bash
sdkmanager "platform-tools" "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager --licenses
```

### Step 4: VS Code + Build

VS Code install করুন, project খুলুন, তারপর:
```bash
cd build_scripts
chmod +x build_apk_mac_linux.sh
./build_apk_mac_linux.sh
```

---

## 🛠️ Troubleshooting (সমস্যা ও সমাধান)

### Problem 1: `java: command not found`

**Solution:** JDK install হয়নি বা PATH এ নেই।

Windows:
- Environment Variables খুলুন
- "Path" variable এ JDK এর bin folder যোগ করুন
- যেমন: `C:\Program Files\Eclipse Adoptium\jdk-17.x.x\bin`
- PC restart করুন

Mac/Linux:
- `~/.zshrc` বা `~/.bashrc` ফাইলে যান
- নিশ্চিত করুন JDK path ঠিক আছে

---

### Problem 2: `ANDROID_HOME is not set`

**Solution:** Environment variable সেট করা নেই।

Windows:
- Environment Variables খুলুন
- New System Variable:
  - Name: `ANDROID_HOME`
  - Value: `C:\Android\Sdk`
- PC restart করুন

Mac/Linux:
```bash
echo 'export ANDROID_HOME=YOUR_SDK_PATH' >> ~/.zshrc
source ~/.zshrc
```

---

### Problem 3: `sdkmanager: command not found`

**Solution:** SDK Command-line Tools সঠিকভাবে install হয়নি।

ফোল্ডার structure ঠিক আছে কিনা চেক করুন:
```
C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat  (Windows)
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager  (Mac/Linux)
```

`latest` ফোল্ডারটি থাকতে হবে — এটা খুবই গুরুত্বপূর্ণ!

---

### Problem 4: `Could not resolve com.android.tools.build:gradle`

**Solution:** Internet connection সমস্যা বা proxy।

1. Internet connection চেক করুন
2. যদি VPN চালু থাকে, বন্ধ করে দেখুন
3. `gradle.properties` ফাইলে নিচের লাইন যোগ করুন:
   ```
   systemProp.https.proxyHost=
   systemProp.https.proxyPort=
   ```

---

### Problem 5: `Build failed with signing config`

**Solution:** এই প্রজেক্টে debug keystore এর প্রয়োজন।

`app/build.gradle.kts` ফাইল খুলুন, `debug` block এ যান, এই লাইনটি comment করুন:
```kotlin
debug {
    // signingConfig = signingConfigs.getByName("debugConfig")  // এই লাইনটি comment করুন
}
```

---

### Problem 6: `Failed to install the following Android SDK packages`

**Solution:** SDK এর ভার্সন মিলছে না।

`app/build.gradle.kts` খুলুন ও নিচের লাইন খুঁজুন:
```kotlin
compileSdk { version = release(36) { minorApiLevel = 1 } }
```
এটাকে পাল্টে দিন:
```kotlin
compileSdk = 34
```

এবং:
```kotlin
targetSdk = 36
```
এটাকে পাল্টে দিন:
```kotlin
targetSdk = 34
```

তারপর আবার build করুন।

---

### Problem 7: Gradle অনেক সময় নিচ্ছে / Hang করছে

**Solution:** প্রথমবার ৫-১৫ মিনিট স্বাভাবিক। যদি আরো বেশি সময় নেয়:

1. Internet speed চেক করুন (Gradle অনেক dependencies download করে)
2. `gradle.properties` ফাইলে নিচের লাইন যোগ করুন:
   ```
   org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
   org.gradle.parallel=true
   org.gradle.caching=true
   ```

---

### Problem 8: `Google Services` বা `Firebase` related error

**Solution:** এই প্রজেক্টে Firebase dependency আছে কিন্তু `google-services.json` ফাইল নেই।

দুটি option:

**Option A (সহজ):** Firebase বাদ দিন
`app/build.gradle.kts` খুলুন ও এই লাইনগুলো comment করুন:
```kotlin
// implementation(platform(libs.firebase.bom))
// implementation(libs.firebase.ai)
// implementation(libs.firebase.appcheck.recaptcha)
```
এবং plugins এর ভেতরে:
```kotlin
// alias(libs.plugins.google.services)
```

**Option B:** নিজের Firebase project তৈরি করুন
1. https://console.firebase.google.com/ এ যান
2. New project তৈরি করুন
3. Android app যোগ করুন (package name: `com.aistudio.whatsappautomator.xtrqwb`)
4. `google-services.json` download করুন
5. সেটি `app/` ফোল্ডারে রাখুন

---

## 📞 সাহায্যের জন্য

যদি কোনো step এ আটকে যান:
1. Error message টি পুরোপুরি copy করুন
2. কোন step এ আছেন সেটা লিখুন
3. আমাকে পাঠান — আমি help করব!

---

## ✅ Quick Checklist

- [ ] JDK 17 install করা হয়েছে
- [ ] `java -version` command কাজ করছে
- [ ] Android SDK Command-line Tools install করা হয়েছে
- [ ] `ANDROID_HOME` environment variable সেট করা হয়েছে
- [ ] `sdkmanager` command কাজ করছে
- [ ] `platform-tools` ও `platforms;android-34` install করা হয়েছে
- [ ] VS Code install করা হয়েছে
- [ ] `local.properties` ফাইল তৈরি করা হয়েছে (template থেকে)
- [ ] `local.properties` এ SDK path ঠিক আছে
- [ ] `gradlew assembleDebug` command চালানো হয়েছে
- [ ] APK তৈরি হয়েছে (`app/build/outputs/apk/debug/app-debug.apk`)

সব ঠিক থাকলে আপনার APK প্রস্তুত! 🎉
