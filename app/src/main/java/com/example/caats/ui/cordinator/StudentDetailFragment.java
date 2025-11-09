package com.example.caats.ui.cordinator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;
import com.example.caats.repository.CoordinatorRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentDetailFragment extends Fragment {

    private static final String ARG_STUDENT_ID = "student_id";
    private static final String ARG_STUDENT_NAME = "student_name";
    private static final String ARG_STUDENT_NUMBER = "student_number";
    private static final String ARG_OVERALL_PCT = "overall_pct";

    private String studentId, studentName, studentNumber;
    private double overallPercentage;

    private MaterialToolbar toolbar;
    private TextView nameTextView, idTextView, overallTextView;
    private RecyclerView subjectRecyclerView;
    private SubjectDetailAdapter adapter;

    public static StudentDetailFragment newInstance(String studentId, String studentName, String studentNumber, double overallPercentage) {
        StudentDetailFragment fragment = new StudentDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_ID, studentId);
        args.putString(ARG_STUDENT_NAME, studentName);
        args.putString(ARG_STUDENT_NUMBER, studentNumber);
        args.putDouble(ARG_OVERALL_PCT, overallPercentage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            studentId = getArguments().getString(ARG_STUDENT_ID);
            studentName = getArguments().getString(ARG_STUDENT_NAME);
            studentNumber = getArguments().getString(ARG_STUDENT_NUMBER);
            overallPercentage = getArguments().getDouble(ARG_OVERALL_PCT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        toolbar = view.findViewById(R.id.toolbar);
        nameTextView = view.findViewById(R.id.detailStudentNameTextView);
        idTextView = view.findViewById(R.id.detailStudentIdTextView);
        overallTextView = view.findViewById(R.id.detailStudentOverallTextView);
        subjectRecyclerView = view.findViewById(R.id.subjectDetailRecyclerView);

        // Setup Toolbar
        toolbar.setTitle(studentName + "'s Report");
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof CoordinatorActivity) {
                ((CoordinatorActivity) getActivity()).goBack();
            }
        });

        // Populate header
        nameTextView.setText(studentName);
        idTextView.setText(studentNumber);
        overallTextView.setText(String.format(Locale.getDefault(), "Overall Attendance: %.0f%%", overallPercentage));

        // Setup RecyclerView
        adapter = new SubjectDetailAdapter();
        subjectRecyclerView.setAdapter(adapter);

        // Load data
        loadSubjectData();
    }

    private void loadSubjectData() {
        CoordinatorRepository.fetchStudentSubjectReport(requireContext(), studentId, new CoordinatorRepository.StudentReportCallback() {
            @Override
            public void onSuccess(JsonArray report) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    List<JsonObject> subjectList = new ArrayList<>();
                    for (JsonElement subjectElement : report) {
                        subjectList.add(subjectElement.getAsJsonObject());
                    }
                    adapter.setSubjects(subjectList);
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }
}