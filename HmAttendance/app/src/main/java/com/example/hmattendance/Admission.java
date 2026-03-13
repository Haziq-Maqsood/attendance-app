package com.example.hmattendance; // Adjust package name

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager; // Added import
import android.graphics.Bitmap;
import android.graphics.BitmapFactory; // Added import
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler; // Added import
import android.os.Looper; // Added import
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat; // Added import
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.hmattendance.models.Student;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream; // Added import
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService; // Added import
import java.util.concurrent.Executors; // Added import

import de.hdodenhof.circleimageview.CircleImageView;

public class Admission extends AppCompatActivity {

    // UI Elements
    private Toolbar toolbar;
    private ImageView backButton;
    private TextView toolbarTitle;
    private CircleImageView ivStudentImage;
    private TextView tvStudentId;
    private TextInputEditText etStudentName, etFatherName, etAge, etAdmissionDate, etEmail, etPhone, etAddress;
    private RadioGroup rgGender;
    private Spinner spinnerBelt;
    private Button btnSubmitAdmission, btnClearForm;

    // Data
    private StudentDbHelper studentDbHelper;
    private String selectedImagePath;
    private Uri currentPhotoUri; // For camera capture output
    private String currentStudentId;

    // SharedPreferences variables for club info
    private String clubName;
    private String clubinitials;

    // SharedPreferences constants (ensure these match LoginActivity)
    private static final String APP_PREFS = "app_prefs";
    private static final String KEY_USER_EMAIL = "email";
    private static final String KEY_CLUB_INITIALS = "club_initials";

    // Activity Result Launchers for Image Selection
    // Renamed for clarity and to use TakePicture contract
    private ActivityResultLauncher<Uri> takePictureLauncher; // Uses TakePicture contract directly
    private ActivityResultLauncher<Intent> pickImageLauncher; // Uses StartActivityForResult for gallery

    // Launcher for requesting multiple permissions
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;


    // Compression parameters for saving to file
    private static final int IMAGE_COMPRESSION_QUALITY = 80;

    // Target size for downsampling images (e.g., for display in ImageView)
    private static final int TARGET_WIDTH = 500;
    private static final int TARGET_HEIGHT = 500;

