package com.example.hmattendance;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
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

import com.example.hmattendance.models.Club; // Import the Club model
import com.google.android.material.textfield.TextInputEditText;

public class AddClubActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;

    private TextInputEditText etClubName, etInstructorName, etEmail, etPassword, etInitials;
    private Button btnAddClub;

    private ClubDbHelper clubDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_club);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.add_club_title);
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
        etClubName = findViewById(R.id.et_club_name);
        etInstructorName = findViewById(R.id.et_instructor_name);
        etEmail = findViewById(R.id.et_club_email);
        etPassword = findViewById(R.id.et_club_password);
        etInitials = findViewById(R.id.et_initials);
        btnAddClub = findViewById(R.id.btn_add_club);

        // --- Initialize Database Helper ---
        clubDbHelper = new ClubDbHelper(this);

        // --- Set Listener ---
        btnAddClub.setOnClickListener(v -> addClub());
    }

    /**
     * Handles the logic for adding a new club to the database.
     */
    private void addClub() {
        String clubName = etClubName.getText().toString().trim().toUpperCase();
        String instructorName = etInstructorName.getText().toString().trim().toUpperCase();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String initials = etInitials.getText().toString().trim().toUpperCase();

        // --- Input Validation ---
        boolean isValid = true;

        if (TextUtils.isEmpty(clubName)) {
            etClubName.setError(getString(R.string.error_empty_field));
            isValid = false;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_empty_field));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.error_empty_field));
            isValid = false;
        }

        if (!isValid) {
            Toast.makeText(this, R.string.error_empty_field, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Club object
        Club newClub = new Club(clubName, instructorName, email, password, initials);

        // Add to database
        boolean success = clubDbHelper.addClub(newClub);

        if (success) {
            Toast.makeText(this, R.string.add_club_success, Toast.LENGTH_SHORT).show();
            clearFields();
        } else {
            Toast.makeText(this, R.string.add_club_failure, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clears all input fields.
     */
    private void clearFields() {
        etClubName.setText("");
        etInstructorName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        etInitials.setText("");

        // Clear errors
        etClubName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clubDbHelper != null) {
            clubDbHelper.close(); // Close the database connection when the activity is destroyed
        }
    }
}