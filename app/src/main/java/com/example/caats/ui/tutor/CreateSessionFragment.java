package com.example.caats.ui.tutor;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.caats.R;
import com.example.caats.repository.AuthRepository;
import com.example.caats.repository.TutorRepository;
import com.example.caats.utils.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreateSessionFragment extends BottomSheetDialogFragment {

    private static final String TAG = "CAATS-CreateSession";

    private MaterialAutoCompleteTextView subjectField, BuildingField, classroomDropdown, programDropdown, semesterDropdown, sectionDropdown;
    private TextInputEditText startTimeField, endTimeField;
    private MaterialButton btnCreateSession;
    private TextInputLayout startTimeLayout, endTimeLayout;

    // Maps: Display name → UUID from Supabase
    private final Map<String, String> subjectMap = new HashMap<>();
    private final Map<String, String> BuildingMap = new HashMap<>();

    private final Map<String, String> classroomMap = new HashMap<>();
    private final Map<String, String> programMap = new HashMap<>();
    private final Map<String, String> semesterMap = new HashMap<>();
    private final Map<String, String> sectionMap = new HashMap<>();

    public CreateSessionFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_session_fragment, container, false);

        initViews(view);
        setupDropdowns();
        setupTimePickers();

        btnCreateSession.setOnClickListener(v -> handleCreateSession());

        return view;
    }

    private void initViews(View view) {
        subjectField = view.findViewById(R.id.subjectField);
        BuildingField = view.findViewById(R.id.BuildingField);
        classroomDropdown = view.findViewById(R.id.classroomDropdown);
        programDropdown = view.findViewById(R.id.programDropdown);
        semesterDropdown = view.findViewById(R.id.semesterDropdown);
        sectionDropdown = view.findViewById(R.id.sectionDropdown);
        startTimeField = view.findViewById(R.id.startTimeField);
        endTimeField = view.findViewById(R.id.endTimeField);
        btnCreateSession = view.findViewById(R.id.btnCreateSession);
        startTimeLayout = view.findViewById(R.id.startTimeInputLayout);
        endTimeLayout = view.findViewById(R.id.endTimeInputLayout);
    }

    // ----------------- DROPDOWNS -----------------
    private void setupDropdowns() {
        fetchTable("courses", subjectField, subjectMap);
        fetchTable("buildings", BuildingField, BuildingMap);
        fetchTable("classrooms", classroomDropdown, classroomMap);
        fetchTable("programs", programDropdown, programMap);
        fetchTable("semesters", semesterDropdown, semesterMap);
        fetchTable("sections", sectionDropdown, sectionMap);
    }

    private void fetchTable(String table, MaterialAutoCompleteTextView target, Map<String, String> map) {
        AuthRepository.fetchPublicTable(requireContext(), table, new AuthRepository.FetchTableCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray arr = new JSONArray(response);
                    Log.d(TAG, "Fetched " + table + ": " + arr);

                    ArrayList<String> items = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        Log.d(TAG, "Item " + i + ": " + obj.toString());

                        String id = obj.getString("id");
                        String name = obj.has("name")
                                ? obj.getString("name")
                                : obj.has("title")
                                ? obj.getString("title")
                                : obj.has("code")
                                ? obj.getString("code")
                                : "Item " + (i + 1);

                        items.add(name);
                        map.put(name, id);
                    }

                    requireActivity().runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                requireContext(),
                                R.layout.list_item_dropdown,
                                items
                        );
                        target.setAdapter(adapter);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing table " + table, e);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to fetch " + table + ": " + error);
            }
        });
    }

    // ----------------- TIME PICKERS -----------------
    private void setupTimePickers() {
        startTimeField.setOnClickListener(v -> showTimePicker(startTimeField));
        endTimeField.setOnClickListener(v -> showTimePicker(endTimeField));
    }

    private void showTimePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
            target.setText(time);
        }, hour, minute, true).show();
    }

    // ----------------- CREATE SESSION -----------------
    private void handleCreateSession() {
        String subject = subjectField.getText().toString().trim();
        String classroom = classroomDropdown.getText().toString().trim();
        String program = programDropdown.getText().toString().trim();
        String semester = semesterDropdown.getText().toString().trim();
        String section = sectionDropdown.getText().toString().trim();
        String startTime = startTimeField.getText().toString().trim();
        String endTime = endTimeField.getText().toString().trim();

        if (subject.isEmpty() || classroom.isEmpty() || startTime.isEmpty() || endTime.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        String sessionStart = today + "T" + startTime + ":00";
        String sessionEnd = today + "T" + endTime + ":00";

        String accessToken = PreferenceManager.getToken(requireContext());
        String tutorProfileId = PreferenceManager.getUserId(requireContext());


        Map<String, Object> metadata = new HashMap<>();
        metadata.put("subject", subject);
        metadata.put("created_via", "mobile_app");

        btnCreateSession.setEnabled(false);
        btnCreateSession.setText("Creating...");

        TutorRepository.createAttendanceSession(
                requireContext(),
                subjectMap.get(subject),
                semesterMap.get(semester),
                classroomMap.get(classroom),
                tutorProfileId,
                sectionMap.get(section),
                sessionStart,
                sessionEnd,
                new TutorRepository.TutorDataCallback() {
                    @Override
                    public void onSuccess(com.google.gson.JsonObject tutorData) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Session Created Successfully!", Toast.LENGTH_SHORT).show();
                            dismiss();
                            //refresh main activity
                            requireActivity().recreate();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error creating session: " + error);
                            btnCreateSession.setEnabled(true);
                            btnCreateSession.setText("Create Session");
                        });
                    }
                }
        );
    }

    @Override
    public int getTheme() {
        return R.style.App_Theme_BottomSheetDialog;
    }
}
