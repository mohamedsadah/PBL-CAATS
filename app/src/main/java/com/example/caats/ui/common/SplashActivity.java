package com.example.caats.ui.common;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.caats.R;
import com.example.caats.auth.LoginActivity;
import com.example.caats.ui.cordinator.CoordinatorActivity;
import com.example.caats.ui.student.StudentDashboardActivity;
import com.example.caats.ui.tutor.TutorDashboardActivity;
import com.example.caats.utils.MyFirebaseMessagingService;
import com.example.caats.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    // 1. Declare the permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("SplashActivity", "Notification permission granted.");
                } else {
                    Log.w("SplashActivity", "Notification permission denied.");
                }
                // Continue to app after permission is handled
                goToNextActivity();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        RelativeLayout rootLayout = findViewById(R.id.splashRootLayout);
        if (rootLayout.getBackground() instanceof android.graphics.drawable.AnimationDrawable) {
            android.graphics.drawable.AnimationDrawable anim = (android.graphics.drawable.AnimationDrawable) rootLayout.getBackground();
            anim.setEnterFadeDuration(2500);
            anim.setExitFadeDuration(4500);
            anim.start();
        }

        // 1. Create the Notification Channel
        createNotificationChannel();

        // 2. Ask for permission (if needed) or just proceed
        askNotificationPermissionOrProceed();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Session Notifications";
            String description = "Notifications for new attendance sessions";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(
                    MyFirebaseMessagingService.CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void askNotificationPermissionOrProceed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                goToNextActivity();
            } else {
                // Permission not granted, request it
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            goToNextActivity();
        }
    }

    private void goToNextActivity() {
        String destination = null;
        if (getIntent().getExtras() != null) {
            destination = getIntent().getExtras().getString("navigate_to");
        }
        final String finalDestination = destination;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String token = PreferenceManager.getToken(this);
            String userRole = PreferenceManager.getRole(this);

            Intent nextActivityIntent;

            if (token != null && !token.isEmpty() && userRole != null) {
                switch (userRole) {
                    case "student":
                        nextActivityIntent = new Intent(SplashActivity.this, StudentDashboardActivity.class);
                        if ("MARK_ATTENDANCE".equals(finalDestination)) {
                            nextActivityIntent.putExtra("navigate_to", "MARK_ATTENDANCE");
                        }
                        break;
                    case "tutor":
                        nextActivityIntent = new Intent(SplashActivity.this, TutorDashboardActivity.class);
                        break;
                    case "coordinator":
                        nextActivityIntent = new Intent(SplashActivity.this, CoordinatorActivity.class);
                        break;
                    default:
                        nextActivityIntent = new Intent(SplashActivity.this, LoginActivity.class);
                        break;
                }
            } else {
                nextActivityIntent = new Intent(SplashActivity.this, LoginActivity.class);
            }

            startActivity(nextActivityIntent);
            finish();
        }, 5000);
    }
}