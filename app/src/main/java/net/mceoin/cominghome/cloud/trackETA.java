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
package net.mceoin.cominghome.cloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import net.mceoin.cominghome.Installation;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.geofence.SimpleGeofence;
import net.mceoin.cominghome.geofence.SimpleGeofenceStore;
import net.mceoin.cominghome.api.myApi.MyApi;
import net.mceoin.cominghome.api.myApi.model.StatusBean;
import net.mceoin.cominghome.oauth.OAuthFlowApp;

import java.io.IOException;

public class trackETA extends AsyncTask<Void, Void, StatusBean> {
    private static final String TAG = trackETA.class.getSimpleName();
    private static final boolean debug = false;

    private static MyApi myApiService = null;
    private Context context;
    private String access_token;
    private String structure_id;
    private String InstallationId;
    private double latitude;
    private double longitude;
    SimpleGeofence homeGeofence;

    public trackETA(Context context, double latitude, double longitude) {
        this.context = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        InstallationId = Installation.id(context);
        this.latitude = latitude;
        this.longitude = longitude;

        SimpleGeofenceStore mGeofenceStorage;
        mGeofenceStorage = new SimpleGeofenceStore(context);
        homeGeofence = mGeofenceStorage.getGeofence(MainActivity.FENCE_HOME);
    }

    @Override
    protected StatusBean doInBackground(Void... params) {
        if (myApiService == null) { // Only do this once
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null);

            myApiService = builder.build();
        }

        if ((access_token == null) || (access_token.isEmpty())) {
            Log.w(TAG, "missing access_token");
            return null;
        }
        if ((structure_id == null) || (structure_id.isEmpty())) {
            Log.w(TAG, "missing structure_id");
            return null;
        }
        if (homeGeofence == null) {
            Log.w(TAG,"missing home fence");
            return null;
        }
        try {
            return myApiService.trackETA(InstallationId, access_token, structure_id, latitude,
                    longitude, homeGeofence.getLatitude(), homeGeofence.getLongitude()).execute();
        } catch (IOException e) {
            Log.w(TAG, "IOException: " + e.getLocalizedMessage());
            return null;
        }
    }

    @Override
    protected void onPostExecute(StatusBean result) {
        if (debug) Log.d(TAG, "got result: " + result.getMessage());
//        HistoryUpdate.add(context, "Track ETA");
    }
}