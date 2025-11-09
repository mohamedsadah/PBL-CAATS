// Create new file: ui/tutor/StudentReportAdapter.java

package com.example.caats.ui.tutor;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentReportAdapter extends RecyclerView.Adapter<StudentReportAdapter.ReportViewHolder> implements Filterable {

    private final List<StudentReport> studentListFull;
    private List<StudentReport> studentListFiltered;

    public StudentReportAdapter() {
        this.studentListFull = new ArrayList<>();
        this.studentListFiltered = new ArrayList<>();
    }

    public void setStudentReports(List<StudentReport> reports) {
        this.studentListFull.clear();
        this.studentListFull.addAll(reports);
        this.studentListFiltered.clear();
        this.studentListFiltered.addAll(reports);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_student, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
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
            List<StudentReport> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(studentListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                for (StudentReport item : studentListFull) {
                    if (item.getFullName().toLowerCase(Locale.getDefault()).contains(filterPattern)) {
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

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView;
        TextView studentPercentageTextView;
        LinearProgressIndicator studentProgressBar;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            studentPercentageTextView = itemView.findViewById(R.id.studentPercentageTextView);
            studentProgressBar = itemView.findViewById(R.id.studentProgressBar);
        }

        public void bind(StudentReport student) {
            studentNameTextView.setText(student.getFullName());
            String percentageStr = String.format(Locale.getDefault(), "%.0f%%", student.getPercentage());
            studentPercentageTextView.setText(percentageStr);
            studentProgressBar.setProgress((int) student.getPercentage(), true);
        }

    }

    public List<StudentReport> getStudentList() {
        return this.studentListFiltered;
    }
}