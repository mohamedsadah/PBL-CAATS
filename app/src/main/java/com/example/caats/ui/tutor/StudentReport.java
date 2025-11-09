package com.example.caats.ui.tutor;

import com.google.gson.JsonObject;

public class StudentReport {

    private String fullName;
    private String studentNumber;
    private double percentage;

    public StudentReport(JsonObject jsonObject) {
        // Parse full_name safely
        if (jsonObject.has("full_name") && !jsonObject.get("full_name").isJsonNull()) {
            this.fullName = jsonObject.get("full_name").getAsString();
        } else {
            this.fullName = "Unknown Student";
        }

        // Parse student_number safely
        if (jsonObject.has("student_number") && !jsonObject.get("student_number").isJsonNull()) {
            this.studentNumber = jsonObject.get("student_number").getAsString();
        } else {
            this.studentNumber = "N/A";
        }

        // Parse percentage safely
        if (jsonObject.has("percentage") && !jsonObject.get("percentage").isJsonNull()) {
            this.percentage = jsonObject.get("percentage").getAsDouble();
        } else {
            this.percentage = 0.0;
        }
    }

    // --- Getters ---
    public String getFullName() {
        return fullName;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public double getPercentage() {
        return percentage;
    }
}