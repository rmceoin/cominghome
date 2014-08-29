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
package net.mceoin.cominghome;

import net.mceoin.cominghome.HistoryValues.History;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * Use this to add entries to the history
 */
public class HistoryUpdate {

    public static final String TAG = HistoryUpdate.class.getSimpleName();
    public static final boolean debug = true;

    public static void add(Context context, String entry)
    {
        Uri mUri;

        if (debug) Log.d(TAG,"updateHistory("+entry+")");
        ContentValues values = new ContentValues();
        values.put(History.ENTRY, entry);
        values.put(History.CREATED_DATE, System.currentTimeMillis());

        mUri = History.CONTENT_URI;
        Uri uri=context.getContentResolver().insert(mUri, values);

        if (debug) Log.d(TAG,"updateHistory: inserted uri="+uri);
    }

}
