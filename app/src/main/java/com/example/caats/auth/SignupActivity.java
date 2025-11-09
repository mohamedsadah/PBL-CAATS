package com.example.caats.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.caats.R;
import com.example.caats.repository.AuthRepository;
import com.example.caats.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "CAATS-Signup";

    private com.google.android.material.textfield.TextInputEditText fullNameField, emailField, passwordField, phoneField;
    private RadioGroup roleGroup;

    private View studentFields, tutorFields, coordinatorFields;
    private com.google.android.material.textfield.TextInputEditText studentNumberField, enrollmentYearField, cohortField;
    private AutoCompleteTextView programDropdown, semesterDropdown, sectionDropdown, academicYearDropdown;

    private com.google.android.material.textfield.TextInputEditText tutorNumberField;
    private AutoCompleteTextView tutorDeptDropdown;

    private com.google.android.material.textfield.TextInputEditText coordinatorNumberField;
    private AutoCompleteTextView coordinatorDeptDropdown, coordinatorProgramDropdown, coordinatorSectionDropdown;

    private Button signupButton;

    private final Map<String, String> programMap = new HashMap<>();
    private final Map<String, String> deptMap = new HashMap<>();
    private final Map<String, String> semesterMap = new HashMap<>();
    private final Map<String, String> sectionMap = new HashMap<>();
    private final Map<String, String> academicYearMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        loadDropdownData();

        roleGroup.setOnCheckedChangeListener((group, checkedId) -> {
            studentFields.setVisibility(View.GONE);
            tutorFields.setVisibility(View.GONE);
            coordinatorFields.setVisibility(View.GONE);

            if (checkedId == R.id.rRoleStudent) studentFields.setVisibility(View.VISIBLE);
            else if (checkedId == R.id.rRoleTutor) tutorFields.setVisibility(View.VISIBLE);
            else if (checkedId == R.id.rRoleCoordinator) coordinatorFields.setVisibility(View.VISIBLE);
        });

        signupButton.setOnClickListener(v -> handleSignup());
    }

    private void initViews() {
        fullNameField = findViewById(R.id.fullNameField);
        emailField = findViewById(R.id.emailField);
        passwordField = findViewById(R.id.passwordField);
        phoneField = findViewById(R.id.phoneField);
        roleGroup = findViewById(R.id.roleGroup);

        studentFields = findViewById(R.id.studentFields);
        studentNumberField = findViewById(R.id.studentNumberField);
        programDropdown = findViewById(R.id.programDropdown);
        semesterDropdown = findViewById(R.id.semesterDropdown);
        sectionDropdown = findViewById(R.id.sectionDropdown);
        academicYearDropdown = findViewById(R.id.yearDropdown);


        tutorFields = findViewById(R.id.tutorFields);
        tutorNumberField = findViewById(R.id.tutorNumberField);
        tutorDeptDropdown = findViewById(R.id.tutorDeptDropdown);

        coordinatorFields = findViewById(R.id.coordinatorFields);
        coordinatorNumberField = findViewById(R.id.coordinatorNumberField);
        coordinatorDeptDropdown = findViewById(R.id.coordinatorDeptDropdown);
        coordinatorProgramDropdown = findViewById(R.id.coordinatorProgramDropdown);
        coordinatorSectionDropdown = findViewById(R.id.coordinatorSectionDropdown);

        signupButton = findViewById(R.id.signupButton);

        Button loginbtn = findViewById(R.id.loginbtn);
        loginbtn.setOnClickListener(v -> {
            finish();
        });
    }

    private void loadDropdownData() {
        fetchTable("programs", programDropdown, programMap);
        fetchTable("departments", tutorDeptDropdown, deptMap);
        fetchTable("departments", coordinatorDeptDropdown, deptMap);
        fetchTable("programs", coordinatorProgramDropdown, programMap);
        fetchTable("semesters", semesterDropdown, semesterMap);
        fetchTable("sections", sectionDropdown, sectionMap);
        fetchTable("sections", coordinatorSectionDropdown, sectionMap);
        fetchTable("academic_years", academicYearDropdown, academicYearMap);
    }

    private void fetchTable(String table, AutoCompleteTextView target, Map<String, String> map) {
        AuthRepository.fetchPublicTable(this, table, new AuthRepository.FetchTableCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray arr = new JSONArray(response);
                    ArrayList<String> items = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String id = obj.getString("id");
                        String name = obj.has("name") ? obj.getString("name") : obj.getString("code");
                        items.add(name);
                        map.put(name, id);
                    }
                    runOnUiThread(() -> target.setAdapter(
                            new ArrayAdapter<>(SignupActivity.this, android.R.layout.simple_dropdown_item_1line, items)
                    ));
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing " + table, e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Fetch " + table + " failed: " + error);
            }
        });
    }


    private void handleSignup() {
        String fullName = getText(fullNameField);
        String email = getText(emailField);
        String password = getText(passwordField);
        String phone = getText(phoneField);

        int selected = roleGroup.getCheckedRadioButtonId();
        if (selected == -1) {
            showToast("Please select a role");
            return;
        }
        String role = selected == R.id.rRoleStudent ? "student" :
                selected == R.id.rRoleTutor ? "tutor" : "coordinator";

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showToast("Please fill in all required fields");
            return;
        }

        Map<String, Object> roleFields = new HashMap<>();

        switch (role) {
            case "student":
                if (!getText(studentNumberField).isEmpty())
                    roleFields.put("p_student_number", Long.parseLong(getText(studentNumberField)));

                putIfExists(roleFields, "p_program_id", programMap, programDropdown.getText().toString());
                putIfExists(roleFields, "p_semester_id", semesterMap, semesterDropdown.getText().toString());
                putIfExists(roleFields, "p_section_id", sectionMap, sectionDropdown.getText().toString());
                putIfExists(roleFields, "p_academic_year_id", academicYearMap, academicYearDropdown.getText().toString());


            case "tutor":
                if (!getText(tutorNumberField).isEmpty())
                    roleFields.put("p_tutor_number", Long.parseLong(getText(tutorNumberField)));

                putIfExists(roleFields, "p_department_id", deptMap, tutorDeptDropdown.getText().toString());
                break;

            case "coordinator":
                if (!getText(coordinatorNumberField).isEmpty())
                    roleFields.put("p_coordinator_number", Long.parseLong(getText(coordinatorNumberField)));

                putIfExists(roleFields, "p_department_id", deptMap, coordinatorDeptDropdown.getText().toString());
                putIfExists(roleFields, "p_program_id", programMap, coordinatorProgramDropdown.getText().toString());
                putIfExists(roleFields, "p_section_id", sectionMap, coordinatorSectionDropdown.getText().toString());
                break;
        }

        AuthRepository.signup(this, fullName, email, password, phone, role, roleFields, new AuthRepository.SignupCallback() {
            @Override
            public void onSuccess(String accessToken, String profileId, String message) {
                showToast(message);
                finish();
            }

            @Override
            public void onError(String error) {
                showToast(error);
            }
        });
    }

    private String getText(com.google.android.material.textfield.TextInputEditText field) {
        return field.getText() != null ? field.getText().toString().trim() : "";
    }

    private void putIfExists(Map<String, Object> map, String key, Map<String, String> source, String displayValue) {
        if (source.containsKey(displayValue)) {
            map.put(key, source.get(displayValue));
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
