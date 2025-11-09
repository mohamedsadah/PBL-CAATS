package com.example.caats.ui.cordinator;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectDetailAdapter extends RecyclerView.Adapter<SubjectDetailAdapter.SubjectViewHolder> {

    private final List<JsonObject> subjects = new ArrayList<>();

    public void setSubjects(List<JsonObject> newSubjects) {
        this.subjects.clear();
        this.subjects.addAll(newSubjects);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject_detail_card, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        holder.bind(subjects.get(position));
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        TextView subjectNameTextView, subjectAttendanceTextView;
        LinearProgressIndicator subjectProgressBar;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectNameTextView = itemView.findViewById(R.id.subjectNameTextView);
            subjectAttendanceTextView = itemView.findViewById(R.id.subjectAttendanceTextView);
            subjectProgressBar = itemView.findViewById(R.id.subjectProgressBar);
        }

        public void bind(JsonObject subject) {
            String name = subject.get("subject_name").getAsString();
            int present = subject.get("present_sessions").getAsInt();
            int total = subject.get("total_sessions").getAsInt();
            double percentage = subject.get("percentage").getAsDouble();

            String attendanceStr = String.format(Locale.getDefault(),
                    "Attendance: %d / %d (%.0f%%)", present, total, percentage);

            subjectNameTextView.setText(name);
            subjectAttendanceTextView.setText(attendanceStr);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                subjectProgressBar.setProgress((int) percentage, true);
            }
        }
    }
}