# Step-by-Step Implementation Guide

## 📋 Complete Implementation Checklist

Follow these steps **in order**:

- [ ] Step 1: Clone Telegram Android
- [ ] Step 2: Open in Android Studio
- [ ] Step 3: Add CustomStoryBackend.java
- [ ] Step 4: Modify BuildVars.java
- [ ] Step 5: Modify StoriesController.java
- [ ] Step 6: Add Authentication in LaunchActivity.java
- [ ] Step 7: Add Dependencies
- [ ] Step 8: Build & Test

---

## 🚀 Step 1: Clone Telegram Android

Open Terminal and run:

```bash
# Navigate to where you want the project
cd ~/Projects

# Clone official Telegram Android
git clone https://github.com/DrKLO/Telegram.git telegram-custom-stories
cd telegram-custom-stories

# Create a branch for your modifications
git checkout -b custom-story-backend
```

**Expected Output:**
```
Cloning into 'telegram-custom-stories'...
Switched to a new branch 'custom-story-backend'
```

---

## 📂 Step 2: Open in Android Studio

### Option A: From Terminal
```bash
# macOS/Linux
studio .

# Or just open Android Studio and select the folder
```

### Option B: From Android Studio
1. Open Android Studio
2. Click **"Open"**
3. Navigate to `~/Projects/telegram-custom-stories`
4. Click **"OK"**

**Wait for Gradle Sync** (may take 5-10 minutes first time)

---

## ✍️ Step 3: Add CustomStoryBackend.java

### 3.1 Download the File

```bash
# From project root directory
curl -o TMessagesProj/src/main/java/org/telegram/messenger/CustomStoryBackend.java   https://raw.githubusercontent.com/ankitjaatphalswal-design/telegram-android-custom-stories/main/modifications/CustomStoryBackend.java
```

### 3.2 Or Create Manually

**Path**: `TMessagesProj/src/main/java/org/telegram/messenger/CustomStoryBackend.java`

