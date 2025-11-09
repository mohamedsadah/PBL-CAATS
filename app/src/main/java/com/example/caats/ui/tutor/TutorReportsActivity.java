// Create new file: ui/tutor/ReportsActivity.java

package com.example.caats.ui.tutor;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.caats.R;
import com.example.caats.repository.TutorRepository;
import com.example.caats.ui.common.ProfilesActivity;
import com.example.caats.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TutorReportsActivity extends AppCompatActivity {

    private AutoCompleteTextView subjectDropdown;
    private MaterialCardView overallCard;
    private TextView overallSubjectPercentage;
    private TextInputEditText studentSearchEditText;
    private RecyclerView studentReportRecyclerView;
    private TextView emptyViewTextView;

    private FloatingActionButton downloadPdfButton;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private String selectedSubjectTitle = "";

    private StudentReportAdapter adapter;
    private final Map<String, String> subjectMap = new HashMap<>();
    private String tutorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_reports);

        tutorId = PreferenceManager.getUserId(this); 

        initializeViews();
        setupRecyclerView();
        setupDropdown();
        setupSearchListener();

        downloadPdfButton.setOnClickListener(v -> {

            if (!selectedSubjectTitle.isEmpty()){
                checkPermissionAndDownloadPdf();
            } else {
                Toast.makeText(this, "Please select a subject to generate a report.", Toast.LENGTH_SHORT).show();
            }

        });


        subjectDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTitle = (String) parent.getItemAtPosition(position);
            this.selectedSubjectTitle = selectedTitle;
            String selectedCourseId = subjectMap.get(selectedTitle);
            if (selectedCourseId != null) {
                loadReportData(selectedCourseId);
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_sessions) {
                    startActivity(new Intent(this, TutorDashboardActivity.class));
                    return true;
                } else if (id == R.id.nav_reports) {
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfilesActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    private void initializeViews() {
        subjectDropdown = findViewById(R.id.subjectDropdown);
        overallCard = findViewById(R.id.overallCard);
        overallSubjectPercentage = findViewById(R.id.overallSubjectPercentage);
        studentSearchEditText = findViewById(R.id.studentSearchEditText);
        studentReportRecyclerView = findViewById(R.id.studentReportRecyclerView);
        downloadPdfButton = findViewById(R.id.downloadPdfButton);
        emptyViewTextView = findViewById(R.id.emptyViewTextView);
    }

    private void setupRecyclerView() {
        adapter = new StudentReportAdapter();
        studentReportRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentReportRecyclerView.setAdapter(adapter);
    }

    private void setupDropdown() {

        TutorRepository.fetchTutorSubjects(this, tutorId, new TutorRepository.SubjectsCallback() {
            @Override
            public void onSuccess(JsonArray subjects) {
                runOnUiThread(() -> {
                    subjectMap.clear();
                    List<String> subjectTitles = new ArrayList<>();
                    for (JsonElement subjectElement : subjects) {
                        JsonObject subject = subjectElement.getAsJsonObject();
                        String title = subject.get("title").getAsString();
                        String id = subject.get("id").getAsString();
                        subjectTitles.add(title);
                        subjectMap.put(title, id);
                    }
                    ArrayAdapter<String> dropdownAdapter = new ArrayAdapter<>(
                            TutorReportsActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            subjectTitles
                    );
                    subjectDropdown.setAdapter(dropdownAdapter);
                });

            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(TutorReportsActivity.this, error, Toast.LENGTH_SHORT).show());
            }
        });

        subjectDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedTitle = (String) parent.getItemAtPosition(position);
            String selectedCourseId = subjectMap.get(selectedTitle);
            if (selectedCourseId != null) {
                loadReportData(selectedCourseId);
            }
        });
    }

    private void loadReportData(String courseId) {
        // Show loading state
        emptyViewTextView.setVisibility(View.VISIBLE);
        emptyViewTextView.setText("Loading report...");
        studentReportRecyclerView.setVisibility(View.GONE);
        overallCard.setVisibility(View.GONE);

        TutorRepository.fetchSubjectReport(this, courseId, tutorId, new TutorRepository.ReportCallback() {
            @Override
            public void onSuccess(JsonObject report) {
                runOnUiThread(() -> {
                    // 1. Update Overall Card
                    double overallPct = report.get("overall_percentage").getAsDouble();
                    overallSubjectPercentage.setText(String.format(Locale.getDefault(), "%.0f%%", overallPct));
                    overallCard.setVisibility(View.VISIBLE);

                    // 2. Update Student List
                    JsonArray studentReports = report.getAsJsonArray("student_reports");
                    if (studentReports == null || studentReports.size() == 0) {
                        emptyViewTextView.setText("No student attendance data found for this subject.");
                        studentReportRecyclerView.setVisibility(View.GONE);
                    } else {
                        List<StudentReport> studentList = new ArrayList<>();
                        for (JsonElement studentElement : studentReports) {
                            studentList.add(new StudentReport(studentElement.getAsJsonObject()));
                        }
                        adapter.setStudentReports(studentList);
                        emptyViewTextView.setVisibility(View.GONE);
                        studentReportRecyclerView.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(TutorReportsActivity.this, error, Toast.LENGTH_SHORT).show();
                    emptyViewTextView.setText("Failed to load report. Please try again.");
                    studentReportRecyclerView.setVisibility(View.GONE);
                    overallCard.setVisibility(View.GONE);
                });
            }
        });
    }

    private void setupSearchListener() {
        studentSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void checkPermissionAndDownloadPdf() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // Android 9 (Pie) or older
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                createAndSavePdf();
            }
        } else {
            // No runtime permission needed for MediaStore (API 29+) or app-specific storage
            createAndSavePdf();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createAndSavePdf();
            } else {
                Toast.makeText(this, "Storage permission is required to download PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createAndSavePdf() {
        List<StudentReport> studentList = adapter.getStudentList();

        PdfDocument pdfDocument = new PdfDocument();

        int pageWidth = 595;
        int pageHeight = 842;
        int leftMargin = 40;
        int topMargin = 40;
        int bottomMargin = 40;
        int pageNumber = 1;

        Paint titlePaint = new Paint();
        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        Paint headerPaint = new Paint();
        headerPaint.setTextSize(16f);
        headerPaint.setFakeBoldText(true);

        Paint headerCol1 = new Paint(headerPaint);
        headerCol1.setTextAlign(Paint.Align.LEFT);
        Paint headerCol2 = new Paint(headerPaint);
        headerCol2.setTextAlign(Paint.Align.CENTER);
        Paint headerCol3 = new Paint(headerPaint);
        headerCol3.setTextAlign(Paint.Align.RIGHT);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(14f);

        Paint bodyCol1 = new Paint(bodyPaint);
        bodyCol1.setTextAlign(Paint.Align.LEFT);
        Paint bodyCol2 = new Paint(bodyPaint);
        bodyCol2.setTextAlign(Paint.Align.CENTER);
        Paint bodyCol3 = new Paint(bodyPaint);
        bodyCol3.setTextAlign(Paint.Align.RIGHT);

        int col1x = leftMargin;
        int col2x = pageWidth / 2;
        int col3x = pageWidth - leftMargin;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int yPosition = topMargin;

        try {
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_logo);
            Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, 80, 80, false);
            int logoX = (pageWidth - scaledLogo.getWidth()) / 2;
            canvas.drawBitmap(scaledLogo, logoX, yPosition, null);
            yPosition += scaledLogo.getHeight() + 20;
        } catch (Exception e) {
            Log.e("ReportsActivity", "Failed to load logo drawable. Skipping logo.");
        }

        canvas.drawText("Attendance Report", pageWidth / 2, yPosition, titlePaint);
        yPosition += 40;

        canvas.drawText("Subject: " + this.selectedSubjectTitle, leftMargin, yPosition, headerPaint);
        yPosition += 25;

        String overallText = overallSubjectPercentage.getText().toString();
        canvas.drawText("Overall Attendance: " + overallText, leftMargin, yPosition, headerPaint);
        yPosition += 40;

        canvas.drawText("Student Name", col1x, yPosition, headerCol1);
        canvas.drawText("Student ID", col2x, yPosition, headerCol2);
        canvas.drawText("Percentage", col3x, yPosition, headerCol3);
        yPosition += 30;

        if (studentList.isEmpty()) {
            bodyCol1.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("No student data available for this subject.", pageWidth / 2, yPosition, bodyCol1);
        } else {
            for (StudentReport student : studentList) {
                if (yPosition > (pageHeight - bottomMargin)) {
                    pdfDocument.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    yPosition = topMargin;

                    canvas.drawText("Student Name", col1x, yPosition, headerCol1);
                    canvas.drawText("Student ID", col2x, yPosition, headerCol2);
                    canvas.drawText("Percentage", col3x, yPosition, headerCol3);
                    yPosition += 30;
                }

                canvas.drawText(student.getFullName(), col1x, yPosition, bodyCol1);
                canvas.drawText(student.getStudentNumber(), col2x, yPosition, bodyCol2);
                String percentageStr = String.format(Locale.getDefault(), "%.0f%%", student.getPercentage());
                canvas.drawText(percentageStr, col3x, yPosition, bodyCol3);

                yPosition += 25;
            }
        }

        pdfDocument.finishPage(page);
        savePdfToFile(pdfDocument);
    }

    private void savePdfToFile(PdfDocument document) {
        String fileName = "Attendance_Report_" + this.selectedSubjectTitle.replaceAll("\\s+", "_") + ".pdf";
        OutputStream fos = null;
        Uri pdfUri = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                // Use MediaStore for Android 10 (Q) and above
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                pdfUri = getContentResolver().insert(collection, values);
                fos = getContentResolver().openOutputStream(pdfUri);
            } else {

                // Use classic storage for Android 9 (P) and below
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                fos = new FileOutputStream(file);
            }

            document.writeTo(fos);
            Toast.makeText(this, String.format("Report for %s saved Successfully", this.selectedSubjectTitle), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e("ReportsActivity", "Error saving PDF", e);
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (pdfUri != null) {

                // Clean up the incomplete file if an error occurred
                getContentResolver().delete(pdfUri, null, null);
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            document.close();
        }
    }
}
