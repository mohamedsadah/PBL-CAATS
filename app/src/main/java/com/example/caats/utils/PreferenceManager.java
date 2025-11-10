package com.example.caats.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "CAATS_PREFS";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_ROLE = "role";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PROFILE_IMAGE_URL = "profileImageUrl";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Save access token
    public static void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        return getPrefs(context).getString(KEY_ACCESS_TOKEN, null);
    }

    public static void saveRefreshToken(Context context, String refreshToken) {
        getPrefs(context).edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply();
    }

    public static String getRefreshToken(Context context) {
        return getPrefs(context).getString(KEY_REFRESH_TOKEN, null);
    }

    public static void saveUserId(Context context, String userId) {
        getPrefs(context).edit().putString(KEY_USER_ID, userId).apply();
    }

    public static String getUserId(Context context) {
        return getPrefs(context).getString(KEY_USER_ID, null);
    }

    // Save role
    public static void saveRole(Context context, String role) {
        getPrefs(context).edit().putString(KEY_ROLE, role).apply();
    }

    public static String getRole(Context context) {
        return getPrefs(context).getString(KEY_ROLE, null);
    }

    public static void saveFullName(Context context, String fullName) {
        getPrefs(context).edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    public static String getFullName(Context context) {
        return getPrefs(context).getString(KEY_FULL_NAME, null);
    }

    public static void saveEmail(Context context, String email) {
        getPrefs(context).edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getEmail(Context context) {
        return getPrefs(context).getString(KEY_EMAIL, null);
    }

    public static void saveImageUrl(Context context, String imageUrl) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            editor.putString(KEY_PROFILE_IMAGE_URL, imageUrl);
        } else {
            editor.remove(KEY_PROFILE_IMAGE_URL);
        }
        editor.apply();
    }

    public static String getImageUrl(Context context) {
        return getPrefs(context).getString(KEY_PROFILE_IMAGE_URL, null);
    }

    public static void clearAll(Context context) {
        getPrefs(context).edit().clear().apply();
    }
}