package com.intersistemi.ldaplogin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Constants for Database name, table name, and column samples
    public static final String DB_NAME = "android";
    public static final String TABLE_NAME = "samples";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_BARCODE = "barcode";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_LDAP_USER = "ldap_user";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_STATUS_TEXT = "status_text";


    //database version
    private static final int DB_VERSION = 1;

    //Constructor
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    //creating the database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_NAME
                + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_BARCODE + " VARCHAR, "
                + COLUMN_STATUS + " TINYINT, "
                + COLUMN_LDAP_USER + " VARCHAR, "
                + COLUMN_TIME + " INTEGER, "
                + COLUMN_STATUS_TEXT + " VARCHAR "
                + ");";
        db.execSQL(sql);
    }

    //upgrading the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

    /**
     * Method to add sample to local database
     *
     * @param barcode     barcpde to add
     * @param status      status to add 0 means unsynced; 1 means synced
     * @param ldap_user   ldap_user to add
     * @param time        unix timestamp in milliseconds
     * @param status_text text explaining status
     */
    public void addSample(String barcode, int status, String ldap_user, long time, String status_text) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_BARCODE, barcode);
        contentValues.put(COLUMN_STATUS, status);
        contentValues.put(COLUMN_LDAP_USER, ldap_user);
        contentValues.put(COLUMN_TIME, time);
        contentValues.put(COLUMN_STATUS_TEXT, status_text);

        db.insert(TABLE_NAME, null, contentValues);
        db.close();
    }

    /**
     * this metod will update the sync status
     *
     * @param id          of sample to update
     * @param status      status to update
     * @param status_text text explaining status
     */
    public void updateSampleStatus(int id, int status, String status_text) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_STATUS, status);
        contentValues.put(COLUMN_STATUS_TEXT, status_text);
        db.update(TABLE_NAME, contentValues, COLUMN_ID + "=" + id, null);
        db.close();
    }

    /**
     * @return this method will give us all the samples stored in sqlite
     */
    public Cursor getSamples() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " ASC;";
        return db.rawQuery(sql, null);
    }

    /**
     * @return return all the unsynced sample
     */
    public Cursor getUnsyncedSamples() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_STATUS + " != 1;";
        return db.rawQuery(sql, null);
    }
}