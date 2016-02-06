/*
 * Copyright (C) 2016 Randy McEoin
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
 *
 * Grabbed from OpenIntents OI Safe project
 */
package net.mceoin.cominghome.gcm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import net.mceoin.cominghome.cloud.StatusLeftHome;
import net.mceoin.cominghome.history.HistoryUpdate;

/**
 * Handles actions from {@link GcmLeftHomeNotification}.
 */
public class GcmLeftHomeService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "GcmLeftHomeService";
    private static final boolean debug = true;

    public static final String ACTION_START = "net.mceoin.cominghome.gcm.gcmlefthomeservice.START";
    public static final String ACTION_CANCEL = "net.mceoin.cominghome.gcm.gcmlefthomeservice.CANCEL_TIMER";
    public static final String ACTION_AWAY = "net.mceoin.cominghome.gcm.gcmlefthomeservice.AWAY";

    SharedPreferences mPreferences;
    private BroadcastReceiver mIntentReceiver;

    private AsyncTask statusLeftHome;

    protected GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        if (debug) { Log.d(TAG, "onCreate"); }

        mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (debug) { Log.d(TAG, "onReceive"); }
                if (intent.getAction().equals(ACTION_START)) {
                    if (debug) { Log.d(TAG, "ACTION_START"); }
//                    GcmLeftHomeNotification.startNotification(context);
                } else if (intent.getAction().equals(ACTION_CANCEL)) {
                    if (debug) { Log.d(TAG, "ACTION_CANCEL"); }
                    cancelNotification();
                } else if (intent.getAction().equals(ACTION_AWAY)) {
                    if (debug) { Log.d(TAG, "ACTION_AWAY"); }
                    triggerBackendAway(context);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_START);
        filter.addAction(ACTION_CANCEL);
        filter.addAction(ACTION_AWAY);
        registerReceiver(mIntentReceiver, filter);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        buildGoogleApiClient();
    }

    /**
     * Clear the notification and issue {@link GcmLeftHome} cancelAllTasks().
     */
    private void cancelNotification() {
        if (debug) {
            Log.d(TAG, "cancelNotification");
        }
        GcmLeftHomeNotification.clearNotification(GcmLeftHomeService.this);
        GcmLeftHome.cancelAllTasks(getApplicationContext());
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        if (debug) {
            HistoryUpdate.add(getApplicationContext(), "onDestroy()");
            Log.d(TAG, "onDestroy");
        }
        unregisterReceiver(mIntentReceiver);
        cancelNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Tell the backend that we're now away.
     */
    private void triggerBackendAway(@NonNull Context context) {
        if (debug) { Log.d(TAG, "triggerBackendAway"); }
        if (statusLeftHome != null) {
            statusLeftHome.cancel(true);
        }
        statusLeftHome = new StatusLeftHome(context).execute(1);
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (debug) {
            Log.d(TAG, "onConnected()");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (debug) {
            Log.d(TAG, "onConnectionSuspended()");
        }
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (debug) {
            Log.d(TAG, "onConnectionFailed()");
        }

    }
}
