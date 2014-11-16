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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import net.mceoin.cominghome.LocationService;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.PrefsFragment;
import net.mceoin.cominghome.cloud.StatusArrivedHome;
import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;
import net.mceoin.cominghome.timehome.TimeHomeUpdate;

import java.util.List;

/**
 * Process entering and exiting of fences
 */
public class FenceHandling {

    public final static String TAG = FenceHandling.class.getSimpleName();
    public final static boolean debug = false;

    private static SharedPreferences prefs;
    private static FenceHandlingAlarm alarm= new FenceHandlingAlarm();

    public static void process(int transition, List<Geofence> geofences, Context context) {
        for (Geofence geofence : geofences) {
            if (geofence.getRequestId().equals(MainActivity.FENCE_HOME)) {
                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        arrivedHome(context);
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        leftHome(context);
                        break;
                    default:
                        Log.e(TAG, "unknown transition: " + transition);
                }
            } else if (geofence.getRequestId().equals(MainActivity.FENCE_WORK)) {
                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        leftWork(context);
                        break;
                    default:
                        Log.e(TAG, "unknown transition: " + transition);
                }
            }
        }

    }

    public static void arrivedHome(Context context) {
        if (debug) Log.d(TAG, "arrived home");

        // make sure there isn't an alarm set from a leftHome event
        alarm.CancelAlarm(context);

        HistoryUpdate.add(context, "Geofence arrived home");
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        long time_left_work = prefs.getLong(MainActivity.PREFS_TIME_LEFT_WORK, 0);

        if (time_left_work>0) {
            long currentTimeMillis=System.currentTimeMillis();
            long timeFromWorkToHome = (currentTimeMillis - time_left_work) / 1000;

            if ((timeFromWorkToHome < (4 * 60*60)) && (timeFromWorkToHome > (5 * 60))) {
                // only keep track if it took less than 4 hours and more than 5 minutes
                TimeHomeUpdate.add(context, timeFromWorkToHome);
            } else {
                if (debug) Log.d(TAG, "too much or not enough time: "+timeFromWorkToHome);
            }

        }

        if (!access_token.isEmpty()) {
            if (!structure_id.isEmpty()) {
                new StatusArrivedHome(context).execute();
            } else {
                if (debug) Log.d(TAG, "arrived home, but no structure_id");
            }

        } else {
            Log.e(TAG, "no access_token");
        }
        if (LocationService.isRunning(context)) {
            if (debug) Log.d(TAG, "LocationService is running, ensure tracking is stopped");
            Intent intent = new Intent(LocationService.TRACKING);
            intent.putExtra(LocationService.TRACKING_TYPE, LocationService.TRACKING_STOP);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    public static void leftHome(Context context) {
        if (debug) Log.d(TAG, "left home");

        HistoryUpdate.add(context,"Geofence left home");

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");

        if (!structure_id.isEmpty()) {
            // set an alarm to wait before we tell nest we're home.
            // sometimes the phone's geolocation will bounce out of an area and
            // then come back.
            alarm.SetAlarm(context);

        } else {
            Log.e(TAG, "missing structure_id");
        }
    }

    public static void leftWork(Context context) {
        if (debug) Log.d(TAG, "left work");

        HistoryUpdate.add(context,"Geofence left work");

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long currentTimeMillis=System.currentTimeMillis();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(MainActivity.PREFS_TIME_LEFT_WORK, currentTimeMillis);
        editor.apply();

        boolean trackEta = prefs.getBoolean(PrefsFragment.PREFERENCE_TRACK_ETA, false);

        if (trackEta) {
            if (LocationService.isRunning(context)) {
                if (debug) Log.d(TAG, "LocationService is running");
                Intent intent = new Intent(LocationService.TRACKING);
                intent.putExtra(LocationService.TRACKING_TYPE, LocationService.TRACKING_START);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } else {
                if (debug) Log.d(TAG, "Starting LocationService");
                LocationService.startService();
            }
        }
    }
}
