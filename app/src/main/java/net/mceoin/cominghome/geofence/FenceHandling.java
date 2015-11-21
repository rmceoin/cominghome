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
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.cloud.StatusArrivedHome;
import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;
import net.mceoin.cominghome.service.DelayAwayService;

import java.util.List;

/**
 * Process entering and exiting of fences
 */
public class FenceHandling {

    private static final String TAG = FenceHandling.class.getSimpleName();
    private static final boolean debug = false;

    private static SharedPreferences prefs;

    public static void process(int transition, List<Geofence> geofences, @NonNull Context context) {
        String fenceEnterId = MainActivity.FENCE_HOME;
        String fenceExitId = fenceEnterId + "-Exit";

        for (Geofence geofence : geofences) {
            if (geofence.getRequestId().equals(fenceEnterId) ||
                    geofence.getRequestId().equals(fenceExitId)) {
                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        HistoryUpdate.add(context, "Geofence arrived home");
                        arrivedHome(context);
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        HistoryUpdate.add(context, "Geofence left home");
                        leftHome(context);
                        break;
                    default:
                        Log.e(TAG, "unknown transition: " + transition);
                }
            }
        }

    }

    /**
     * If there is a known structure_id, then contact the backend to set home status.
     *
     * Also tell {@link DelayAwayService} to cancel any timer it might have running.
     *
     * @param context Application context
     */
    public static void arrivedHome(@NonNull Context context) {
        if (debug) Log.d(TAG, "arrived home");

        Intent intent = new Intent();
        intent.setAction(DelayAwayService.ACTION_CANCEL_TIMER);
        context.sendBroadcast(intent);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");

        if (!access_token.isEmpty()) {
            if (!structure_id.isEmpty()) {
                new StatusArrivedHome(context).execute();
            } else {
                if (debug) Log.d(TAG, "arrived home, but no structure_id");
            }

        } else {
            Log.e(TAG, "no access_token");
        }
    }

    /**
     * If there is a known structure_id, then start the DelayAwayService
     *
     * @param context Application context
     *
     * @see DelayAwayService
     */
    public static void leftHome(@NonNull Context context) {
        if (debug) Log.d(TAG, "left home");

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");

        if (!structure_id.isEmpty()) {
            Intent myIntent = new Intent(context, DelayAwayService.class);
            context.startService(myIntent);
        } else {
            Log.e(TAG, "missing structure_id");
        }
    }
}
