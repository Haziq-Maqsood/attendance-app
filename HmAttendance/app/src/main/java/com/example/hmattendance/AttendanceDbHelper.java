package com.example.hmattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.hmattendance.models.Attendance;
import com.example.hmattendance.models.MonthlyAttendanceSummary;
import com.example.hmattendance.models.Student; // Student model for foreign key check

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceDbHelper extends SQLiteOpenHelper {

    // --- MUST MATCH StudentDbHelper's DATABASE_NAME ---
    private static final String DATABASE_NAME = "student_records.db";
    // --- MUST MATCH StudentDbHelper's DATABASE_VERSION ---
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ATTENDANCE = "attendance";

    public static final String COL_ATTENDANCE_ID = "attendance_id";
    public static final String COL_DATE = "date";
    public static final String COL_STUDENT_ID = "student_id";
    public static final String COL_STATUS = "status";
    public static final String COL_CLUB_NAME = "club_name"; // This column remains but is not a FK to the separate club DB
    public static final String COL_TIMESTAMP = "timestamp";

    // SQL statement to create the attendance table
    public static final String SQL_CREATE_ATTENDANCE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE + " (" +
            COL_ATTENDANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COL_DATE + " TEXT NOT NULL," +
            COL_STUDENT_ID + " TEXT NOT NULL," +
            COL_STATUS + " TEXT NOT NULL," +
            COL_CLUB_NAME + " TEXT NOT NULL," +
            COL_TIMESTAMP + " TEXT," +
            "UNIQUE (" + COL_DATE + ", " + COL_STUDENT_ID + ", " + COL_CLUB_NAME + ") ON CONFLICT REPLACE," +
            // Foreign key to Student table (within the same database file)
            "FOREIGN KEY (" + COL_STUDENT_ID + ") REFERENCES " + StudentDbHelper.TABLE_STUDENTS + "(" + StudentDbHelper.COL_ID + ") ON DELETE CASCADE" +
            // REMOVED FOREIGN KEY to ClubDbHelper.TABLE_CLUBS
            ");";

    public AttendanceDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("AttendanceDbHelper", "Creating attendance table in " + DATABASE_NAME);
        db.execSQL("PRAGMA foreign_keys = ON;"); // Enable foreign keys for this connection
        try {
            db.execSQL(SQL_CREATE_ATTENDANCE_TABLE);
            Log.d("AttendanceDbHelper", "Attendance table created successfully.");
        } catch (Exception e) {
            Log.e("AttendanceDbHelper", "Error creating Attendance table: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("AttendanceDbHelper", "Upgrading Attendance table from version " + oldVersion + " to " + newVersion + ", data will be destroyed for this table.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ATTENDANCE);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;"); // Ensure foreign keys are enabled every time db is opened
        }
    }


    /**
     * Adds a new attendance record to the database.
     * If a record for the same student, date, and club already exists, it will be replaced (due to ON CONFLICT REPLACE).
     *
     * @param attendance The Attendance object containing the data to be added.
     * @return The row ID of the newly inserted (or replaced) row, or -1 if an error occurred.
     */
    public long addAttendance(Attendance attendance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_DATE, attendance.getDate());
        values.put(COL_STUDENT_ID, attendance.getStudentId());
        values.put(COL_STATUS, attendance.getStatus());
        values.put(COL_CLUB_NAME, attendance.getClubName());
        values.put(COL_TIMESTAMP, attendance.getTimestamp() != null ? attendance.getTimestamp() : getCurrentTimestamp());

        long result = db.insertWithOnConflict(TABLE_ATTENDANCE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close(); // Close the database after the operation

        if (result == -1) {
            Log.e("AttendanceDbHelper", "Failed to add/update attendance for student: " + attendance.getStudentId() + " on " + attendance.getDate());
        } else {
            Log.d("AttendanceDbHelper", "Attendance added/updated successfully for student: " + attendance.getStudentId() + " on " + attendance.getDate());
        }
        return result;
    }

    /**
     * Retrieves an attendance record for a specific student, date, and club.
     *
     * @param studentId The ID of the student.
     * @param date The date of the attendance (YYYY-MM-DD).
     * @param clubName The name of the club.
     * @return The Attendance object if found, null otherwise.
     */
    public Attendance getAttendanceByStudentDateClub(String studentId, String date, String clubName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Attendance attendance = null;

        String selection = COL_STUDENT_ID + " = ? AND " + COL_DATE + " = ? AND " + COL_CLUB_NAME + " = ?";
        String[] selectionArgs = {studentId, date, clubName};

        try {
            cursor = db.query(TABLE_ATTENDANCE, null, selection, selectionArgs,
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                attendance = new Attendance(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATTENDANCE_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
                );
            }
        } catch (Exception e) {
            Log.e("AttendanceDbHelper", "Error getting attendance for student " + studentId + " on " + date + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close(); // Close the database
        }
        return attendance;
    }

    /**
     * Retrieves all attendance records for a specific club on a given date.
     *
     * @param date The date to filter by (YYYY-MM-DD).
     * @param clubName The name of the club to filter by.
     * @return A list of Attendance objects.
     */
    public List<Attendance> getAttendanceForDateByClub(String date, String clubName) {
        List<Attendance> attendanceList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String selection = COL_DATE + " = ? AND " + COL_CLUB_NAME + " = ?";
        String[] selectionArgs = {date, clubName};

        try {
            cursor = db.query(TABLE_ATTENDANCE, null, selection, selectionArgs,
                    null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    Attendance attendance = new Attendance(
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATTENDANCE_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
                    );
                    attendanceList.add(attendance);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("AttendanceDbHelper", "Error getting attendance for date " + date + " and club " + clubName + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close(); // Close the database
        }
        return attendanceList;
    }

    /**
     * Retrieves all attendance records for a specific student across all dates and clubs.
     *
     * @param studentId The ID of the student.
     * @return A list of Attendance objects for the student.
     */
    public List<Attendance> getAttendanceByStudentId(String studentId) {
        List<Attendance> attendanceList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        String selection = COL_STUDENT_ID + " = ?";
        String[] selectionArgs = {studentId};

        try {
            cursor = db.query(TABLE_ATTENDANCE, null, selection, selectionArgs,
                    null, null, COL_DATE + " DESC"); // Order by date descending

            if (cursor.moveToFirst()) {
                do {
                    Attendance attendance = new Attendance(
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_ATTENDANCE_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
                    );
                    attendanceList.add(attendance);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("AttendanceDbHelper", "Error getting all attendance for student " + studentId + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close(); // Close the database
        }
        return attendanceList;
    }

    /**
     * Updates an existing attendance record.
     *
     * @param attendance The Attendance object with updated data. The ID must match an existing record.
     * @return true if the record was updated successfully, false otherwise.
     */
    public boolean updateAttendance(Attendance attendance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_DATE, attendance.getDate());
        values.put(COL_STUDENT_ID, attendance.getStudentId());
        values.put(COL_STATUS, attendance.getStatus());
        values.put(COL_CLUB_NAME, attendance.getClubName());
        values.put(COL_TIMESTAMP, getCurrentTimestamp()); // Update timestamp on modification

        int rowsAffected = db.update(TABLE_ATTENDANCE, values, COL_ATTENDANCE_ID + " = ?", new String[]{String.valueOf(attendance.getId())});
        db.close(); // Close the database
        return rowsAffected > 0;
    }

    /**
     * Deletes an attendance record by its ID.
     *
     * @param attendanceId The ID of the attendance record to delete.
     * @return true if the record was deleted successfully, false otherwise.
     */
    public boolean deleteAttendance(int attendanceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_ATTENDANCE, COL_ATTENDANCE_ID + " = ?", new String[]{String.valueOf(attendanceId)});
        db.close(); // Close the database
        return rowsAffected > 0;
    }

    /**
     * Helper method to get the current timestamp in YYYY-MM-DD HH:MM:SS format.
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Retrieves a monthly attendance summary for all students in a specific club.
     * Includes student names by joining with the students table.
     *
     * @param yearMonth The year and month in YYYY-MM format (e.g., "2024-07").
     * @param clubName The name of the club.
     * @return A List of MonthlyAttendanceSummary objects.
     */
    public List<MonthlyAttendanceSummary> getMonthlyAttendanceSummary(String yearMonth, String clubName) {
        List<MonthlyAttendanceSummary> summaryList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        // SQL Query to join attendance and students tables, filter by month and club, and group by student
        String query = "SELECT " +
                "A." + COL_STUDENT_ID + ", " +
                "S." + StudentDbHelper.COL_NAME + " AS student_name, " + // Alias student name for clarity
                "SUM(CASE WHEN A." + COL_STATUS + " = 'Present' THEN 1 ELSE 0 END) AS TotalPresentDays, " +
                "SUM(CASE WHEN A." + COL_STATUS + " = 'Absent' THEN 1 ELSE 0 END) AS TotalAbsentDays, " +
                "SUM(CASE WHEN A." + COL_STATUS + " = 'Late' THEN 1 ELSE 0 END) AS TotalLateDays, " +
                "SUM(CASE WHEN A." + COL_STATUS + " = 'Excused' THEN 1 ELSE 0 END) AS TotalExcusedDays, " +
                "COUNT(A." + COL_ATTENDANCE_ID + ") AS TotalMarkedDays " + // Count distinct days student was marked
                "FROM " + TABLE_ATTENDANCE + " A " +
                "JOIN " + StudentDbHelper.TABLE_STUDENTS + " S ON A." + COL_STUDENT_ID + " = S." + StudentDbHelper.COL_ID + " " +
                "WHERE strftime('%Y-%m', A." + COL_DATE + ") = ? AND A." + COL_CLUB_NAME + " = ? " +
                "GROUP BY A." + COL_STUDENT_ID + ", S." + StudentDbHelper.COL_NAME + " " +
                "ORDER BY S." + StudentDbHelper.COL_NAME + " ASC;"; // Order by student name for readability

        String[] selectionArgs = {yearMonth, clubName};

        try {
            cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst()) {
                do {
                    MonthlyAttendanceSummary summary = new MonthlyAttendanceSummary(
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow("student_name")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TotalPresentDays")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TotalAbsentDays")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TotalLateDays")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TotalExcusedDays")),
                            cursor.getInt(cursor.getColumnIndexOrThrow("TotalMarkedDays"))
                    );
                    summaryList.add(summary);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("AttendanceDbHelper", "Error getting monthly attendance summary for " + yearMonth + " and club " + clubName + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close(); // Close the database
        }
        return summaryList;
    }
}