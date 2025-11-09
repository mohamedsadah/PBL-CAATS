package com.example.caats.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.caats.R;
import com.example.caats.network.SupabaseClient;
import com.example.caats.repository.AuthRepository;
import com.example.caats.ui.cordinator.CoordinatorActivity;
import com.example.caats.ui.student.StudentDashboardActivity;
import com.example.caats.ui.tutor.TutorDashboardActivity;
import com.example.caats.utils.PreferenceManager;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "CAATS-Login";

    private TextInputEditText emailField, passwordField;
    private RadioGroup roleGroup;
    private Button loginButton;
    private TextView signupRedirect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Bind UI elements
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        roleGroup = findViewById(R.id.roleGroup);
        loginButton = findViewById(R.id.loginButton);
        signupRedirect = findViewById(R.id.signupRedirect);

        // Login button click
        loginButton.setOnClickListener(v -> attemptLogin());

        // Signup redirect
        signupRedirect.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(i);
        });
    }

    private void attemptLogin() {
        String email = emailField.getText() != null ? emailField.getText().toString().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedRoleId = roleGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Select a role", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRoleBtn = findViewById(selectedRoleId);
        String role = selectedRoleBtn.getText().toString().toLowerCase();

        // Perform login via Supabase Auth API
        AuthRepository.login(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(String accessToken, String refreshToken, String userId, String uname, String urole) {
                Log.d(TAG, "Access token: " + accessToken);
                Log.d(TAG, "Refresh token: " + refreshToken);
                Log.d(TAG, "User ID: " + userId);

                // Redirect user to dashboard
                switch (role) {
                    case "student":
                        if(role.equals(urole)) {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, StudentDashboardActivity.class));
                        }else{
                            Toast.makeText(LoginActivity.this, "You are not a student", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        break;
                    case "tutor":
                        if(role.equals(urole)) {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, TutorDashboardActivity.class));
                        }else{
                            Toast.makeText(LoginActivity.this, "You are not a tutor", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        break;
                    case "coordinator":
                        if(role.equals(urole)) {
                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, CoordinatorActivity.class));
                        }else{
                            Toast.makeText(LoginActivity.this, "You are not a coordinator", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        break;
                }

                // Save session details
                PreferenceManager.saveToken(LoginActivity.this, accessToken);
                PreferenceManager.saveRefreshToken(LoginActivity.this, refreshToken);
                PreferenceManager.saveEmail(LoginActivity.this, email);
                PreferenceManager.saveRole(LoginActivity.this, urole);
                PreferenceManager.saveUserId(LoginActivity.this, userId);
                PreferenceManager.saveFullName(LoginActivity.this, uname);
                Log.d(TAG, "Login successful");


                finish(); // Close login screen
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Login failed: " + error);
                if(error.contains("Invalid login credentials")){
                    Toast.makeText(LoginActivity.this, "Invalid login credentials", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(LoginActivity.this, "Login failed, Try again", Toast.LENGTH_LONG).show();
                }

                if (error.contains("JWT expired")) {
                    Toast.makeText(LoginActivity.this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                    PreferenceManager.clearAll(LoginActivity.this);
                }
            }
        });

    }
}
