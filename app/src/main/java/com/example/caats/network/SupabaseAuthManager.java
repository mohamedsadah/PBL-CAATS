package com.example.caats.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseAuthManager {

    private static final String TAG = "SupabaseAuth";
    private static final OkHttpClient client = new OkHttpClient();

    public interface TokenCallback {
        void onSuccess(String newAccessToken);
        void onError(String error);
    }

    /**
     * Retrieve current token, refresh asynchronously if expired
     */
    public static void getAccessToken(Context context, TokenCallback callback) {
        SharedPreferences prefs = context.getSharedPreferences("SupabasePrefs", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("access_token", null);
        String refreshToken = prefs.getString("refresh_token", null);
        long expiresAt = prefs.getLong("expires_at", 0);

        if (accessToken == null || System.currentTimeMillis() > expiresAt) {
            if (refreshToken != null) {
                Log.d(TAG, "Refreshing expired token...");
                refreshAccessToken(context, refreshToken, callback);
            } else {
                Log.e(TAG, "No refresh token found. User must log in again.");
                callback.onError("No refresh token");
            }
        } else {
            callback.onSuccess(accessToken);
        }
    }

    /**
     * Refresh token in background thread using OkHttp enqueue
     */
    public static void refreshAccessToken(Context context, String refreshToken, TokenCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("refresh_token", refreshToken);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SupabaseClient.SUPABASE_URL + "/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", SupabaseClient.API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Token refresh failed: " + e.getMessage());
                    new Handler(Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage())
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject res = new JSONObject(response.body().string());
                            String newAccessToken = res.getString("access_token");
                            String newRefreshToken = res.optString("refresh_token", refreshToken);
                            long expiresIn = res.optLong("expires_in", 3600);

                            SharedPreferences prefs = context.getSharedPreferences("SupabasePrefs", Context.MODE_PRIVATE);
                            prefs.edit()
                                    .putString("access_token", newAccessToken)
                                    .putString("refresh_token", newRefreshToken)
                                    .putLong("expires_at", System.currentTimeMillis() + (expiresIn - 60) * 1000)
                                    .apply();

                            Log.d(TAG, "Token refreshed successfully");

                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onSuccess(newAccessToken)
                            );
                        } catch (Exception e) {
                            new Handler(Looper.getMainLooper()).post(() ->
                                    callback.onError(e.getMessage())
                            );
                        }
                    } else {
                        String errorMsg = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "Token refresh failed: " + errorMsg);
                        new Handler(Looper.getMainLooper()).post(() ->
                                callback.onError(errorMsg)
                        );
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing token", e);
            callback.onError(e.getMessage());
        }
    }
}
