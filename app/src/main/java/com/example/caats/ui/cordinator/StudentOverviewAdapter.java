package com.example.caats.ui.cordinator;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentOverviewAdapter extends RecyclerView.Adapter<StudentOverviewAdapter.StudentViewHolder> implements Filterable {

    private final List<JsonObject> studentListFull;
    private List<JsonObject> studentListFiltered;
    private final OnStudentClickListener listener;

    public interface OnStudentClickListener {
        void onStudentClick(JsonObject student);
    }

    public StudentOverviewAdapter(OnStudentClickListener listener) {
        this.listener = listener;
        this.studentListFull = new ArrayList<>();
        this.studentListFiltered = new ArrayList<>();
    }

    public void setStudents(List<JsonObject> newStudents) {
        this.studentListFull.clear();
        this.studentListFull.addAll(newStudents);
        this.studentListFiltered.clear();
        this.studentListFiltered.addAll(newStudents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_overview_card, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        JsonObject student = studentListFiltered.get(position);
        holder.bind(student, listener);
    }

    @Override
    public int getItemCount() {
        return studentListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return studentFilter;
    }

    private final Filter studentFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<JsonObject> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(studentListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                for (JsonObject item : studentListFull) {
                    if (item.has("full_name") && !item.get("full_name").isJsonNull()) {
                        String studentName = item.get("full_name").getAsString();
                        if (studentName.toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                            filteredList.add(item);
                        }
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            studentListFiltered.clear();
            studentListFiltered.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView, studentNumberTextView, studentPercentageTextView;
        LinearProgressIndicator studentProgressBar;

        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            studentNumberTextView = itemView.findViewById(R.id.studentNumberTextView);
            studentPercentageTextView = itemView.findViewById(R.id.studentOverallPercentageTextView);
            studentProgressBar = itemView.findViewById(R.id.studentProgressBar);
        }

        public void bind(JsonObject student, OnStudentClickListener listener) {
            String name = "Unknown";
            if (student.has("full_name") && !student.get("full_name").isJsonNull()) {
                name = student.get("full_name").getAsString();
            }

            String number = "N/A";
            if (student.has("student_number") && !student.get("student_number").isJsonNull()) {
                number = student.get("student_number").getAsString();
            }

            double percentage = 0;
            if (student.has("percentage") && !student.get("percentage").isJsonNull()) {
                percentage = student.get("percentage").getAsDouble();
            }

            String percentageStr = String.format(Locale.getDefault(), "%.0f%%", percentage);

            studentNameTextView.setText(name);
            studentNumberTextView.setText(number);
            studentPercentageTextView.setText(percentageStr);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                studentProgressBar.setProgress((int) percentage, true);
            }

            itemView.setOnClickListener(v -> listener.onStudentClick(student));
        }
    }
}