package com.example.hmattendance;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView; // Import for Spinner
import android.widget.ArrayAdapter; // Import for Spinner
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner; // Import for Spinner
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hmattendance.models.Club; // Make sure to import your Club model

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpdateClubActivity extends AppCompatActivity {

    // Removed EXTRA_CLUB_NAME as we are now selecting from a spinner

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;

    private Spinner spinnerSelectClub; // NEW: Spinner for club selection
    private TextView tvClubName;
    private EditText etInstructorName;
    private EditText etEmail;
    private EditText etPassword;
    private TextView tvInitials;
    private Button btnSaveChanges;

    private ClubDbHelper clubDbHelper;
    private StudentDbHelper studentDbHelper; // Needed for student count
    private String currentSelectedClubName = null; // Renamed to clarify it's the selected club

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_club);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = findViewById(R.id.toolbar_title);
        toolbarTitle.setText(R.string.update_club_title);

        backButton = findViewById(R.id.back_button);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
        backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // --- Apply Window Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Initialize UI Elements ---
        spinnerSelectClub = findViewById(R.id.spinner_select_club); // NEW
        tvClubName = findViewById(R.id.tv_club_name);
        etInstructorName = findViewById(R.id.et_instructor_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        tvInitials = findViewById(R.id.tv_initials);
        btnSaveChanges = findViewById(R.id.btn_save_changes);

        clubDbHelper = new ClubDbHelper(this);
        studentDbHelper = new StudentDbHelper(this); // Initialize StudentDbHelper for counts

        // Initial setup of the form (disable fields until a club is selected)
        setFormFieldsEnabled(false);

        // Load clubs into the spinner
        loadClubsIntoSpinner();

        // Set listener for the spinner
        spinnerSelectClub.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemText = parent.getItemAtPosition(position).toString();

                if (selectedItemText.equals(getString(R.string.select_a_club_prompt))) {
                    // "Select a Club" or initial state
                    currentSelectedClubName = null;
                    clearFormFields();
                    setFormFieldsEnabled(false);
                } else {
                    // Extract the actual club name from "Club Name (Count)"
                    int parenIndex = selectedItemText.indexOf(" (");
                    if (parenIndex != -1) {
                        currentSelectedClubName = selectedItemText.substring(0, parenIndex);
                    } else {
                        currentSelectedClubName = selectedItemText; // Fallback
                    }
                    loadClubData(currentSelectedClubName); // Load data for the selected club
                    setFormFieldsEnabled(true); // Enable fields once a club is selected
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        btnSaveChanges.setOnClickListener(v -> saveClubChanges());
    }

    private void loadClubsIntoSpinner() {
        List<String> rawClubNames = clubDbHelper.getAllClubNames(); // Get raw club names
        Collections.sort(rawClubNames); // Sort clubs alphabetically

        List<String> spinnerItems = new ArrayList<>();
        spinnerItems.add(getString(R.string.select_a_club_prompt)); // Prompt for selection

        if (rawClubNames.isEmpty()) {
            Toast.makeText(this, R.string.no_clubs_found, Toast.LENGTH_LONG).show();
            // Keep the "Select a Club" prompt, disable save button
            btnSaveChanges.setEnabled(false);
        } else {
            for (String clubName : rawClubNames) {
                int studentCount = studentDbHelper.getStudentCountForClub(clubName);
                spinnerItems.add(clubName + " (" + studentCount + ")");
            }
            btnSaveChanges.setEnabled(true); // Enable if clubs are present
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerItems
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSelectClub.setAdapter(adapter);
    }

    private void loadClubData(String clubName) {
        Club club = clubDbHelper.getClubByClubName(clubName);
        if (club != null) {
            tvClubName.setText(club.getClubName());
            etInstructorName.setText(club.getInstructorName());
            etEmail.setText(club.getEmail());
            etPassword.setText(club.getPassword());
            tvInitials.setText(club.getInitials());

            // Make non-editable fields visually distinct and un-focusable
            tvClubName.setFocusable(false);
            tvClubName.setClickable(false);
            tvInitials.setFocusable(false);
            tvInitials.setClickable(false);

            // Ensure editable fields are enabled and focusable
            setFormFieldsEnabled(true);

        } else {
            // This case should ideally not happen if club names come from the DB
            Toast.makeText(this, R.string.club_not_found, Toast.LENGTH_SHORT).show();
            clearFormFields();
            setFormFieldsEnabled(false);
        }
    }

    private void clearFormFields() {
        tvClubName.setText("");
        etInstructorName.setText("");
        etEmail.setText("");
        etPassword.setText("");
        tvInitials.setText("");
    }

    private void setFormFieldsEnabled(boolean enabled) {
        etInstructorName.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        btnSaveChanges.setEnabled(enabled); // Only enable save button if fields are enabled
    }


    private void saveClubChanges() {
        if (currentSelectedClubName == null || currentSelectedClubName.isEmpty()) {
            Toast.makeText(this, R.string.select_a_club_prompt, Toast.LENGTH_SHORT).show();
            return;
        }

        String newInstructorName = etInstructorName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim(); // CONSIDER HASHING THIS!

        // Basic validation for editable fields
        if (newInstructorName.isEmpty() || newEmail.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all editable fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get original initials (since they are not editable and needed for the Club object)
        Club originalClub = clubDbHelper.getClubByClubName(currentSelectedClubName);
        if (originalClub == null) {
            Toast.makeText(this, R.string.club_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        String originalInitials = originalClub.getInitials();

        Club updatedClub = new Club(
                currentSelectedClubName, // The selected club name (primary key)
                newInstructorName,
                newEmail,
                newPassword,
                originalInitials
        );

        boolean isUpdated = clubDbHelper.updateClub(updatedClub);

        if (isUpdated) {
            Toast.makeText(this, R.string.club_updated_successfully, Toast.LENGTH_SHORT).show();
            // After successful update, reload spinner to reflect any potential changes (though not for count in this case)
            // and clear/reset the form
            loadClubsIntoSpinner(); // Reload clubs and counts
            spinnerSelectClub.setSelection(0); // Reset spinner to "Select a Club"
            clearFormFields();
            setFormFieldsEnabled(false);

        } else {
            Toast.makeText(this, R.string.failed_to_update_club, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload the spinner content in onResume in case clubs were added/deleted
        // while this activity was in the background
        loadClubsIntoSpinner();
        // Reset form state
        spinnerSelectClub.setSelection(0); // Select the initial prompt
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clubDbHelper != null) {
            clubDbHelper.close();
        }
        if (studentDbHelper != null) { // Close studentDbHelper as well
            studentDbHelper.close();
        }
    }
}