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
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import net.mceoin.cominghome.R;
import net.mceoin.cominghome.history.HistoryUpdate;

/**
 * Wait for about 15 minutes before telling the backend or Nest that we've left home.
 * This is needed because sometimes the phone will bounce out of the area briefly.
 * <p/>
 * Some really good detail on dumping alarms at
 * http://stackoverflow.com/questions/28742884/how-to-read-adb-shell-dumpsys-alarm-output#31600886
 * <p/>
 * Use the following to examine alarms:
 * <pre>
 * adb -e shell dumpsys alarm
 *
 *  ELAPSED_WAKEUP #2: Alarm{386b1e78 type 2 when 900000 net.mceoin.cominghome}
 *    tag=*walarm*:net.mceoin.cominghome/.geofence.FenceHandlingAlarm
 *    type=2 whenElapsed=+12m45s825ms when=+12m45s825ms
 *    window=-1 repeatInterval=900000 count=0
 *    operation=PendingIntent{31b4b951: PendingIntentRecord{200fc5b6 net.mceoin.cominghome broadcastIntent}}
 * </pre>
 * In the above example, the alarm will trigger in 12 minutes 45 seconds.
 */
public class FenceHandlingAlarm extends BroadcastReceiver {

    public final static String TAG = FenceHandlingAlarm.class.getSimpleName();
    public final static boolean debug = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (debug) Log.d(TAG, "onReceive()");

        long currentTime = System.currentTimeMillis();
        long alarmStartTime = AlarmTime.getTime(context);

        long timeElapsedSeconds = (currentTime - alarmStartTime) / 1000;
        if (debug)
            Log.d(TAG, "extra alarmStartTime=" + alarmStartTime + " timeElapsedSeconds=" + timeElapsedSeconds);

        int minimumMinutes = 10;
        if (timeElapsedSeconds < (minimumMinutes * 60)) {
            if (debug)
                Log.d(TAG, "not enough time has passed: (" + timeElapsedSeconds + " = " + currentTime + " - " + alarmStartTime + ")/1000");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SetAlarm(context, false);
            }
            return;
        }

        if (WiFiUtils.isCurrentSsidSameAsStored(context)) {
            HistoryUpdate.add(context, context.getString(R.string.still_on_home_wifi));
        } else {
            if (debug) {
                Log.d(TAG, "kick off executeLeftHome()");
                HistoryUpdate.add(context, "kick off executeLeftHome()");
            }
            //
            // FYI, the alarm will be on the main thread, so don't try network calls directly
            //
            FenceHandling.executeLeftHome(context);
        }

        CancelAlarm(context);
    }

    /**
     * Set an alarm to wait before we actually send the backend a left home status update.
     *
     * @param context Context
     * @param saveStart Save the starting time of this alarm
     */
    public void SetAlarm(@NonNull Context context, boolean saveStart) {
        if (debug) Log.d(TAG, "SetAlarm()");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, FenceHandlingAlarm.class);

        long alarmStartTime = System.currentTimeMillis();
        if (saveStart) {
            AlarmTime.setTime(context, alarmStartTime);
        }

        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (debug) {
            Log.d(TAG, "alarmStartTime=" + alarmStartTime);
            HistoryUpdate.add(context, "alarmStartTime=" + alarmStartTime);
        }

//        long timeToWakeup = SystemClock.elapsedRealtime() + (5 * 60 * 1000);
        long timeToWakeup = alarmStartTime + (15 * 60 * 1000);
        long interval = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //
            // With Marshmallow and up we're not using a repeating alarm since this
            // specific alarm call is documented as good for use with Doze mode
            //
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeToWakeup, pi);
            if (debug) Log.d(TAG, "used setAndAllowWhileIdle()");
        } else {
            am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    timeToWakeup,
                    interval, pi);
        }
    }

    public void CancelAlarm(@NonNull Context context) {
        Intent intent = new Intent(context, FenceHandlingAlarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}