    // Executor for background tasks
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Handler to post results back to the UI thread
    private final Handler handler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admission);

        // --- Retrieve Club Info from SharedPreferences ---
        SharedPreferences prefs = getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE);

        clubName = prefs.getString(KEY_USER_EMAIL, null);
        clubinitials = prefs.getString(KEY_CLUB_INITIALS, null);

        if (clubName == null || clubinitials == null || clubinitials.isEmpty()) {
            Toast.makeText(this, "Error: Club information incomplete. Please re-login.", Toast.LENGTH_LONG).show();
            Log.e("Admission", "Club user logged in but clubName or clubInitials is null/empty.");
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
            toolbarTitle.setText(R.string.admission_form_title);
        }

        backButton = findViewById(R.id.back_button);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
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
        ivStudentImage = findViewById(R.id.iv_student_image);
        tvStudentId = findViewById(R.id.tv_student_id);
        etStudentName = findViewById(R.id.et_student_name);
        etFatherName = findViewById(R.id.et_father_name);
        etAge = findViewById(R.id.et_age);
        etAdmissionDate = findViewById(R.id.et_admission_date);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        rgGender = findViewById(R.id.rg_gender);
        spinnerBelt = findViewById(R.id.spinner_belt);
        btnSubmitAdmission = findViewById(R.id.btn_submit_admission);
        btnClearForm = findViewById(R.id.btn_clear_form);

        // --- Initialize Database Helper ---
        studentDbHelper = new StudentDbHelper(this);

        // --- Pre-generate and display Student ID ---
        currentStudentId = studentDbHelper.generateStudentId(clubName, clubinitials);
        if (currentStudentId == null) {
            Toast.makeText(this, "Failed to generate student ID. Check club data.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        tvStudentId.setText(getString(R.string.student_id_placeholder, currentStudentId));

        // --- Setup Image Selection ---
        setupImageSelectionLaunchers(); // Setup launchers first
        setupPermissionLauncher(); // Setup permission launcher
        ivStudentImage.setOnClickListener(v -> checkPermissionsAndShowDialog()); // Call permission check first

        // Set default image and clear any previously selected path
        ivStudentImage.setImageResource(R.drawable.user);
        selectedImagePath = null;

        // --- Setup Admission Date Picker ---
        etAdmissionDate.setOnClickListener(v -> showDatePickerDialog());
        etAdmissionDate.setFocusable(false);
        etAdmissionDate.setKeyListener(null);

        // --- Setup Belt Spinner ---
        ArrayAdapter<CharSequence> beltAdapter = ArrayAdapter.createFromResource(
                this, R.array.belt_levels, android.R.layout.simple_spinner_item);
        beltAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBelt.setAdapter(beltAdapter);

        // --- Set Button Listeners ---
        btnSubmitAdmission.setOnClickListener(v -> validateAndSaveStudent());
        btnClearForm.setOnClickListener(v -> clearForm());
    }

    /**
     * Sets up ActivityResultLaunchers for picking images from gallery and taking pictures.
     */
    private void setupImageSelectionLaunchers() {
        // For picking an image from the gallery
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handlePickedImage(imageUri);
                        }
                    } else {
                        Toast.makeText(this, "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });

        // For taking a picture with the camera (using TakePicture contract directly)
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        if (currentPhotoUri != null) {
                            handleCapturedImage(currentPhotoUri);
                        } else {
                            Toast.makeText(this, "Failed to capture image: URI not set.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Photo capture cancelled or failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Sets up the ActivityResultLauncher for requesting permissions.
     */
    private void setupPermissionLauncher() {
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean cameraGranted = result.getOrDefault(android.Manifest.permission.CAMERA, false);
                    Boolean readStorageGranted = result.getOrDefault(android.Manifest.permission.READ_EXTERNAL_STORAGE, false);
                    // For API 33 (Android 13) and above, READ_MEDIA_IMAGES is preferred over READ_EXTERNAL_STORAGE for images.
                    // For simplicity, we'll stick to READ_EXTERNAL_STORAGE here for broader compatibility,
                    // but be aware of newer API changes. If targeting API 33+, also check READ_MEDIA_IMAGES.
                    // Boolean readMediaImagesGranted = result.getOrDefault(android.Manifest.permission.READ_MEDIA_IMAGES, false);

                    if (cameraGranted != null && cameraGranted && readStorageGranted != null && readStorageGranted) {
                        // All necessary permissions granted, proceed to show the dialog
                        showImagePickerDialog();
                    } else {
                        // Permissions denied, inform the user
                        Toast.makeText(this, "Camera and storage permissions are required to select or capture images.", Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    /**
     * Checks if necessary permissions are granted, if not, requests them.
     * Otherwise, shows the image picker dialog.
     */
    private void checkPermissionsAndShowDialog() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean readStoragePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        // Add more specific permission checks for Android 13+ (API 33+) if needed:
        // boolean readMediaImagesPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;

        if (cameraPermission && readStoragePermission) { // Add readMediaImagesPermission if checking for API 33+
            // Permissions are already granted, show the dialog directly
            showImagePickerDialog();
        } else {
            // Request permissions
            requestPermissionsLauncher.launch(new String[]{
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                    // Add android.Manifest.permission.READ_MEDIA_IMAGES for API 33+
            });
        }
    }


    /**
     * Shows a dialog to let the user choose between taking a picture or picking from gallery.
     * This method is now called AFTER permissions are checked/granted.
     */
    private void showImagePickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_image_source)
                .setItems(new CharSequence[]{getString(R.string.option_camera), getString(R.string.option_gallery)}, (dialog, which) -> {
                    if (which == 0) { // Camera
                        dispatchTakePictureIntent(); // Launch camera intent
                    } else { // Gallery
                        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pickImageLauncher.launch(pickPhotoIntent);
                    }
                })
                .show();
    }

    /**
     * Launches the camera app to take a picture.
     */
    private void dispatchTakePictureIntent() {
        File photoFile = null;
        try {
            photoFile = createImageFile(); // Create a file to save the image
        } catch (IOException ex) {
            Log.e("AdmissionForm", "Error creating image file: " + ex.getMessage());
            Toast.makeText(this, "Error creating image file.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            currentPhotoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureLauncher.launch(currentPhotoUri); // Use the TakePicture launcher
        }
    }


    /**
     * Handles the image picked from the gallery on a background thread.
     */
    private void handlePickedImage(Uri imageUri) {
        executor.execute(() -> {
            Bitmap pickedBitmap = null;
            InputStream inputStream = null;
            String savedPath = null;
            try {
                // Get image dimensions for downsampling without loading full bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    BitmapFactory.decodeStream(inputStream, null, options);
                    inputStream.close(); // Close after reading bounds
                }

                // Re-open stream for actual decoding
                inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    // Calculate inSampleSize
                    options.inSampleSize = calculateInSampleSize(options, TARGET_WIDTH, TARGET_HEIGHT);
                    options.inJustDecodeBounds = false; // Now decode pixels

                    pickedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                }

                if (pickedBitmap != null) {
                    savedPath = saveBitmapToFile(pickedBitmap); // Save the scaled bitmap
                }

            } catch (IOException e) {
                Log.e("AdmissionForm", "Error loading picked image: " + e.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            final Bitmap bitmapToDisplay = pickedBitmap;
            final String finalSavedPath = savedPath;

            // Post results back to the UI thread
            handler.post(() -> {
                if (bitmapToDisplay != null && finalSavedPath != null) {
                    selectedImagePath = finalSavedPath;
                    ivStudentImage.setImageBitmap(bitmapToDisplay);
                    Toast.makeText(Admission.this, "Image selected and saved.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Admission.this, "Failed to load or save selected image.", Toast.LENGTH_SHORT).show();
                    ivStudentImage.setImageResource(R.drawable.user);
                    selectedImagePath = null;
                }
            });
        });
    }

    /**
     * Handles the image captured by the camera on a background thread.
     */
    private void handleCapturedImage(Uri imageUri) {
        executor.execute(() -> {
            Bitmap capturedBitmap = null;
            InputStream inputStream = null;
            String savedPath = null;
            try {
                // Get image dimensions for downsampling without loading full bitmap
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    BitmapFactory.decodeStream(inputStream, null, options);
                    inputStream.close(); // Close after reading bounds
                }

                // Re-open stream for actual decoding
                inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    // Calculate inSampleSize
                    options.inSampleSize = calculateInSampleSize(options, TARGET_WIDTH, TARGET_HEIGHT);
                    options.inJustDecodeBounds = false; // Now decode pixels

                    capturedBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                }

                if (capturedBitmap != null) {
                    savedPath = saveBitmapToFile(capturedBitmap); // Save the scaled bitmap
                }

            } catch (IOException e) {
                Log.e("AdmissionForm", "Error loading captured image: " + e.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            final Bitmap bitmapToDisplay = capturedBitmap;
            final String finalSavedPath = savedPath;

            // Post results back to the UI thread
            handler.post(() -> {
                if (bitmapToDisplay != null && finalSavedPath != null) {
                    selectedImagePath = finalSavedPath;
                    ivStudentImage.setImageBitmap(bitmapToDisplay);
                    Toast.makeText(Admission.this, "Photo captured and saved.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Admission.this, "Failed to process captured image.", Toast.LENGTH_SHORT).show();
                    ivStudentImage.setImageResource(R.drawable.user);
                    selectedImagePath = null;
                }
            });
        });
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

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than or equal to the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    /**
     * Creates a temporary image file in the app's private external storage.
     * @return The File object for the new image.
     * @throws IOException If the file cannot be created.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs(); // Ensure directory exists
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    /**
     * Saves a Bitmap to an external app-specific file and returns its absolute path.
     * This method also handles image compression.
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
        String imageFileName = "IMG_" + currentStudentId + "_" + timeStamp + ".jpg";
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
     * Shows a DatePickerDialog for selecting the admission date.
     */
    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%d-%02d-%02d",
                            selectedYear, (selectedMonth + 1), selectedDay);
                    etAdmissionDate.setText(formattedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    /**
     * Validates form inputs and saves the student record to the database.
     */
    private void validateAndSaveStudent() {
        String name = etStudentName.getText().toString().trim();
        String fatherName = etFatherName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String admissionDate = etAdmissionDate.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        String gender = "";
        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedGenderId);
            gender = selectedRadioButton.getText().toString();
        }

        String belt = spinnerBelt.getSelectedItem().toString();
        if (belt.equals(getString(R.string.select_belt_hint))) {
            belt = "";
        }

        boolean isValid = true;
        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            Toast.makeText(this, "Please select a student image.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        if (name.isEmpty()) { etStudentName.setError("Name is required"); isValid = false; }
        if (fatherName.isEmpty()) { etFatherName.setError("Father's Name is required"); isValid = false; }
        if (ageStr.isEmpty()) { etAge.setError("Age is required"); isValid = false; }
        if (admissionDate.isEmpty()) { etAdmissionDate.setError("Admission Date is required"); isValid = false; }
        if (gender.isEmpty()) { Toast.makeText(this, "Please select a gender.", Toast.LENGTH_SHORT).show(); isValid = false; }
        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address"); isValid = false;
        }
        if (!phone.isEmpty() && !phone.matches("\\d{10,15}")) {
            etPhone.setError("Enter a valid phone number"); isValid = false;
        }

        if (isValid) {
            int age = Integer.parseInt(ageStr);

            Student student = new Student(
                    selectedImagePath,
                    name,
                    fatherName,
                    age,
                    admissionDate,
                    gender,
                    belt,
                    email,
                    phone,
                    address,
                    clubName
            );

            String addedId = studentDbHelper.addStudent(student, clubinitials);

            if (addedId != null) {
                Toast.makeText(this, "Student " + name + " added successfully with ID: " + addedId, Toast.LENGTH_LONG).show();
                clearForm();
                currentStudentId = studentDbHelper.generateStudentId(clubName, clubinitials);
                tvStudentId.setText(getString(R.string.student_id_placeholder, currentStudentId));
            } else {
                Toast.makeText(this, "Failed to add student. Please try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please correct the highlighted fields.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Clears all input fields and resets the form to its initial state.
     */
    private void clearForm() {
        ivStudentImage.setImageResource(R.drawable.user);
        selectedImagePath = null;

        etStudentName.setText("");
        etFatherName.setText("");
        etAge.setText("");
        etAdmissionDate.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etAddress.setText("");
        rgGender.clearCheck();
        spinnerBelt.setSelection(0);

        etStudentName.setError(null);
        etFatherName.setError(null);
        etAge.setError(null);
        etAdmissionDate.setError(null);
        etEmail.setError(null);
        etPhone.setError(null);
        etAddress.setError(null);

        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            currentFocus.clearFocus();
        }
        findViewById(R.id.main).requestFocus();
        Toast.makeText(this, "Form cleared.", Toast.LENGTH_SHORT).show();
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