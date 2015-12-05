/* 
 * Copyright 2015 Randy McEoin
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
 * Grabbed from OpenIntents OI Safe
 */
package net.mceoin.cominghome.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import net.mceoin.cominghome.LocationUtils;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.PrefsFragment;
import net.mceoin.cominghome.R;
import net.mceoin.cominghome.geofence.FenceHandling;
import net.mceoin.cominghome.geofence.SimpleGeofence;
import net.mceoin.cominghome.geofence.SimpleGeofenceStore;
import net.mceoin.cominghome.geofence.WiFiUtils;
import net.mceoin.cominghome.history.HistoryUpdate;

/**
 * Delay for 15 minutes before contacting the backend to set away status.
 */
public class DelayAwayService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DelayAwayService";
    private static final boolean debug = true;

    private static int tickCount;
    private static long timeRemaining = 0;
    private static boolean sawHomeWiFi = false;
    private static boolean sawHomeLocation = false;
    SharedPreferences mPreferences;
    private CountDownTimer t;
    private BroadcastReceiver mIntentReceiver;

    public static final String ACTION_START_TIMER = "net.mceoin.cominghome.action.START_TIMER";
    public static final String ACTION_CANCEL_TIMER = "net.mceoin.cominghome.action.CANCEL_TIMER";
    public static final String ACTION_AWAY = "net.mceoin.cominghome.action.AWAY";

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        if (debug) {
            Log.d(TAG, "onCreate");
        }
        mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (debug) {
                    Log.d(TAG, "onReceive");
                }
                if (intent.getAction().equals(ACTION_START_TIMER)) {
                    if (debug) HistoryUpdate.add(context, "onReceive: ACTION_START_TIMER");
                    startTimer();
                } else if (intent.getAction().equals(ACTION_CANCEL_TIMER)) {
                    cancelTimer();
                } else if (intent.getAction().equals(ACTION_AWAY)) {
                    triggerBackendAway(context);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_START_TIMER);
        filter.addAction(ACTION_CANCEL_TIMER);
        filter.addAction(ACTION_AWAY);
        registerReceiver(mIntentReceiver, filter);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        buildGoogleApiClient();
    }

    /**
     * Clear the progress notification and cancel the {@link CountDownTimer}.
     */
    private void cancelTimer() {
        if (debug) {
            Log.d(TAG, "cancelTimer");
            HistoryUpdate.add(getApplicationContext(), "cancelTimer()");
        }
        DelayAwayNotification.clearNotification(DelayAwayService.this);
        if (t != null) {
            t.cancel();
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug) {
            Log.d(TAG, "Received start id " + startId + ": " + intent + ": " + this);
            HistoryUpdate.add(getApplicationContext(), "onStartCommand()");
        }
        tickCount = 0;
        sawHomeWiFi = false;
        sawHomeLocation = false;
        mGoogleApiClient.connect();
        startTimer();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (debug) {
            Log.d(TAG, "onDestroy");
        }
        unregisterReceiver(mIntentReceiver);
        cancelTimer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * <p>Tell the backend that we're now away.</p>
     * Cancel the timer with {@link #cancelTimer()}, and execute
     * Google Cloud Endpoint StatusLeftHome.
     */
    private void triggerBackendAway(@NonNull Context context) {
        if (debug) {
            Log.d(TAG, "triggerBackendAway");
        }
        cancelTimer();
        FenceHandling.executeLeftHome(context);
    }

    /**
     * Start a {@link CountDownTimer} that will trigger an away status after
     * {@link PrefsFragment#PREFERENCE_AWAY_DELAY} minutes.
     * The {@link CountDownTimer} will have an interval of 30 seconds
     * in order to update the progress bar in the notification.
     */
    private void startTimer() {
        int timeoutMinutes = mPreferences.getInt(
                PrefsFragment.PREFERENCE_AWAY_DELAY,
                getResources().getInteger(R.integer.away_delay_default)
        );
        final long timeoutUntilStop = timeoutMinutes * 60000;

        if (debug) {
            Log.d(TAG, "startTimer with timeoutUntilStop=" + timeoutUntilStop);
            HistoryUpdate.add(getApplicationContext(), "startTimer()");
        }

        if (t != null) {
            // if there was a previous timer, make sure it's cancelled
            t.cancel();
        }

        t = new CountDownTimer(timeoutUntilStop, 30000) {

            public void onTick(long millisUntilFinished) {
                // at each interval tick update the notification progress
                if (debug) {
                    Log.d(TAG, "tick: " + millisUntilFinished + " this=" + this);
                }
                tickCount++;
                timeRemaining = millisUntilFinished;
                int timeElapsed = (int) (timeoutUntilStop - timeRemaining);

                if (tickCount == 4) {
                    //
                    // let the user know we're watching after 120 seconds
                    //
                    DelayAwayNotification.startNotification(DelayAwayService.this);
                }
                if (tickCount < 5) {
                    //
                    // kill time for the first 120 seconds after a geofence away trigger
                    //
                    return;
                }
                //
                // 2:30 minutes should have elapsed by now, so start checking WiFi and location
                //
                if (!sawHomeWiFi && !sawHomeLocation) {
                    if (WiFiUtils.isCurrentSsidSameAsStored(getApplicationContext())) {
                        //
                        // we have not seen the home wifi yet, but we do now
                        //
                        sawHomeWiFi = true;
                        DelayAwayNotification.clearNotification(getApplicationContext());
                        HistoryUpdate.add(getApplicationContext(), getString(R.string.back_at_home_wifi));

                        //
                        // can't directly cancel the timer from within the timer
                        // so we send a broadcast to ourselves
                        //
                        Intent intent = new Intent();
                        intent.setAction(DelayAwayService.ACTION_CANCEL_TIMER);
                        getApplicationContext().sendBroadcast(intent);

                    } else if (atHome()) {
                        sawHomeLocation = true;
                        if (debug) {
                            Log.d(TAG, "we're at home!");
                        }
                        DelayAwayNotification.clearNotification(getApplicationContext());
                        HistoryUpdate.add(getApplicationContext(), getString(R.string.still_at_home_location));

                        Intent intent = new Intent();
                        intent.setAction(DelayAwayService.ACTION_CANCEL_TIMER);
                        getApplicationContext().sendBroadcast(intent);

                    } else {
                        DelayAwayNotification.updateProgress(
                                getApplicationContext(),
                                (int) timeoutUntilStop,
                                timeElapsed
                        );
                    }
                }
            }

            public void onFinish() {
                if (debug) {
                    Log.d(TAG, "onFinish()");
                }
                if (!sawHomeWiFi && WiFiUtils.isCurrentSsidSameAsStored(getApplicationContext())) {
                    //
                    // one more check for home wifi if we haven't already seen it
                    //
                    sawHomeWiFi = true;
                    HistoryUpdate.add(getApplicationContext(), getString(R.string.back_at_home_wifi));
                }
                if (!sawHomeLocation && atHome()) {
                    sawHomeLocation = true;
                    HistoryUpdate.add(getApplicationContext(), getString(R.string.still_at_home_location));
                }
                if (sawHomeWiFi || sawHomeLocation) {
                    DelayAwayNotification.clearNotification(getApplicationContext());
                    cancelTimer();
                } else {
                    triggerBackendAway(getApplicationContext());
                }
                timeRemaining = 0;
            }
        };
        t.start();
        timeRemaining = timeoutUntilStop;
        if (debug) {
            Log.d(TAG, "Timer started with: " + timeoutUntilStop);
        }
    }

    /**
     * Check geolocation to see if within home radius
     *
     * @return true if at home
     */
    private boolean atHome() {
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mCurrentLocation == null) {
            return false;
        }
        if (debug) Log.d(TAG, mCurrentLocation.toString());
        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();

        SimpleGeofenceStore mGeofenceStorage;
        mGeofenceStorage = new SimpleGeofenceStore(getApplicationContext());
        SimpleGeofence geofence = mGeofenceStorage.getGeofence(MainActivity.FENCE_HOME);

        float distFromHome = LocationUtils.distFrom(latitude, longitude, geofence.getLatitude(),
                geofence.getLongitude());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int fenceRadius = prefs.getInt(PrefsFragment.PREFERENCE_GEOFENCE_RADIUS,
                getResources().getInteger(R.integer.geofence_radius_default));

        if (debug) Log.d(TAG, "distFromHome=" + distFromHome);
        return distFromHome < fenceRadius;
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
        if (t != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (debug) {
            Log.d(TAG, "onConnectionFailed()");
        }

    }
}
