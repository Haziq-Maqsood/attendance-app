package com.example.hmattendance;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hmattendance.models.Student;     // Ensure this import is correct
import com.example.hmattendance.models.FeesPayment; // Ensure this import is correct

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SimpleFeesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private RecyclerView recyclerView;
    private TextView tvNoStudents;

    // NEW UI elements for month filter
    private TextView tvSelectedMonthYear;
    private ImageView ivPickMonth;

    private SimpleFeesAdapter adapter;

    // Database helpers (as you are not consolidating)
    private StudentDbHelper studentDbHelper;
    private FeesDbHelper feesDbHelper;

    // SharedPreferences for club info
    private String clubName;

    // Current filter selection
    private String selectedYearMonth; // Stores YYYY-MM format for database query

    // SharedPreferences constants (ensure these match LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_CLUB_INITIALS = "club_initials"; // Still good to retrieve

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_simple_fees);

        // --- Retrieve Club Info from SharedPreferences ---
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);
        clubName = prefs.getString(KEY_USER_EMAIL, null);
        String clubInitials = prefs.getString(KEY_CLUB_INITIALS, null); // Retrieved, but not directly used here

        if (clubName == null || clubInitials == null || clubInitials.isEmpty()) {
            Toast.makeText(this, "Error: Club information incomplete. Please re-login.", Toast.LENGTH_LONG).show();
            Log.e("SimpleFeesActivity", "Club user logged in but clubName or clubInitials is null/empty.");
            finish();
            return;
        }

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.simple_fees_title);
        }

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
        recyclerView = findViewById(R.id.recycler_view_simple_fees);
        tvNoStudents = findViewById(R.id.tv_no_students);

        // Initialize NEW UI elements
        tvSelectedMonthYear = findViewById(R.id.tv_selected_month_year);
        ivPickMonth = findViewById(R.id.iv_pick_month);

        // --- Initialize Database Helpers ---
        studentDbHelper = new StudentDbHelper(this);
        feesDbHelper = new FeesDbHelper(this);

        // --- Setup Month/Year Selector ---
        // Set default month/year to current month
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdfFilter = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        selectedYearMonth = sdfFilter.format(cal.getTime());
        tvSelectedMonthYear.setText(new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.getTime())); // Display in readable format

        ivPickMonth.setOnClickListener(v -> showMonthPickerDialog());

        // --- Setup RecyclerView ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleFeesAdapter(this);
        recyclerView.setAdapter(adapter);

        // Load data initially
        loadFeesStatus();
    }

    private void showMonthPickerDialog() {
        final Calendar c = Calendar.getInstance();
        // Use the currently selected month/year for initial dialog display
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            c.setTime(sdf.parse(selectedYearMonth));
        } catch (java.text.ParseException e) {
            Log.e("SimpleFeesActivity", "Error parsing selectedYearMonth: " + e.getMessage());
            // Fallback to current date if parsing fails
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);

        MonthYearPickerDialogFragment newFragment = new MonthYearPickerDialogFragment();
        newFragment.setListener((view, year1, month1, dayOfMonth) -> {
            // month1 is 0-indexed, so add 1 is not needed for Calendar.set(MONTH)
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(Calendar.YEAR, year1);
            selectedCalendar.set(Calendar.MONTH, month1);

            SimpleDateFormat sdfDisplay = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
            SimpleDateFormat sdfFilter = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

            tvSelectedMonthYear.setText(sdfDisplay.format(selectedCalendar.getTime()));
            selectedYearMonth = sdfFilter.format(selectedCalendar.getTime());
            loadFeesStatus(); // Reload data when month changes
        });
        newFragment.show(getSupportFragmentManager(), "MonthYearPicker");
    }

    private void loadFeesStatus() {
        if (clubName == null) {
            Toast.makeText(this, "Club information not available.", Toast.LENGTH_SHORT).show();
            tvNoStudents.setVisibility(View.VISIBLE);
            return;
        }

        // Get all students for the current club
        List<Student> students = studentDbHelper.getStudentsByClubName(clubName);

        if (students.isEmpty()) {
            tvNoStudents.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoStudents.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            adapter.updateData(students, selectedYearMonth, clubName); // Pass selected month and clubName
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentDbHelper != null) {
            studentDbHelper.close();
        }
        if (feesDbHelper != null) {
            feesDbHelper.close();
        }
    }

    // --- RecyclerView Adapter ---
    private class SimpleFeesAdapter extends RecyclerView.Adapter<SimpleFeesAdapter.ViewHolder> {

        private Context context;
        private List<Student> studentList;
        private String currentYearMonth; // Added to hold the filter month
        private String currentClubName;  // Added to hold the filter club name

        public SimpleFeesAdapter(Context context) {
            this.context = context;
        }

        // Modified updateData to accept filter parameters
        public void updateData(List<Student> students, String yearMonth, String clubName) {
            this.studentList = students;
            this.currentYearMonth = yearMonth;
            this.currentClubName = clubName;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_simple_fees_student, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Student student = studentList.get(position);
            holder.tvStudentName.setText(student.getName());
            holder.tvStudentId.setText(student.getId());

            // NOW, check fee status for the specific month/year and a default fee type (e.g., "Monthly")
            // Make sure your FeesDbHelper has this method:
            // public boolean hasFeeBeenPaidForMonth(String studentId, String feeType, String clubName, String yearMonth)
            boolean hasPaid = feesDbHelper.hasFeeBeenPaidForMonth(
                    student.getId(), getString(R.string.fee_type_monthly), currentClubName, currentYearMonth); // Defaulting to "Monthly" fee type

            if (hasPaid) {
                holder.tvFeeStatus.setText("Paid");
                holder.tvFeeStatus.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_status_paid));
                holder.tvFeeStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                holder.tvFeeStatus.setText("Unpaid");
                holder.tvFeeStatus.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_status_unpaid));
                holder.tvFeeStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
            }
        }

        @Override
        public int getItemCount() {
            return studentList != null ? studentList.size() : 0;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvStudentId, tvFeeStatus;

            public ViewHolder(View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.tv_student_name);
                tvStudentId = itemView.findViewById(R.id.tv_student_id);
                tvFeeStatus = itemView.findViewById(R.id.tv_fee_status);
            }
        }
    }
}