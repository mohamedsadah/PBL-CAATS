package com.example.caats.ui.tutor;

import android.content.Intent;
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

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<JsonObject> sessions = new ArrayList<>();

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_session_card, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        JsonObject session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    public void setSessions(List<JsonObject> newSessions) {
        this.sessions = newSessions;
        notifyDataSetChanged();
    }

    class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView courseNameTextView;
        TextView dateTimeTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            courseNameTextView = itemView.findViewById(R.id.courseNameTextView);
            dateTimeTextView = itemView.findViewById(R.id.dateTimeTextView);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    JsonObject session = sessions.get(position);
                    String sessionId = session.get("id").getAsString();
                    String sessionName = session.get("courses").getAsJsonObject().get("title").getAsString();

                    Intent intent = new Intent(itemView.getContext(), AttendanceListActivity.class);
                    intent.putExtra(AttendanceListActivity.EXTRA_SESSION_ID, sessionId);
                    intent.putExtra("Session_Name", sessionName);
                    itemView.getContext().startActivity(intent);
                }
            });
        }

        public void bind(JsonObject session) {
            // Set Course Name (from the nested object)
            String courseName = "Unnamed Session";
            if (session.has("courses") && !session.get("courses").isJsonNull()) {
                JsonObject courseObj = session.getAsJsonObject("courses");
                if (courseObj.has("title") && !courseObj.get("title").isJsonNull()) {
                    courseName = courseObj.get("title").getAsString();
                }
            }
            courseNameTextView.setText(courseName);

            // Set Date and Time
            String date = "N/A";
            if (session.has("end_time") && !session.get("end_time").isJsonNull()) {
                // Simple formatting, you can use a DateFormatter for a nicer look
                date = "End Time: " + session.get("end_time").getAsString().replace("T", " ");
            }
            dateTimeTextView.setText(date);
        }
    }
}