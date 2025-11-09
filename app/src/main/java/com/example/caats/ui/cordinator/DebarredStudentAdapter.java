package com.example.caats.ui.cordinator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DebarredStudentAdapter extends RecyclerView.Adapter<DebarredStudentAdapter.DebarredViewHolder> implements Filterable {

    private final List<JsonObject> studentListFull;
    private final List<JsonObject> studentListFiltered;

    public DebarredStudentAdapter() {
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
    public DebarredViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_debarred_students, parent, false);
        return new DebarredViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebarredViewHolder holder, int position) {
        holder.bind(studentListFiltered.get(position));
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
                    String studentName = item.get("full_name").getAsString();
                    if (studentName.toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                        filteredList.add(item);
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

    static class DebarredViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView, studentNumberTextView, studentPercentageTextView;

        public DebarredViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            studentNumberTextView = itemView.findViewById(R.id.studentNumberTextView);
            studentPercentageTextView = itemView.findViewById(R.id.studentPercentageTextView);
        }

        public void bind(JsonObject student) {
            studentNameTextView.setText(student.get("full_name").getAsString());
            studentNumberTextView.setText(student.get("student_number").getAsString());
            double percentage = student.get("percentage").getAsDouble();
            String percentageStr = String.format(Locale.getDefault(), "%.0f%%", percentage);
            studentPercentageTextView.setText(percentageStr);
        }
    }
}