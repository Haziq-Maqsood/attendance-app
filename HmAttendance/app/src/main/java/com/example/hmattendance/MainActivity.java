package com.example.hmattendance; // Corrected package name based on your provided code

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu; // Import Menu
import android.view.MenuItem; // Import MenuItem
import android.view.View;
import android.widget.ImageView; // Import ImageView for toolbar icon
import android.widget.LinearLayout;
import android.widget.TextView; // Import TextView for toolbar title
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    LinearLayout studentRecord, attendance, checkAttendance, addStudent, updateStudent, fees, feesPaid;
    private Toolbar mainToolbar; // Declare Toolbar
    private ImageView toolbarIcon; // Declare ImageView for toolbar icon
    private TextView toolbarTitle; // Declare TextView for toolbar title

    // SharedPreferences constants (ensure these match your LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email"; // Key for user's email/username

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- Initialize Toolbar and set it as ActionBar ---
        mainToolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolbar);

        // Remove default title if you're using a custom TextView for the title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize custom toolbar elements
        toolbarIcon = findViewById(R.id.toolbar_icon);
        toolbarTitle = findViewById(R.id.toolbar_title);
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        String clubName=prefs.getString(KEY_USER_EMAIL,null);

        // Set toolbar title and potentially icon content description
        toolbarTitle.setText(clubName.toUpperCase()); // Set your desired title for the main page
        // toolbarIcon.setContentDescription(getString(R.string.app_icon_description)); // If you want a specific string resource


        // --- Apply Window Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize LinearLayouts for navigation
        studentRecord = findViewById(R.id.showStudent);
        attendance = findViewById(R.id.markAttendance);
        checkAttendance = findViewById(R.id.checkAttendance);
        addStudent = findViewById(R.id.addStudent);
        updateStudent = findViewById(R.id.updateStudent);
        fees = findViewById(R.id.fees);
        feesPaid = findViewById(R.id.feesPaid);

        // Set OnClickListeners
        studentRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent showStudentintent = new Intent(MainActivity.this, StudentRecord.class); // Assuming StudentRecord.class is your display activity
                startActivity(showStudentintent);
            }
        });

        attendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent markattendanceIntent = new Intent(MainActivity.this, MarkAttendanceActivity.class);
                startActivity(markattendanceIntent);
                Toast.makeText(MainActivity.this, "Mark Attendance Clicked (Implement soon!)", Toast.LENGTH_SHORT).show();
                // Example: Intent intent = new Intent(MainActivity.this, MarkAttendanceActivity.class);
                // startActivity(intent);
            }
        });

        checkAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent checkattendanceIntent = new Intent(MainActivity.this, MonthlyAttendanceViewActivity.class);
                startActivity(checkattendanceIntent);
                Toast.makeText(MainActivity.this, "Check Attendance Clicked (Implement soon!)", Toast.LENGTH_SHORT).show();
                // Example: Intent intent = new Intent(MainActivity.this, CheckAttendanceHistoryActivity.class);
                // startActivity(intent);
            }
        });

        addStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentadmission = new Intent(MainActivity.this, Admission.class); // Assuming Admission.class is your admission form
                startActivity(intentadmission);
            }
        });

        updateStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent updateStudentintent = new Intent(MainActivity.this , UpdatStudent.class);
                startActivity(updateStudentintent);
                Toast.makeText(MainActivity.this, "Update Student Clicked (Implement soon!)", Toast.LENGTH_SHORT).show();
                // Example: Intent intent = new Intent(MainActivity.this, UpdateStudentActivity.class);
                // startActivity(intent);
            }
        });

        fees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent feesintent= new Intent(MainActivity.this, fees.class);
                startActivity(feesintent);
                Toast.makeText(MainActivity.this, "Fees Clicked (Implement soon!)", Toast.LENGTH_SHORT).show();
                // Example: Intent intent = new Intent(MainActivity.this, FeesActivity.class);
                // startActivity(intent);
            }
        });
        feesPaid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent feesPaidintent = new Intent(MainActivity.this, SimpleFeesActivity.class);
                startActivity(feesPaidintent);
                Toast.makeText(MainActivity.this, "Fees paid Clicked (Implement soon!)", Toast.LENGTH_SHORT).show();
                // Example: Intent intent = new Intent(MainActivity.this, FeesActivity.class);
                // startActivity(intent);
            }
        });
    }

    // --- Override onCreateOptionsMenu to inflate your menu ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu); // Ensure main_menu.xml exists in res/menu
        return true;
    }

    // --- Override onOptionsItemSelected to handle menu item clicks ---
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_logout) { // This ID must match the one in main_menu.xml
            performLogout();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Clears user session and navigates back to the LoginActivity.
     */
    private void performLogout() {
        // 1. Clear SharedPreferences (if you store login state or user info)
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_USER_EMAIL); // Remove the stored email/username
        // If you have a separate boolean key for "isLoggedIn", remove that too
        editor.apply();

        // 2. Navigate back to the LoginActivity
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear all previous activities from the stack
        startActivity(intent);
        finish(); // Finish MainActivity so user cannot go back using the back button
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}