package com.example.caats.ui.common;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.caats.R;

public class ProfilesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

            // Only add the fragment if the activity is newly created
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.profile_fragment_container, new ProfileCommonFragment())
                    .commit();
        }
    }
}