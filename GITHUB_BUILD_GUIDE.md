# 🐙 GitHub Actions দিয়ে APK Build করার গাইড

এই গাইড অনুযায়ী করলে **কোনো local setup ছাড়াই** শুধু GitHub-এ push করলেই APK তৈরি হয়ে যাবে। JDK, Android SDK, Gradle — কিছুই ইনস্টল করতে হবে না!

---

## 📋 যা যা দরকার

| # | জিনিস | লিংক |
|---|-------|------|
| 1 | GitHub অ্যাকাউন্ট | https://github.com |
| 2 | Git (অপশনাল, ওয়েব আপলোডও চলবে) | https://git-scm.com |

**ব্যস!** আর কিছু লাগবে না — JDK 17, Android SDK, Android Studio কিছুই না।

---

## 🚀 Step-by-Step (৫ মিনিট)

### Step 1: GitHub-এ নতুন Repository তৈরি

1. https://github.com/new খুলুন
2. **Repository name:** `WhatsApp_automata` (বা যা খুশি)
3. **Private** সিলেক্ট করুন (নিজের প্রজেক্ট তাই)
4. ✅ **Add a README file** — চেক করুন
5. **Create repository** ক্লিক করুন

---

### Step 2: প্রজেক্ট ফাইল আপলোড

#### সহজ উপায় (ওয়েব দিয়ে, Git না জানলেও চলবে):

1. `WhatsApp_automata_FIXED.zip` ফাইলটি আপনার কম্পিউটারে extract করুন
2. GitHub repository তৈরি হওয়ার পর, **`Add file` → `Upload files`** ক্লিক করুন
3. extract করা ফোল্ডারের ভেতরের **সব ফাইল ও ফোল্ডার** drag-drop করুন
   - ⚠️ খেয়াল রাখবেন: `.github` ফোল্ডারটি (hidden folder) যেন আপলোড হয়
   - Mac-এ: `Cmd + Shift + .` চাপলে hidden files দেখা যায়
   - Windows-এ: File Explorer-এ **View → Show → Hidden items** চালু করুন
4. **Commit changes** ক্লিক করুন

#### Git দিয়ে (যারা পারেন):

```bash
# extract করা ফোল্ডারে যান
cd WhatsApp_automata-main

# Git initialize
git init
git branch -M main

# সব ফাইল add করুন
git add .

# Commit করুন
git commit -m "Initial commit - WhatsApp Automata"

# আপনার repository তে push করুন (URL বদলে নিজেরটা বসান)
git remote add origin https://github.com/YOUR_USERNAME/WhatsApp_automata.git
git push -u origin main
```

---

### Step 3: Build হওয়ার জন্য অপেক্ষা করুন

1. আপনার repository তে যান
2. উপরে **`Actions`** ট্যাবে ক্লিক করুন
3. বাম পাশে **`Build Android Debug APK`** ওয়ার্কফ্লো দেখতে পাবেন
4. সবুজ রঙের ✅ দেখলে বুঝবেন build successful
   - 🟡 হলুুদ = চলছে (৩-৮ মিনিট)
   - 🔴 লাল = কোনো সমস্যা হয়েছে

---

### Step 4: APK Download করুন

1. **`Actions`** ট্যাবে গিয়ে সফল build-টিতে ক্লিক করুন
2. নিচে স্ক্রল করলে **`Artifacts`** সেকশন দেখতে পাবেন
3. **`whatsapp-automata-debug-apk`** ফাইলটি ক্লিক করে download করুন
4. Download হওয়া `.zip` ফাইলটি extract করলে ভেতরে `app-debug.apk` পাবেন
5. সেটি ফোনে পাঠিয়ে install করুন

---

## 🔄 নতুন কোড push করলে কী হয়?

প্রতিবার আপনি `main` বা `master` ব্রাঞ্চে push করলে:
1. GitHub স্বয়ংক্রিয়ভাবে build শুরু করবে
2. ৩-৮ মিনিটে নতুন APK তৈরি হবে
3. Actions → সর্বশেষ run → Artifacts থেকে download করুন

---

## 🎯 ম্যানুয়ালি build চালাতে চাইলে

