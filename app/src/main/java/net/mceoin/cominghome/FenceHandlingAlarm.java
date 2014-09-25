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

package net.mceoin.cominghome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import net.mceoin.cominghome.cloud.StatusLeftHome;

/**
 * Wait for about 15 minutes before telling the backend or Nest that we've left home.
 * This is needed because sometimes the phone will bounce out of the area briefly.
 */
public class FenceHandlingAlarm extends BroadcastReceiver {

    public final static String TAG = FenceHandlingAlarm.class.getSimpleName();
    public final static boolean debug = true;


    @Override
    public void onReceive(Context context, Intent intent) {
        if (debug) Log.d(TAG, "onReceive()");


        long currentTime = System.currentTimeMillis();
        long alarmStartTime = intent.getLongExtra("start", 0);
        long timeElapsedSeconds = (currentTime - alarmStartTime) / 1000;
        if (debug)
            Log.d(TAG, "extra alarmStartTime=" + alarmStartTime + " timeElapsedSeconds=" + timeElapsedSeconds);
        if (timeElapsedSeconds < (10 * 60)) {
            if (debug)
                Log.d(TAG, "not enough time has passed: " + timeElapsedSeconds + " = " + currentTime + " - " + alarmStartTime + "/1000");
            return;
        }

        new StatusLeftHome(context).execute();

        CancelAlarm(context);
    }

    public void SetAlarm(Context context) {
        if (debug) Log.d(TAG, "SetAlarm()");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, FenceHandlingAlarm.class);

        long alarmStartTime = System.currentTimeMillis();

        Bundle mBundle = new Bundle();
        mBundle.putLong("start", alarmStartTime);
        i.putExtras(mBundle);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (debug) Log.d(TAG, "alarmStartTime=" + alarmStartTime);

        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, FenceHandlingAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}