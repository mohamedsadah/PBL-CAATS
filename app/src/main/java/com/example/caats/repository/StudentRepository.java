package com.example.caats.repository;

import android.content.Context;
import android.util.Log;

import com.example.caats.network.AuthService;
import com.example.caats.network.SupabaseClient;
import com.example.caats.utils.PreferenceManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentRepository {
    private static String TAG = "Student Repository";
    private static String studentProfileID;


    public interface DashboardCallback {
        void onSuccess(JsonObject dashboardData);
        void onError(String error);
    }

    public interface studentDataCallback {
        void onSuccess(JsonObject dashboardData);
        void onError(String error);
    }

    public interface ActiveSessionsCallback {
        void onSuccess(JsonArray activeSessions);
        void onError(String error);
    }

    public interface MarkAttendanceCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public static void fetchDashboardData(Context context, String studentId, DashboardCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_student_id", studentId);

        service.getStudentDashboard(body)
                .enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load dashboard.");
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public static void fetchStudentProfile(Context context, String authUid, studentDataCallback callback) {
        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        String endpoint = "profiles?auth_uid=eq." + authUid;
        Log.d(TAG, "Fetching student profile: " + endpoint);

        restService.getTableFiltered(SupabaseClient.SUPABASE_URL + "rest/v1/" + endpoint)
                .enqueue(new Callback<JsonObject[]>() {
                    @Override
                    public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "student profile response: " + Arrays.toString(response.body()));

                            studentProfileID = response.body()[0].get("id").getAsString();

                            if (response.body().length > 0) {
                                callback.onSuccess(response.body()[0]);
                            } else {
                                callback.onError("student profile not found");
                                Log.d(TAG, "student profile not found");
                            }
                        } else {
                            callback.onError("Failed to fetch student profile: " + response.code());
                            Log.e(TAG, "Profile fetch failed: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject[]> call, Throwable t) {
                        callback.onError("Profile fetch error: " + t.getMessage());
                        Log.e(TAG, "Profile fetch error: ", t);
                    }
                });
    }



    public static void fetchActiveSessions(Context context, ActiveSessionsCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_student_id", studentProfileID);

        service.getActiveSessions(body).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to load sessions.");
                }
            }
            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Mark attendance
    public static void markAttendance(
            Context context, String sessionId, String studentId,
            double latitude, double longitude,
            MarkAttendanceCallback callback
    ) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);

        JsonObject body = new JsonObject();
        body.addProperty("p_session_id", sessionId);
        body.addProperty("p_student_id", studentId);
        body.addProperty("p_student_lat", latitude);
        body.addProperty("p_student_lng", longitude);

        service.markAttendanceGeofenced(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().get("status").getAsString();
                    String message = response.body().get("message").getAsString();
                    if ("success".equals(status)) {
                        callback.onSuccess(message);
                    } else {
                        callback.onError(message); // e.g., "You are not inside the classroom."
                    }
                } else {
                    callback.onError("Failed to mark attendance.");
                }
            }
            // ... onFailure ...
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
