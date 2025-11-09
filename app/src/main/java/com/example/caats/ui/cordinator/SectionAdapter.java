package com.example.caats.ui.cordinator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.SectionViewHolder> {

    private final List<JsonObject> sections = new ArrayList<>();
    private final OnSectionClickListener listener;

    public interface OnSectionClickListener {
        void onSectionClick(JsonObject section);
    }

    public SectionAdapter(OnSectionClickListener listener) {
        this.listener = listener;
    }

    public void setSections(List<JsonObject> newSections) {
        this.sections.clear();
        this.sections.addAll(newSections);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section_card, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        JsonObject section = sections.get(position);
        holder.bind(section, listener);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView sectionNameTextView;
        TextView programNameTextView;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionNameTextView = itemView.findViewById(R.id.sectionNameTextView);
            programNameTextView = itemView.findViewById(R.id.programNameTextView);
        }

        public void bind(JsonObject section, OnSectionClickListener listener) {
            // Extract data from JSON
            String sectionName = "Section " + section.get("section_name").getAsString();
            String programName = section.get("program_name").getAsString();

            sectionNameTextView.setText(sectionName);
            programNameTextView.setText(programName);

            // Set the click listener on the whole card
            itemView.setOnClickListener(v -> listener.onSectionClick(section));
        }
    }
}