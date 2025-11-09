package com.example.caats.ui.tutor;

import android.util.Log;

import com.google.gson.JsonObject;

public class AttendanceRecord {
    private final String id;
    private final String studentid;
    private final String studentName;
    private final String status;

    public AttendanceRecord(JsonObject record) {
        Log.d("AttendanceRecord", "Creating AttendanceRecord from JSON: " + record);

        this.id = record.get("id").getAsString();
        this.status = record.get("status").getAsString();

        String name = "Unknown Student";
        String Sid = "unknown";
        if (record.has("profiles") && !record.get("profiles").isJsonNull()) {
            JsonObject profile = record.getAsJsonObject("profiles");
            if (profile.has("full_name") && !profile.get("full_name").isJsonNull()) {
                name = profile.get("full_name").getAsString();
            }
        }
        this.studentName = name;

        if (record.has("students") && !record.get("students").isJsonNull()){
            JsonObject student = record.getAsJsonObject("students");
            if (student.has("student_number") && !student.get("student_number").isJsonNull()){
                Sid = student.get("student_number").getAsString();
            }
        }

        this.studentid = Sid;
    }

    public String getId() { return id; }

    public String getSId() { return studentid; }
    public String getStudentName() { return studentName; }
    public String getStatus() { return status; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttendanceRecord that = (AttendanceRecord) o;
        return id.equals(that.id) &&
                studentName.equals(that.studentName) &&
                status.equals(that.status);
    }
}