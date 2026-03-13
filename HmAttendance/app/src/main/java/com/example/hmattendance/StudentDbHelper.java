package com.example.hmattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap; // Keep if Student model still uses Bitmap temporarily
import android.graphics.BitmapFactory; // Keep for external image loading

import android.util.Log;

import com.example.hmattendance.models.Student; // Assuming Student model is updated to use imagePath:String

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StudentDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "student_records.db";
    // *** IMPORTANT ***
    // ONLY INCREMENT THIS VERSION IF YOU HAVE SCHEMA CHANGES AND ARE OK WITH DATA WIPE.
    // FOR LOGIC CHANGES OR BUG FIXES, KEEP THE SAME VERSION TO PRESERVE DATA.
    // INCREMENTED TO 2 BECAUSE COL_IMAGE TYPE IS CHANGING FROM BLOB TO TEXT (image_path)
    private static final int DATABASE_VERSION = 1; // INCREMENTED VERSION

    public static final String TABLE_STUDENTS = "students";

    public static final String COL_ID = "id";
    public static final String COL_IMAGE_PATH = "image_path"; // Renamed for clarity, column name in DB will be 'image_path' now
    public static final String COL_NAME = "name";
    public static final String COL_FATHER_NAME = "father_name";
    public static final String COL_AGE = "age";
    public static final String COL_ADMISSION_DATE = "admission_date";
    public static final String COL_GENDER = "gender";
    public static final String COL_BELT = "belt";
    public static final String COL_EMAIL = "email";
    public static final String COL_PHONE = "phone";
    public static final String COL_ADDRESS = "address";
    public static final String COL_CLUB_NAME = "club_name";

    public static final String SQL_CREATE_STUDENTS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_STUDENTS + " (" +
            COL_ID + " TEXT PRIMARY KEY NOT NULL," +
            COL_IMAGE_PATH + " TEXT," + // CHANGED from BLOB to TEXT
            COL_NAME + " TEXT NOT NULL," +
            COL_FATHER_NAME + " TEXT NOT NULL," +
            COL_AGE + " INTEGER NOT NULL," +
            COL_ADMISSION_DATE + " TEXT NOT NULL," +
            COL_GENDER + " TEXT NOT NULL," +
            COL_BELT + " TEXT DEFAULT 'White'," +
            COL_EMAIL + " TEXT," +
            COL_PHONE + " TEXT," +
            COL_ADDRESS + " TEXT," +
            COL_CLUB_NAME + " TEXT NOT NULL" +
            ");";

    public StudentDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_STUDENTS_TABLE);
        db.execSQL(AttendanceDbHelper.SQL_CREATE_ATTENDANCE_TABLE); // Assuming these exist elsewhere
        db.execSQL(FeesDbHelper.SQL_CREATE_FEES_PAYMENTS_TABLE ); // Assuming these exist elsewhere
        Log.d("StudentDbHelper", "Students table created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("StudentDbHelper", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data.");
        // This will drop the existing table and recreate it with the new schema (image_path TEXT)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDENTS);
        onCreate(db);
    }

    @Override
    public synchronized void close() {
        super.close();
    }

    /**
     * Generates a unique student ID for a specific club using its pre-defined initials.
     * The ID format will be "CLUB_INITIALS-NUMBER" (e.g., "RC-1", "FH-2").
     *
     * @param clubName     The full name of the club (used for the COL_CLUB_NAME filter).
     * @param clubInitials The pre-defined initials of the club (e.g., "RC", "FH").
     * @return A unique student ID string.
     */
    public String generateStudentId(String clubName, String clubInitials) {
        SQLiteDatabase db = this.getReadableDatabase();
        int maxSuffix = 0;

        if (clubInitials == null || clubInitials.trim().isEmpty()) {
            Log.e("StudentDbHelper", "Cannot generate student ID: clubInitials is null or empty.");
            return null;
        }

        Log.d("StudentDbHelper", "generateStudentId: Input clubName='" + clubName + "', clubInitials='" + clubInitials + "'");

        int suffixStartIndex = clubInitials.length() + 2;

        String query = "SELECT MAX(CAST(SUBSTR(" + COL_ID + ", " + suffixStartIndex + ") AS INTEGER)) FROM " + TABLE_STUDENTS +
                " WHERE " + COL_CLUB_NAME + " = ? AND " + COL_ID + " LIKE ? || '-%'";

        String[] selectionArgs = new String[]{
                clubName,
                clubInitials
        };

        Log.d("StudentDbHelper", "generateStudentId Query: " + query);
        Log.d("StudentDbHelper", "generateStudentId Args: " + Arrays.toString(selectionArgs));

        try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                maxSuffix = cursor.getInt(0);
                Log.d("StudentDbHelper", "generateStudentId: Found maxSuffix=" + maxSuffix + " for club initials '" + clubInitials + "' and club name '" + clubName + "'");
            } else {
                Log.d("StudentDbHelper", "generateStudentId: No existing IDs found for club initials '" + clubInitials + "' and club name '" + clubName + "'. Starting with suffix 0.");
            }
        } catch (Exception e) {
            Log.e("StudentDbHelper", "ID generation error for club '" + clubName + "' (Initials: '" + clubInitials + "'): " + e.getMessage());
        }

        String nextId = clubInitials + "-" + (maxSuffix + 1);
        Log.d("StudentDbHelper", "generateStudentId: Returning next ID='" + nextId + "'");
        return nextId;
    }

    /**
     * Retrieves a single student record by their student ID.
     *
     * @param studentId The unique ID of the student.
     * @return A Student object if found, or null otherwise.
     */
    public Student getStudent(String studentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Student student = null;

        try {
            String selection = COL_ID + " = ?";
            String[] selectionArgs = {studentId};

            cursor = db.query(
                    TABLE_STUDENTS,
                    null, // Select all columns
                    selection,
                    selectionArgs,
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(COL_ID);
                int imagePathIndex = cursor.getColumnIndexOrThrow(COL_IMAGE_PATH); // NEW: Get image path index
                int nameIndex = cursor.getColumnIndexOrThrow(COL_NAME);
                int fatherNameIndex = cursor.getColumnIndexOrThrow(COL_FATHER_NAME);
                int ageIndex = cursor.getColumnIndexOrThrow(COL_AGE);
                int admissionDateIndex = cursor.getColumnIndexOrThrow(COL_ADMISSION_DATE);
                int genderIndex = cursor.getColumnIndexOrThrow(COL_GENDER);
                int beltIndex = cursor.getColumnIndexOrThrow(COL_BELT);
                int emailIndex = cursor.getColumnIndexOrThrow(COL_EMAIL);
                int phoneIndex = cursor.getColumnIndexOrThrow(COL_PHONE);
                int addressIndex = cursor.getColumnIndexOrThrow(COL_ADDRESS);
                int clubNameIndex = cursor.getColumnIndexOrThrow(COL_CLUB_NAME);


                String id = cursor.getString(idIndex);
                String imagePath = cursor.getString(imagePathIndex); // NEW: Get image path as String
                String name = cursor.getString(nameIndex);
                String fatherName = cursor.getString(fatherNameIndex);
                int age = cursor.getInt(ageIndex);
                String admissionDate = cursor.getString(admissionDateIndex);
                String gender = cursor.getString(genderIndex);
                String belt = cursor.getString(beltIndex);
                String email = cursor.getString(emailIndex);
                String phone = cursor.getString(phoneIndex);
                String address = cursor.getString(addressIndex);
                String clubName = cursor.getString(clubNameIndex);

                // Assuming Student constructor needs to be updated to accept imagePath (String) or handle it
                student = new Student(id, imagePath, name, fatherName, age, admissionDate, gender, belt, email, phone, address, clubName);
            }
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error getting student by ID: " + e.getMessage());
            student = null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return student;
    }

    public String addStudent(Student student, String clubInitials) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (student.getClubName() == null || student.getClubName().trim().isEmpty()) {
            Log.e("StudentDbHelper", "Error: Student cannot be added without a valid club name.");
            return null;
        }
        if (clubInitials == null || clubInitials.trim().isEmpty()) {
            Log.e("StudentDbHelper", "Error: Club initials missing for student ID generation.");
            return null;
        }

        Log.d("StudentDbHelper", "addStudent: Calling generateStudentId with clubName='" + student.getClubName() + "', clubInitials='" + clubInitials + "'");
        String generatedId = generateStudentId(student.getClubName(), clubInitials);

        if (generatedId == null) {
            Log.e("StudentDbHelper", "Failed to generate ID for student: " + student.getName());
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(COL_ID, generatedId);
        values.put(COL_IMAGE_PATH, student.getImagePath()); // CHANGED: Store image path
        values.put(COL_NAME, student.getName());
        values.put(COL_FATHER_NAME, student.getFatherName());
        values.put(COL_AGE, student.getAge());
        values.put(COL_ADMISSION_DATE, student.getAdmissionDate());
        values.put(COL_GENDER, student.getGender());

        if (student.getBelt() != null) {
            values.put(COL_BELT, student.getBelt());
        } else {
            values.put(COL_BELT, "White"); // Ensure a default belt if not provided
        }

        values.put(COL_EMAIL, student.getEmail());
        values.put(COL_PHONE, student.getPhone());
        values.put(COL_ADDRESS, student.getAddress());
        values.put(COL_CLUB_NAME, student.getClubName());

        long result = -1;
        try {
            result = db.insert(TABLE_STUDENTS, null, values);
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error inserting student '" + student.getName() + "' with ID '" + generatedId + "': " + e.getMessage());
        } finally {
            // No db.close() here as it's handled by SQLiteOpenHelper lifecycle or external close.
        }

        if (result == -1) {
            Log.e("StudentDbHelper", "Failed to add student: " + student.getName() + ". Possible ID conflict or other DB error.");
            return null;
        } else {
            Log.d("StudentDbHelper", "Student added successfully with ID: " + generatedId);
            student.setId(generatedId); // Update the student object with the generated ID
            return generatedId;
        }
    }

    public Student getStudentById(String id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Student student = null;

        try {
            cursor = db.query(TABLE_STUDENTS, null, COL_ID + " = ?", new String[]{id},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                student = new Student(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)), // CHANGED: Get image path as String
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_FATHER_NAME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_AGE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ADMISSION_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_BELT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME))
                );
            }
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error getting student by ID: " + id + " - " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return student;
    }

    public List<Student> getAllStudents() {
        List<Student> studentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_STUDENTS, null);

            if (cursor.moveToFirst()) {
                do {
                    Student student = new Student(
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)), // CHANGED: Get image path as String
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_FATHER_NAME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_AGE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_ADMISSION_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_BELT)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME))
                    );
                    studentList.add(student);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error getting all students: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return studentList;
    }

    public List<Student> getStudentsByClubName(String clubName) {
        List<Student> studentList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selection = COL_CLUB_NAME + " = ?";
            String[] selectionArgs = {clubName};

            cursor = db.query(
                    TABLE_STUDENTS,
                    null,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Student student = new Student(
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_IMAGE_PATH)), // CHANGED: Get image path as String
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_FATHER_NAME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_AGE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_ADMISSION_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_BELT)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_ADDRESS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME))
                    );
                    studentList.add(student);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error getting students by club name: " + clubName + " - " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return studentList;
    }

    /**
     * Counts the total number of students in the database.
     *
     * @return The total number of students.
     */
    public int getTotalStudentCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;

        try {
            count = (int) DatabaseUtils.queryNumEntries(db, TABLE_STUDENTS);
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error counting total students: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    /**
     * Counts the number of students belonging to a specific club.
     *
     * @param clubName The name of the club to count students for.
     * @return The number of students in the specified club. Returns 0 if no students are found or on error.
     */
    public int getStudentCountForClub(String clubName) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;

        try {
            String selection = COL_CLUB_NAME + " = ?";
            String[] selectionArgs = {clubName};
            count = (int) DatabaseUtils.queryNumEntries(db, TABLE_STUDENTS, selection, selectionArgs);
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error counting students for club '" + clubName + "': " + e.getMessage());
            count = 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }
    public boolean updateStudent(Student student) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_IMAGE_PATH, student.getImagePath()); // CHANGED: Store image path
        values.put(COL_NAME, student.getName());
        values.put(COL_FATHER_NAME, student.getFatherName());
        values.put(COL_AGE, student.getAge());
        values.put(COL_ADMISSION_DATE, student.getAdmissionDate());
        values.put(COL_GENDER, student.getGender());
        values.put(COL_BELT, student.getBelt());
        values.put(COL_EMAIL, student.getEmail());
        values.put(COL_PHONE, student.getPhone());
        values.put(COL_ADDRESS, student.getAddress());
        values.put(COL_CLUB_NAME, student.getClubName());

        int rowsAffected = -1;
        try {
            rowsAffected = db.update(TABLE_STUDENTS, values, COL_ID + " = ?", new String[]{student.getId()});
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error updating student: " + e.getMessage());
        }
        return rowsAffected > 0;
    }

    public boolean deleteStudent(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = -1;
        try {
            rowsAffected = db.delete(TABLE_STUDENTS, COL_ID + " = ?", new String[]{id});
        } catch (Exception e) {
            Log.e("StudentDbHelper", "Error deleting student: " + e.getMessage());
        }
        return rowsAffected > 0;
    }
}