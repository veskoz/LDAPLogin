package com.intersistemi.ldaplogin;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    //Constants for Database name, table name, and column names
    public static final String DB_NAME = "android";
    public static final String TABLE_NAME = "names";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_BARCODE = "barcode";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_LDAP_USER = "ldap_user";
    public static final String COLUMN_TIME = "time";

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
                + COLUMN_TIME + " INTEGER "
                + ");";
        db.execSQL(sql);
        //CREATE TABLE names(id INTEGER PRIMARY KEY AUTOINCREMENT, barcode VARCHAR, status TINYINT, ldap_user VARCHAR, time INTEGER );
    }

    //upgrading the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS Persons";
        db.execSQL(sql);
        onCreate(db);
    }

    /*
     * This method is taking two arguments
     * first one is the name that is to be saved
     * second one is the status
     * 0 means the name is synced with the server
     * 1 means the name is not synced with the server
     * */
    public void addName(String barcode, int status, String ldap_user, long time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(COLUMN_BARCODE, barcode);
        contentValues.put(COLUMN_STATUS, status);
        contentValues.put(COLUMN_LDAP_USER, ldap_user);
        contentValues.put(COLUMN_TIME, time);

        db.insert(TABLE_NAME, null, contentValues);
        db.close();
    }

    /*
     * This method taking two arguments
     * first one is the id of the name for which
     * we have to update the sync status
     * and the second one is the status that will be changed
     * */
    public void updateNameStatus(int id, int status, String ldap_user, long time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_STATUS, status);
        db.update(TABLE_NAME, contentValues, COLUMN_ID + "=" + id, null);
        db.close();
    }

    /*
     * this method will give us all the name stored in sqlite
     * */
    public Cursor getNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_ID + " ASC;";
        return db.rawQuery(sql, null);
    }

    /*
     * this method is for getting all the unsynced name
     * so that we can sync it with database
     * */
    public Cursor getUnsyncedNames() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_STATUS + " = 0;";
        return db.rawQuery(sql, null);
    }
}