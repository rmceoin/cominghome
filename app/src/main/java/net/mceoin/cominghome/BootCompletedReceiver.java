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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import net.mceoin.cominghome.geofence.GeofenceRegister;
import net.mceoin.cominghome.geofence.SimpleGeofence;
import net.mceoin.cominghome.geofence.SimpleGeofenceStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Register GeoFences at boot time
 */
public class BootCompletedReceiver extends BroadcastReceiver {

    private final static String TAG = BootCompletedReceiver.class.getSimpleName();

    private GeofenceRegister mGeofenceRegister;

    List<Geofence> mCurrentGeofences;

    @Override
    public void onReceive(Context context, Intent arg1) {

        mGeofenceRegister = new GeofenceRegister(context);

        SimpleGeofenceStore mGeofenceStorage = new SimpleGeofenceStore(context);
        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<>();

        SimpleGeofence homeGeofence = mGeofenceStorage.getGeofence(MainActivity.FENCE_HOME);
        if (homeGeofence != null) {
            mCurrentGeofences.add(homeGeofence.toGeofence());
        }

        if (!mCurrentGeofences.isEmpty()) {
            updateGeofences();
        }
    }

    private void updateGeofences() {

        // Start the request. Fail if there's already a request in progress
        try {
            // Try to add geofences
            mGeofenceRegister.populateGeofenceList();
            mGeofenceRegister.addGeofences();
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "already queued");
        }
    }

}