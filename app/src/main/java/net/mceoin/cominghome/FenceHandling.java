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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import net.mceoin.cominghome.oauth.OAuthFlowApp;

import java.util.List;

/**
 * Process entering and exiting of fences
 */
public class FenceHandling {

    public final static String TAG = FenceHandling.class.getSimpleName();
    public final static boolean debug = true;

    private static SharedPreferences prefs;

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
                        Log.e(TAG,"unknown transition: "+transition);
                }
            }
        }

    }

/*
    static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (debug) {
                Log.d(TAG, "handling messages");
            }
            Bundle b = msg.getData();
            String msgType = b.getString("type");

            if (msgType.equals(NestUtils.MSG_STRUCTURES)) {
                String away_status = b.getString("away_status");
                if (debug)
                    Log.d(TAG, "away_status=" + away_status);

                if ((away_status.equals("away") || (away_status.equals("auto-away")))) {
                    if (debug) Log.d(TAG, "Nest set to away, but we've arrived home!");
                    if (prefs!=null) {
                        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
                        String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");

                        NestUtils.sendAwayStatus(access_token, handler, structure_id, "home");
                    } else {
                        Log.e(TAG,"missing prefs");
                    }
                } else if (away_status.equals("home")) {
                    if (debug) Log.d(TAG, "Nest is already set to home.");
                } else {
                    if (debug) Log.e(TAG, "Nest is set to unknown status: " + away_status);
                }
            } else {
                Log.e(TAG,"unknown msgType: "+msgType);
            }
            // allow this thread to die off
//            Looper.myLooper().quit();
        }
    };
*/

    private static void arrivedHome(Context context) {
        if (debug) Log.d(TAG, "arrived home");
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        boolean tellNest = prefs.getBoolean(PrefsFragment.key_tell_nest_on_arrival_home, true);

        if (!access_token.isEmpty()) {
            if (tellNest) {
                NestUtils.getInfo(context, access_token, null, NestUtils.POST_ACTION_IF_AWAY_SET_HOME);
            } else {
                if (debug) Log.d(TAG,"don't tell nest");
            }

            if (!structure_id.isEmpty()) {
                BackendUtils.updateStatus(context, structure_id, "home");
            } else {
                if (debug) Log.d(TAG,"arrived home, but no structure_id");
            }

            // Loop so this thread stays alive in order to receive the handler message
//            if (Looper.myLooper() == null) Looper.prepare();
//            Looper.loop();
        } else {
            Log.e(TAG, "no access_token");
        }
    }

    private static void leftHome(Context context) {
        if (debug) Log.d(TAG, "left home");

        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        boolean tellNest = prefs.getBoolean(PrefsFragment.key_tell_nest_on_leaving_home, true);

        if (!structure_id.isEmpty()) {
            if (tellNest) {
                BackendUtils.getOthers(null, null, structure_id, BackendUtils.POST_ACTION_IF_NOBODY_HOME_SET_AWAY);
            }
            BackendUtils.updateStatus(context, structure_id, "away");

            // Loop so this thread stays alive in order to receive the handler message
//            if (Looper.myLooper() == null) Looper.prepare();
//            Looper.loop();
        } else {
            Log.e(TAG,"missing structure_id");
        }
    }
}
