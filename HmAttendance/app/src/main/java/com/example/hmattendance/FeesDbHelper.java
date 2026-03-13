// FeesDbHelper.java
package com.example.hmattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.hmattendance.models.FeesPayment;

import java.util.ArrayList;
import java.util.List;

public class FeesDbHelper extends SQLiteOpenHelper {

    // Database information
    private static final String DATABASE_NAME = "student_records.db"; // Assuming shared DB file
    private static final int DATABASE_VERSION = 1; // Start with version 1 for this specific helper

    // Table Name
    public static final String TABLE_FEES_PAYMENTS = "fees_payments";

    // Columns
    public static final String COL_PAYMENT_ID = "payment_id"; // Primary key for fees record
    public static final String COL_PAYMENT_STUDENT_ID = "student_id"; // FK to students table
    public static final String COL_FEE_TYPE = "fee_type"; // e.g., "Monthly", "Exam", "Annual", "Miscellaneous"
    public static final String COL_AMOUNT = "amount"; // Store as REAL for currency
    public static final String COL_PAYMENT_DATE = "payment_date"; // YYYY-MM-DD
    public static final String COL_PAYMENT_TIMESTAMP = "timestamp"; // YYYY-MM-DD HH:MM:SS
    public static final String COL_CLUB_NAME = "club_name"; // To easily filter fees by club

    // SQL statement for Fees Payments table creation
    public static final String SQL_CREATE_FEES_PAYMENTS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_FEES_PAYMENTS + " (" +
                    COL_PAYMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COL_PAYMENT_STUDENT_ID + " TEXT NOT NULL," +
                    COL_FEE_TYPE + " TEXT NOT NULL," +
                    COL_AMOUNT + " REAL NOT NULL," +
                    COL_PAYMENT_DATE + " TEXT NOT NULL," +
                    COL_PAYMENT_TIMESTAMP + " TEXT," +
                    COL_CLUB_NAME + " TEXT NOT NULL," +
                    "FOREIGN KEY (" + COL_PAYMENT_STUDENT_ID + ") REFERENCES students(" + StudentDbHelper.COL_ID + ") ON DELETE CASCADE" + // Assuming 'students' table exists
                    ");";

    public FeesDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // IMPORTANT: In this independent setup, this onCreate ONLY handles the fees_payments table.
        // If 'student_records.db' is already created by another helper (e.g., StudentDbHelper)
        // and its onCreate didn't include this table, this onCreate will NOT be called.
        // This is the core challenge of independent helpers on a shared DB file.
        // The foreign key constraint 'REFERENCES students(id)' will fail if the 'students' table doesn't exist.
        db.execSQL(SQL_CREATE_FEES_PAYMENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This simple onUpgrade wipes data for this specific table.
        // For production, implement proper migration logic.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FEES_PAYMENTS);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For development, treat downgrade as upgrade (wipe and recreate this table).
        onUpgrade(db, oldVersion, newVersion);
    }

    // New method to enable foreign key support (important for SQLite)
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Adds a new fee payment record to the database.
     * @param payment The FeesPayment object containing details.
     * @return The row ID of the newly inserted row, or -1 if an error occurred.
     */
    public long addFeePayment(FeesPayment payment) {
        Log.d("FEE_SAVE_DEBUG", "Saving fee: studentId=" + payment.getStudentId()
                + ", feeType=" + payment.getFeeType()
                + ", club=" + payment.getClubName()
                + ", date=" + payment.getPaymentDate());

        SQLiteDatabase db = this.getWritableDatabase(); // Get DB from this helper
        ContentValues values = new ContentValues();
        values.put(COL_PAYMENT_STUDENT_ID, payment.getStudentId());
        values.put(COL_FEE_TYPE, payment.getFeeType());
        values.put(COL_AMOUNT, payment.getAmount());
        values.put(COL_PAYMENT_DATE, payment.getPaymentDate());
        values.put(COL_PAYMENT_TIMESTAMP, payment.getTimestamp());
        values.put(COL_CLUB_NAME, payment.getClubName());

        long result = -1;
        try {
            result = db.insert(TABLE_FEES_PAYMENTS, null, values);
        } catch (Exception e) {
            Log.e("FeesDbHelper", "Error adding fee payment: " + e.getMessage());
        } finally {
            db.close();
        }
        return result;
    }

    /**
     * Retrieves all fee payments for a specific student.
     * @param studentId The ID of the student.
     * @return A list of FeesPayment objects.
     */
    public List<FeesPayment> getFeePaymentsForStudent(String studentId) {
        List<FeesPayment> payments = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selection = COL_PAYMENT_STUDENT_ID + " = ?";
            String[] selectionArgs = {studentId};

            cursor = db.query(
                    TABLE_FEES_PAYMENTS,
                    new String[]{COL_PAYMENT_ID, COL_PAYMENT_STUDENT_ID, COL_FEE_TYPE, COL_AMOUNT, COL_PAYMENT_DATE, COL_PAYMENT_TIMESTAMP, COL_CLUB_NAME},
                    selection,
                    selectionArgs,
                    null, null, COL_PAYMENT_DATE + " DESC, " + COL_PAYMENT_ID + " DESC"
            );

            if (cursor.moveToFirst()) {
                do {
                    FeesPayment payment = new FeesPayment(
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_PAYMENT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_STUDENT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_FEE_TYPE)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_DATE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYMENT_TIMESTAMP)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME))
                    );
                    payments.add(payment);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("FeesDbHelper", "Error getting fee payments for student: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return payments;
    }

    /**
     * Checks if a specific fee type (e.g., "Monthly") has been paid by a student for a given month and club.
     * This method assumes 'Monthly' fees are tracked by month and others by individual records.
     *
     * @param studentId The ID of the student.
     * @param feeType The type of fee (e.g., "Monthly").
     * @param clubName The club name.
     * @param yearMonth The year and month in YYYY-MM format (e.g., "2025-07").
     * @return True if the fee has been paid for the specified period, false otherwise.
     */
    public boolean hasFeeBeenPaidForMonth(String studentId, String feeType, String clubName, String yearMonth) {
        Log.d("FEE_CHECK_DEBUG", "Checking: studentId=" + studentId
                + ", feeType=" + feeType
                + ", club=" + clubName
                + ", LIKE date=" + yearMonth + "-%");

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean paid = false;

        try {
            String query = "SELECT COUNT(*) FROM " + TABLE_FEES_PAYMENTS +
                    " WHERE " + COL_PAYMENT_STUDENT_ID + " = ?" +
                    " AND " + COL_FEE_TYPE + " = ?" +
                    " AND " + COL_CLUB_NAME + " = ?" +
                    " AND " + COL_PAYMENT_DATE + " LIKE ?";

            String[] selectionArgs = {studentId, feeType, clubName, yearMonth + "-%"};

            cursor = db.rawQuery(query, selectionArgs);

            if (cursor.moveToFirst()) {
                if (cursor.getInt(0) > 0) {
                    paid = true;
                }
            }
        } catch (Exception e) {
            Log.e("FeesDbHelper", "Error checking fee payment status: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return paid;
    }
}