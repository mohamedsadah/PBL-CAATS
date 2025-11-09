package com.example.caats.repository;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.caats.network.AuthService;
import com.example.caats.network.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorRepository {

    private static final String TAG = "CAATS-TutorRepo";

    public interface TutorDataCallback {
        void onSuccess(JsonObject tutorData);
        void onError(String error);
    }

    public interface SessionDataCallback {
        void onSuccess(JsonObject[] sessions);
        void onError(String error);
    }

    public interface AttendanceDataCallback {
        void onSuccess(JsonObject[] records);
        void onError(String error);
    }

    public interface SubjectsCallback {
        void onSuccess(JsonArray subjects);
        void onError(String error);
    }

    public interface ReportCallback {
        void onSuccess(JsonObject report);
        void onError(String error);
    }


    // Fetch tutor profile details by auth_uid

    public static void fetchTutorProfile(Context context, String authUid, TutorDataCallback callback) {
        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        String endpoint = "profiles?auth_uid=eq." + authUid;
        Log.d(TAG, "Fetching tutor profile: " + endpoint);

        restService.getTableFiltered(SupabaseClient.SUPABASE_URL + "rest/v1/" + endpoint)
                .enqueue(new Callback<JsonObject[]>() {
                    @Override
                    public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Tutor profile response: " + Arrays.toString(response.body()));
                            if (response.body().length > 0) {
                                callback.onSuccess(response.body()[0]);
                            } else {
                                callback.onError("Tutor profile not found");
                                Log.d(TAG, "Tutor profile not found");
                            }
                        } else {
                            callback.onError("Failed to fetch tutor profile: " + response.code());
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

    // Fetch recent tutor sessions by tutor_id

    public static void fetchRecentSessions(Context context, String tutorId, SessionDataCallback callback) {
        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        String endpoint = "attendance_sessions?tutor_id=eq." + tutorId + "&order=created_at.desc&limit=10&select=*,courses(title)";
        Log.d(TAG, "Fetching recent sessions: " + endpoint);

        restService.getTableFiltered(SupabaseClient.SUPABASE_URL + "rest/v1/" + endpoint)
                .enqueue(new Callback<JsonObject[]>() {
                    @Override
                    public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d(TAG, "Tutor sessions response: " + Arrays.toString(response.body()));
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("No sessions found or request failed (" + response.code() + ")");
                            Log.w(TAG, "No sessions found, response: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject[]> call, Throwable t) {
                        callback.onError("Session fetch error: " + t.getMessage());
                        Log.e(TAG, "Session fetch error: ", t);
                    }
                });
    }

    // ================= CREATE ATTENDANCE SESSION =================
    public static void createAttendanceSession(
            Context context,
            String courseId,
            String semesterId,
            String classroomId,
            String tutorProfileId,
            String sectionId,
            String sessionStart,
            String sessionEnd,
            TutorDataCallback callback) {

        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        JsonObject sessionBody = new JsonObject();
        sessionBody.addProperty("tutor_id", tutorProfileId);
        sessionBody.addProperty("course_id", courseId);
        sessionBody.addProperty("classroom_id", classroomId);
        sessionBody.addProperty("start_time", sessionStart);
        sessionBody.addProperty("end_time", sessionEnd);
        sessionBody.addProperty("semester_id", semesterId);
        sessionBody.addProperty("section_id", sectionId);


        Log.d("CAATS-TutorRepo", "Creating session with body: " + sessionBody);

        restService.insertRow(
                "attendance_sessions",
                sessionBody
        ).enqueue(new Callback<JsonObject[]>() {
            @Override
            public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                if (response.isSuccessful()) {
                    JsonObject createdSession = response.body()[0];
                    Log.d("CAATS-TutorRepo", "Session created successfully");
                    callback.onSuccess(createdSession);
                } else {
                    String errorMsg = "Failed to create session";
                    try {
                        if (response.errorBody() != null)
                            errorMsg = response.errorBody().string();
                    } catch (Exception ignored) {}
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject[]> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
                Log.d("CAATS-TutorRepo", "Network error: " + t.getMessage());
            }
        });
    }

    // ------------ Fetch session attendance ------------

    public static void fetchSessionAttendance(Context context, String sessionId, AttendanceDataCallback callback) {
        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        // Fetch records from attendance_records and the full_name from the related profiles table
        String endpoint = "attendance_records?session_id=eq." + sessionId + "&select=*,profiles(full_name),students(student_number)";

        Log.d(TAG, "Fetching session attendance: " + endpoint); //debug

        restService.getTableFiltered(SupabaseClient.SUPABASE_URL + "rest/v1/" + endpoint)
                .enqueue(new Callback<JsonObject[]>() {
                    @Override
                    public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                        Log.d("AttendanceListActivity", "Records " + Arrays.toString(response.body()));

                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onError("Failed to fetch attendance: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject[]> call, Throwable t) {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    // Fetch students attendance per selected subject, for Reports Activity

    public static void fetchTutorSubjects(Context context, String tutorId, SubjectsCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_tutor_id", tutorId);

        service.getTutorSubjects(body).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Failed to fetch subjects: " + response.code());
                    callback.onError("Failed to fetch subjects");
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e(TAG, "Network error fetching subjects", t);
                callback.onError("Network error");
            }
        });
    }

    public static void fetchSubjectReport(Context context, String courseId, String tutorId, ReportCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_course_id", courseId);
        body.addProperty("p_tutor_id", tutorId);

        service.getSubjectReport(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    Log.e(TAG, "Failed to fetch report: " + response.code());
                    callback.onError("Failed to fetch report");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error fetching report", t);
                callback.onError("Network error");
            }
        });
    }

}
