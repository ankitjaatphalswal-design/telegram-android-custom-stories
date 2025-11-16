# Telegram Android - Custom Story Backend

**Modified Telegram Android app that uses YOUR custom backend server for story features**

[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Backend](https://img.shields.io/badge/Backend-Custom-orange.svg)](https://github.com/ankitjaatphalswal-design/telegram-story-backend)

## 🎯 What This Does

This is a **modified version** of [Telegram Android](https://github.com/DrKLO/Telegram) that:

✅ Keeps 100% of Telegram's **native UI** and features  
✅ Redirects **story uploads** to YOUR custom backend  
✅ Fetches **stories** from YOUR server  
✅ Uses YOUR **authentication system**  
✅ Allows full **control over story data**  

## 🏗️ Architecture

```
┌─────────────────────┐
│  Your Modified App  │
│  (Telegram Fork)    │
└──────────┬──────────┘
           │
           ├─── Regular Chat ────► Official Telegram Servers
           │
           └─── Stories ─────────► YOUR Backend Server
                                   (Railway/Custom)
```

## 📋 Prerequisites

Before you start, you need:

1. **Android Studio** (Arctic Fox or newer)
2. **JDK 11** or higher
3. **Your Backend Server** deployed (see: [telegram-story-backend](https://github.com/ankitjaatphalswal-design/telegram-story-backend))
4. **Git** installed

## 🚀 Quick Start

### Step 1: Fork Official Telegram

```bash
# Clone official Telegram Android repository
git clone https://github.com/DrKLO/Telegram.git telegram-custom-stories
cd telegram-custom-stories

# Add this repository as a remote for the modifications
git remote add custom https://github.com/ankitjaatphalswal-design/telegram-android-custom-stories.git
git fetch custom
```

### Step 2: Apply Modifications

```bash
# Create a new branch for your modifications
git checkout -b feature/custom-story-backend

# Copy modification files from this repository
git checkout custom/main -- modifications/

# Apply the patches
./apply-modifications.sh
```

### Step 3: Configure Your Backend

Edit `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java`:

```java
// Your custom backend URL
public static final String CUSTOM_STORY_BACKEND = "https://your-app.up.railway.app";

// Enable custom backend
public static final boolean USE_CUSTOM_STORY_BACKEND = true;
```

### Step 4: Build

```bash
# Open in Android Studio
studio .

# Or build from command line
./gradlew assembleDebug
```

### Step 5: Install

```bash
adb install -r TMessagesProj/build/outputs/apk/debug/app-debug.apk
```

## 📁 Project Structure

```
telegram-custom-stories/
├── modifications/               # Our custom modifications
│   ├── CustomStoryBackend.java  # API client for your backend
│   ├── patches/                 # Git patches for core files
│   └── README.md                # Detailed modification guide
├── TMessagesProj/               # Main Telegram app project
│   └── src/main/java/org/telegram/
│       ├── messenger/
│       │   ├── BuildVars.java        # Config (MODIFIED)
│       │   └── CustomStoryBackend.java  # NEW FILE
│       └── ui/
│           └── Stories/
│               └── StoriesController.java  # (MODIFIED)
└── README.md                    # This file
```

## 🔧 Key Modifications

### 1. Custom Backend Configuration
**File**: `BuildVars.java`
- Added `CUSTOM_STORY_BACKEND` constant
- Added `USE_CUSTOM_STORY_BACKEND` toggle

### 2. Backend API Client
**File**: `CustomStoryBackend.java` (NEW)
- Handles authentication with your backend
- Uploads stories via multipart/form-data
- Fetches stories from your API
- Records views and likes

### 3. Stories Controller
**File**: `StoriesController.java`
- Intercepts story uploads
- Redirects to custom backend when enabled
- Maintains Telegram UI/UX

### 4. Authentication Bridge
- Auto-registers Telegram users on your backend
- Stores JWT tokens securely
- Includes token in all API requests

## 🎨 Features

### What Works Out of the Box

- ✅ **Upload Stories** (photos/videos) to your backend
- ✅ **View Stories** from your backend with Telegram UI
- ✅ **Like/Unlike** stories
- ✅ **View Count** tracking
- ✅ **Story Expiration** (managed by your backend)
- ✅ **Auto-Authentication** using Telegram credentials
- ✅ **All other Telegram features** work normally

### What's Preserved

- ✅ Original Telegram UI/UX
- ✅ All chat features
- ✅ Voice/Video calls
- ✅ Channels & Groups
- ✅ Settings & Preferences

## 🔒 Security & Privacy

### Data Flow

1. **Stories**: YOUR server only
2. **Chats**: Telegram servers only
3. **User Authentication**: Hybrid (Telegram + Your JWT)

### Security Features

- JWT token encryption
- HTTPS-only communication
- Certificate pinning (optional)
- Secure token storage in SharedPreferences

## 📱 Distribution

### For Testing (Debug Build)

```bash
# Build debug APK
./gradlew assembleDebug

# Install on device
adb install -r TMessagesProj/build/outputs/apk/debug/app-debug.apk
```

### For Production (Release Build)

1. **Generate Keystore**:
```bash
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

2. **Configure `keystore.properties`**:
```properties
storeFile=my-release-key.keystore
storePassword=****
keyAlias=my-key-alias
keyPassword=****
```

3. **Build Release APK**:
```bash
./gradlew assembleRelease
```

4. **Distribute**:
   - Direct download
   - Firebase App Distribution
   - Internal app store
   - TestFlight

## 🧪 Testing

### Manual Test Cases

1. **Story Upload**:
   - Upload a photo story
   - Upload a video story
   - Verify it appears on your backend

2. **Story Viewing**:
   - View your own stories
   - View others' stories
   - Verify view count increases

3. **Story Interaction**:
   - Like a story
   - Unlike a story
   - Verify like count updates

4. **Expiration**:
   - Wait for story to expire (24h default)
   - Verify it disappears

### API Testing

```bash
# Test your backend is reachable
curl https://your-app.up.railway.app/health

# Test story upload (from app)
# Check Android Studio Logcat for API responses
```

## 🐛 Troubleshooting

### Common Issues

#### "Cannot connect to backend"
- ✅ Check `BuildVars.CUSTOM_STORY_BACKEND` is correct
- ✅ Ensure backend is deployed and running
- ✅ Check backend health endpoint
- ✅ Verify HTTPS (not HTTP)

#### "Authentication failed"
- ✅ Check JWT token storage
- ✅ Verify `/api/auth/register` endpoint works
- ✅ Check Logcat for API errors

#### "Story not uploading"
- ✅ Check file size (max 50MB)
- ✅ Verify Cloudinary credentials on backend
- ✅ Check network connectivity
- ✅ Look at Logcat for errors

### Debug Mode

Enable verbose logging in `BuildVars.java`:
```java
public static final boolean DEBUG_VERSION = true;
public static final boolean LOGS_ENABLED = true;
```

View logs:
```bash
adb logcat | grep "CustomStoryBackend"
```

## ⚖️ Legal & Compliance

### GPL v2 License

This project is based on Telegram Android, which is licensed under GPL v2.  
**You MUST**:
- ✅ Keep source code open source
- ✅ Include GPL v2 license
- ✅ Credit original Telegram developers
- ✅ Share modifications publicly

### Branding

- ❌ **DON'T** use "Telegram" in app name
- ✅ **DO** use your own name (e.g., "MyApp Stories")
- ✅ **DO** change package name: `com.yourcompany.app`
- ✅ **DO** use your own app icon
- ✅ **DO** add disclaimer: "Based on Telegram, not affiliated"

### Trademark

Telegram® is a registered trademark. This is an independent modification.

## 📚 Documentation

- [**Modification Guide**](modifications/README.md) - Detailed step-by-step
- [**API Reference**](docs/API.md) - Custom backend API docs
- [**Building Guide**](docs/BUILD.md) - Complete build instructions
- [**FAQ**](docs/FAQ.md) - Frequently asked questions

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork this repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/ankitjaatphalswal-design/telegram-android-custom-stories/issues)
- **Discussions**: [GitHub Discussions](https://github.com/ankitjaatphalswal-design/telegram-android-custom-stories/discussions)
- **Backend Repo**: [telegram-story-backend](https://github.com/ankitjaatphalswal-design/telegram-story-backend)

## 🙏 Credits

- **Original Telegram**: [DrKLO/Telegram](https://github.com/DrKLO/Telegram)
- **Telegram Team**: For amazing open source app
- **Contributors**: Everyone who helped improve this mod

## 📄 License

This project is licensed under GPL v2 - see the [LICENSE](LICENSE) file.

**Based on Telegram Android** - Copyright (C) 2013-2024 Telegram FZ-LLC

---

**⚠️ Disclaimer**: This is an independent modification of Telegram. Not affiliated with or endorsed by Telegram FZ-LLC.

**Made with ❤️ for developers who want control over their story data**
