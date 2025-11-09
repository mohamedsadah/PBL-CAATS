package com.example.caats.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

public interface AuthService {

    // ---------------- AUTH ----------------
    @POST("signup")
    Call<JsonObject> signup(@Body JsonObject body);

    @POST("token?grant_type=password")
    Call<JsonObject> login(@Body JsonObject body);

    // ---------------- RPC (Generic) ----------------
    // Dynamic endpoint, can call rpc_create_student_profile, rpc_create_tutor_profile, etc.
    @POST("{function}")
    Call<JsonObject> rpcCreateProfile(
            @Path(value = "function", encoded = true) String function,
            @Header("Authorization") String userToken,
            @Body JsonObject body
    );

    @GET("{table}")
    Call<JsonObject[]> getTable(
            @Path(value = "table", encoded = true) String table
    );

    @GET
    Call<JsonObject[]> getTableFiltered(@Url String url);

    @Headers("Prefer: return=representation")
    @POST("{table}")
    Call<JsonObject[]> insertRow(
            @Path("table") String table,
            @Body JsonObject body
    );


    @POST("rpc/get_student_dashboard")
    Call<JsonObject> getStudentDashboard(@Body JsonObject body);


    @POST("rpc/get_active_sessions_for_student")
    Call<JsonArray> getActiveSessions(@Body JsonObject body);

    @POST("rpc/mark_attendance_geofenced")
    Call<JsonObject> markAttendanceGeofenced(@Body JsonObject body);

    @PUT("user")
    Call<JsonObject> updateUser(
            @Header("Authorization") String authHeader,
            @Body JsonObject body
    );

    @Headers("Prefer: return=representation")
    @PATCH("profiles")
    Call<List<JsonObject>> updateProfile(
            @Query("auth_uid") String authUidEquals,
            @Body JsonObject body);

    @POST("rpc/get_tutor_subjects")
    Call<JsonArray> getTutorSubjects(@Body JsonObject body);

    @POST("rpc/get_subject_attendance_report")
    Call<JsonObject> getSubjectReport(@Body JsonObject body);

    @POST("rpc/get_coordinator_sections")
    Call<JsonArray> getCoordinatorSections(@Body JsonObject body);

    @POST("rpc/get_section_overview_report")
    Call<JsonObject> getSectionOverviewReport(@Body JsonObject body);

    @POST("rpc/get_student_subject_report")
    Call<JsonArray> getStudentSubjectReport(@Body JsonObject body);

    @POST("rpc/get_debarred_students_for_section")
    Call<JsonArray> getDebarredStudentsForSection(@Body JsonObject body);

}
