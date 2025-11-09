package com.example.caats.ui.cordinator;

import android.os.Bundle;
import android.util.Log;
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
import com.example.caats.utils.PreferenceManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class CoordinatorDashboardFragment extends Fragment implements SectionAdapter.OnSectionClickListener {

    private RecyclerView sectionRecyclerView;
    private SectionAdapter adapter;
    private TextView welcomeTextView;
    private String coordinatorId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cordinator_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get coordinator info from preferences
        coordinatorId = PreferenceManager.getUserId(requireContext());
        String coordinatorName = PreferenceManager.getFullName(requireContext());

        // Initialize views
        welcomeTextView = view.findViewById(R.id.welcomeCoordinatorTextView);
        sectionRecyclerView = view.findViewById(R.id.sectionRecyclerView);

        CoordinatorRepository.fetchCoordinatorProfile(requireContext(), coordinatorId, new CoordinatorRepository.CoordinatorProfileCallback() {
            @Override
            public void onSuccess(JsonObject coordinatorData) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {

                    if (coordinatorData.has("image_url") && !coordinatorData.get("image_url").isJsonNull()) {
                        String imageUrl = coordinatorData.get("image_url").getAsString();
                        PreferenceManager.saveImageUrl(requireContext(), imageUrl);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Log.e("CoordinatorDashboard", "Failed to load coordinator profile: " + error);
                });
            }
        });

        // Set welcome message
        welcomeTextView.setText("Welcome, " + coordinatorName);

        // Setup RecyclerView
        adapter = new SectionAdapter(this);
        sectionRecyclerView.setAdapter(adapter);

        // Load data
        loadSections();
    }

    private void loadSections() {
        if (coordinatorId == null || coordinatorId.isEmpty()) {
            Toast.makeText(getContext(), "Error: Coordinator ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        CoordinatorRepository.fetchCoordinatorSections(requireContext(), coordinatorId, new CoordinatorRepository.SectionsCallback() {
            @Override
            public void onSuccess(JsonArray sections) {
                if (getActivity() == null) return; // Fragment no longer attached

                getActivity().runOnUiThread(() -> {
                    List<JsonObject> sectionList = new ArrayList<>();
                    for (JsonElement sectionElement : sections) {
                        sectionList.add(sectionElement.getAsJsonObject());
                    }
                    adapter.setSections(sectionList);
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

    @Override
    public void onSectionClick(JsonObject section) {
        String sectionId = section.get("section_id").getAsString();
        String sectionName = "Section " + section.get("section_name").getAsString();
        String programName = section.get("program_name").getAsString();
        String title = sectionName + " (" + programName + ")";

        // Create the new fragment and pass data to it
        Fragment overviewFragment = SectionOverviewFragment.newInstance(sectionId, title);

        // Tell the activity to navigate
        if (getActivity() instanceof CoordinatorActivity) {
            ((CoordinatorActivity) getActivity()).navigateToFragment(overviewFragment, true);
        }
    }
}