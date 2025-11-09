package com.example.caats.ui.cordinator;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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

public class DebarredStudentsFragment extends Fragment {

    private static final String ARG_SECTION_ID = "section_id";
    private static final String ARG_SECTION_TITLE = "section_title";

    private String sectionId;
    private String sectionTitle;

    private RecyclerView debarredRecyclerView;
    private DebarredStudentAdapter adapter;
    private MaterialToolbar toolbar;
    private TextInputEditText studentSearchEditText;

    public static DebarredStudentsFragment newInstance(String sectionId, String sectionTitle) {
        DebarredStudentsFragment fragment = new DebarredStudentsFragment();
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
        return inflater.inflate(R.layout.fragment_debarred_students, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar);
        debarredRecyclerView = view.findViewById(R.id.debarredRecyclerView);
        studentSearchEditText = view.findViewById(R.id.studentSearchEditText);

        setupToolbar();
        setupRecyclerView();
        setupSearchListener();
        loadDebarredStudents();
    }

    private void setupToolbar() {
        toolbar.setTitle("Debarred Students");
        toolbar.setSubtitle(sectionTitle);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof CoordinatorActivity) {
                ((CoordinatorActivity) getActivity()).goBack();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new DebarredStudentAdapter();
        debarredRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        debarredRecyclerView.setAdapter(adapter);
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

    private void loadDebarredStudents() {
        if (sectionId == null || sectionId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Section ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        CoordinatorRepository.fetchDebarredStudentsForSection(requireContext(), sectionId, new CoordinatorRepository.DebarredStudentsCallback() {
            @Override
            public void onSuccess(JsonArray students) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    List<JsonObject> studentList = new ArrayList<>();
                    for (JsonElement studentElement : students) {
                        studentList.add(studentElement.getAsJsonObject());
                    }
                    adapter.setStudents(studentList);
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}