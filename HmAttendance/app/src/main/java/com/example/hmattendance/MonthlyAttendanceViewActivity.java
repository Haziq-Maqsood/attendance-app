package com.example.hmattendance;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
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

import com.example.hmattendance.models.MonthlyAttendanceSummary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthlyAttendanceViewActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private Spinner spinnerMonth;
    private EditText etYear;
    private RecyclerView rvMonthlyAttendanceSummary;
    private TextView tvNoDataMessage;

    private AttendanceDbHelper attendanceDbHelper;
    private MonthlyAttendanceSummaryAdapter adapter;
    private List<MonthlyAttendanceSummary> summaryList;

    private String loggedInClubName;
    private int selectedMonth; // 0-indexed (Calendar.JANUARY = 0)
    private int selectedYear;

    // SharedPreferences constants (ensure these match LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email"; // Used for clubName as per your request

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_monthly_attendance_view);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.monthly_attendance_title);
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
        spinnerMonth = findViewById(R.id.spinner_month);
        etYear = findViewById(R.id.et_year);
        rvMonthlyAttendanceSummary = findViewById(R.id.rv_monthly_attendance_summary);
        tvNoDataMessage = findViewById(R.id.tv_no_data_message);

        // --- Initialize Database Helper ---
        attendanceDbHelper = new AttendanceDbHelper(this);

        // --- Retrieve Logged-in Club Name ---
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        loggedInClubName = prefs.getString(KEY_USER_EMAIL, null);

        if (loggedInClubName == null) {
            Toast.makeText(this, "Error: Club information not found. Please re-login.", Toast.LENGTH_LONG).show();
            Log.e("MonthlyAttendanceView", "Logged-in club name is null. Cannot view attendance.");
            finish();
            return;
        }

        // --- Set current month and year as default ---
        Calendar calendar = Calendar.getInstance();
        selectedMonth = calendar.get(Calendar.MONTH); // Calendar.MONTH is 0-indexed
        selectedYear = calendar.get(Calendar.YEAR);

        // --- Setup Month Spinner ---
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                this, R.array.month_names, android.R.layout.simple_spinner_item);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setSelection(selectedMonth); // Set current month as default

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = position; // Position is 0-indexed, matching Calendar.MONTH
                loadMonthlyAttendanceData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // --- Setup Year EditText ---
        etYear.setText(String.valueOf(selectedYear));
        etYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 4) { // Only attempt to load if a full 4-digit year is entered
                    try {
                        int year = Integer.parseInt(s.toString());
                        if (year >= 1900 && year <= 2100) { // Basic year validation
                            selectedYear = year;
                            loadMonthlyAttendanceData();
                        } else {
                            etYear.setError("Enter a valid year (e.g., 2024)");
                        }
                    } catch (NumberFormatException e) {
                        etYear.setError("Invalid year format");
                    }
                } else if (s.length() > 0 && s.length() < 4) {
                    // User is still typing, or entered less than 4 digits. Clear error if any.
                    etYear.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- Setup RecyclerView ---
        summaryList = new ArrayList<>();
        adapter = new MonthlyAttendanceSummaryAdapter(summaryList);
        rvMonthlyAttendanceSummary.setLayoutManager(new LinearLayoutManager(this));
        rvMonthlyAttendanceSummary.setAdapter(adapter);

        // --- Load initial data ---
        loadMonthlyAttendanceData();
    }

    /**
     * Loads monthly attendance data based on selected month, year, and club.
     */
    private void loadMonthlyAttendanceData() {
        // Format month for query (MM, e.g., 01 for January)
        String yearMonth = String.format(Locale.getDefault(), "%d-%02d", selectedYear, selectedMonth + 1); // +1 because Calendar.MONTH is 0-indexed

        Log.d("MonthlyAttendanceView", "Loading attendance for YearMonth: " + yearMonth + ", Club: " + loggedInClubName);

        List<MonthlyAttendanceSummary> fetchedSummaries =
                attendanceDbHelper.getMonthlyAttendanceSummary(yearMonth, loggedInClubName);

        summaryList.clear();
        if (fetchedSummaries != null && !fetchedSummaries.isEmpty()) {
            summaryList.addAll(fetchedSummaries);
            tvNoDataMessage.setVisibility(View.GONE);
            rvMonthlyAttendanceSummary.setVisibility(View.VISIBLE);
            Log.d("MonthlyAttendanceView", "Found " + fetchedSummaries.size() + " monthly summaries.");
        } else {
            tvNoDataMessage.setVisibility(View.VISIBLE);
            rvMonthlyAttendanceSummary.setVisibility(View.GONE);
            Log.d("MonthlyAttendanceView", "No monthly attendance data found for " + yearMonth + " in club " + loggedInClubName);
        }
        adapter.updateData(summaryList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceDbHelper != null) {
            attendanceDbHelper.close();
        }
    }
}