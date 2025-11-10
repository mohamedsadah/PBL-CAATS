package com.example.caats.repository;

import android.content.Context;
import android.util.Log;

import com.example.caats.network.AuthService;
import com.example.caats.network.SupabaseClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CoordinatorRepository {

    private static final String TAG = "CoordinatorRepo";

    public interface SectionsCallback {
        void onSuccess(JsonArray sections);
        void onError(String error);
    }

    public interface SectionReportCallback {
        void onSuccess(JsonObject report);
        void onError(String error);
    }

    public interface StudentReportCallback {
        void onSuccess(JsonArray report);
        void onError(String error);
    }

    public interface CoordinatorProfileCallback {
        void onSuccess(JsonObject coordinatorData);
        void onError(String error);
    }

    public interface DebarredStudentsCallback {
        void onSuccess(JsonArray students);
        void onError(String error);
    }

    //Fetch coordinator profile details from the 'profiles' table by auth_uid.

    public static void fetchCoordinatorProfile(Context context, String authUid, CoordinatorProfileCallback callback) {
        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        String endpoint = "profiles?auth_uid=eq." + authUid;
        Log.d(TAG, "Fetching coordinator profile: " + endpoint);

        restService.getTableFiltered(SupabaseClient.SUPABASE_URL + "rest/v1/" + endpoint)
                .enqueue(new Callback<JsonObject[]>() {
                    @Override
                    public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().length > 0) {
                            Log.d(TAG, "Coordinator profile response: " + Arrays.toString(response.body()));
                            // Success, return the first profile found
                            callback.onSuccess(response.body()[0]);
                        } else {
                            Log.e(TAG, "Profile fetch failed: " + response.message());
                            callback.onError("Coordinator profile not found or error: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject[]> call, Throwable t) {
                        Log.e(TAG, "Profile fetch error: ", t);
                        callback.onError("Profile fetch error: " + t.getMessage());
                    }
                });
    }


     // Fetches the list of sections assigned to a specific coordinator.
     // Calls RPC: get_coordinator_sections
    public static void fetchCoordinatorSections(Context context, String coordinatorId, SectionsCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_coordinator_id", coordinatorId);

        service.getCoordinatorSections(body).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e(TAG, "Network error fetching sections", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    // Fetches the full attendance overview for a specific section.
    // Calls RPC: get_section_overview_report

    public static void fetchSectionOverview(Context context, String sectionId, SectionReportCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_section_id", sectionId);

        service.getSectionOverviewReport(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Network error fetching section overview", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }


     // Fetches a detailed, subject-by-subject report for a single student.
     // Calls RPC: get_student_subject_report

    public static void fetchStudentSubjectReport(Context context, String studentId, StudentReportCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_student_id", studentId);

        service.getStudentSubjectReport(body).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e(TAG, "Network error fetching student report", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }



    // Helper to parse error messages from failed Retrofit responses.

    static String parseError(Response<?> response) {
        String errorMsg = "An unknown error occurred (Code: " + response.code() + ")";
        if (response.errorBody() != null) {
            try {
                errorMsg = response.errorBody().string();
            } catch (IOException e) {
                Log.e(TAG, "Error parsing error body", e);
            }
        }
        Log.e(TAG, "API call failed: " + errorMsg);
        return errorMsg;
    }


    public static void fetchDebarredStudentsForSection(Context context, String sectionId, DebarredStudentsCallback callback) {
        AuthService service = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject body = new JsonObject();
        body.addProperty("p_section_id", sectionId); // Pass section_id

        service.getDebarredStudentsForSection(body).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError(parseError(response));
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                Log.e(TAG, "Network error fetching debarred students", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

}