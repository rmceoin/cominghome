/*
 *
 * Copyright (C) 2014 Randy McEoin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.mceoin.cominghome.timehome;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import net.mceoin.cominghome.timehome.TimeHomeValues.TimeHome;

import net.mceoin.cominghome.PrefsFragment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class TimeHomeProvider extends ContentProvider {

    private static final String TAG = TimeHomeProvider.class.getSimpleName();
    private static final boolean debug = false;

    private static final String DATABASE_NAME = "timehome.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TIMEHOME_TABLE_NAME = "timehome";

    private static HashMap<String, String> sTimeHomeProjectionMap;

    private static final int TIMEHOME = 1;
    private static final int TIMEHOME_ID = 2;

    private static final UriMatcher sUriMatcher;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TIMEHOME_TABLE_NAME + " ("
                    + TimeHome._ID + " INTEGER PRIMARY KEY,"
                    + TimeHome.TIMEHOME + " TEXT,"
                    + TimeHome.TIMESECONDS + " INTEGER,"
                    + TimeHome.CREATED_DATE + " INTEGER,"
                    + TimeHome.CREATED_DATE_STR + " TEXT"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TIMEHOME_TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case TIMEHOME:
                qb.setTables(TIMEHOME_TABLE_NAME);
                qb.setProjectionMap(sTimeHomeProjectionMap);
                break;

            case TIMEHOME_ID:
                qb.setTables(TIMEHOME_TABLE_NAME);
                qb.setProjectionMap(sTimeHomeProjectionMap);
                List path = uri.getPathSegments();
                if (path != null) {
                    String id = (String) path.get(1);
                    if (id != null) {
                        qb.appendWhere(TimeHome._ID + "=" + id);
                    }
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = TimeHome.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // Get the database and run the query
        Cursor c;
        try {
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        } catch (SQLiteException e) {
            e.printStackTrace();
            return null;
        }

        // Tell the cursor what uri to watch, so it knows when its source data changes
        Context context = getContext();
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                c.setNotificationUri(resolver, uri);
            }
        }
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case TIMEHOME:
                return TimeHome.CONTENT_TYPE;

            case TIMEHOME_ID:
                return TimeHome.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    public Uri insert(Uri uri, ContentValues initialValues) {
        // Validate the requested uri
        if (sUriMatcher.match(uri) != TIMEHOME) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = System.currentTimeMillis();

        if (!values.containsKey(TimeHome.CREATED_DATE)) {
            values.put(TimeHome.CREATED_DATE, now);
        }

        if (!values.containsKey(TimeHome.CREATED_DATE_STR)) {
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime();
            String createdDateStr = df.format(today);
            values.put(TimeHome.CREATED_DATE_STR, createdDateStr);
        }

        if (!values.containsKey(TimeHome.TIMEHOME)) {
            values.put(TimeHome.TIMEHOME, "");
        }

        if (!values.containsKey(TimeHome.TIMESECONDS)) {
            values.put(TimeHome.TIMESECONDS, 0);
        }

        Uri noteUri = null;
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            long rowId = db.insert(TIMEHOME_TABLE_NAME, null, values);
            if (rowId > 0) {
                noteUri = ContentUris.withAppendedId(TimeHome.CONTENT_URI, rowId);
                notifyChange(uri);
                keepCheck(db);
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return noteUri;
    }

    /**
     * Ensure that the database only keeps maxRows worth of records.
     * This keep the table from growing forever in size.
     *
     * @param db a writable SQLiteDatabase
     */
    private void keepCheck(SQLiteDatabase db) {
        if (debug) Log.d(TAG, "keepCheck()");
        long count;
        try {
            SQLiteStatement r = db.compileStatement("SELECT count(*) FROM " +
                    TIMEHOME_TABLE_NAME);
            count = r.simpleQueryForLong();
        } catch (SQLiteException e) {
            e.printStackTrace();
            return;
        }

        Context context = getContext();
        if (context == null) {
            return;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String timehomeEntries = sp.getString(PrefsFragment.PREFERENCE_TIMEHOME_ENTRIES, "100");
        int maxRows = Integer.parseInt(timehomeEntries);
        if (count > maxRows) {
            if (debug) Log.d(TAG, "keepCheck: greater than " + maxRows);
            Cursor c;
            try {
                String[] projection = {TimeHome._ID};
                c = db.query(TIMEHOME_TABLE_NAME, projection, null, null, null, null, TimeHome.DEFAULT_SORT_ORDER);
                c.moveToPosition(maxRows);
                while (!c.isAfterLast()) {
                    int rowId = c.getInt(0);
                    if (debug) Log.d(TAG, "keepCheck: need to delete " + rowId);
                    db.delete(TIMEHOME_TABLE_NAME, TimeHome._ID + "=" + rowId, null);
                    c.moveToNext();
                }
                c.close();
            } catch (SQLiteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case TIMEHOME:
                count = db.delete(TIMEHOME_TABLE_NAME, where, whereArgs);
                break;

            case TIMEHOME_ID:
                List path = uri.getPathSegments();
                if (path != null) {
                    String noteId = (String) path.get(1);
                    count = db.delete(TIMEHOME_TABLE_NAME, TimeHome._ID + "=" + noteId
                            + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        notifyChange(uri);
        return count;
    }

    private void notifyChange(Uri uri) {
        Context context = getContext();
        if (context != null) {
            context.getContentResolver().notifyChange(uri, null);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if (debug) Log.d(TAG, "update(" + uri + "," + values + ",,");
        int count = 0;
        try {
            SQLiteDatabase db = mOpenHelper.getWritableDatabase();
            switch (sUriMatcher.match(uri)) {
                case TIMEHOME:
                    count = db.update(TIMEHOME_TABLE_NAME, values, where, whereArgs);
                    break;

                case TIMEHOME_ID:
                    List path = uri.getPathSegments();
                    if (path != null) {
                        String noteId = (String) path.get(1);
                        count = db.update(TIMEHOME_TABLE_NAME, values, TimeHome._ID + "=" + noteId
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }

            notifyChange(uri);
        } catch (SQLException e) {
            if (debug) Log.d(TAG, "SQL error: " + e.getLocalizedMessage());
        }
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(TimeHomeValues.AUTHORITY, "timehome", TIMEHOME);
        sUriMatcher.addURI(TimeHomeValues.AUTHORITY, "timehome/#", TIMEHOME_ID);

        sTimeHomeProjectionMap = new HashMap<String, String>();
        sTimeHomeProjectionMap.put(TimeHome._ID, TimeHome._ID);
        sTimeHomeProjectionMap.put(TimeHome.TIMEHOME, TimeHome.TIMEHOME);
        sTimeHomeProjectionMap.put(TimeHome.TIMESECONDS, TimeHome.TIMESECONDS);
        sTimeHomeProjectionMap.put(TimeHome.CREATED_DATE, TimeHome.CREATED_DATE);
        sTimeHomeProjectionMap.put(TimeHome.CREATED_DATE_STR, TimeHome.CREATED_DATE_STR);
    }
}
