package com.example.caats.ui.student;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.caats.R;
import com.example.caats.repository.StudentRepository;
import com.example.caats.ui.common.ProfilesActivity;
import com.example.caats.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MarkAttendanceActivity extends AppCompatActivity implements ActiveSessionAdapter.OnMarkPresentClickListener {

    private static final int LOCATION_REQUEST_CODE = 1001;

    private ActiveSessionAdapter adapter;
    private RecyclerView recyclerView;
    private TextView noSessionsTextView;
    private List<JsonObject> activeSessions = new ArrayList<>();

    private JsonObject pendingSession;
    private int pendingPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_attendance);

        noSessionsTextView = findViewById(R.id.noSessionsTextView);
        recyclerView = findViewById(R.id.activeSessionsRecyclerView);
        adapter = new ActiveSessionAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadActiveSessions();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_Dashboard) {
                    startActivity(new Intent(this, StudentDashboardActivity.class));
                    return true;
                }  else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfilesActivity.class));
                    return true;
                }
                return false;
            });
        }

        new android.os.Handler().postDelayed(this::loadActiveSessions, 5 * 60 * 1000);
    }

    private void loadActiveSessions() {
        String studentId = PreferenceManager.getUserId(this);

        StudentRepository.fetchActiveSessions(this, new StudentRepository.ActiveSessionsCallback() {
            @Override
            public void onSuccess(JsonArray sessions) {
                runOnUiThread(() -> {
                    if (sessions == null || sessions.size() == 0) {
                        noSessionsTextView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        noSessionsTextView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        activeSessions.clear();
                        for (JsonElement session : sessions) {
                            activeSessions.add(session.getAsJsonObject());
                        }
                        adapter.submitList(new ArrayList<>(activeSessions));

                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.d("MarkAttendanceActivity", "Error fetching active sessions: " + error);
                    Toast.makeText(MarkAttendanceActivity.this,
                            "Error fetching active sessions.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onMarkPresentClick(JsonObject session, int position) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, store the action and request permission
            this.pendingSession = session;
            this.pendingPosition = position;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            // Permission is already granted, proceed directly
            this.pendingSession = null;
            this.pendingPosition = -1;
            getLocationAndMarkAttendance(session, position);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was just granted, check for a pending action and run it
                if (this.pendingSession != null) {
                    getLocationAndMarkAttendance(this.pendingSession, this.pendingPosition);
                }
            } else {
                Toast.makeText(this, "Location permission is required to mark attendance.", Toast.LENGTH_SHORT).show();
            }
            // Clear the pending action regardless of the outcome
            this.pendingSession = null;
            this.pendingPosition = -1;
        }
    }

    @SuppressWarnings("MissingPermission") // We only call this after checking permission
    private void getLocationAndMarkAttendance(JsonObject session, int position) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        String sessionId = session.get("session_id").getAsString();
                        String studentId = PreferenceManager.getUserId(this);

                        StudentRepository.markAttendance(
                                this, sessionId, studentId,
                                location.getLatitude(), location.getLongitude(),
                                new StudentRepository.MarkAttendanceCallback() {
                                    @Override
                                    public void onSuccess(String message) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(MarkAttendanceActivity.this, message, Toast.LENGTH_SHORT).show();
                                            // Remove the card from the list so it can't be clicked again
                                            activeSessions.remove(position);
                                            adapter.submitList(new ArrayList<>(activeSessions));
                                            adapter.notifyItemRemoved(position);
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(MarkAttendanceActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                                        });
                                    }
                                }
                        );
                    } else {
                        Toast.makeText(this, "Could not get your location. Please turn on GPS and try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    Log.e("MarkAttendanceActivity", "Location failure", e);
                    Toast.makeText(this, "Error getting location. Please turn on GPS.", Toast.LENGTH_SHORT).show();
                });
    }
}