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
import android.os.CountDownTimer;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import net.mceoin.cominghome.PrefsFragment;
import net.mceoin.cominghome.R;
import net.mceoin.cominghome.geofence.FenceHandling;

/**
 * Delay for 15 minutes before contacting the backend to set away status.
 */
public class DelayAwayService extends Service {
    private static final String TAG = "DelayAwayService";
    private static final boolean debug = false;

    private static long timeRemaining = 0;
    SharedPreferences mPreferences;
    private CountDownTimer t;
    private BroadcastReceiver mIntentReceiver;

    public static final String ACTION_START_TIMER = "net.mceoin.cominghome.action.START_TIMER";
    public static final String ACTION_CANCEL_TIMER = "net.mceoin.cominghome.action.CANCEL_TIMER";
    public static final String ACTION_AWAY = "net.mceoin.cominghome.action.AWAY";

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
    }

    /**
     * Clear the progress notification and cancel the {@link CountDownTimer}.
     */
    private void cancelTimer() {
        if (debug) {
            Log.d(TAG, "cancelTimer");
        }
        DelayAwayNotification.clearNotification(DelayAwayService.this);
        if (t != null) {
            t.cancel();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug) {
            Log.d(TAG, "Received start id " + startId + ": " + intent + ": " + this);
        }
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
        DelayAwayNotification.startNotification(DelayAwayService.this);
        int timeoutMinutes = mPreferences.getInt(
                PrefsFragment.PREFERENCE_AWAY_DELAY,
                getResources().getInteger(R.integer.away_delay_default)
        );
        final long timeoutUntilStop = timeoutMinutes * 60000;

        if (debug) {
            Log.d(TAG, "startTimer with timeoutUntilStop=" + timeoutUntilStop);
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
                timeRemaining = millisUntilFinished;
                int timeElapsed = (int) (timeoutUntilStop - timeRemaining);
                DelayAwayNotification.updateProgress(
                        getApplicationContext(),
                        (int) timeoutUntilStop,
                        timeElapsed
                );
            }

            public void onFinish() {
                if (debug) {
                    Log.d(TAG, "onFinish()");
                }
                triggerBackendAway(getApplicationContext());
                timeRemaining = 0;
            }
        };
        t.start();
        timeRemaining = timeoutUntilStop;
        if (debug) {
            Log.d(TAG, "Timer started with: " + timeoutUntilStop);
        }
    }
}