কোড push না করেও build চালানো যায়:

1. **`Actions`** ট্যাবে যান
2. বাম পাশে **`Build Android Debug APK`** সিলেক্ট করুন
3. ডান পাশে **`Run workflow`** বাটনে ক্লিক করুন
4. আবার **`Run workflow`** ক্লিক করুন

---

## 🔧 Build কেন ব্যর্থ হতে পারে? (Troubleshooting)

### Problem 1: `google-services.json` related

আমাদের workflow টি স্বয়ংক্রিয়ভাবে একটা placeholder `google-services.json` তৈরি করে দেয়। তাই এই সমস্যা আসার কথা না। তবু যদি আসে:

**সমাধান:** Firebase project তৈরি করে আসল `google-services.json` ফাইলটি `app/` ফোল্ডারে রেখে commit করুন।
1. https://console.firebase.google.com/ এ যান
2. New project তৈরি করুন
3. Android app যোগ করুন (package: `com.aistudio.whatsappautomator.xtrqwb`)
4. `google-services.json` download করে `app/` ফোল্ডারে রাখুন
5. Commit & push করুন

---

### Problem 2: `GEMINI_API_KEY` related

`.env` ফাইল নেই এমন সমস্যা আমাদের workflow স্বয়ংক্রিয়ভাবে হ্যান্ডেল করে।

**তবে AI ফিচার কাজ করাতে চাইলে আসল API key দরকার:**
1. GitHub repository → **Settings** → **Secrets and variables** → **Actions**
2. **`New repository secret`** ক্লিক করুন
3. Name: `GEMINI_API_KEY`, Value: (আপনার Gemini API key)
4. workflow-এ এই step যোগ করুন `.env` তৈরির জায়গায়:
   ```yaml
   - name: Create .env with real key
     run: echo "GEMINI_API_KEY=${{ secrets.GEMINI_API_KEY }}" > .env
   ```

---

### Problem 3: Build timeout / অনেক সময় নিচ্ছে

প্রথমবার ৫-১০ মিনিট স্বাভাবিক (dependencies download হয়)। দ্বিতীয়বার থেকে cache থাকায় ২-৩ মিনিটে হবে।

---

### Problem 4: `compileSdk 35` available না

আমাদের workflow-এ `platforms;android-35` ও `build-tools;35.0.0` ইনস্টল করা আছে। তাই সমস্যা আসবে না।

---

## 📊 Workflow কী কী করে? (সংক্ষেপে)

| Step | কাজ |
|------|-----|
| 1 | কোড checkout করে |
| 2 | JDK 17 (Temurin) ইনস্টল করে |
| 3 | Android SDK + platform-35 + build-tools ইনস্টল করে |
| 4 | `local.properties` ফাইল তৈরি করে (SDK path সহ) |
| 5 | `.env` ফাইল তৈরি করে (placeholder key সহ) |
| 6 | `google-services.json` placeholder তৈরি করে (না থাকলে) |
| 7 | Gradle cache করে (পরেরবার দ্রুত হবে) |
| 8 | `./gradlew assembleDebug` চালায় |
| 9 | APK artifact হিসেবে upload করে (৯০ দিন থাকে) |

---

## ✅ Quick Checklist

- [ ] GitHub-এ private repository তৈরি হয়েছে
- [ ] `.github/workflows/build-apk.yml` ফাইল আপলোড হয়েছে
- [ ] `Actions` ট্যাবে build শুরু হয়েছে
- [ ] সবুজ ✅ দেখা গেছে
- [ ] Artifacts থেকে APK download করা হয়েছে
- [ ] ফোনে install করা হয়েছে

**সব ঠিক থাকলে আপনার APK প্রস্তুত! 🎉**

---

## 📞 সাহায্যের জন্য

build ব্যর্থ হলে:
1. `Actions` ট্যাবে ব্যর্থ run-টিতে ক্লিক করুন
2. কোন step-এ 🔴 লাল আইকন আছে সেটায় ক্লিক করুন
3. error log টি পুরোপুরি copy করুন
4. আমাকে পাঠান — আমি help করব!
