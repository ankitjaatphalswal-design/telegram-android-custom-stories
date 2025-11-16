package org.telegram.messenger;

import android.util.Log;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Custom Story Backend API Client
 * Handles all communication with your custom story backend server
 * 
 * @author ankitjaatphalswal-design
 * @version 1.0
 */
public class CustomStoryBackend {
    private static final String TAG = "CustomStoryBackend";
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private static String jwtToken = null;
    private static boolean isAuthenticated = false;

    /**
     * Authenticate user with custom backend
     * Called automatically when app starts
     */
    public static void authenticate(String telegramId, String username, 
                                   String firstName, String lastName, 
                                   final AuthCallback callback) {
        new Thread(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("telegramId", telegramId);
                json.put("username", username != null ? username : "user_" + telegramId);
                json.put("firstName", firstName != null ? firstName : "");
                json.put("lastName", lastName != null ? lastName : "");

                RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(BuildVars.CUSTOM_STORY_BACKEND + "/api/auth/register")
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Auth response: " + responseBody);

                JSONObject result = new JSONObject(responseBody);

                if (result.getBoolean("success")) {
                    JSONObject data = result.getJSONObject("data");
                    jwtToken = data.getString("token");
                    isAuthenticated = true;

                    // Save token securely
                    SharedConfig.saveCustomStoryToken(jwtToken);

                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onSuccess());
                    }
                    Log.i(TAG, "Authentication successful");
                } else {
                    String error = result.optString("error", "Unknown error");
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onError(error));
                    }
                    Log.e(TAG, "Authentication failed: " + error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Authentication error", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * Upload story to custom backend
     * Supports both image and video uploads
     */
    public static void uploadStory(String filePath, String type, String caption,
                                  String backgroundColor, int duration, String visibility,
                                  final UploadCallback callback) {
        if (!isAuthenticated || jwtToken == null) {
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return;
        }

        new Thread(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    callback.onError("File not found");
                    return;
                }

                // Determine media type
                String mimeType = type.equals("video") ? "video/*" : "image/*";

                RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse(mimeType)))
                    .addFormDataPart("type", type)
                    .addFormDataPart("caption", caption != null ? caption : "")
                    .addFormDataPart("backgroundColor", backgroundColor != null ? backgroundColor : "#FFFFFF")
                    .addFormDataPart("duration", String.valueOf(duration))
                    .addFormDataPart("visibility", visibility != null ? visibility : "public")
                    .build();

                Request request = new Request.Builder()
                    .url(BuildVars.CUSTOM_STORY_BACKEND + "/api/stories/create")
                    .header("Authorization", "Bearer " + jwtToken)
                    .post(requestBody)
                    .build();

                Log.d(TAG, "Uploading story: " + file.getName());

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Upload response: " + responseBody);

                JSONObject result = new JSONObject(responseBody);

                if (result.getBoolean("success")) {
                    JSONObject data = result.getJSONObject("data");
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onSuccess(data));
                    }
                    Log.i(TAG, "Story uploaded successfully");
                } else {
                    String error = result.optString("error", "Upload failed");
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onError(error));
                    }
                    Log.e(TAG, "Upload failed: " + error);
                }
            } catch (Exception e) {
                Log.e(TAG, "Upload error", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * Fetch stories from custom backend
     * Returns all active (non-expired) stories
     */
    public static void fetchStories(final StoriesCallback callback) {
        if (!isAuthenticated || jwtToken == null) {
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                    .url(BuildVars.CUSTOM_STORY_BACKEND + "/api/stories")
                    .header("Authorization", "Bearer " + jwtToken)
                    .get()
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();
                Log.d(TAG, "Fetch stories response: " + responseBody);

                JSONObject result = new JSONObject(responseBody);

                if (result.getBoolean("success")) {
                    JSONObject data = result.getJSONObject("data");
                    JSONArray stories = data.optJSONArray("stories");

                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onSuccess(stories));
                    }
                    Log.i(TAG, "Fetched " + (stories != null ? stories.length() : 0) + " stories");
                } else {
                    String error = result.optString("error", "Fetch failed");
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onError(error));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch error", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * Record a view on a story
     */
    public static void recordView(String storyId) {
        if (!isAuthenticated || jwtToken == null) return;

        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create("", MediaType.parse("application/json"));

                Request request = new Request.Builder()
                    .url(BuildVars.CUSTOM_STORY_BACKEND + "/api/stories/" + storyId + "/view")
                    .header("Authorization", "Bearer " + jwtToken)
                    .post(body)
                    .build();

                client.newCall(request).execute();
                Log.d(TAG, "View recorded for story: " + storyId);
            } catch (Exception e) {
                Log.e(TAG, "Record view error", e);
            }
        }).start();
    }

    /**
     * Toggle like on a story
     */
    public static void toggleLike(String storyId, final LikeCallback callback) {
        if (!isAuthenticated || jwtToken == null) {
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return;
        }

        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create("", MediaType.parse("application/json"));

                Request request = new Request.Builder()
                    .url(BuildVars.CUSTOM_STORY_BACKEND + "/api/stories/" + storyId + "/like")
                    .header("Authorization", "Bearer " + jwtToken)
                    .post(body)
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                JSONObject result = new JSONObject(responseBody);

                if (result.getBoolean("success")) {
                    JSONObject data = result.getJSONObject("data");
                    boolean isLiked = data.getBoolean("isLiked");
                    int likesCount = data.optInt("likesCount", 0);

                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onSuccess(isLiked, likesCount));
                    }
                    Log.d(TAG, "Like toggled for story: " + storyId + " -> " + isLiked);
                }
            } catch (Exception e) {
                Log.e(TAG, "Toggle like error", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }

    /**
     * Delete a story (owner only)
     */
    public static void deleteStory(String storyId, final DeleteCallback callback) {
        if (!isAuthenticated || jwtToken == null) {
            if (callback != null) {
                callback.onError("Not authenticated");
            }
            return;
        }

        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                    .url(BuildVars.CUSTOM_STORY_BACKEND + "/api/stories/" + storyId)
                    .header("Authorization", "Bearer " + jwtToken)
                    .delete()
                    .build();

                Response response = client.newCall(request).execute();
                String responseBody = response.body().string();

                JSONObject result = new JSONObject(responseBody);

                if (result.getBoolean("success")) {
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onSuccess());
                    }
                    Log.i(TAG, "Story deleted: " + storyId);
                } else {
                    String error = result.optString("error", "Delete failed");
                    if (callback != null) {
                        AndroidUtilities.runOnUIThread(() -> callback.onError(error));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Delete error", e);
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
                }
            }
        }).start();
    }

    // Callback interfaces
    public interface AuthCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface UploadCallback {
        void onSuccess(JSONObject data);
        void onError(String error);
    }

    public interface StoriesCallback {
        void onSuccess(JSONArray stories);
        void onError(String error);
    }

    public interface LikeCallback {
        void onSuccess(boolean isLiked, int likesCount);
        void onError(String error);
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}
