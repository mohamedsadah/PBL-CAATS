package com.example.caats.ui.cordinator;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.example.caats.R;
import com.example.caats.ui.common.ProfilesActivity;
import com.example.caats.ui.tutor.TutorReportsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CoordinatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coordinator);

        if (savedInstanceState == null) {
            // Load the main dashboard fragment
            navigateToFragment(new CoordinatorDashboardFragment(), false);
        }

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfilesActivity.class));
                    return true;
                }
                return false;
            });
        }

    }


    public void navigateToFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
        );

        transaction.replace(R.id.coordinator_fragment_container, fragment);

        if (addToBackStack) {
            transaction.addToBackStack(null); // Allows user to press "back"
        }
        transaction.commit();
    }

    //Call this from fragments to go back

    public void goBack() {
        getSupportFragmentManager().popBackStack();
    }
}