package com.example.hmattendance; // Adjust package name

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hmattendance.models.Student;
import com.example.hmattendance.models.FeesPayment; // Import your new FeesPayment model
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class fees extends AppCompatActivity {

    // UI Elements
    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private TextInputEditText etStudentIdInput;
    private TextInputLayout hint;
    private Button btnSearchStudent;
    private CardView cardStudentDetails;
    private TextView tvStudentNameDisplay;
    private TextView tvStudentIdDisplay;
    private LinearLayout feesCheckboxSection;
    private CheckBox cbMonthlyFee, cbExamFee, cbAnnualFee, cbMiscellaneousFee;
    private Button btnSubmitFees;

    // Data
    private StudentDbHelper studentDbHelper;
    private FeesDbHelper feesDbHelper; // Initialize new FeesDbHelper
    private Student currentStudent = null; // To hold the found student's data

    // Default amount for fees (you might want to make this configurable or add input fields)
    private static final double DEFAULT_FEE_AMOUNT = 500.00; // Example amount

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fees);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.fees_submission_title);
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

        hint = findViewById(R.id.hint);
        SharedPreferences intial=getSharedPreferences("app_prefs",MODE_PRIVATE);
        // Get club initials for the hint (assuming it's stored in shared preferences)
        String clubInitials = intial.getString("club_initials", null);
        if (clubInitials != null) {
            String idExample = clubInitials + "-1";
            hint.setHint("Student ID  ( e.g , " + idExample + " )");
        } else {
            hint.setHint("Student ID ( e.g., ABC-1 )"); // Generic hint if initials not found
        }


        // --- Apply Window Insets ---
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Initialize UI Elements ---
        etStudentIdInput = findViewById(R.id.et_student_id_input);
        btnSearchStudent = findViewById(R.id.btn_search_student);
        cardStudentDetails = findViewById(R.id.card_student_details);
        tvStudentNameDisplay = findViewById(R.id.tv_student_name_display);
        tvStudentIdDisplay = findViewById(R.id.tv_student_id_display);
        feesCheckboxSection = findViewById(R.id.fees_checkbox_section);
        cbMonthlyFee = findViewById(R.id.cb_monthly_fee);
        cbExamFee = findViewById(R.id.cb_exam_fee);
        cbAnnualFee = findViewById(R.id.cb_annual_fee);
        cbMiscellaneousFee = findViewById(R.id.cb_miscellaneous_fee);
        btnSubmitFees = findViewById(R.id.btn_submit_fees);

        // --- Initialize Database Helpers ---
        studentDbHelper = new StudentDbHelper(this);
        feesDbHelper = new FeesDbHelper(this); // Initialize FeesDbHelper

        // --- Set Listeners ---
        btnSearchStudent.setOnClickListener(v -> searchStudentById());
        btnSubmitFees.setOnClickListener(v -> submitFees());
    }

    /**
     * Searches for a student by ID and displays their details if found.
     */
    private void searchStudentById() {
        String studentId = etStudentIdInput.getText().toString().trim();

        if (TextUtils.isEmpty(studentId)) {
            etStudentIdInput.setError(getString(R.string.error_empty_student_id));
            return;
        }

        currentStudent = studentDbHelper.getStudent(studentId); // Assuming getStudent() works by ID

        if (currentStudent != null) {
            tvStudentNameDisplay.setText(getString(R.string.student_name_format, currentStudent.getName()));
            tvStudentIdDisplay.setText(getString(R.string.student_id_placeholder, currentStudent.getId()));

            cardStudentDetails.setVisibility(View.VISIBLE);
            feesCheckboxSection.setVisibility(View.VISIBLE);
            btnSubmitFees.setVisibility(View.VISIBLE);

            // Reset checkboxes and check current fee status
            resetAndCheckFeesStatus();

            Toast.makeText(this, R.string.student_found, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.student_not_found, Toast.LENGTH_SHORT).show();
            cardStudentDetails.setVisibility(View.GONE);
            feesCheckboxSection.setVisibility(View.GONE);
            btnSubmitFees.setVisibility(View.GONE);
            currentStudent = null; // Clear student data
        }
    }

    /**
     * Resets checkboxes and checks if monthly fee is already paid for the current month.
     */
    private void resetAndCheckFeesStatus() {
        cbMonthlyFee.setChecked(false);
        cbExamFee.setChecked(false);
        cbAnnualFee.setChecked(false);
        cbMiscellaneousFee.setChecked(false);

        // Check if monthly fee is already paid for the current month for this student
        if (currentStudent != null) {
            SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            String currentYearMonth = yearMonthFormat.format(new Date());

            // You'll need the club name to check this. Assuming currentStudent has club name.
            // Or get it from SharedPreferences if the app is always for one club.
            String clubName = currentStudent.getClubName(); // Assuming Student model has getClubName()

            if (clubName != null && feesDbHelper.hasFeeBeenPaidForMonth(
                    currentStudent.getId(),
                    getString(R.string.fee_type_monthly), // Use the string resource for consistency
                    clubName,
                    currentYearMonth)) {
                cbMonthlyFee.setChecked(true);
                cbMonthlyFee.setEnabled(false); // Disable if already paid for the month
                Toast.makeText(this, "Monthly fee already paid for this month.", Toast.LENGTH_SHORT).show();
            } else {
                cbMonthlyFee.setEnabled(true);
            }
            // For other fee types (Exam, Annual, Misc), you might implement similar checks
            // based on your business logic (e.g., has exam fee been paid for this year?).
            // For simplicity, we're only doing it for monthly now.
        }
    }

    /**
     * Processes the fees submission based on selected checkboxes and saves to database.
     */
    private void submitFees() {
        if (currentStudent == null) {
            Toast.makeText(this, R.string.error_no_student_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> submittedFeeTypes = new ArrayList<>();
        if (cbMonthlyFee.isChecked() && cbMonthlyFee.isEnabled()) { // Only add if checked and enabled (not already paid)
//            submittedFeeTypes.add(getString(R.string.fee_type_monthly));
            submittedFeeTypes.add(getString(R.string.fee_type_monthly));
        }
        if (cbExamFee.isChecked()) {
            submittedFeeTypes.add(getString(R.string.fee_type_exam));
        }
        if (cbAnnualFee.isChecked()) {
            submittedFeeTypes.add(getString(R.string.fee_type_annual));
        }
        if (cbMiscellaneousFee.isChecked()) {
            submittedFeeTypes.add(getString(R.string.fee_type_miscellaneous));
        }

        if (submittedFeeTypes.isEmpty()) {
            Toast.makeText(this, R.string.error_no_fees_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current date and timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        String currentTimestamp = timestampFormat.format(new Date());

        String clubName = currentStudent.getClubName(); // Get club name from the current student

        if (clubName == null || clubName.isEmpty()) {
            Toast.makeText(this, "Error: Club name not available for student.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean allFeesSaved = true;
        for (String feeType : submittedFeeTypes) {
            // Create a FeesPayment object
            FeesPayment newPayment = new FeesPayment(
                    currentStudent.getId(),
                    feeType,
                    DEFAULT_FEE_AMOUNT, // Using default amount
                    currentDate,
                    currentTimestamp,
                    clubName
            );

            // Save to database
            long result = feesDbHelper.addFeePayment(newPayment);

            if (result == -1) {
                allFeesSaved = false;
                Toast.makeText(this, "Failed to record " + feeType + " fee.", Toast.LENGTH_SHORT).show();
            }
        }

        if (allFeesSaved) {
            String message = getString(R.string.fees_submitted_for, currentStudent.getName()) + ":\n" +
                    TextUtils.join(", ", submittedFeeTypes);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Some fees failed to submit. Check logs.", Toast.LENGTH_LONG).show();
        }

        clearForm(); // Clear the form after submission
    }

    /**
     * Clears the form fields and hides student details/fees section.
     */
    private void clearForm() {
        etStudentIdInput.setText("");
        etStudentIdInput.setError(null);
        cardStudentDetails.setVisibility(View.GONE);
        feesCheckboxSection.setVisibility(View.GONE);
        btnSubmitFees.setVisibility(View.GONE);
        currentStudent = null; // Clear current student
        cbMonthlyFee.setChecked(false);
        cbExamFee.setChecked(false);
        cbAnnualFee.setChecked(false);
        cbMiscellaneousFee.setChecked(false);
        cbMonthlyFee.setEnabled(true); // Re-enable for next search
        Toast.makeText(this, R.string.form_cleared, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close both database helpers
        if (studentDbHelper != null) {
            studentDbHelper.close();
        }
        if (feesDbHelper != null) {
            feesDbHelper.close();
        }
    }
}