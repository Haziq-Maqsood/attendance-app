package com.example.hmattendance;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.models.Attendance;
import com.example.hmattendance.models.Student;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MarkAttendanceActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private EditText etAttendanceDate;
    private RecyclerView rvStudentAttendance;
    private Button btnSaveAttendance;

    private StudentDbHelper studentDbHelper;
    private AttendanceDbHelper attendanceDbHelper;
    private AttendanceAdapter attendanceAdapter;

    private List<Student> studentList;
    private Map<String, String> attendanceStatusMap; // Student ID -> Status

    private String selectedDate;
    private String loggedInClubName;

    // SharedPreferences constants (ensure these match LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email"; // Used for clubName as per your request

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mark_attendance);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.mark_attendance_title);
        }

        backButton = findViewById(R.id.back_button);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish(); // Simply go back
            }
        });
        backButton.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

        // --- Apply Window Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Initialize UI Elements ---
        etAttendanceDate = findViewById(R.id.et_attendance_date);
        rvStudentAttendance = findViewById(R.id.rv_student_attendance);
        btnSaveAttendance = findViewById(R.id.btn_save_attendance);

        // --- Initialize Database Helpers ---
        studentDbHelper = new StudentDbHelper(this);
        attendanceDbHelper = new AttendanceDbHelper(this);

        // --- Retrieve Logged-in Club Name ---
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        loggedInClubName = prefs.getString(KEY_USER_EMAIL, null);

        if (loggedInClubName == null) {
            Toast.makeText(this, "Error: Club information not found. Please re-login.", Toast.LENGTH_LONG).show();
            Log.e("MarkAttendanceActivity", "Logged-in club name is null. Cannot mark attendance.");
            finish();
            return;
        }

        // --- Set current date as default ---
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etAttendanceDate.setText(selectedDate);
        etAttendanceDate.setOnClickListener(v -> showDatePickerDialog());
        etAttendanceDate.setFocusable(false);
        etAttendanceDate.setKeyListener(null);

        // --- Setup RecyclerView ---
        studentList = new ArrayList<>();
        attendanceStatusMap = new HashMap<>();
        String[] attendanceStatuses = getResources().getStringArray(R.array.attendance_statuses); // Define this array in strings.xml

        attendanceAdapter = new AttendanceAdapter(studentList, attendanceStatusMap, attendanceStatuses);
        rvStudentAttendance.setLayoutManager(new LinearLayoutManager(this));
        rvStudentAttendance.setAdapter(attendanceAdapter);

        // --- Load Students and Existing Attendance ---
        loadStudentsAndAttendance();

        // --- Set Save Button Listener ---
        btnSaveAttendance.setOnClickListener(v -> saveAttendanceRecords());
    }

    /**
     * Shows a DatePickerDialog for selecting the attendance date.
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Set date to EditText in YYYY-MM-DD format
                    selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                            selectedYear, (selectedMonth + 1), selectedDay);
                    etAttendanceDate.setText(selectedDate);
                    loadStudentsAndAttendance(); // Reload students and attendance for the new date
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Loads students for the current club and their existing attendance status for the selected date.
     */
    private void loadStudentsAndAttendance() {
        studentList.clear();
        attendanceStatusMap.clear();

        // Get all students for the logged-in club
        List<Student> students = studentDbHelper.getStudentsByClubName(loggedInClubName);
        if (students != null) {
            studentList.addAll(students);

            // For each student, check if attendance is already marked for selectedDate
            for (Student student : studentList) {
                Attendance existingAttendance = attendanceDbHelper.getAttendanceByStudentDateClub(
                        student.getId(), selectedDate, loggedInClubName);
                if (existingAttendance != null) {
                    attendanceStatusMap.put(student.getId(), existingAttendance.getStatus());
                } else {
                    // Default to 'Absent' if no attendance record exists for this date
                    attendanceStatusMap.put(student.getId(), "Absent");
                }
            }
        } else {
            Toast.makeText(this, "No students found for this club.", Toast.LENGTH_SHORT).show();
            Log.w("MarkAttendanceActivity", "No students found for club: " + loggedInClubName);
        }

        // Update the RecyclerView adapter with the new data
        attendanceAdapter.updateData(studentList, attendanceStatusMap);
    }

    /**
     * Saves all attendance records displayed in the RecyclerView to the database.
     */
    private void saveAttendanceRecords() {
        if (studentList.isEmpty()) {
            Toast.makeText(this, "No students to save attendance for.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> finalStatusMap = attendanceAdapter.getAttendanceStatusMap();
        String currentTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        int successfulSaves = 0;

        for (Student student : studentList) {
            String status = finalStatusMap.get(student.getId());
            if (status == null || status.isEmpty()) {
                status = "Absent"; // Default to Absent if somehow unselected
                Log.w("MarkAttendanceActivity", "Student " + student.getId() + " had no status, defaulting to Absent.");
            }

            Attendance attendance = new Attendance(
                    selectedDate,
                    student.getId(),
                    status,
                    loggedInClubName,
                    currentTimestamp
            );

            long result = attendanceDbHelper.addAttendance(attendance); // This will insert or update due to ON CONFLICT REPLACE
            if (result != -1) {
                successfulSaves++;
            } else {
                Log.e("MarkAttendanceActivity", "Failed to save attendance for student: " + student.getId());
            }
        }

        Toast.makeText(this, "Saved attendance for " + successfulSaves + " out of " + studentList.size() + " students.", Toast.LENGTH_SHORT).show();
        // Optional: reload to ensure UI reflects saved state if necessary, or simply clear the map if no more marking is expected for this date.
        // loadStudentsAndAttendance();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentDbHelper != null) {
            studentDbHelper.close();
        }
        if (attendanceDbHelper != null) {
            attendanceDbHelper.close();
        }
    }
}