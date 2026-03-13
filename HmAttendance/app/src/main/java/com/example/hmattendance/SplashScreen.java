package com.example.hmattendance;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreen extends AppCompatActivity {

    // Splash screen duration in milliseconds
    private static final int SPLASH_DURATION = 3000;

    // SharedPreferences constants (must match what's used in LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email";

    // Hardcoded root username (must match LoginActivity's VALID_USERNAME)
    private static final String ROOT_USERNAME = "root"; // Ensure this matches LoginActivity.VALID_USERNAME

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
//        dropTablesOnFirstRun();

        // Using Handler instead of Thread.sleep for better performance
        new Handler().postDelayed(() -> {
            checkUserAndNavigate();
        }, SPLASH_DURATION);
    }

    private void checkUserAndNavigate() {
        SharedPreferences sharedPreferences = getSharedPreferences(APP_PREFS, MODE_PRIVATE);
        String userEmail = sharedPreferences.getString(KEY_USER_EMAIL, null);

        Class<?> destinationActivity;

        if (userEmail == null) {
            // No user logged in, go to LoginActivity
            destinationActivity = LoginActivity.class;
        } else {
            // A user is logged in, determine if it's the root or a club user
            if (userEmail.equals(ROOT_USERNAME)) {
                // It's the root user
                destinationActivity = rootActivity.class; // Navigate to rootActivity
            } else {
                // It's a club user (any other email)
                destinationActivity = MainActivity.class; // Navigate to MainActivity
            }
        }

        Intent intent = new Intent(SplashScreen.this, destinationActivity);
        startActivity(intent);
        finish(); // Close splash activity so user can't go back to it
    }

    private void dropTablesOnFirstRun() {
        SharedPreferences prefs =getSharedPreferences("app_pr", MODE_PRIVATE);
//        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("first_run_done", false);
        boolean isFirstRun = !prefs.getBoolean("first_run_done", false);

        if (isFirstRun) {
            Log.d(TAG, "First run detected. Dropping and recreating all tables.");

            // Use any one helper to access the database
            StudentDbHelper studentHelper = new StudentDbHelper(this);
            SQLiteDatabase db = studentHelper.getWritableDatabase();

            // Drop all tables
            db.execSQL("DROP TABLE IF EXISTS students");
            db.execSQL("DROP TABLE IF EXISTS attendance");
            db.execSQL("DROP TABLE IF EXISTS fees_payments");
            prefs.edit().putBoolean("first_run_done", true).apply();
        }
    }


}