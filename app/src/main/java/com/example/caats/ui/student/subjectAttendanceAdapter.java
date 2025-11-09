package com.example.caats.ui.student;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.caats.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonObject;

public class subjectAttendanceAdapter extends ListAdapter<JsonObject, subjectAttendanceAdapter.SubjectViewHolder> {

    public subjectAttendanceAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject_attendance, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        JsonObject subjectData = getItem(position);
        holder.bind(subjectData);
    }

    class SubjectViewHolder extends RecyclerView.ViewHolder {
        TextView subjectNameTextView;
        TextView subjectPercentageTextView;
        LinearProgressIndicator subjectProgressBar;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectNameTextView = itemView.findViewById(R.id.subjectNameTextView);
            subjectPercentageTextView = itemView.findViewById(R.id.subjectPercentageTextView);
            subjectProgressBar = itemView.findViewById(R.id.subjectProgressBar);
        }

        public void bind(JsonObject subjectData) {
            String courseName = subjectData.get("course_name").getAsString();
            int percentage = subjectData.get("percentage").getAsInt();

            subjectNameTextView.setText(courseName);
            subjectPercentageTextView.setText(String.format("%d%%", percentage));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                subjectProgressBar.setProgress(percentage, true);
            }
        }
    }

    private static final DiffUtil.ItemCallback<JsonObject> DIFF_CALLBACK = new DiffUtil.ItemCallback<JsonObject>() {
        @Override
        public boolean areItemsTheSame(@NonNull JsonObject oldItem, @NonNull JsonObject newItem) {
            return oldItem.get("course_id").getAsString().equals(newItem.get("course_id").getAsString());
        }

        @Override
        public boolean areContentsTheSame(@NonNull JsonObject oldItem, @NonNull JsonObject newItem) {
            // For simplicity, we compare string representations. For complex objects, compare individual fields.
            return oldItem.equals(newItem);
        }
    };
}