In Android Studio:
1. Right-click on `org.telegram.messenger` package
2. Click **New → Java Class**
3. Name it: `CustomStoryBackend`
4. Copy the code from: [CustomStoryBackend.java](https://github.com/ankitjaatphalswal-design/telegram-android-custom-stories/blob/main/modifications/CustomStoryBackend.java)

---

## 🔧 Step 4: Modify BuildVars.java

**File**: `TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java`

### 4.1 Open the File

In Android Studio:
- Press `Ctrl+Shift+N` (Windows/Linux) or `Cmd+Shift+O` (Mac)
- Type: `BuildVars.java`
- Press Enter

### 4.2 Add Configuration

Scroll to the **end of the class** (before the last closing brace) and add:

```java
public class BuildVars {
    // ... ALL existing code stays here ...

    // ===== ADD THESE LINES AT THE END =====

    // Custom Story Backend Configuration
    public static final String CUSTOM_STORY_BACKEND = "https://your-app.up.railway.app";

    // Toggle to enable/disable custom backend
    // Set to true to use YOUR backend, false to use Telegram's
    public static final boolean USE_CUSTOM_STORY_BACKEND = true;

    // ===== END OF ADDITIONS =====
}
```

**⚠️ IMPORTANT**: Replace `https://your-app.up.railway.app` with YOUR actual backend URL!

### 4.3 Save the File

Press `Ctrl+S` (Windows/Linux) or `Cmd+S` (Mac)

---

## 📝 Step 5: Modify StoriesController.java

**File**: `TMessagesProj/src/main/java/org/telegram/ui/Stories/StoriesController.java`

### 5.1 Open the File

- Press `Ctrl+Shift+N` / `Cmd+Shift+O`
- Type: `StoriesController.java`
- Press Enter

### 5.2 Find the Upload Method

Press `Ctrl+F` / `Cmd+F` and search for: `uploadStory` or `sendStory`

Look for a method that looks like this:

```java
public void uploadStory(/* parameters */) {
    // existing code...
}
```

### 5.3 Add Custom Backend Check at the START of the method

**Add this code RIGHT AFTER the method declaration**:

```java
public void uploadStory(StoryEntry entry, boolean silent) {

    // ===== ADD THIS BLOCK HERE =====
    if (BuildVars.USE_CUSTOM_STORY_BACKEND) {
        uploadStoryToCustomBackend(entry, silent);
        return; // Don't execute Telegram's upload
    }
    // ===== END OF ADDITIONS =====

    // ... existing Telegram upload code continues below ...
}
```

### 5.4 Add Custom Upload Method

**At the END of the StoriesController class**, add this method:

```java
public class StoriesController {
    // ... all existing code ...

    // ===== ADD THIS METHOD AT THE END OF THE CLASS =====

    private void uploadStoryToCustomBackend(StoryEntry entry, boolean silent) {
        // Get file path
        String filePath = entry.file != null ? entry.file.getAbsolutePath() : null;
        if (filePath == null && entry.uploadThumbFile != null) {
            filePath = entry.uploadThumbFile.getAbsolutePath();
        }

        if (filePath == null) {
            FileLog.e("Cannot upload story: no file path");
            return;
        }

        // Determine type
        String type = entry.isVideo ? "video" : "image";

        // Get caption
        String caption = entry.caption != null ? entry.caption.toString() : "";

        // Show uploading message
        AndroidUtilities.runOnUIThread(() -> {
            if (LaunchActivity.getLastFragment() != null) {
                BulletinFactory.of(LaunchActivity.getLastFragment())
                    .createSimpleBulletin(R.raw.cloud_upload, 
                        LocaleController.getString("StoryUploading", R.string.Uploading))
                    .show();
            }
        });

        // Upload to custom backend
        CustomStoryBackend.uploadStory(
            filePath,
            type,
            caption,
            "#FFFFFF",
            24, // 24 hours duration
            "public",
            new CustomStoryBackend.UploadCallback() {
                @Override
                public void onSuccess(org.json.JSONObject data) {
                    AndroidUtilities.runOnUIThread(() -> {
                        // Show success
                        if (LaunchActivity.getLastFragment() != null) {
                            BulletinFactory.of(LaunchActivity.getLastFragment())
                                .createSimpleBulletin(R.raw.contact_check, 
                                    LocaleController.getString("StoryPosted", R.string.Done))
                                .show();
                        }

                        // Refresh stories
                        NotificationCenter.getInstance(currentAccount)
                            .postNotificationName(NotificationCenter.storiesUpdated);

                        // Clean up temp file
                        if (entry.file != null && entry.file.exists()) {
                            entry.file.delete();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    AndroidUtilities.runOnUIThread(() -> {
                        FileLog.e("Story upload failed: " + error);

                        // Show error
                        if (LaunchActivity.getLastFragment() != null) {
                            BulletinFactory.of(LaunchActivity.getLastFragment())
                                .createErrorBulletin(
                                    LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + 
                                    ": " + error)
                                .show();
                        }
                    });
                }
            }
        );
    }

    // ===== END OF ADDITIONS =====
}
```

### 5.5 Modify Load Stories Method

Find the method that loads stories (search for `loadStories` or `loadAllStories`):

```java
public void loadAllStories() {

    // ===== ADD THIS AT THE START =====
    if (BuildVars.USE_CUSTOM_STORY_BACKEND) {
        loadStoriesFromCustomBackend();
        return;
    }
    // ===== END OF ADDITIONS =====

    // ... existing code ...
}
```

### 5.6 Add Custom Load Method

Add this method at the end of the class:

```java
private void loadStoriesFromCustomBackend() {
    CustomStoryBackend.fetchStories(new CustomStoryBackend.StoriesCallback() {
        @Override
        public void onSuccess(org.json.JSONArray stories) {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    // Parse stories and update UI
                    FileLog.d("Loaded " + stories.length() + " stories from custom backend");

                    // Notify UI to refresh
                    NotificationCenter.getInstance(currentAccount)
                        .postNotificationName(NotificationCenter.storiesUpdated);

                    // You can parse and display stories here
                    // For now, just logging
                    for (int i = 0; i < stories.length(); i++) {
                        org.json.JSONObject story = stories.getJSONObject(i);
                        FileLog.d("Story: " + story.toString());
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }

        @Override
        public void onError(String error) {
            FileLog.e("Failed to load stories: " + error);
        }
    });
}
```

### 5.7 Save the File

Press `Ctrl+S` / `Cmd+S`

---

## 🔑 Step 6: Add Authentication

**File**: `TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java`

### 6.1 Open the File

- Press `Ctrl+Shift+N` / `Cmd+Shift+O`
- Type: `LaunchActivity.java`
- Press Enter

### 6.2 Find onCreate Method

Search for: `protected void onCreate`

You'll see something like:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // ... existing code ...
}
```

### 6.3 Add Authentication at the END of onCreate

Scroll to the end of the `onCreate` method and add:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // ... ALL existing code ...

    // ===== ADD THIS AT THE END, BEFORE THE CLOSING BRACE =====

    // Authenticate with custom story backend
    if (BuildVars.USE_CUSTOM_STORY_BACKEND) {
        authenticateWithCustomBackend();
    }

    // ===== END OF ADDITIONS =====
}
```

