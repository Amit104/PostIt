package com.example.amwadatk.postit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;


public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "personGroup";
    private static final String TABLE_GROUP = "groupData";
    private static final String KEY_NAME = "name";
    private static final String KEY_COUNTER = "counter";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        //3rd argument to be passed is CursorFactory instance
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_GROUP_TABLE = "CREATE TABLE " + TABLE_GROUP + "("
                +  KEY_NAME + " TEXT PRIMARY KEY,"
                + KEY_COUNTER + " INTEGER" + ")";
        db.execSQL(CREATE_GROUP_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP);

        // Create tables again
        onCreate(db);
    }

    // code to add the new contact
    void addPerson (Groupdata data) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, data.getName());
        values.put(KEY_COUNTER, data.getCounter());

        db.insert(TABLE_GROUP, null, values);
        db.close();
    }

    int getPersonCount(String name) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_GROUP, new String[] {
                        KEY_NAME, KEY_COUNTER }, KEY_NAME + "=?",
                new String[] { name }, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();

        return Integer.parseInt(cursor.getString(1));
    }

    // code to get all contacts in a list view
    public List<Groupdata> getAllPersons() {
        List<Groupdata> personsList = new ArrayList<Groupdata>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_GROUP;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Groupdata data = new Groupdata();
                data.setName(cursor.getString(0));
                data.setCounter(Integer.parseInt(cursor.getString(1)));
                // Adding contact to list
                personsList.add(data);
            } while (cursor.moveToNext());
        }

        // return contact list
        return personsList;
    }

    // code to update the single contact
    public int incrementPersonCount(String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_COUNTER, getPersonCount(name) + 1);

        // updating row
        return db.update(TABLE_GROUP, values, KEY_NAME + " = ?",
                new String[] { name });
    }

    // Deleting single contact
    public void deleteContact(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_GROUP, KEY_NAME + " = ?",
                new String[] { name });
        db.close();
    }

    // Getting contacts Count
    public int getTotalPersons() {
        String countQuery = "SELECT  * FROM " + TABLE_GROUP;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.close();

        // return count
        return cursor.getCount();
    }

    public int getMaxCount() {
        String countQuery = "SELECT  MAX("+ KEY_COUNTER +") FROM " + TABLE_GROUP ;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        cursor.moveToFirst();

        int c = cursor.getInt(0);
        cursor.close();
        return c;
    }

}