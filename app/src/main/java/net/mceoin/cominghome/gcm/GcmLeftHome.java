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
package net.mceoin.cominghome.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import net.mceoin.cominghome.Installation;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.NestUtils;
import net.mceoin.cominghome.PrefsFragment;
import net.mceoin.cominghome.R;
import net.mceoin.cominghome.api.myApi.MyApi;
import net.mceoin.cominghome.api.myApi.model.StatusBean;
import net.mceoin.cominghome.cloud.CloudUtil;
import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;

import java.io.IOException;
import java.util.Random;

/**
 * Leverage the {@link GcmNetworkManager} to efficiently schedule telling the backend
 * that we've left home.
 * <p/>
 * Use the following to view scheduled tasks:
 * <pre>
 * adb shell dumpsys activity service GcmService --endpoints GcmLeftHome
 * </pre>
 * Inspired by:
 * https://github.com/jacktech24/gcmnetworkmanager-android-example
 */
public class GcmLeftHome extends GcmTaskService {
    private static final String TAG = GcmLeftHome.class.getSimpleName();
    private static final boolean debug = true;

    public static final String GCM_ONEOFF_TAG = "oneoff";

    /**
     * Schedule a {@link OneoffTask} with the
     * {@link GcmNetworkManager} for immediate execution of our network call to the backend.
     * <p/>
     * {@link GcmNetworkManager} is used so that it handles waking us up when the
     * network is actually available.  Because this is a background task, Android
     * Marshmallow can potentially place the app into Doze mode and remove network
     * from the app, even if the network is technically still available to the device.
     * <p/>
     * Even though it is scheduled to start immediately, {@link GcmNetworkManager}
     * will unlikely initiate the task immediately.  It is designed to try to batch
     * tasks across all apps on the device together.
     *
     * @param context Application context
     */
    public static void scheduleOneOff(Context context) {
        if (debug) {
            Log.d(TAG, "scheduleOneOff()");
            HistoryUpdate.add(context, "scheduleOneOff()");
        }
        long windowStartDelaySeconds = 30;
        long windowEndDelaySeconds = 5 * 60; // 5 minutes
        try {
            OneoffTask oneoff = new OneoffTask.Builder()
                    .setService(GcmLeftHome.class)
                    .setTag(GCM_ONEOFF_TAG)
                    .setPersisted(true)
                    .setExecutionWindow(windowStartDelaySeconds, windowEndDelaySeconds)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setUpdateCurrent(true)
                    .build();
            GcmNetworkManager.getInstance(context).schedule(oneoff);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelAllTasks(Context context) {
        GcmNetworkManager.getInstance(context).cancelAllTasks(GcmLeftHome.class);
    }

    /**
     * Uses the generated Google Cloud Endpoint {@link MyApi} to try to contact the
     * backend over the network.
     *
     * @param taskParams Parameters provided by {@link GcmNetworkManager}
     * @return {@link GcmNetworkManager#RESULT_FAILURE},
     *      {@link GcmNetworkManager#RESULT_RESCHEDULE}, or
     *      {@link GcmNetworkManager#RESULT_FAILURE} based on the outcome of the network call.
     */
    @Override
    public int onRunTask(TaskParams taskParams) {
        if (debug) Log.d(TAG, "onRunTask()");
        Context context = getApplicationContext();
        MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(),
                new AndroidJsonFactory(), null);

        // This name gets used as part of the HTTP User-Agent
        builder.setApplicationName(CloudUtil.getApplicationName(context));
        MyApi myApiService = builder.build();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        String structure_id = prefs.getString(MainActivity.PREFS_STRUCTURE_ID, "");
        String InstallationId = Installation.id(context);
        boolean tell_nest = prefs.getBoolean(PrefsFragment.key_tell_nest_on_leaving_home, true);
        String regid = prefs.getString(GcmRegister.PROPERTY_REG_ID, GcmRegister.PROPERTY_REG_ID_NONE);

        if (access_token.isEmpty()) {
            Log.w(TAG, "missing access_token");
            return GcmNetworkManager.RESULT_SUCCESS;
        }
        if (structure_id.isEmpty()) {
            Log.w(TAG, "missing structure_id");
            return GcmNetworkManager.RESULT_SUCCESS;
        }
        if (regid.isEmpty()) {
            Log.w(TAG, "missing regid");
            return GcmNetworkManager.RESULT_SUCCESS;
        }

        int retry = 0;
        while (retry < 2) {
            try {
                StatusBean result = myApiService.leftHome(InstallationId, access_token, structure_id, tell_nest, false, "-", regid).execute();
                handleResult(context, result, tell_nest);
                return GcmNetworkManager.RESULT_SUCCESS;

            } catch (IOException e) {
                Log.w(TAG, "IOException: " + e.getLocalizedMessage());
            }
            try {
                Random randomGenerator = new Random();
                int seconds = (retry * 60) + randomGenerator.nextInt(15);
                int maxSeconds = 15 * 60;
                if (seconds > maxSeconds) seconds = maxSeconds;
                if (debug) Log.d(TAG, "retry in " + seconds + " seconds");
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                if (debug) Log.d(TAG, "interrupted");
                return GcmNetworkManager.RESULT_FAILURE;
            }
            retry++;
        }

        return GcmNetworkManager.RESULT_RESCHEDULE;
    }

    private void handleResult(Context context, StatusBean result, boolean tell_nest) {
        if (debug) { Log.d(TAG, "handleResult()"); }

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