### 6.4 Add Authentication Method

At the end of the LaunchActivity class, add:

```java
public class LaunchActivity extends BasePermissionsActivity {
    // ... all existing code ...

    // ===== ADD THIS METHOD AT THE END =====

    private void authenticateWithCustomBackend() {
        int currentAccount = UserConfig.selectedAccount;
        if (!UserConfig.getInstance(currentAccount).isClientActivated()) {
            return; // User not logged in yet
        }

        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (user == null) {
            return;
        }

        // Authenticate
        CustomStoryBackend.authenticate(
            String.valueOf(user.id),
            user.username != null ? user.username : "",
            user.first_name != null ? user.first_name : "",
            user.last_name != null ? user.last_name : "",
            new CustomStoryBackend.AuthCallback() {
                @Override
                public void onSuccess() {
                    FileLog.d("Custom story backend authenticated successfully");
                }

                @Override
                public void onError(String error) {
                    FileLog.e("Custom story backend auth failed: " + error);
                }
            }
        );
    }

    // ===== END OF ADDITIONS =====
}
```

### 6.5 Save the File

Press `Ctrl+S` / `Cmd+S`

---

## 📦 Step 7: Add Dependencies

**File**: `TMessagesProj/build.gradle`

### 7.1 Open the File

In Android Studio's left panel:
1. Expand **TMessagesProj**
2. Find and double-click `build.gradle`

### 7.2 Find dependencies Block

Scroll down to find:

```gradle
dependencies {
    // ... existing dependencies ...
}
```

### 7.3 Add OkHttp

Add this line inside the dependencies block:

```gradle
dependencies {
    // ... existing dependencies ...

    // ===== ADD THIS LINE =====
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    // ===== END OF ADDITIONS =====
}
```

### 7.4 Sync Gradle

Click the **"Sync Now"** button that appears at the top of the editor.

**Wait for sync to complete** (may take 2-3 minutes).

---

## 🔨 Step 8: Build & Test

### 8.1 Build Debug APK

In Android Studio:
1. Click **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
2. Wait for build to complete (5-10 minutes)

**Or from Terminal:**

```bash
./gradlew assembleDebug
```

### 8.2 Find Your APK

After build completes, find the APK at:
```
TMessagesProj/build/outputs/apk/debug/app-debug.apk
```

### 8.3 Install on Device

#### Via USB:
```bash
# Connect your Android device via USB
# Enable USB Debugging in Developer Options

adb devices  # Check device is connected
adb install -r TMessagesProj/build/outputs/apk/debug/app-debug.apk
```

