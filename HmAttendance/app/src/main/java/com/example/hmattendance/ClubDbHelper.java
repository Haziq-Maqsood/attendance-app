package com.example.hmattendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.hmattendance.models.Club; // Import the new Club model

import java.util.ArrayList;
import java.util.List;

public class ClubDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "club_records.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_CLUBS = "clubs";

    public static final String COL_CLUB_NAME = "club_name"; // Primary Key
    public static final String COL_INSTRUCTOR_NAME = "instructor_name";
    public static final String COL_EMAIL = "email";
    public static final String COL_PASSWORD = "password";
    public static final String COL_INITIALS = "initials";

    // SQL statement to create the clubs table
    public static final String SQL_CREATE_CLUBS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CLUBS + " (" +
            COL_CLUB_NAME + " TEXT PRIMARY KEY NOT NULL," +
            COL_INSTRUCTOR_NAME + " TEXT," +
            COL_EMAIL + " TEXT NOT NULL," +
            COL_PASSWORD + " TEXT NOT NULL," +
            COL_INITIALS + " TEXT" +
            ");";

    public ClubDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_CLUBS_TABLE);
        Log.d("ClubDbHelper", "Clubs table created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("ClubDbHelper", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data.");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLUBS);
        onCreate(db);
    }

    /**
     * Adds a new club record to the database.
     *
     * @param club The Club object containing the data to be added.
     * @return true if the club was added successfully, false otherwise (e.g., if club name already exists).
     */
    public boolean addClub(Club club) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_CLUB_NAME, club.getClubName());
        values.put(COL_INSTRUCTOR_NAME, club.getInstructorName());
        values.put(COL_EMAIL, club.getEmail());
        values.put(COL_PASSWORD, club.getPassword());
        values.put(COL_INITIALS, club.getInitials());

        long result = db.insert(TABLE_CLUBS, null, values);
        db.close();

        if (result == -1) {
            Log.e("ClubDbHelper", "Failed to add club: " + club.getClubName() + ". It might already exist.");
            return false;
        } else {
            Log.d("ClubDbHelper", "Club added successfully: " + club.getClubName());
            return true;
        }
    }

    /**
     * Retrieves a club record by its club name (primary key).
     *
     * @param clubName The name of the club to retrieve.
     * @return The Club object if found, null otherwise.
     */
    public Club getClubByClubName(String clubName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Club club = null;

        try {
            cursor = db.query(TABLE_CLUBS, null, COL_CLUB_NAME + " = ?", new String[]{clubName},
                    null, null, null, null); // Added selectionArgs
            // Added selectionArgs to fix potential issue:
            // https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase#query(java.lang.String,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String)


            if (cursor != null && cursor.moveToFirst()) {
                club = new Club(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_INSTRUCTOR_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_INITIALS))
                );
            }
        } catch (Exception e) {
            Log.e("ClubDbHelper", "Error getting club by name: " + clubName + " - " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return club;
    }

    /**
     * Retrieves a club record by matching email and password.
     *
     * @param email The email of the club.
     * @param password The password of the club.
     * @return The Club object if found, null otherwise.
     */
    public Club getClubByEmailAndPassword(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        Club club = null;

        String[] selectionArgs = {email, password};

        try {
            cursor = db.query(
                    TABLE_CLUBS,
                    null, // All columns
                    COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?",
                    selectionArgs,
                    null, null, null, null // No grouping, filtering, ordering, or limit
            );

            if (cursor != null && cursor.moveToFirst()) {
                club = new Club(
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_INSTRUCTOR_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_INITIALS))
                );
            }
        } catch (Exception e) {
            Log.e("ClubDbHelper", "Error getting club by email and password: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return club;
    }


    /**
     * Updates an existing club record in the database.
     *
     * @param club The Club object with updated data. The clubName must match an existing record.
     * @return true if the club was updated successfully, false otherwise.
     */
    public boolean updateClub(Club club) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COL_INSTRUCTOR_NAME, club.getInstructorName());
        values.put(COL_EMAIL, club.getEmail());
        values.put(COL_PASSWORD, club.getPassword());
        values.put(COL_INITIALS, club.getInitials());

        int rowsAffected = db.update(TABLE_CLUBS, values, COL_CLUB_NAME + " = ?", new String[]{club.getClubName()});
        db.close();
        return rowsAffected > 0;
    }

    /**
     * Deletes a club record from the database.
     *
     * @param clubName The name of the club to delete.
     * @return true if the club was deleted successfully, false otherwise.
     */
    public boolean deleteClub(String clubName) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_CLUBS, COL_CLUB_NAME + " = ?", new String[]{clubName});
        db.close();
        return rowsAffected > 0;
    }

    /**
     * Retrieves all club records from the database.
     *
     * @return A list of all Club objects.
     */
    public List<Club> getAllClubs() {
        List<Club> clubList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_CLUBS, null);

            if (cursor.moveToFirst()) {
                do {
                    Club club = new Club(
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_INSTRUCTOR_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_INITIALS))
                    );
                    clubList.add(club);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("ClubDbHelper", "Error getting all clubs: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return clubList;
    }
    public List<String> getAllClubNames() {
        List<String> clubNames = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    true, // distinct
                    TABLE_CLUBS,
                    new String[]{COL_CLUB_NAME}, // Select only the club name column
                    null, null, null, null, COL_CLUB_NAME + " ASC", null
            );

            if (cursor.moveToFirst()) {
                do {
                    String clubName = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLUB_NAME));
                    if (clubName != null && !clubName.trim().isEmpty()) {
                        clubNames.add(clubName);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("ClubDbHelper", "Error getting all club names: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // As discussed, db.close() is handled by the helper's lifecycle, not in individual methods.
        }
        return clubNames;
    }
}