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
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActiveSessionAdapter extends ListAdapter<JsonObject, ActiveSessionAdapter.SessionViewHolder> {

    /**
     * Interface for handling the "Mark Present" button click.
     * This will be implemented by the Activity.
     */
    public interface OnMarkPresentClickListener {
        void onMarkPresentClick(JsonObject session, int position);
    }

    private final OnMarkPresentClickListener clickListener;

    /**
     * Constructor for the adapter.
     * @param listener The activity that implements the click listener interface.
     */
    public ActiveSessionAdapter(@NonNull OnMarkPresentClickListener listener) {
        super(DIFF_CALLBACK);
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_session, parent, false);
        return new SessionViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        JsonObject session = getItem(position);
        holder.bind(session);
    }

    /**
     * The ViewHolder class for each session item.
     */
    class SessionViewHolder extends RecyclerView.ViewHolder {

        TextView courseNameTextView;
        TextView tutorNameTextView;
        TextView timeTextView;
        MaterialButton markPresentButton;

        public SessionViewHolder(@NonNull View itemView, OnMarkPresentClickListener listener) {
            super(itemView);

            // Find all the views in the card layout
            courseNameTextView = itemView.findViewById(R.id.courseNameTextView);
            tutorNameTextView = itemView.findViewById(R.id.tutorNameTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            markPresentButton = itemView.findViewById(R.id.markPresentButton);

            // Set the click listener on the button
            markPresentButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    // Call the interface method, passing the item and its position
                    listener.onMarkPresentClick(getItem(position), position);
                }
            });
        }

        /**
         * Binds the JsonObject data to the views in the card.
         */
        public void bind(JsonObject session) {
            courseNameTextView.setText(session.get("course_name").getAsString());
            tutorNameTextView.setText("by " + session.get("tutor_name").getAsString());

            // Format and set the end time
            String endTimeString = session.get("end_time").getAsString();
            String formattedTime = formatTime(endTimeString);
            timeTextView.setText("Ends at " + formattedTime);
        }

        /**
         * A helper method to format the timestamp string (e.g., "2025-10-21T13:50:00")
         * into a more readable format (e.g., "01:50 PM").
         */
        private String formatTime(String dateTimeString) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                    LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    return dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
                } else {
                    // fallback for older Android versions
                    return dateTimeString.substring(11, 16);
                }
            } catch (Exception e) {
                return dateTimeString;
            }
        }
    }

    /**
     * DiffUtil callback for calculating list differences efficiently.
     * This makes the adapter fast and enables default animations.
     */
    private static final DiffUtil.ItemCallback<JsonObject> DIFF_CALLBACK = new DiffUtil.ItemCallback<JsonObject>() {
        @Override
        public boolean areItemsTheSame(@NonNull JsonObject oldItem, @NonNull JsonObject newItem) {
            // Check for a unique ID
            return oldItem.get("session_id").getAsString().equals(newItem.get("session_id").getAsString());
        }

        @Override
        public boolean areContentsTheSame(@NonNull JsonObject oldItem, @NonNull JsonObject newItem) {
            // Check if the content is the same
            return oldItem.equals(newItem);
        }
    };
}