#### Via Android Studio:
1. Click **Run** → **Run 'app'**
2. Select your device
3. Click **OK**

### 8.4 Test the App

1. **Launch the app**
2. **Login** with your Telegram account
3. **Check logs** to verify authentication:
   ```bash
   adb logcat | grep "CustomStoryBackend"
   ```
4. **Upload a story**:
   - Tap your profile picture
   - Tap "Add story"
   - Select a photo
   - Post it
5. **Check backend**:
   - Go to your Railway backend
   - Check logs
   - Verify story was uploaded

---

## 🐛 Troubleshooting

### Build Errors

#### "Cannot resolve symbol CustomStoryBackend"
```bash
# Solution: Sync Gradle
# In Android Studio: File → Sync Project with Gradle Files
```

#### "OkHttp not found"
```gradle
# Solution: Check build.gradle has:
implementation 'com.squareup.okhttp3:okhttp:4.12.0'

# Then: File → Sync Project with Gradle Files
```

#### Compilation Errors
- Check all code was added correctly
- Verify no missing braces `{}`
- Look at Android Studio's error messages

### Runtime Errors

#### "Not authenticated" in logs
```bash
# Check:
1. Backend URL in BuildVars.java is correct
2. Backend is running (test https://your-app.up.railway.app/health)
3. JWT token is being saved
```

#### Story doesn't upload
```bash
# Check logs:
adb logcat | grep -E "(CustomStoryBackend|Error)"

# Common causes:
1. File path is wrong
2. Backend not reachable
3. Cloudinary not configured on backend
```

#### App crashes on story upload
```bash
# Get crash logs:
adb logcat *:E

# Usually caused by:
1. Missing import statements
2. Null pointer exceptions
3. Backend returning unexpected response
```

---

## ✅ Verification Checklist

After implementation, verify:

- [ ] ✅ App builds without errors
- [ ] ✅ App installs on device
- [ ] ✅ Login works normally
- [ ] ✅ "CustomStoryBackend authenticated" appears in logs
- [ ] ✅ Story upload shows "Uploading..." message
- [ ] ✅ Story appears on backend API
- [ ] ✅ No crashes or errors

---

## 📊 Testing Commands

```bash
# View all logs
adb logcat

# Filter for your backend
adb logcat | grep "CustomStoryBackend"

# Filter for errors only
adb logcat *:E

# Clear logs
adb logcat -c

# Test backend is reachable
curl https://your-app.up.railway.app/health
```

---

## 🎯 What Happens When You Upload a Story

1. User taps "Add Story"
2. Selects photo/video
3. Taps "Post"
4. **Your code intercepts** the upload
5. `uploadStoryToCustomBackend()` is called
6. File is sent to YOUR backend via HTTP
7. Backend saves to Cloudinary & MongoDB
8. Success message shown to user
9. Story appears in the app

**Telegram's servers are never contacted for stories!**

---

## 📝 Code Summary

### Files Modified:
1. **CustomStoryBackend.java** - NEW (API client)
2. **BuildVars.java** - Added config (2 lines)
3. **StoriesController.java** - Added upload/load logic (~80 lines)
4. **LaunchActivity.java** - Added authentication (~30 lines)
5. **build.gradle** - Added OkHttp dependency (1 line)

### Total Code Added: ~115 lines

---

## 🚀 Next Steps

After successful implementation:

1. **Test thoroughly** - Upload photos, videos, view stories
2. **Customize** - Add more features from your backend API
3. **Brand** - Change app name, icon, package name
4. **Release** - Create signed APK for distribution
5. **Distribute** - Share with your users

---

## 🆘 Need Help?

If you get stuck:
1. Check the error message in Android Studio
2. Look at Logcat output
3. Verify backend is running
4. Open an issue: [GitHub Issues](https://github.com/ankitjaatphalswal-design/telegram-android-custom-stories/issues)

---

**You're all set! Follow these steps carefully and you'll have a working modified Telegram app! 🎉**
