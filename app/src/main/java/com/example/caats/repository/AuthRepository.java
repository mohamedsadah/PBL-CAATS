package com.example.caats.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.caats.network.AuthService;
import com.example.caats.network.SupabaseClient;
import com.example.caats.network.SupabaseAuthManager;
import com.example.caats.utils.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private static final String TAG = "CAATS-AuthRepo";

    // ----------------- CALLBACKS -----------------
    public interface LoginCallback {
        void onSuccess(String accessToken, String refreshToken, String userId, String uname, String urole);
        void onError(String error);
    }

    public interface SignupCallback {
        void onSuccess(String authUid, String profileId, String message);
        void onError(String error);
    }

    public interface FetchTableCallback {
        void onSuccess(String responseJson);
        void onError(String error);
    }

    // ----------------- LOGIN -----------------
    public static void login(String email, String password, LoginCallback callback) {
        AuthService authService = SupabaseClient.getAuthClient().create(AuthService.class);

        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        authService.login(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Login response: " + response.body());
                    try {
                        JsonObject json = response.body();
                        Log.d(TAG, "Login response: " + json);

                        String accessToken = json.get("access_token").getAsString();
                        String refreshToken = json.get("refresh_token").getAsString();
                        Log.d(TAG, "rtoken"+ refreshToken);
                        String userId = json.getAsJsonObject("user").get("id").getAsString();
                        String userName = json.getAsJsonObject("user").get("user_metadata").getAsJsonObject().get("full_name").getAsString();
                        String role = json.getAsJsonObject("user").get("user_metadata").getAsJsonObject().get("role").getAsString();


                        callback.onSuccess(accessToken, refreshToken, userId, userName, role);

                    } catch (Exception e) {
                        callback.onError("Login parsing error: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Unknown login error";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError("Login failed: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Login error: " + t.getMessage());
            }
        });
    }


    // ----------------- FETCH TABLE -----------------

    public static void fetchPublicTable(Context context, String tableName, FetchTableCallback callback) {
        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        restService.getTable(tableName).enqueue(new Callback<JsonObject[]>() {
            @Override
            public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(new Gson().toJson(response.body()));
                } else {
                    String errorMsg = "Failed to fetch " + tableName;
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Log.e("CAATS-AuthRepo", errorMsg);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject[]> call, Throwable t) {
                Log.e("CAATS-AuthRepo", "Fetch error: " + t.getMessage());
                callback.onError("Fetch error: " + t.getMessage());
            }
        });
    }

    public static void fetchTable(Context context, String tableName, FetchTableCallback callback) {

        AuthService restService = SupabaseClient.getRestClient(context).create(AuthService.class);

        // Get access token asynchronously - refreshes if expired -
        SupabaseAuthManager.getAccessToken(context, new SupabaseAuthManager.TokenCallback() {
            @Override
            public void onSuccess(String accessToken) {


                restService.getTable(tableName).enqueue(new Callback<JsonObject[]>() {
                    @Override
                    public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(new Gson().toJson(response.body()));
                        } else {
                            String errorMsg = "Unknown fetch error";
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = response.errorBody().string();
                                }
                            } catch (Exception ignored) {}

                            // Check if token expired — retry once
                            if (errorMsg.contains("JWT expired") || errorMsg.contains("PGRST303")) {
                                Log.w("CAATS-Auth", "Access token expired, retrying with refresh...");

                                // Trigger refresh again using refreshAccessToken async version
//                                SharedPreferences prefs = context.getSharedPreferences("SupabasePrefs", Context.MODE_PRIVATE);
                                String refreshToken = PreferenceManager.getToken(context);

                                if (refreshToken == null) {
                                    callback.onError("No refresh token found. Please log in again.");
                                    return;
                                }

                                SupabaseAuthManager.refreshAccessToken(context, refreshToken, new SupabaseAuthManager.TokenCallback() {
                                    @Override
                                    public void onSuccess(String newAccessToken) {
                                        restService.getTable(tableName).enqueue(new Callback<JsonObject[]>() {
                                            @Override
                                            public void onResponse(Call<JsonObject[]> call, Response<JsonObject[]> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                    callback.onSuccess(new Gson().toJson(response.body()));
                                                } else {
                                                    callback.onError("Retry failed: " + response.message());
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<JsonObject[]> call, Throwable t) {
                                                callback.onError("Retry fetch error: " + t.getMessage());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        callback.onError("Session expired. Please log in again.");
                                    }
                                });

                            } else {
                                callback.onError(errorMsg);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject[]> call, Throwable t) {
                        callback.onError("Fetch error: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError("Authentication expired. Please login again.");
            }
        });
    }


    // ----------------- SIGNUP -----------------
    public static void signup(Context context,
                              String fullName,
                              String email,
                              String password,
                              String phone,
                              String role,
                              Map<String, Object> roleFields,
                              SignupCallback callback) {

        AuthService authService = SupabaseClient.getAuthClient().create(AuthService.class);

        // Step 1: Supabase Auth signup
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        JsonObject data = new JsonObject();
        data.addProperty("full_name", fullName);
        data.addProperty("role", role);
        if (phone != null && !phone.isEmpty()) {
            data.addProperty("phone", phone);
        }
        body.add("data", data);

        authService.signup(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject json = response.body();
                        Log.d(TAG, "Signup response: " + json);

                        String accessToken = json.get("access_token").getAsString();
                        String authUid = json.getAsJsonObject("user").get("id").getAsString();

                        Log.d(TAG, "Signup OK, authUid=" + authUid);

                        // Step 2: Create profile via RPC
                        createProfile(context, accessToken, authUid, fullName, email, phone, role, roleFields, callback);

                    } catch (Exception e) {
                        callback.onError("Signup parsing error: " + e.getMessage());
                    }
                } else {
                    String errorMsg = "Unknown signup error";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    callback.onError("Signup failed: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Signup error: " + t.getMessage());
            }
        });
    }

    // ----------------- CREATE PROFILE -----------------
    private static void createProfile(Context context,
                                      String userAccessToken,
                                      String authUid,
                                      String fullName,
                                      String email,
                                      String phone,
                                      String role,
                                      Map<String, Object> roleFields,
                                      SignupCallback callback) {

        AuthService rpcService = SupabaseClient.getRestClient(context).create(AuthService.class);
        JsonObject rpcBody = new JsonObject();

        // Common fields
        rpcBody.addProperty("p_auth_uid", authUid);
        rpcBody.addProperty("p_full_name", fullName);
        rpcBody.addProperty("p_email", email);
        if (phone != null && !phone.isEmpty()) {
            rpcBody.addProperty("p_phone", phone);
        }

        // Role-specific fields
        String rpcName = "";
        if ("student".equals(role)) {
            rpcName = "rpc_create_student_profile";
            rpcBody.addProperty("p_student_number", (Number) roleFields.get("p_student_number"));
            rpcBody.addProperty("p_program_id", (String) roleFields.get("p_program_id"));
            rpcBody.addProperty("p_semester_id", (String) roleFields.get("p_semester_id"));
            rpcBody.addProperty("p_section_id", (String) roleFields.get("p_section_id"));
            rpcBody.addProperty("p_academic_year_id", (String) roleFields.get("p_academic_year_id"));
        } else if ("tutor".equals(role)) {
            rpcName = "rpc_create_tutor_profile";
            rpcBody.addProperty("p_tutor_number", (Number) roleFields.get("p_tutor_number"));
            rpcBody.addProperty("p_department_id", (String) roleFields.get("p_department_id"));
        } else if ("coordinator".equals(role)) {
            rpcName = "rpc_create_coordinator_profile";
            rpcBody.addProperty("p_coordinator_number", (Number) roleFields.get("p_coordinator_number"));
            rpcBody.addProperty("p_department_id", (String) roleFields.get("p_department_id"));
            rpcBody.addProperty("p_program_id", (String) roleFields.get("p_program_id"));
            rpcBody.addProperty("p_section_id", (String) roleFields.get("p_section_id"));
        }

        Log.d(TAG, "RPC Payload (" + rpcName + "): " + rpcBody);

        rpcService.rpcCreateProfile("rpc/" + rpcName, "Bearer " + userAccessToken, rpcBody)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                JsonObject json = response.body();
                                Log.d(TAG, "RPC Response: " + json);

                                boolean success = json.has("success") && json.get("success").getAsBoolean();
                                String profileId = json.has("profile_id") && !json.get("profile_id").isJsonNull()
                                        ? json.get("profile_id").getAsString()
                                        : null;
                                String message = json.has("message") ? json.get("message").getAsString() : "unknown";

                                if (success) {
                                    callback.onSuccess(authUid, profileId, message);
                                } else {
                                    callback.onError("Profile creation failed: " + message);
                                }

                            } catch (Exception e) {
                                callback.onError("RPC parsing error: " + e.getMessage());
                            }
                        } else {
                            String errorMsg = "Unknown RPC error";
                            try {
                                if (response.errorBody() != null) {
                                    errorMsg = response.errorBody().string();
                                }
                            } catch (Exception ignored) {}
                            callback.onError("RPC failed: " + errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        callback.onError("RPC error: " + t.getMessage());
                    }
                });
    }
}
