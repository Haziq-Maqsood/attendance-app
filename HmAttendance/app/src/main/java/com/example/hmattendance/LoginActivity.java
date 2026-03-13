package com.example.hmattendance; // Adjust package name

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hmattendance.models.Club; // Import the Club model

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;

    // SharedPreferences constants (must match what's used in FeedbackFragment/onlineTransaction)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email"; // This is the key for the email/username
    private static final String KEY_CLUB_INITIALS = "club_initials";//

    // Hardcoded credentials for demonstration (for root user)
    private static final String VALID_USERNAME = "root";
    private static final String VALID_PASSWORD = "user";

    private ClubDbHelper clubDbHelper; // Declare ClubDbHelper

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Apply window insets for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        // Initialize ClubDbHelper
        clubDbHelper = new ClubDbHelper(this);


        // Set login button click listener
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Set Sign Up text click listener (optional, if you have a signup activity)
    }

    /**
     * Handles the login attempt, validates input, and authenticates against hardcoded or club database credentials.
     */
    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Clear previous errors
        etUsername.setError(null);
        etPassword.setError(null);

        boolean cancel = false;
        View focusView = null;

        // Input validation
        if (TextUtils.isEmpty(password)) {
            etPassword.setError(getString(R.string.password_empty_error));
            focusView = etPassword;
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            etUsername.setError(getString(R.string.username_empty_error));
            focusView = etUsername;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first invalid field.
            focusView.requestFocus();
        } else {
            // 1. Attempt login with hardcoded root credentials
            if (username.equals(VALID_USERNAME) && password.equals(VALID_PASSWORD)) {
                Toast.makeText(this, R.string.login_success_message, Toast.LENGTH_SHORT).show();

                // Save username/email to SharedPreferences (for the root user)
                SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_USER_EMAIL, username);
                editor.apply();

                // Navigate to root activity
                Intent intent = new Intent(LoginActivity.this, rootActivity.class); // Ensure rootActivity.class exists
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
            // 2. Else, attempt login with club credentials from the database
            else {
                Club club = clubDbHelper.getClubByEmailAndPassword(username, password);
                if (club != null) {
                    Toast.makeText(this, R.string.login_success_message, Toast.LENGTH_SHORT).show();

                    // Save club email to SharedPreferences
                    SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(KEY_USER_EMAIL, club.getClubName()); // Save the club's email
                    editor.putString(KEY_CLUB_INITIALS, club.getInitials().toUpperCase());
                    editor.apply();

                    // Navigate to MainActivity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class); // Ensure MainActivity.class exists
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    // 3. Neither hardcoded nor club credentials matched
                    Toast.makeText(this, R.string.login_failed_message, Toast.LENGTH_SHORT).show();
                    etPassword.setError(getString(R.string.login_failed_message)); // Indicate incorrect credentials
                    etPassword.requestFocus();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clubDbHelper != null) {
            clubDbHelper.close(); // Close the database connection when the activity is destroyed
        }
    }
}