package com.example.hmattendance;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hmattendance.models.Student;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Executors;     // Import Executors

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdatStudent extends AppCompatActivity {

    // UI Elements
    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private TextInputEditText etSearchStudentIdInput;
    private Button btnSearchStudent;
    private CardView cardStudentDetailsEdit;

    // Student Detail Fields
    private CircleImageView ivStudentImage;
    private TextInputEditText etStudentName, etFatherName, etAge, etEmail, etStudentPhone, etStudentAddress, etAdmissionDate;
    private TextView tvGenderDisplay;
    private Spinner spinnerBelt;
    private Button btnCancelUpdate, btnSaveUpdate;

    // Data
    private StudentDbHelper studentDbHelper;
    private Student currentStudent = null;
    private String selectedBelt = "";
    private String selectedImagePath = null;

    // Compression parameter
    private static final int IMAGE_COMPRESSION_QUALITY = 80;

    // Target size for downsampling images
    private static final int TARGET_WIDTH = 500;
    private static final int TARGET_HEIGHT = 500;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Or newFixedThreadPool(2) for more concurrent tasks
    // Handler to post results back to the UI thread
    private final Handler handler = new Handler(Looper.getMainLooper());


    // ActivityResultLauncher for image selection
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // Use the ExecutorService to perform background work
                        executor.execute(() -> {
                            Bitmap originalBitmap = null;
                            InputStream inputStream = null;
                            try {
                                inputStream = getContentResolver().openInputStream(imageUri);
                                if (inputStream != null) {
                                    // --- Downsampling logic for the selected image ---
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    BitmapFactory.decodeStream(inputStream, null, options); // Decode bounds only

                                    // Close and re-open the stream for actual decode
                                    inputStream.close();
                                    inputStream = getContentResolver().openInputStream(imageUri);

                                    // Calculate inSampleSize based on target size for display
                                    options.inSampleSize = calculateInSampleSize(options, TARGET_WIDTH, TARGET_HEIGHT);
                                    options.inJustDecodeBounds = false; // Now decode pixels

                                    originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                                    // --- End of downsampling logic ---
                                }

                            } catch (IOException e) {
                                Log.e("UpdatStudent", "Error loading image from gallery: " + e.getMessage());
                            } finally {
                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (IOException e) {
                                        Log.e("UpdatStudent", "Error closing input stream: " + e.getMessage());
                                    }
                                }
                            }

                            final Bitmap bitmapToDisplay = originalBitmap;
                            final String savedPath;
                            if (bitmapToDisplay != null) {
                                savedPath = saveBitmapToFile(bitmapToDisplay); // Save the (potentially scaled) bitmap
                            } else {
                                savedPath = null;
                            }

                            // Post results back to the UI thread using the Handler
                            handler.post(() -> {
                                if (bitmapToDisplay != null && savedPath != null) {
                                    selectedImagePath = savedPath;
                                    ivStudentImage.setImageBitmap(bitmapToDisplay);
                                    Toast.makeText(UpdatStudent.this, "Image selected and saved.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(UpdatStudent.this, "Failed to load or save selected image.", Toast.LENGTH_SHORT).show();
                                    ivStudentImage.setImageResource(R.drawable.user);
                                    selectedImagePath = null;
                                }
                            });
                        });
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_updat_student);

        // --- Initialize Toolbar ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbarTitle = findViewById(R.id.toolbar_title);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.update_student_title);
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
        etSearchStudentIdInput = findViewById(R.id.et_search_student_id_input);
        btnSearchStudent = findViewById(R.id.btn_search_student);
        cardStudentDetailsEdit = findViewById(R.id.card_student_details_edit);

        // Student Detail Fields Initialization
        ivStudentImage = findViewById(R.id.iv_student_image);
        etStudentName = findViewById(R.id.et_student_name);
        etFatherName = findViewById(R.id.et_father_name);
        etAge = findViewById(R.id.et_age);
        etEmail = findViewById(R.id.et_email);
        etStudentPhone = findViewById(R.id.et_phone);
        etStudentAddress = findViewById(R.id.et_address);
        etAdmissionDate = findViewById(R.id.et_admission_date);

        tvGenderDisplay = findViewById(R.id.tv_gender_display);
        spinnerBelt = findViewById(R.id.spinner_belt);

        btnCancelUpdate = findViewById(R.id.btn_cancel_update);
        btnSaveUpdate = findViewById(R.id.btn_save_update);

        // --- Initialize Database Helper ---
        studentDbHelper = new StudentDbHelper(this);

        // --- Set Listeners ---
        btnSearchStudent.setOnClickListener(v -> searchStudentToUpdate());
        btnSaveUpdate.setOnClickListener(v -> saveUpdatedStudent());
        btnCancelUpdate.setOnClickListener(v -> clearForm());

        // --- Set Listener for Image selection ---
        ivStudentImage.setOnClickListener(v -> pickImage());

        // --- Setup Belt Spinner (This remains editable) ---
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.belt_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBelt.setAdapter(adapter);

        spinnerBelt.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBelt = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBelt = ""; // Or set to a default like "White"
            }
        });
    }

    /**
     * Launches an intent to pick an image from the gallery.
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    /**
     * Saves a Bitmap to an external app-specific file and returns its absolute path.
     * This method also handles image compression by using a defined quality.
     *
     * @param bitmap The Bitmap to save.
     * @return The absolute path to the saved file, or null if saving fails.
     */
    private String saveBitmapToFile(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }

        File mediaStorageDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (mediaStorageDir != null && !mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("ImageStorage", "Failed to create directory: " + mediaStorageDir.getAbsolutePath());
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File imageFile = new File(mediaStorageDir.getPath() + File.separator + imageFileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_QUALITY, fos);
            fos.flush();
            Log.d("ImageStorage", "Image saved to: " + imageFile.getAbsolutePath());
            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e("ImageStorage", "Error saving image to file: " + e.getMessage(), e);
            return null;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method to calculate inSampleSize for bitmap downsampling.
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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


    /**
     * Searches for a student by ID and populates the editable fields.
     */
    private void searchStudentToUpdate() {
        String studentId = etSearchStudentIdInput.getText().toString().trim();

        if (TextUtils.isEmpty(studentId)) {
            etSearchStudentIdInput.setError(getString(R.string.error_empty_search_id));
            cardStudentDetailsEdit.setVisibility(View.GONE);
            return;
        }

        currentStudent = studentDbHelper.getStudentById(studentId);

        if (currentStudent != null) {
            etStudentName.setText(currentStudent.getName());
            etFatherName.setText(currentStudent.getFatherName());
            etAge.setText(String.valueOf(currentStudent.getAge()));
            etEmail.setText(currentStudent.getEmail());
            etStudentPhone.setText(currentStudent.getPhone());
            etStudentAddress.setText(currentStudent.getAddress());
            etAdmissionDate.setText(currentStudent.getAdmissionDate());
            tvGenderDisplay.setText(currentStudent.getGender());

            String[] beltLevels = getResources().getStringArray(R.array.belt_levels);
            int spinnerPosition = 0;
            if (currentStudent.getBelt() != null) {
                for (int i = 0; i < beltLevels.length; i++) {
                    if (beltLevels[i].equalsIgnoreCase(currentStudent.getBelt())) {
                        spinnerPosition = i;
                        break;
                    }
                }
            }
            spinnerBelt.setSelection(spinnerPosition);
            selectedBelt = beltLevels[spinnerPosition];

            // Load student image if available using ExecutorService
            String studentImagePath = currentStudent.getImagePath();
            if (studentImagePath != null && !studentImagePath.isEmpty()) {
                executor.execute(() -> {
                    Bitmap loadedBitmap = null;
                    String finalImagePath = studentImagePath; // Final variable for lambda
                    File imgFile = new File(finalImagePath);
                    if (imgFile.exists()) {
                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);

                            options.inSampleSize = calculateInSampleSize(options, TARGET_WIDTH, TARGET_HEIGHT);
                            options.inJustDecodeBounds = false;

                            loadedBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                        } catch (Exception e) {
                            Log.e("UpdatStudent", "Error decoding existing image: " + e.getMessage());
                        }
                    }

                    final Bitmap bitmapResult = loadedBitmap;
                    handler.post(() -> {
                        if (bitmapResult != null) {
                            ivStudentImage.setImageBitmap(bitmapResult);
                            selectedImagePath = finalImagePath; // Keep the existing path
                        } else {
                            ivStudentImage.setImageResource(R.drawable.user);
                            selectedImagePath = null;
                            Toast.makeText(UpdatStudent.this, "Existing image file not found or failed to load.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                ivStudentImage.setImageResource(R.drawable.user);
                selectedImagePath = null;
            }

            cardStudentDetailsEdit.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.student_found_for_update, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.student_not_found_update, Toast.LENGTH_SHORT).show();
            clearForm();
        }
    }

    /**
     * Saves the updated student information to the database.
     */
    private void saveUpdatedStudent() {
        if (currentStudent == null) {
            Toast.makeText(this, R.string.error_no_student_selected, Toast.LENGTH_SHORT).show();
            return;
        }

        String newName = etStudentName.getText().toString().trim();
        String newFatherName = etFatherName.getText().toString().trim();
        String newAgeStr = etAge.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        String newPhone = etStudentPhone.getText().toString().trim();
        String newAddress = etStudentAddress.getText().toString().trim();

        String originalAdmissionDate = currentStudent.getAdmissionDate();
        String originalGender = currentStudent.getGender();

        if (TextUtils.isEmpty(newName)) { etStudentName.setError("Name is required"); return; }
        if (TextUtils.isEmpty(newFatherName)) { etFatherName.setError("Father's Name is required"); return; }
        if (TextUtils.isEmpty(newAgeStr)) { etAge.setError("Age is required"); return; }
        if (TextUtils.isEmpty(selectedBelt) || selectedBelt.equals(getString(R.string.select_belt_hint))) {
            Toast.makeText(this, "Please select a belt.", Toast.LENGTH_SHORT).show(); return;
        }
        if (!newEmail.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Enter a valid email address"); return;
        }
        if (!newPhone.isEmpty() && !newPhone.matches("\\d{10,15}")) {
            etStudentPhone.setError("Enter a valid phone number"); return;
        }

        int newAge;
        try {
            newAge = Integer.parseInt(newAgeStr);
        } catch (NumberFormatException e) {
            etAge.setError(getString(R.string.error_invalid_age));
            return;
        }

        String studentOriginalClubName = currentStudent.getClubName();

        Student updatedStudent = new Student(
                currentStudent.getId(),
                selectedImagePath,
                newName,
                newFatherName,
                newAge,
                originalAdmissionDate,
                originalGender,
                selectedBelt,
                newEmail,
                newPhone,
                newAddress,
                studentOriginalClubName
        );

        // Database update can also be done on a background thread if it's slow,
        // but for now, we'll keep it synchronous here as image loading is the main culprit.
        boolean success = studentDbHelper.updateStudent(updatedStudent);

        if (success) {
            Toast.makeText(this, R.string.update_successful, Toast.LENGTH_SHORT).show();
            clearForm();
        } else {
            Toast.makeText(this, R.string.update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clears all form fields and hides the student details section.
     */
    private void clearForm() {
        etSearchStudentIdInput.setText("");
        etSearchStudentIdInput.setError(null);
        ivStudentImage.setImageResource(R.drawable.user);
        etStudentName.setText("");
        etFatherName.setText("");
        etAge.setText("");
        etEmail.setText("");
        etStudentPhone.setText("");
        etStudentAddress.setText("");
        etAdmissionDate.setText("");
        tvGenderDisplay.setText("");
        spinnerBelt.setSelection(0);
        selectedBelt = "";
        selectedImagePath = null;
        cardStudentDetailsEdit.setVisibility(View.GONE);
        currentStudent = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (studentDbHelper != null) {
            studentDbHelper.close();
        }
        // Shut down the executor when the activity is destroyed
        if (!executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}