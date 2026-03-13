package com.example.hmattendance;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap; // Added import
import android.graphics.BitmapFactory; // Added import
import android.net.Uri; // Added import
import android.os.Bundle;
import android.os.Handler; // Added import
import android.os.Looper; // Added import
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
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

import com.example.hmattendance.models.Student;
import com.example.hmattendance.models.BeltCount;
import com.example.hmattendance.StudentDbHelper;
import com.example.hmattendance.ClubDbHelper;

import java.io.File; // Added import
import java.io.InputStream; // Added import
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService; // Added import
import java.util.concurrent.Executors; // Added import

import de.hdodenhof.circleimageview.CircleImageView; // Added import

public class AllStudentsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private RecyclerView rvStudents;
    private TextView tvNoRecordsFound;
    private Spinner spinnerClubFilter;
    private RecyclerView rvBeltCounts;

    private StudentDbHelper studentDbHelper;
    private ClubDbHelper clubDbHelper;
    private StudentAdapter studentAdapter;
    private BeltCountAdapter beltCountAdapter;

    private String currentSelectedClub = null;

    // Executor for background tasks for image loading in RecyclerView
    private final ExecutorService executor = Executors.newFixedThreadPool(4); // Use a thread pool
    // Handler to post results back to the UI thread for image loading
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_students);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.all_students_title);
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
        rvStudents = findViewById(R.id.rv_students);
        tvNoRecordsFound = findViewById(R.id.tv_no_records_found);
        spinnerClubFilter = findViewById(R.id.spinner_club_filter);
        rvBeltCounts = findViewById(R.id.rv_belt_counts);

        // --- Initialize Database Helpers ---
        studentDbHelper = new StudentDbHelper(this);
        clubDbHelper = new ClubDbHelper(this);

        // --- Setup Student RecyclerView ---
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        // Pass executor and handler to the adapter for background image loading
        studentAdapter = new StudentAdapter(this, new ArrayList<>(), executor, handler);
        rvStudents.setAdapter(studentAdapter);

        // --- Setup Belt Counts RecyclerView ---
        LinearLayoutManager beltLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvBeltCounts.setLayoutManager(beltLayoutManager);
        beltCountAdapter = new BeltCountAdapter(this, new ArrayList<>());
        rvBeltCounts.setAdapter(beltCountAdapter);

        // --- Load Club Names and Setup Filter Spinner ---
        loadClubNamesForFilter();

        // Initial load of students and belt counts (for "All Clubs")
        loadStudents(currentSelectedClub);
        updateBeltCounts(currentSelectedClub);
    }

    private void loadClubNamesForFilter() {
        List<String> rawClubNames = clubDbHelper.getAllClubNames();
        Collections.sort(rawClubNames);

        List<String> spinnerItemsWithCounts = new ArrayList<>();

        int totalStudents = studentDbHelper.getTotalStudentCount();
        spinnerItemsWithCounts.add(getString(R.string.select_all_clubs) + " (" + totalStudents + ")");

        for (String clubName : rawClubNames) {
            int studentCount = studentDbHelper.getStudentCountForClub(clubName);
            spinnerItemsWithCounts.add(clubName + " (" + studentCount + ")");
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                spinnerItemsWithCounts
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerClubFilter.setAdapter(spinnerAdapter);

        spinnerClubFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemFullText = parent.getItemAtPosition(position).toString();

                String extractedClubName;
                if (selectedItemFullText.startsWith(getString(R.string.select_all_clubs))) {
                    extractedClubName = null;
                } else {
                    int parenIndex = selectedItemFullText.indexOf(" (");
                    if (parenIndex != -1) {
                        extractedClubName = selectedItemFullText.substring(0, parenIndex);
                    } else {
                        extractedClubName = selectedItemFullText;
                        Log.w("AllStudentsActivity", "Unexpected spinner item format: " + selectedItemFullText);
                    }
                }
                currentSelectedClub = extractedClubName;

                loadStudents(currentSelectedClub);
                updateBeltCounts(currentSelectedClub);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void loadStudents(String clubName) {
        List<Student> students;
        if (clubName == null) {
            students = studentDbHelper.getAllStudents();
            toolbarTitle.setText(R.string.all_students_title);
        } else {
            students = studentDbHelper.getStudentsByClubName(clubName);
            toolbarTitle.setText(clubName);
        }

        if (students.isEmpty()) {
            tvNoRecordsFound.setVisibility(View.VISIBLE);
            rvStudents.setVisibility(View.GONE);
        } else {
            tvNoRecordsFound.setVisibility(View.GONE);
            rvStudents.setVisibility(View.VISIBLE);
            studentAdapter.updateStudentList(students);
        }
    }

    private void updateBeltCounts(String clubName) {
        Map<String, Integer> beltCounts = new LinkedHashMap<>();

        // IMPORTANT: Define your actual belt order and names here using string resources.
        // Ensure these match the belts used during student admission.
        beltCounts.put(getString(R.string.belt_white), 0);
        beltCounts.put(getString(R.string.belt_yellow), 0);
        beltCounts.put(getString(R.string.belt_green), 0);
        beltCounts.put(getString(R.string.belt_blue), 0);
        beltCounts.put(getString(R.string.belt_red), 0);
        beltCounts.put(getString(R.string.belt_black), 0);
        // Add all your belt types here in the desired display order, e.g.,
        // beltCounts.put(getString(R.string.belt_orange), 0); etc.

        List<Student> studentsToCount;
        if (clubName == null) {
            studentsToCount = studentDbHelper.getAllStudents();
        } else {
            studentsToCount = studentDbHelper.getStudentsByClubName(clubName);
        }

        for (Student student : studentsToCount) {
            String belt = student.getBelt();
            if (beltCounts.containsKey(belt)) {
                beltCounts.put(belt, beltCounts.get(belt) + 1);
            } else {
                // If a student has a belt not predefined, add it to the map for counting
                Log.w("AllStudentsActivity", "Unknown belt: " + belt + " for student " + student.getName() + ". Adding to counts.");
                beltCounts.put(belt, 1);
            }
        }

        List<BeltCount> countsList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : beltCounts.entrySet()) {
            countsList.add(new BeltCount(entry.getKey(), entry.getValue()));
        }
        beltCountAdapter.updateData(countsList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload club names in case new clubs were added or students count changed
        loadClubNamesForFilter();
        // Reload student data and belt counts
        loadStudents(currentSelectedClub);
        updateBeltCounts(currentSelectedClub);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentDbHelper != null) {
            studentDbHelper.close();
        }
        if (clubDbHelper != null) {
            clubDbHelper.close();
        }
        // Shut down the executor when the activity is destroyed
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    // --- StudentAdapter (using item_student_all.xml now) ---
    private class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
        private Context context;
        private List<Student> studentList;
        private final ExecutorService adapterExecutor; // Executor passed from activity
        private final Handler adapterHandler; // Handler passed from activity

        // Target size for images in RecyclerView items
        private static final int IMAGE_TARGET_SIZE = 72; // Matches your XML width/height

        public StudentAdapter(Context context, List<Student> studentList, ExecutorService executor, Handler handler) {
            this.context = context;
            this.studentList = studentList;
            this.adapterExecutor = executor;
            this.adapterHandler = handler;
        }

        public void updateStudentList(List<Student> newStudents) {
            this.studentList = newStudents;
            notifyDataSetChanged();
        }

        @Override
        public StudentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_student_all, parent, false);
            return new StudentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(StudentViewHolder holder, int position) {
            Student student = studentList.get(position);
            holder.tvStudentName.setText(student.getName());
            holder.tvStudentId.setText(context.getString(R.string.student_id_placeholder, student.getId())); // Format ID
            holder.tvStudentClub.setText(context.getString(R.string.club_name_format, student.getClubName())); // Format Club Name
            holder.tvStudentBelt.setText(context.getString(R.string.belt_placeholder21, student.getBelt()));
            if (student.getBelt() != null) {
                switch (student.getBelt()) {
                    case "White Belt":
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
                        break;
                    case "Yellow Belt":
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.yellow));
                        break;
                    case "Green Belt":
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
                        break;
                    case "Blue Belt":
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.blue));
                        break;
                    case "Red Belt":
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
                        break;
                    case "Black Belt":
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.black));
                        break;
                    default:
                        holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
                        break;
                }
            } else {
                holder.tvStudentBelt.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_text_color));
            }// Format Belt

            // Load student image
            loadAndDisplayImage(student.getImagePath(), holder.ivStudentItemImage);

            // You can also add an OnClickListener to the whole item here if needed
            // holder.itemView.setOnClickListener(v -> {
            //     Intent intent = new Intent(context, StudentDetailActivity.class);
            //     intent.putExtra("student_id", student.getId());
            //     context.startActivity(intent);
            // });
        }

        @Override
        public int getItemCount() {
            return studentList.size();
        }

        public class StudentViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvStudentId, tvStudentClub, tvStudentBelt;
            CircleImageView ivStudentItemImage; // Declared CircleImageView

            public StudentViewHolder(View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.tv_student_name);
                tvStudentId = itemView.findViewById(R.id.tv_student_id);
                tvStudentClub = itemView.findViewById(R.id.tv_student_club);
                tvStudentBelt = itemView.findViewById(R.id.tv_student_belt);
                ivStudentItemImage = itemView.findViewById(R.id.iv_student_item_image); // Initialized CircleImageView
            }
        }

        /**
         * Loads and displays an image asynchronously with downsampling.
         * Sets a default user icon if the path is null, empty, or loading fails.
         */
        private void loadAndDisplayImage(String imagePath, CircleImageView imageView) {
            // Set a default image immediately to prevent flicker or show old images
            imageView.setImageResource(R.drawable.user);

            if (imagePath == null || imagePath.isEmpty()) {
                return; // Nothing to load, default image already set
            }

            // Get the target dimensions (use the actual dimensions of the CircleImageView if possible)
            // For now, using a fixed size (72dp from XML)
            final int reqWidth = IMAGE_TARGET_SIZE;
            final int reqHeight = IMAGE_TARGET_SIZE;

            adapterExecutor.execute(() -> {
                Bitmap decodedBitmap = null;
                File imgFile = new File(imagePath);

                if (imgFile.exists()) {
                    try {
                        // Decode image with inJustDecodeBounds to get dimensions
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                        // Calculate inSampleSize
                        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

                        // Decode bitmap with inSampleSize set
                        options.inJustDecodeBounds = false;
                        decodedBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                    } catch (Exception e) {
                        Log.e("StudentAdapter", "Error decoding bitmap: " + imagePath, e);
                    }
                } else {
                    Log.w("StudentAdapter", "Image file not found: " + imagePath);
                }

                final Bitmap finalBitmap = decodedBitmap;
                adapterHandler.post(() -> {
                    if (finalBitmap != null) {
                        imageView.setImageBitmap(finalBitmap);
                    } else {
                        imageView.setImageResource(R.drawable.user); // Fallback to default if loading failed
                    }
                });
            });
        }

        /**
         * Helper method to calculate inSampleSize for bitmap downsampling.
         * Reused from Admission.java / UpdatStudent.java context.
         */
        private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {
                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2;
                }
            }
            return inSampleSize;
        }
    }

    // --- BeltCountAdapter (unchanged) ---
    private class BeltCountAdapter extends RecyclerView.Adapter<BeltCountAdapter.BeltCountViewHolder> {
        private Context context;
        private List<BeltCount> beltCounts;

        public BeltCountAdapter(Context context, List<BeltCount> beltCounts) {
            this.context = context;
            this.beltCounts = beltCounts;
        }

        public void updateData(List<BeltCount> newCounts) {
            this.beltCounts = newCounts;
            notifyDataSetChanged();
        }

        @Override
        public BeltCountViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_belt_count, parent, false);
            return new BeltCountViewHolder(view);
        }

        @Override
        public void onBindViewHolder(BeltCountViewHolder holder, int position) {
            BeltCount belt = beltCounts.get(position);
            holder.tvBeltName.setText(belt.getBeltName());
            holder.tvBeltCount.setText(String.valueOf(belt.getCount()));
        }

        @Override
        public int getItemCount() {
            return beltCounts.size();
        }

        public class BeltCountViewHolder extends RecyclerView.ViewHolder {
            TextView tvBeltName, tvBeltCount;

            public BeltCountViewHolder(View itemView) {
                super(itemView);
                tvBeltName = itemView.findViewById(R.id.tv_belt_name);
                tvBeltCount = itemView.findViewById(R.id.tv_belt_count);
            }
        }
    }
}