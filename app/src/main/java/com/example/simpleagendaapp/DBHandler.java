package com.example.simpleagendaapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHandler extends SQLiteOpenHelper {

    // Πληροφορίες βάσης δεδομένων
    private static final int DB_VERSION = 4;
    private static final String DB_NAME = "schedly.db";

    // Πίνακας: Χρήστες
    private static final String TABLE_USERS = "Users";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    // Πίνακας: Τοποθεσίες
    private static final String TABLE_LOCATIONS = "Locations";
    private static final String KEY_LOCATION_ID = "location_id";
    private static final String KEY_LOCATION_NAME = "name";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    // Πίνακας: Ραντεβού
    private static final String TABLE_APPOINTMENTS = "Appointments";
    private static final String KEY_APPOINTMENT_ID = "appointment_id";
    private static final String KEY_APPOINTMENT_USER_ID = "user_id";
    private static final String KEY_APPOINTMENT_LOCATION_ID = "location_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_NOTE = "note";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_CREATED_AT = "created_at";

    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Δημιουργία πίνακα χρηστών
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "(" +
                KEY_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_USERNAME + " TEXT UNIQUE, " +
                KEY_PASSWORD + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Δημιουργία πίνακα τοποθεσιών
        String CREATE_LOCATIONS_TABLE = "CREATE TABLE " + TABLE_LOCATIONS + "(" +
                KEY_LOCATION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_LOCATION_NAME + " TEXT, " +
                KEY_LATITUDE + " REAL, " +
                KEY_LONGITUDE + " REAL" + ")";
        db.execSQL(CREATE_LOCATIONS_TABLE);

        // Δημιουργία πίνακα ραντεβού
        String CREATE_APPOINTMENTS_TABLE = "CREATE TABLE " + TABLE_APPOINTMENTS + "(" +
                KEY_APPOINTMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                KEY_APPOINTMENT_USER_ID + " INTEGER, " +
                KEY_APPOINTMENT_LOCATION_ID + " INTEGER, " +
                KEY_TITLE + " TEXT, " +
                KEY_NOTE + " TEXT, " +
                KEY_DATE + " TEXT, " +
                KEY_TIME + " TEXT, " +
                KEY_CREATED_AT + " TEXT, " +
                "FOREIGN KEY(" + KEY_APPOINTMENT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + KEY_USER_ID + "), " +
                "FOREIGN KEY(" + KEY_APPOINTMENT_LOCATION_ID + ") REFERENCES " + TABLE_LOCATIONS + "(" + KEY_LOCATION_ID + "))";
        db.execSQL(CREATE_APPOINTMENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Διαγραφή υπαρχόντων πινάκων και δημιουργία από την αρχή
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPOINTMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    // Εισαγωγή νέου χρήστη στη βάση
    public long insertUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_USERNAME, username);
        values.put(KEY_PASSWORD, password);
        return db.insert(TABLE_USERS, null, values);
    }

    // Έλεγχος εγκυρότητας στοιχείων χρήστη
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{KEY_USER_ID},
                KEY_USERNAME + "=? AND " + KEY_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // Ανάκτηση ID χρήστη βάσει ονόματος
    public int getUserId(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{KEY_USER_ID},
                KEY_USERNAME + "=?",
                new String[]{username},
                null, null, null);
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID));
            cursor.close();
            return id;
        }
        cursor.close();
        return -1;
    }

    // Εισαγωγή νέου ραντεβού
    public long insertAppointment(int userId, int locationId, String title, String note, String date, String time, String createdAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_APPOINTMENT_USER_ID, userId);
        values.put(KEY_APPOINTMENT_LOCATION_ID, locationId);
        values.put(KEY_TITLE, title);
        values.put(KEY_NOTE, note);
        values.put(KEY_DATE, date);
        values.put(KEY_TIME, time);
        values.put(KEY_CREATED_AT, createdAt);
        return db.insert(TABLE_APPOINTMENTS, null, values);
    }

    // Ενημέρωση υπάρχοντος ραντεβού
    public void updateAppointment(int id, String title, String note, String time, int locationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_NOTE, note);
        values.put(KEY_TIME, time);
        values.put(KEY_APPOINTMENT_LOCATION_ID, locationId);
        db.update(TABLE_APPOINTMENTS, values, KEY_APPOINTMENT_ID + "=?", new String[]{String.valueOf(id)});
    }

    // Διαγραφή ραντεβού
    public void deleteAppointment(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_APPOINTMENTS, KEY_APPOINTMENT_ID + "=?", new String[]{String.valueOf(id)});
    }

    // Εισαγωγή νέας τοποθεσίας
    public long insertLocation(String name, double lat, double lon) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LOCATION_NAME, name);
        values.put(KEY_LATITUDE, lat);
        values.put(KEY_LONGITUDE, lon);
        return db.insert(TABLE_LOCATIONS, null, values);
    }

    // Ανάκτηση ID τοποθεσίας με βάση το όνομα
    public int getLocationIdByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_LOCATIONS,
                new String[]{KEY_LOCATION_ID},
                KEY_LOCATION_NAME + "=?",
                new String[]{name},
                null, null, null);

        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_LOCATION_ID));
            cursor.close();
            return id;
        }

        cursor.close();
        return -1;
    }

    // Ανάκτηση ραντεβού για συγκεκριμένο χρήστη και ημερομηνία
    public List<Appointment> getAppointmentsForUserOnDate(int userId, String date) {
        List<Appointment> appointments = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT A.appointment_id, A.title, A.note, A.time, L.name " +
                "FROM Appointments A " +
                "JOIN Locations L ON A.location_id = L.location_id " +
                "WHERE A.user_id = ? AND A.date = ? " +
                "ORDER BY A.time ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), date});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                String title = cursor.getString(1);
                String note = cursor.getString(2);
                String time = cursor.getString(3);
                String locationName = cursor.getString(4);

                appointments.add(new Appointment(id, title, note, time, locationName));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return appointments;
    }

}
