package com.example.caats.ui.cordinator;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;
import com.example.caats.repository.CoordinatorRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SectionOverviewFragment extends Fragment implements StudentOverviewAdapter.OnStudentClickListener {

    private static final String ARG_SECTION_ID = "section_id";
    private static final String ARG_SECTION_TITLE = "section_title";

    private String sectionId;
    private String sectionTitle;

    private MaterialToolbar toolbar;
    private TextView sectionOverallPercentage;
    private RecyclerView studentOverviewRecyclerView;
    private StudentOverviewAdapter adapter;
    private Button debarredStudentsButton;
    private TextInputEditText studentSearchEditText;

    public static SectionOverviewFragment newInstance(String sectionId, String sectionTitle) {
        SectionOverviewFragment fragment = new SectionOverviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SECTION_ID, sectionId);
        args.putString(ARG_SECTION_TITLE, sectionTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            sectionId = getArguments().getString(ARG_SECTION_ID);
            sectionTitle = getArguments().getString(ARG_SECTION_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_section_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        sectionOverallPercentage = view.findViewById(R.id.sectionOverallPercentage);
        studentOverviewRecyclerView = view.findViewById(R.id.studentOverviewRecyclerView);
        debarredStudentsButton = view.findViewById(R.id.debarredStudentsButton);
        studentSearchEditText = view.findViewById(R.id.studentSearchEditText);

        setupToolbar();
        setupRecyclerView();
        setupSearchListener();
        loadOverviewData();

        debarredStudentsButton.setOnClickListener(v -> {
            if (getActivity() instanceof CoordinatorActivity) {
                ((CoordinatorActivity) getActivity()).navigateToFragment(
                        DebarredStudentsFragment.newInstance(sectionId, sectionTitle), true
                );
            }
        });
    }

    private void setupToolbar() {
        toolbar.setTitle(sectionTitle);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof CoordinatorActivity) {
                ((CoordinatorActivity) getActivity()).goBack();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new StudentOverviewAdapter(this);
        studentOverviewRecyclerView.setAdapter(adapter);
    }

    private void setupSearchListener() {
        studentSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadOverviewData() {
        CoordinatorRepository.fetchSectionOverview(requireContext(), sectionId, new CoordinatorRepository.SectionReportCallback() {
            @Override
            public void onSuccess(JsonObject report) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    double overallPct = report.get("overall_percentage").getAsDouble();
                    sectionOverallPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", overallPct));

                    JsonArray studentReports = report.getAsJsonArray("student_reports");
                    List<JsonObject> studentList = new ArrayList<>();
                    for (JsonElement studentElement : studentReports) {
                        studentList.add(studentElement.getAsJsonObject());
                    }
                    adapter.setStudents(studentList);
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onStudentClick(JsonObject student) {
        String studentId = student.get("id").getAsString();
        String studentName = student.get("full_name").getAsString();
        String studentNumber = student.get("student_number").getAsString();
        double percentage = student.get("percentage").getAsDouble();

        Fragment detailFragment = StudentDetailFragment.newInstance(studentId, studentName, studentNumber, percentage);

        if (getActivity() instanceof CoordinatorActivity) {
            ((CoordinatorActivity) getActivity()).navigateToFragment(detailFragment, true);
        }
    }
}