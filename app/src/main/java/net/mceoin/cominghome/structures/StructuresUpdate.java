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
package net.mceoin.cominghome.structures;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.util.HashSet;

/**
 * Use this to update entries in the structures database
 */
public class StructuresUpdate {

    public static final String TAG = StructuresUpdate.class.getSimpleName();
    public static final boolean debug = false;

    static final String[] mProjection = {
            StructuresValues.Structures.STRUCTURE_ID,
            StructuresValues.Structures.NAME,
            StructuresValues.Structures.AWAY,
    };

    static final String[] mProjectionStructures = {
            StructuresValues.Structures.STRUCTURE_ID,
    };

    /**
     * Update the structures database.  If the structure_id does not exist, the entry will be inserted.
     * Otherwise the existing entry is updated as needed.
     *
     * @param context      Context of the app
     * @param structure_id ID of the structure
     * @param name         Name of the structure
     * @param away         Away status
     */
    public static void update(Context context, String structure_id, String name, String away) {
        Uri mUri = StructuresValues.Structures.CONTENT_URI;

        if (debug) Log.d(TAG, "updateStructures(" + structure_id + ")");
        ContentValues values = new ContentValues();
        values.put(StructuresValues.Structures.STRUCTURE_ID, structure_id);
        values.put(StructuresValues.Structures.NAME, name);
        values.put(StructuresValues.Structures.AWAY, away);
        values.put(StructuresValues.Structures.CREATED_DATE, System.currentTimeMillis());

        String mSelectionClause = StructuresValues.Structures.STRUCTURE_ID + " = ?";
        String[] mSelectionArgs = {structure_id};

        Cursor mCursor = context.getContentResolver().query(mUri, mProjection, mSelectionClause, mSelectionArgs, "");
        if (mCursor != null) {

            if (mCursor.getCount() > 0) {
                // we found at least one entry ... not good if more than 1
                while (mCursor.moveToNext()) {
                    String row_name = mCursor.getString(1);
                    String row_away = mCursor.getString(2);
                    if ((!row_name.equals(name)) || (!row_away.equals(away))) {
                        //
                        // name or away do not match, so update the database
                        //
                        int updated = context.getContentResolver().update(mUri, values, mSelectionClause, mSelectionArgs);
                        if (updated != 1) {
                            Log.w(TAG, "did not update: " + mUri.toString());
                        }
                        return;
                    }
                }
                // database did contain this structure_id, but didn't need updating, so just return
                return;
            }
            mCursor.close();
        }

        Uri uri = context.getContentResolver().insert(mUri, values);

        if (debug) Log.d(TAG, "updateStructures: inserted uri=" + uri);
    }

    /**
     * Get all the structure_id's in a handy HashSet
     *
     * @param context Context of the app
     * @return a HashSet of structure_id's
     */
    public static HashSet<String> getStructureIds(Context context) {
        Uri mUri = StructuresValues.Structures.CONTENT_URI;

        HashSet<String> structure_ids = new HashSet<String>();

        Cursor mCursor = context.getContentResolver().query(mUri, mProjectionStructures, null, null, "");
        if (mCursor != null) {

            if (mCursor.getCount() > 0) {
                // we found at least one entry ... not good if more than 1
                while (mCursor.moveToNext()) {
                    String row_structure_id = mCursor.getString(0);
                    structure_ids.add(row_structure_id);
                }
            }
        }
        return structure_ids;
    }

    /**
     * Delete a structure_id from the database
     *
     * @param context Context of the app
     * @param structure_id ID of the structure
     * @return Number of rows deleted
     */
    public static int deleteStructureId(Context context, String structure_id) {
        Uri mUri = StructuresValues.Structures.CONTENT_URI;

        String mSelectionClause = StructuresValues.Structures.STRUCTURE_ID + " = ?";
        String[] mSelectionArgs = {structure_id};

        return context.getContentResolver().delete(mUri, mSelectionClause, mSelectionArgs );
    }
}
