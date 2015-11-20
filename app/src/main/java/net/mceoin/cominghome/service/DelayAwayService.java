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
import android.util.Log;

import net.mceoin.cominghome.PrefsFragment;

public class DelayAwayService extends Service {

    private static final boolean debug = true;
    private static final String TAG = "DelayAwayService";
    private static long timeRemaining = 0;
    SharedPreferences mPreferences;
    private CountDownTimer t;
    private BroadcastReceiver mIntentReceiver;

    public static final String ACTION_START_TIMER = "net.mceoin.cominghome.action.START_TIMER";

    @Override
    public void onCreate() {
        if (debug) { Log.d(TAG, "onCreate"); }
        mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_START_TIMER)) {
                    startTimer();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_START_TIMER);
        registerReceiver(mIntentReceiver, filter);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug) { Log.d(TAG, "Received start id " + startId + ": " + intent + ": " + this); }
        startTimer();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (debug) { Log.d(TAG, "onDestroy"); }
        unregisterReceiver(mIntentReceiver);
        ServiceNotification.clearNotification(DelayAwayService.this);
        if (t != null) {
            t.cancel();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Tell the backend that we're now away
     */
    private void triggerBackendAway() {
        if (debug) { Log.d(TAG, "triggerBackendAway"); }
        ServiceNotification.clearNotification(DelayAwayService.this);
        //TODO: tell the backend
        if (t != null) {
            t.cancel();
        }
    }

    /**
     * Start a CountDownTimer() that will trigger an away status
     */
    private void startTimer() {
        ServiceNotification.setNotification(DelayAwayService.this);
        String timeout = mPreferences.getString(
                PrefsFragment.PREFERENCE_AWAY_DELAY,
                PrefsFragment.PREFERENCE_AWAY_DELAY_DEFAULT_VALUE
        );
        int timeoutMinutes = 15; // default to 15
        try {
            timeoutMinutes = Integer.valueOf(timeout);
        } catch (NumberFormatException e) {
            Log.d(TAG, "why is away_delay busted?");
        }
        final long timeoutUntilStop = timeoutMinutes * 60000;

        if (debug) { Log.d(TAG, "startTimer with timeoutUntilStop=" + timeoutUntilStop); }

        if (t != null) {
            // if there was a previous timer, make sure it's cancelled
            t.cancel();
        }

        t = new CountDownTimer(timeoutUntilStop, 30000) {

            public void onTick(long millisUntilFinished) {
                // doing nothing.
                if (debug) {
                    Log.d(TAG, "tick: " + millisUntilFinished + " this=" + this);
                }
                timeRemaining = millisUntilFinished;
                int timeElapsed = (int)(timeoutUntilStop - timeRemaining);
                ServiceNotification.updateProgress(
                        (int) timeoutUntilStop,
                        timeElapsed
                );
            }

            public void onFinish() {
                if (debug) {
                    Log.d(TAG, "onFinish()");
                }
                triggerBackendAway();
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
