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

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import net.mceoin.cominghome.timehome.TimeHomeValues.TimeHome;

/**
 * Use this to add entries to the timehome
 */
public class TimeHomeUpdate {

    public static final String TAG = TimeHomeUpdate.class.getSimpleName();
    public static final boolean debug = false;

    public static void add(Context context, long timeseconds) {
        Uri mUri;

        if (debug) Log.d(TAG, "updateTimeHome(" + timeseconds + ")");
        ContentValues values = new ContentValues();

        long hours = timeseconds % 60;
        long seconds = timeseconds - (hours * 60);
        String timehome;
        try {
            timehome = String.format("%d:%02d", hours, seconds);
            values.put(TimeHome.TIMEHOME, timehome);
            values.put(TimeHome.TIMESECONDS, timeseconds);
            values.put(TimeHome.CREATED_DATE, System.currentTimeMillis());

            mUri = TimeHome.CONTENT_URI;
            Uri uri = context.getContentResolver().insert(mUri, values);

            if (debug) Log.d(TAG, "updateTimeHome: inserted uri=" + uri);
        } catch (Exception e) {
            Log.w(TAG, e.getLocalizedMessage());
        }

    }

}
