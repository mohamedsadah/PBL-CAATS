package com.example.caats.ui.tutor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.caats.R;

public class AttendanceAdapter extends ListAdapter<AttendanceRecord, AttendanceAdapter.AttendanceViewHolder> {

    private static final String STATUS_PRESENT = "Present";
    private final Context context;

    public AttendanceAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        AttendanceRecord record = getItem(position);
        holder.bind(record);
    }

    class AttendanceViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameTextView, studentID;
        TextView statusTextView;

        public AttendanceViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameTextView = itemView.findViewById(R.id.studentNameTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            studentID = itemView.findViewById(R.id.studentid);
        }

        public void bind(AttendanceRecord record) {
            studentNameTextView.setText(record.getStudentName());
            studentID.setText(record.getSId());
            statusTextView.setText(record.getStatus());

            if (STATUS_PRESENT.equalsIgnoreCase(record.getStatus())) {
                statusTextView.setBackground(ContextCompat.getDrawable(context, R.color.light_green));
            } else {
                statusTextView.setBackground(ContextCompat.getDrawable(context, R.color.rose_red));
            }
        }
    }

    private static final DiffUtil.ItemCallback<AttendanceRecord> DIFF_CALLBACK = new DiffUtil.ItemCallback<AttendanceRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull AttendanceRecord oldItem, @NonNull AttendanceRecord newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AttendanceRecord oldItem, @NonNull AttendanceRecord newItem) {
            return oldItem.equals(newItem);
        }
    };
}