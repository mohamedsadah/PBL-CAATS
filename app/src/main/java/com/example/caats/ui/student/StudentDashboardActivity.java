package com.example.caats.ui.student;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.caats.R;
import com.example.caats.repository.StudentRepository;
import com.example.caats.ui.common.ProfilesActivity;
import com.example.caats.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView welcomeTextView, overallPercentageTextView, overallFractionTextView, overallStatusTextView;
    private TextView upcomingClass1TextView, upcomingClass2TextView, recentActivity1TextView, recentActivity2TextView;
    private CircularProgressIndicator overallProgressBar;
    // FIX 1: Correct class name casing
    private subjectAttendanceAdapter subjectAdapter;

    public static String image_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        initializeViews();
        startBackgroundAnimation();
        setupRecyclerView();

        handleNotificationIntent(getIntent());

        String studentId = PreferenceManager.getUserId(this);
        Log.d("StudentDashboardActivity", "Student ID: " + studentId);



        if (studentId != null && !studentId.isEmpty()) {
            fetchStudentDetails(studentId);
            loadDashboardData(studentId);
        } else {
            Toast.makeText(this, "Error: Could not identify student.", Toast.LENGTH_LONG).show();
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_Dashboard) {
                    return true;
                } else if (id == R.id.nav_attendance) {
                    startActivity(new Intent(this, MarkAttendanceActivity.class));
                    return true;
                }  else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfilesActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    private void fetchStudentDetails(String studentId) {
        if (studentId != null) {

            StudentRepository.fetchStudentProfile(this, studentId, new StudentRepository.studentDataCallback()
            {

                @Override
                public void onSuccess (JsonObject dashboardData){

                    Log.d("studentDashboardActivity", "authUid: " + studentId);

                Log.d("StudentDashboardActivity", "StudentData: " + dashboardData);

                image_url = dashboardData.get("image_url").toString();
                Log.d("StudentDashboardActivity", "image_url: " + image_url);
                PreferenceManager.saveImageUrl(StudentDashboardActivity.this, image_url);

                runOnUiThread(() -> {
                    String name = dashboardData.has("full_name")
                            ? dashboardData.get("full_name").getAsString()
                            : "Student";
                    welcomeTextView.setText("Welcome, " + name);

                    PreferenceManager.saveFullName(StudentDashboardActivity.this, name);
                    if (dashboardData.has("email")) {
                        PreferenceManager.saveEmail(
                                StudentDashboardActivity.this,
                                dashboardData.get("email").getAsString()
                        );
                    }
                });
            }

                @Override
                public void onError (String error){
                runOnUiThread(() ->
                        welcomeTextView.setText("Failed to load Student information.")
                );
            }
            });
        }
    }


    private void initializeViews() {
        welcomeTextView = findViewById(R.id.welcomeTextView);
        overallPercentageTextView = findViewById(R.id.overallPercentageTextView);
        overallFractionTextView = findViewById(R.id.overallFractionTextView);
        overallStatusTextView = findViewById(R.id.overallStatusTextView);
        overallProgressBar = findViewById(R.id.overallProgressBar);
        upcomingClass1TextView = findViewById(R.id.upcomingClass1TextView);
        upcomingClass2TextView = findViewById(R.id.upcomingClass2TextView);
        recentActivity1TextView = findViewById(R.id.recentActivity1TextView);
        recentActivity2TextView = findViewById(R.id.recentActivity2TextView);
    }

    private void startBackgroundAnimation() {
        LinearLayout layout = findViewById(R.id.dashboardRootLayout);
        if (layout.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) layout.getBackground();
                    animationDrawable.setEnterFadeDuration(2000);
                    animationDrawable.setExitFadeDuration(4000);
                    animationDrawable.start();
                }
        }

    private void setupRecyclerView() {
        RecyclerView subjectRecyclerView = findViewById(R.id.subjectAttendanceRecyclerView);

        subjectAdapter = new subjectAttendanceAdapter();
        subjectRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        subjectRecyclerView.setAdapter(subjectAdapter);
    }

    private void loadDashboardData(String studentId) {
        StudentRepository.fetchDashboardData(this, studentId, new StudentRepository.DashboardCallback() {
            @Override
            public void onSuccess(JsonObject data) {
                Log.d("StudentDashboardActivity", "Attendance History data" + data);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runOnUiThread(() -> {
                        if (data.has("overall_attendance") && !data.get("overall_attendance").isJsonNull()) {
                            updateOverallCard(data.getAsJsonObject("overall_attendance"));
                        }


                        if (data.has("upcoming_classes") && !data.get("upcoming_classes").isJsonNull()) {
                            JsonArray upcomingClasses = data.getAsJsonArray("upcoming_classes");
                            JsonArray activeClasses = new JsonArray();

                            LocalDateTime now = LocalDateTime.now();

                            DateTimeFormatter parser = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                            for (JsonElement classElement : upcomingClasses) {
                                JsonObject classObj = classElement.getAsJsonObject();

                                if (classObj.has("end_time") && !classObj.get("end_time").isJsonNull()) {
                                    String endTimeString = classObj.get("end_time").getAsString();

                                    try {
                                        LocalDateTime endTime = LocalDateTime.parse(endTimeString, parser);

                                        if (endTime.isAfter(now)) {
                                            activeClasses.add(classObj);
                                        }
                                    } catch (DateTimeParseException e) {
                                        Log.e("StudentDashboard", "Failed to parse end_time: " + endTimeString, e);
                                    }
                                }
                            }
                            Log.d("StudentDashboard", "Active classes: " + activeClasses); //debug
                            updateUpcomingClasses(activeClasses);
                        }else{
                            upcomingClass1TextView.setText("No more classes today.");
                            upcomingClass2TextView.setVisibility(View.GONE);
                        }

                        if (data.has("recent_activity") && !data.get("recent_activity").isJsonNull()) {
                            updateRecentActivity(data.getAsJsonArray("recent_activity"));
                        }

                        if (data.has("subject_breakdown") && !data.get("subject_breakdown").isJsonNull()) {
                            updateSubjectList(data.getAsJsonArray("subject_breakdown"));
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Log.d("StudentDashboardActivity",
                                "Error fetching dashboard data: " + error)
                );
                        Toast.makeText(StudentDashboardActivity.this,
                                "Error fetching dashboard data.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateOverallCard(JsonObject overall) {
        if (overall == null || overall.isJsonNull()) return;

        int percentage = 0;
        if (overall.has("percentage") && !overall.get("percentage").isJsonNull()) {
            percentage = overall.get("percentage").getAsInt();
        }

        int present = overall.get("present_sessions").getAsInt();
        int total = overall.get("total_sessions").getAsInt();

        overallPercentageTextView.setText(String.format("%d%%", percentage));
        overallFractionTextView.setText(String.format("Attended: %d / %d", present, total));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            overallProgressBar.setProgress(percentage, true);
        }

        if (percentage >= 75) {
            overallStatusTextView.setText("On Track");
        } else if (percentage >= 60) {
            overallStatusTextView.setText("Nearing Limit");
        } else {
            overallStatusTextView.setText("Attendance Critical");
        }
    }
    private void updateUpcomingClasses(JsonArray upcoming) {
        if (upcoming == null || upcoming.isJsonNull() || upcoming.size() == 0) {
            upcomingClass1TextView.setText("No more classes today.");
            upcomingClass2TextView.setVisibility(View.GONE);
            return;
        }

        JsonObject class1 = upcoming.get(0).getAsJsonObject();
        String name1 = class1.get("course_name").getAsString();
        String time1 = formatTime(class1.get("start_time").getAsString());
        upcomingClass1TextView.setText(String.format("%s at %s", name1, time1));

        if (upcoming.size() > 1) {
            JsonObject class2 = upcoming.get(1).getAsJsonObject();
            String name2 = class2.get("course_name").getAsString();
            String time2 = formatTime(class2.get("start_time").getAsString());
            upcomingClass2TextView.setText(String.format("%s at %s", name2, time2));
            upcomingClass2TextView.setVisibility(View.VISIBLE);
        } else {
            upcomingClass2TextView.setVisibility(View.GONE);
        }
    }

    private void updateRecentActivity(JsonArray recent) {
        if (recent == null || recent.isJsonNull() || recent.size() == 0) {
            recentActivity1TextView.setText("No recent activity.");
            recentActivity2TextView.setVisibility(View.GONE);
            return;
        }

        JsonObject activity1 = recent.get(0).getAsJsonObject();
        String course1 = activity1.get("course_name").getAsString();
        String status1 = activity1.get("status").getAsString();
        recentActivity1TextView.setText(String.format("%s - Marked %s", course1, status1));

        if (recent.size() > 1) {
            JsonObject activity2 = recent.get(1).getAsJsonObject();
            String course2 = activity2.get("course_name").getAsString();
            String status2 = activity2.get("status").getAsString();
            recentActivity2TextView.setText(String.format("%s - Marked %s", course2, status2));
            recentActivity2TextView.setVisibility(View.VISIBLE);
        } else {
            recentActivity2TextView.setVisibility(View.GONE);
        }
    }

    private void updateSubjectList(JsonArray subjects) {
        if (subjects == null || subjects.isJsonNull()) return;

        List<JsonObject> subjectList = new ArrayList<>();
        for (JsonElement subjectElement : subjects) {
            subjectList.add(subjectElement.getAsJsonObject());
        }
        subjectAdapter.submitList(subjectList);
    }

    private String formatTime(String dateTimeString) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
            } catch (DateTimeParseException e) {
                Log.e("StudentDashboard", "Failed to parse time: " + dateTimeString, e);
                return dateTimeString;
            }
        } else {
            try {
                return dateTimeString.substring(11, 16);
            } catch (Exception e) {
                return dateTimeString;
            }
        }
    }

    private void handleNotificationIntent(Intent intent) {
        Log.d("SplashActivity", "handleNotificationIntent:" + intent);
        if (intent != null && intent.getExtras() != null) {
            String navigateTo = intent.getExtras().getString("navigate_to");

            if ("MARK_ATTENDANCE".equals(navigateTo)) {
                Intent attendanceIntent = new Intent(this, MarkAttendanceActivity.class);
                startActivity(attendanceIntent);

                // Clear the extra so it doesn't trigger again
                intent.removeExtra("navigate_to");
            }
        }
    }

    //  handle notifications if the Dashboard is already open
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }
}