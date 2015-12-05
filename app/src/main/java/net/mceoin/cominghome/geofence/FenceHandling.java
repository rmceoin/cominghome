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
import android.location.Location;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.cloud.StatusArrivedHome;
import net.mceoin.cominghome.cloud.StatusLeftHome;
import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;
import net.mceoin.cominghome.service.DelayAwayService;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Process entering and exiting of fences
 */
public class FenceHandling {

    private static final String TAG = FenceHandling.class.getSimpleName();
    private static final boolean debug = false;

    private static SharedPreferences prefs;
    private static AsyncTask statusArrivedHome = null;
    private static AsyncTask statusLeftHome = null;

    public static void process(int transition, List<Geofence> geofences,
                               Location triggeringLocation, @NonNull Context context) {
        String fenceEnterId = MainActivity.FENCE_HOME;
        String fenceExitId = fenceEnterId + "-Exit";

        for (Geofence geofence : geofences) {
            if (geofence.getRequestId().equals(fenceEnterId) ||
                    geofence.getRequestId().equals(fenceExitId)) {
                switch (transition) {
                    case Geofence.GEOFENCE_TRANSITION_ENTER:
                        HistoryUpdate.add(context, "Geofence arrived home " +
                                locationToLatLon(triggeringLocation));
                        arrivedHome(context);
                        break;
                    case Geofence.GEOFENCE_TRANSITION_EXIT:
                        HistoryUpdate.add(context, "Geofence left home " +
                                locationToLatLon(triggeringLocation));
                        leftHome(context);
                        break;
                    default:
                        Log.e(TAG, "unknown transition: " + transition);
                }
            }
        }

    }

    public static String locationToLatLon(Location location) {
        if (location == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat("##.#######");
        return df.format(location.getLatitude()) + "," + df.format(location.getLongitude());
    }
    /**
     * If there is a known structure_id, then contact the backend to set home status.
     * <p/>
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
                cancelIfNotFinished(statusArrivedHome);
                cancelIfNotFinished(statusLeftHome);
                statusArrivedHome = new StatusArrivedHome(context).execute();
            } else {
                if (debug) Log.d(TAG, "arrived home, but no structure_id");
            }
        } else {
            Log.e(TAG, "no access_token");
        }
    }

    /**
     * If there is a known structure_id, then start the DelayAwayService, which will
     * eventually call {@link #executeLeftHome(Context)}
     *
     * @param context Application context
     * @see DelayAwayService
     */
    public static void leftHome(@NonNull Context context) {
        if (debug) {
            Log.d(TAG, "left home");
            HistoryUpdate.add(context, "leftHome()");
        }

        cancelIfNotFinished(statusArrivedHome);
        cancelIfNotFinished(statusLeftHome);

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");

        if (!structure_id.isEmpty()) {
            Intent myIntent = new Intent(context, DelayAwayService.class);
            context.startService(myIntent);
        } else {
            Log.e(TAG, "missing structure_id");
        }
    }

    public static void executeLeftHome(@NonNull Context context) {
        cancelIfNotFinished(statusArrivedHome);
        cancelIfNotFinished(statusLeftHome);
        statusLeftHome = new StatusLeftHome(context).execute();
    }

    /**
     * If the provided AsyncTask is not finished, then cancel it.
     *
     * @param asyncTask AsyncTask to cancel if needed.
     */
    private static void cancelIfNotFinished(@Nullable AsyncTask asyncTask) {
        if ((asyncTask != null) && (asyncTask.getStatus() != AsyncTask.Status.FINISHED)) {
            if (debug) Log.d(TAG, "cancelled previous task: " + asyncTask.getClass().getName());
            asyncTask.cancel(true);
        }
    }
}
