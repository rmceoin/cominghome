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
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import net.mceoin.cominghome.Installation;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.NestUtils;
import net.mceoin.cominghome.PrefsFragment;
import net.mceoin.cominghome.R;
import net.mceoin.cominghome.api.myApi.MyApi;
import net.mceoin.cominghome.api.myApi.model.StatusBean;
import net.mceoin.cominghome.gcm.GcmRegister;
import net.mceoin.cominghome.geofence.WiFiUtils;
import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;

import java.io.IOException;
import java.util.Random;

/**
 * Informs the backend via Endpoints that the status is now away.  If appropriate, the backend will
 * set Nest to "away".  Multiple attempts are made.  After the last retry, no further
 * attempts are made.
 */
public class StatusLeftHome extends AsyncTask<Void, Void, StatusBean> {
    private static final String TAG = StatusLeftHome.class.getSimpleName();
    private static final boolean debug = false;

    private static MyApi myApiService = null;
    private Context context;
    private String access_token;
    private String structure_id;
    private String InstallationId;
    private boolean tell_nest;
    private String regid;

    public StatusLeftHome(Context context) {
        this.context = context;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        InstallationId = Installation.id(context);
        tell_nest = prefs.getBoolean(PrefsFragment.key_tell_nest_on_leaving_home, true);
        regid = prefs.getString(GcmRegister.PROPERTY_REG_ID, GcmRegister.PROPERTY_REG_ID_NONE);
    }

    @Override
    protected StatusBean doInBackground(Void... params) {
        if (myApiService == null) { // Only do this once
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), null);

            builder.setApplicationName(CloudUtil.getApplicationName(context));
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
        if ((regid == null) || (regid.isEmpty())) {
            Log.w(TAG, "missing regid");
            return null;
        }
        String lastExceptionMessage = null;
        int retry = 0;
        while (retry < 15) {
            try {
                if (WiFiUtils.isCurrentSsidSameAsStored(context)) {
                    if (debug) Log.d(TAG, "we're associated with home SSID, aborting");
                    HistoryUpdate.add(context, context.getString(R.string.back_on_home_wifi));
                    return null;
                }
                return myApiService.leftHome(InstallationId, access_token, structure_id, tell_nest, false, "-", regid).execute();
            } catch (IOException e) {
                Log.w(TAG, "IOException: " + e.getLocalizedMessage());
                String networkStatus = CloudUtil.getNetworkStatus(context);
                lastExceptionMessage = e.getLocalizedMessage() + " " + networkStatus;
            }
            if (isCancelled()) return null;
            try {
                Random randomGenerator = new Random();
                int seconds = (retry * 90) + randomGenerator.nextInt(15);
                int maxSeconds = 15 * 60;
                if (seconds > maxSeconds) seconds = maxSeconds;
                if (debug) Log.d(TAG,
                        "retry in " + seconds + " seconds");
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                if (debug) Log.d(TAG, "interrupted");
                return null;
            }
            retry++;
            if (isCancelled()) return null;
        }
        HistoryUpdate.add(context, "Unable to connect to Backend: " + lastExceptionMessage);
        return null;
    }

    @Override
    protected void onPostExecute(@Nullable StatusBean result) {
        if (debug && (result != null)) Log.d(TAG, "got result: " + result.getMessage());

        if ((result == null) && isCancelled()) {
            if (debug) Log.d(TAG, "onPostExecute: was cancelled");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        tell_nest = prefs.getBoolean(PrefsFragment.key_tell_nest_on_leaving_home, true);

        if (tell_nest) {
            if (result != null) {
                if (result.getNestSuccess()) {
                    if (result.getNestUpdated()) {
                        NestUtils.sendNotificationTransition(context, "Away");
                        HistoryUpdate.add(context, "Backend updated: Nest Away");
                    } else {
                        if (result.getOthersAtHome()) {
                            HistoryUpdate.add(context, "Backend updated: Nest not updated: Others at home");
                        } else {
                            HistoryUpdate.add(context, "Backend updated: Nest already away");
                        }
                    }
                } else {
                    if (result.getMessage().contains("Unauthorized")) {
                        NestUtils.lostAuthorization(context);
                    } else {
                        HistoryUpdate.add(context, "Backend updated: Nest errored: " + result.getMessage());
                    }
                }
            } else {
                HistoryUpdate.add(context, "Backend updated");
            }
        }
    }
}