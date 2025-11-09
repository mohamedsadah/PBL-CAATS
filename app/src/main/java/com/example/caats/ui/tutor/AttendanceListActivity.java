package com.example.caats.ui.tutor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.caats.R;
import com.example.caats.repository.TutorRepository;
import com.example.caats.ui.common.ProfilesActivity;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttendanceListActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "EXTRA_SESSION_ID";
    private AttendanceAdapter adapter;
    private TextView presentCountTextView, absentCountTextView, totalCountTextView, subjectName;
    String sessionName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_list);

        // Initialize Views
        presentCountTextView = findViewById(R.id.presentCountTextView);
        absentCountTextView = findViewById(R.id.absentCountTextView);
        totalCountTextView = findViewById(R.id.totalCountTextView);
        subjectName = findViewById(R.id.SubjectName);

        RecyclerView recyclerView = findViewById(R.id.attendanceRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter(this);
        recyclerView.setAdapter(adapter);

        // Get session ID from intent
        String sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        sessionName = getIntent().getStringExtra("Session_Name");
        if (sessionId != null) {
            fetchAttendanceData(sessionId);
        }


        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_sessions) {
                    startActivity(new Intent(this, TutorDashboardActivity.class));
                    return true;
                } else if (id == R.id.nav_reports) {
                    startActivity(new Intent(this, TutorReportsActivity.class));
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfilesActivity.class));
                    return true;
                }
                return false;
            });
        }

    }

    private void fetchAttendanceData(String sessionId) {
        TutorRepository.fetchSessionAttendance(this, sessionId, new TutorRepository.AttendanceDataCallback() {
            @Override
            public void onSuccess(JsonObject[] records) {
                // Convert JsonObject[] to List<AttendanceRecord>

                List<AttendanceRecord> recordList = new ArrayList<>();
                for (JsonObject recordJson : records) {
                    recordList.add(new AttendanceRecord(recordJson));
                }

                runOnUiThread(() -> {
                    adapter.submitList(recordList);
                    if(sessionName != null) {
                        subjectName.setText(sessionName);
                    }else{
                        subjectName.setText("Unnamed Session");
                    }

                    updateSummary(records);
                    Toast.makeText(AttendanceListActivity.this,
                            "Attendance Records fetched successfully",
                            Toast.LENGTH_LONG).show();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.d("SessionAttendanceActivity",
                            "Error fetching attendance: " + error);

                    Toast.makeText(AttendanceListActivity.this,
                            "Error fetching attendance ",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateSummary(JsonObject[] records) {
        int presentCount = 0;
        for (JsonObject record : records) {
            if ("Present".equalsIgnoreCase(record.get("status").getAsString())) {
                presentCount++;
            }
        }
        int absentCount = records.length - presentCount;

        presentCountTextView.setText(String.format("%d\nPresent", presentCount));
        absentCountTextView.setText(String.format("%d\nAbsent", absentCount));
        totalCountTextView.setText(String.format("%d\nTotal", records.length));
    }
}