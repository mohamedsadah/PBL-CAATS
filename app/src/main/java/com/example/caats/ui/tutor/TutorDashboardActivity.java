package com.example.caats.ui.tutor;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.caats.R;
import com.example.caats.repository.TutorRepository;
import com.example.caats.ui.common.ProfilesActivity;
import com.example.caats.ui.student.StudentDashboardActivity;
import com.example.caats.utils.PreferenceManager;
import com.example.caats.ui.tutor.CreateSessionFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;

import java.util.Arrays;

public class TutorDashboardActivity extends AppCompatActivity {

    private TextView welcomeMessage, sessionInfo;
    private FloatingActionButton fabStartSession;

    private RecyclerView sessionsRecyclerView;
    private SessionAdapter sessionAdapter;

    private MaterialButton viewReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_dashboard);

        // Background animation (if animated drawable is used)
        ImageView bgImage = findViewById(R.id.bg_image);
        if (bgImage != null) {
            bgImage.post(() -> {
                if (bgImage.getBackground() instanceof AnimationDrawable) {
                    AnimationDrawable animationDrawable = (AnimationDrawable) bgImage.getBackground();
                    animationDrawable.setEnterFadeDuration(2000);
                    animationDrawable.setExitFadeDuration(2000);
                    animationDrawable.start();
                }
            });
        }

        // UI References
        welcomeMessage = findViewById(R.id.welcome_message);
        sessionInfo = findViewById(R.id.sessionInfo);
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
        fabStartSession = findViewById(R.id.fab_start_session);
        viewReports = findViewById(R.id.btn_view_reports);

        // Load entry animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        welcomeMessage.startAnimation(fadeIn);
        sessionInfo.startAnimation(fadeIn);
        sessionsRecyclerView.startAnimation(fadeIn);
        fabStartSession.startAnimation(slideUp);

        // Load cached data
        String authUid = PreferenceManager.getUserId(this);

        sessionAdapter = new SessionAdapter();
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(sessionAdapter);


        if (authUid != null) {
            Log.d("TutorDashboardActivity", "authUid: " + authUid);

            // Fetch tutor profile
            TutorRepository.fetchTutorProfile(this, authUid, new TutorRepository.TutorDataCallback() {

                @Override
                public void onSuccess(JsonObject tutorData) {
                    Log.d("TutorDashboardActivity", "tutorData: " + tutorData);

                    runOnUiThread(() -> {
                        String name = tutorData.has("full_name")
                                ? tutorData.get("full_name").getAsString()
                                : "Tutor";
                        welcomeMessage.setText("Welcome, " + name);

                        String image_url = tutorData.get("image_url").toString();
                        PreferenceManager.saveImageUrl(TutorDashboardActivity.this, image_url);

                        // Cache for future use
                        PreferenceManager.saveFullName(TutorDashboardActivity.this, name);

                        if (tutorData.has("email")) {
                            PreferenceManager.saveEmail(
                                    TutorDashboardActivity.this,
                                    tutorData.get("email").getAsString()
                            );
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            sessionInfo.setText("Failed to load tutor information.")
                    );
                }
            });

            // Fetch recent sessions
            TutorRepository.fetchRecentSessions(this, authUid, new TutorRepository.SessionDataCallback() {
                @Override
                public void onSuccess(JsonObject[] sessions) {
                    runOnUiThread(() -> {
                        sessionAdapter.setSessions(Arrays.asList(sessions));
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.d("TutorDashboardActivity", "Error fetching sessions: " + error);
                        sessionInfo.setText("Unable to Fetch sessions, please Retry");
                    });
                }
            });
        }

        // create New Session button
        fabStartSession.setOnClickListener(v -> {
            CreateSessionFragment sessionFragment = new CreateSessionFragment();
            sessionFragment.show(getSupportFragmentManager(), "CreateSessionFragment");
        });

        viewReports.setOnClickListener(v -> {
            startActivity(new Intent(this, TutorReportsActivity.class));
        });


        // Bottom Navigation setup
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_sessions) {
                    return true;
                }else if (id == R.id.nav_reports) {
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
}
