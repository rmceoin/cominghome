/*
 * Copyright 2014 Randy McEoin
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

package net.mceoin.cominghome.geofence;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Log;

import net.mceoin.cominghome.cloud.StatusLeftHome;

/**
 * Wait for about 15 minutes before telling the backend or Nest that we've left home.
 * This is needed because sometimes the phone will bounce out of the area briefly.
 */
public class FenceHandlingAlarm extends BroadcastReceiver {

    public final static String TAG = FenceHandlingAlarm.class.getSimpleName();
    public final static boolean debug = true;

    /**
     * Preference for saving the start time of the alarm.
     */
    private final String PREF_STARTTIME = "alarm_start_time";

    /**
     * Name of dedicated preferences file.
     */
    private final String PREF_NAME = "alarm_preferences";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (debug) Log.d(TAG, "onReceive()");

        long currentTime = System.currentTimeMillis();
        long alarmStartTime = AlarmTime.getTime(context); // getStartTime(context);

        long timeElapsedSeconds = (currentTime - alarmStartTime) / 1000;
        if (debug)
            Log.d(TAG, "extra alarmStartTime=" + alarmStartTime + " timeElapsedSeconds=" + timeElapsedSeconds);

        int minimumMinutes = 10;
        if (timeElapsedSeconds < (minimumMinutes * 60)) {
            if (debug)
                Log.d(TAG, "not enough time has passed: (" + timeElapsedSeconds + " = " + currentTime + " - " + alarmStartTime + ")/1000");
            return;
        }

        new StatusLeftHome(context).execute();

        CancelAlarm(context);
    }

    /**
     * Set an alarm to wait before we actually send the backend a left home status update.
     *
     * @param context Context
     */
    public void SetAlarm(@NonNull Context context) {
        if (debug) Log.d(TAG, "SetAlarm()");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, FenceHandlingAlarm.class);

        long alarmStartTime = System.currentTimeMillis();
        AlarmTime.setTime(context, alarmStartTime);
        saveStartTime(context, alarmStartTime);

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (debug) Log.d(TAG, "alarmStartTime=" + alarmStartTime);

        long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                interval,
                interval, pi);
    }

    public void CancelAlarm(@NonNull Context context) {
        Intent intent = new Intent(context, FenceHandlingAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    /**
     * Saves the start time of the alarm in a MODE_MULTI_PROCESS preference which ensures
     * that different processes will put and get the current value.
     *
     * @param context   Context
     * @param startTime Start time in epoch
     */
    private void saveStartTime(@NonNull Context context, long startTime) {
        SharedPreferences prefs;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREF_STARTTIME, startTime);
        editor.apply();

    }

    /**
     * Get the start time of the alarm in epoch.
     *
     * @param context Context
     * @return Time in epoch
     */
    private long getStartTime(@NonNull Context context) {
        SharedPreferences prefs;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS);
        return prefs.getLong(PREF_STARTTIME, 0);

    }
}