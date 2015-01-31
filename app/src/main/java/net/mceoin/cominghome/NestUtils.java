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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;
import net.mceoin.cominghome.structures.StructuresUpdate;

import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Nest utility functions
 * <p/>
 * Some handy info is at:
 * <p/>
 * http://stackoverflow.com/questions/24601798/acquiring-and-changing-basic-data-on-the-nest-thermostat
 */
public class NestUtils {
    public static final String TAG = NestUtils.class.getSimpleName();
    public static final boolean debug = true;

    public static final String GOT_INFO = "net.mceoin.cominghome.NetUtils.GotInfo";
    public static final String LOST_AUTH = "net.mceoin.cominghome.NetUtils.LostAuth";

    public static void getInfo(final Context context, final String access_token, final String redirectLocation) {
        if (debug) Log.d(TAG, "getInfo()");
        if (context == null) {
            Log.e(TAG, "missing context");
            return;
        }

        // Tag used to cancel the request
        String tag_update_status = "nest_info_req";

        String url = "https://developer-api.nest.com/structures?auth=" + access_token;
        if ((redirectLocation!=null) && (redirectLocation.isEmpty()) ){
            url = redirectLocation;
        }

        JsonObjectRequest updateStatusReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (debug) Log.d(TAG, "response=" + response.toString());

                        // curl -v -L https://developer-api.nest.com/structures?auth=...
                        // {
                        // "structure_id":"njBTS-gAhF1mJ8_oF23ne7JNDyx1m1hULWixOD6IQWEe-SFA",
                        // "thermostats":["n232323jy8Xr1HVc2OGqICVP45i-Mc"],
                        // "smoke_co_alarms":["pt00ag34grggZchI7ICVPddi-Mc"],
                        // "country_code":"US",
                        // "away":"home"
                        // "name":"Home"
                        // }

                        String structure_id = "";
                        String structure_name = "";
                        String away_status = "";

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
                        boolean structure_id_selected = prefs.contains(MainActivity.PREFS_STRUCTURE_ID);
                        String structure_id_current = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
                        boolean current_in_structures = false;
                        String last_structure_name = "";
                        String last_away_status = "";
                        HashSet<String> structure_ids = new HashSet<>();

                        // use this for faking additional structures
                        if (debug) {
                            StructuresUpdate.update(context, "demo_id", "Demo Structure", "home");
                            structure_ids.add("demo_id");
                        }

                        JSONObject structures;
                        try {
                            structures = new JSONObject(response.toString());
                            Iterator<String> keys = structures.keys();
                            while (keys.hasNext()) {
                                String structure = keys.next();
                                JSONObject value = structures.getJSONObject(structure);
                                if (debug) Log.d(TAG, "value=" + value);
                                structure_id = value.getString("structure_id");
                                structure_name = value.getString("name");
                                away_status = value.getString("away");

                                StructuresUpdate.update(context, structure_id, structure_name, away_status);
                                structure_ids.add(structure_id);

                                if (structure_id_selected) {
                                    if (structure_id.equals(structure_id_current)) {
                                        current_in_structures = true;
                                        //
                                        // found the structure_id that we're associated with
                                        // go ahead and update the name and away status
                                        //
                                        SharedPreferences.Editor pref = prefs.edit();
                                        pref.putString(MainActivity.PREFS_STRUCTURE_NAME, structure_name);
                                        pref.putString(MainActivity.PREFS_LAST_AWAY_STATUS, away_status);
                                        pref.apply();

                                        last_structure_name = structure_name;
                                        last_away_status = away_status;
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "error parsing JSON");
                            return;
                        }

                        if (structure_id_selected) {
                            if (!current_in_structures) {
                                // there is a structure_id selected, but it is no longer in the structures
                                // listed from Nest, so clear it out
                                SharedPreferences.Editor pref = prefs.edit();
                                pref.remove(MainActivity.PREFS_STRUCTURE_ID);
                                pref.remove(MainActivity.PREFS_STRUCTURE_NAME);
                                pref.remove(MainActivity.PREFS_LAST_AWAY_STATUS);
                                pref.apply();
                            }
                        } else {
                            //
                            // No structure_id currently selected, so go ahead and pick the
                            // last structure_id.  This should work for most homes that will
                            // only have one structure_id anyway.
                            //
                            SharedPreferences.Editor pref = prefs.edit();
                            pref.putString(MainActivity.PREFS_STRUCTURE_ID, structure_id);
                            pref.putString(MainActivity.PREFS_STRUCTURE_NAME, structure_name);
                            pref.putString(MainActivity.PREFS_LAST_AWAY_STATUS, away_status);
                            pref.apply();

                            last_structure_name = structure_name;
                            last_away_status = away_status;
                        }

                        HashSet<String> stored_structure_ids = StructuresUpdate.getStructureIds(context);
                        for (String id : stored_structure_ids) {
                            if (!structure_ids.contains(id)) {
                                //
                                // if we have a stored id that wasn't in the array Nest just sent us,
                                // then delete it from our database
                                //
                                StructuresUpdate.deleteStructureId(context, id);
                            }
                        }
                        Intent intent = new Intent(GOT_INFO);
                        intent.putExtra("structure_name", last_structure_name);
                        intent.putExtra("away_status", last_away_status);
                        LocalBroadcastManager.getInstance(AppController.getInstance().getApplicationContext()).sendBroadcast(intent);

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (error.networkResponse != null) {
                    if (debug)
                        Log.d(TAG, "getInfo volley statusCode=" + error.networkResponse.statusCode);

                    Context context = AppController.getInstance().getApplicationContext();

                    if (error.networkResponse.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        // We must have been de-authorized at the Nest web site
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor pref = prefs.edit();
                        pref.putString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
                        pref.putString(MainActivity.PREFS_STRUCTURE_ID, "");
                        pref.putString(MainActivity.PREFS_STRUCTURE_NAME, "");
                        pref.putString(MainActivity.PREFS_LAST_AWAY_STATUS, "");
                        pref.apply();

                        HistoryUpdate.add(context, "Lost our Nest authorization");

                        Intent intent = new Intent(LOST_AUTH);
                        LocalBroadcastManager.getInstance(AppController.getInstance().getApplicationContext()).sendBroadcast(intent);
                    } else if (error.networkResponse.statusCode == HttpStatus.SC_TEMPORARY_REDIRECT) {
                        if ((redirectLocation == null) && error.networkResponse.headers.containsKey("Location")) {
                            String location = error.networkResponse.headers.get("Location");
                            getInfo(context, access_token, location);
                        } else {
                            HistoryUpdate.add(context, "getInfo Error: Temporary redirect");
                        }
                    } else {
                        HistoryUpdate.add(context, "getInfo Error: " + error.getLocalizedMessage() + ":" +
                            error.networkResponse.statusCode);
                        VolleyLog.d(TAG, "getInfo Error: " + error.getMessage());
                    }
                }
            }
        });
        AppController.getInstance().addToRequestQueue(updateStatusReq, tag_update_status);
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the main Activity.
     *
     * @param transitionType The type of transition that occurred.
     */
    public static void sendNotification(Context context, String transitionType) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = prefs.getBoolean(MainActivity.PREFS_NOTIFICATIONS, true);

        if (!notifications) {
            if (debug) Log.d(TAG, "notifications are turned off");
            return;
        }

        // Create an explicit content Intent that starts the main Activity
        Intent notificationIntent =
                new Intent(context, MainActivity.class);

        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the main Activity to the task stack as the parent
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Set the notification contents
        builder.setSmallIcon(R.drawable.home)
                .setContentTitle(
                        context.getString(R.string.nest_transition_notification_title,
                                transitionType))
                .setContentText(context.getString(R.string.nest_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


}
