package com.example.hmattendance; // Adjust package name

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import com.example.hmattendance.adapter.StudentAdapter;
import com.example.hmattendance.models.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentRecord extends AppCompatActivity implements StudentAdapter.OnItemClickListener {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private RecyclerView rvStudents;
    private TextView tvNoRecordsFound;

    private StudentDbHelper studentDbHelper;
    private StudentAdapter studentAdapter;
    private List<Student> studentList;

    // SharedPreferences constants (ensure these match LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email"; // Original key


    private String currentClubName; // To store the club name for filtering students

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_record);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.student_records_title); // Initial title
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
        rvStudents = findViewById(R.id.rv_students);
        tvNoRecordsFound = findViewById(R.id.tv_no_records_found);

        // --- Initialize Database Helper ---
        studentDbHelper = new StudentDbHelper(this);

        // --- Get Club Name from SharedPreferences ---
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);

            currentClubName = prefs.getString(KEY_USER_EMAIL, null);


            if (currentClubName == null) {
                // This case indicates an inconsistency (club user type but no club name)
                Toast.makeText(this, "Error: Club name not found. Please re-login.", Toast.LENGTH_LONG).show();
                Log.e("StudentRecord", "Club user logged in but KEY_CLUB_NAME is null.");
                finish(); // Or redirect to login
                return; // Stop further execution
            }
            // Optionally update toolbar title with the actual club name if desired
            if (toolbarTitle != null) {
                toolbarTitle.setText(currentClubName.toUpperCase() + " Students");
            }



        // --- Setup RecyclerView ---
        studentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(studentList, this);
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(studentAdapter);

        // Load students for the specific club
        loadStudents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list whenever the activity comes to the foreground (e.g., after returning from edit form)
        // Only reload if currentClubName is valid
        if (currentClubName != null) {
            loadStudents();
        }
    }

    /**
     * Fetches student records for the current club from the database and updates the RecyclerView.
     */
    private void loadStudents() {
        if (currentClubName == null) {
            // Should have been handled in onCreate, but double-check
            Log.e("StudentRecord", "Attempted to load students without a valid club name.");
            rvStudents.setVisibility(View.GONE);
            tvNoRecordsFound.setText("Error: Could not retrieve club information.");
            tvNoRecordsFound.setVisibility(View.VISIBLE);
            return;
        }

        List<Student> fetchedStudents = studentDbHelper.getStudentsByClubName(currentClubName); // Use the new method
        studentAdapter.setStudentList(fetchedStudents); // Update adapter's data

        if (fetchedStudents.isEmpty()) {
            rvStudents.setVisibility(View.GONE);
            tvNoRecordsFound.setText(getString(R.string.no_students_found_for_club, currentClubName)); // Dynamic message
            tvNoRecordsFound.setVisibility(View.VISIBLE);
        } else {
            rvStudents.setVisibility(View.VISIBLE);
            tvNoRecordsFound.setVisibility(View.GONE);
        }
    }

    @Override
    public void onItemClick(Student student) {
        Toast.makeText(this, "Clicked on student: " + student.getName() + " (ID: " + student.getId() + ")", Toast.LENGTH_SHORT).show();
        Log.d("StudentRecord", "Student details: " + student.getName() + ", " + student.getAge() + ", " + student.getAdmissionDate() + ", Club: " + student.getClubName());
        // You can start a new activity here to show full student details or an edit form
        // Intent detailIntent = new Intent(this, StudentDetailActivity.class);
        // detailIntent.putExtra("student_id", student.getId());
        // startActivity(detailIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentDbHelper != null) {
            studentDbHelper.close();
        }
    }
}