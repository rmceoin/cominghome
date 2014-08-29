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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import net.mceoin.cominghome.oauth.OAuthFlowApp;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Backend Utilities
 */
public class BackendUtils {
    public static final String TAG = BackendUtils.class.getSimpleName();
    public static final boolean debug = true;

    public static final String POST_ACTION_IF_NOBODY_HOME_SET_AWAY = "if-nobody-home-set-away";

    public static void updateStatus(Context context, String structure_id,
                                    String away_status, boolean tellNest) {
        if (debug) Log.d(TAG, "updateStatus(,," + away_status + ")");
        if (context == null) {
            Log.e(TAG, "missing context");
            return;
        }

        if ((away_status == null) || (away_status.isEmpty())) return;

        UpdateStatusAsyncTask updateStatusAsyncTask = new UpdateStatusAsyncTask();
        updateStatusAsyncTask.setContext(context);
        updateStatusAsyncTask.setStructureId(structure_id);
        updateStatusAsyncTask.setAwayStatus(away_status);
        updateStatusAsyncTask.setTellNest(tellNest);
        updateStatusAsyncTask.execute();
    }

    private static class UpdateStatusAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        private String structure_id;
        private String away_status;
        private boolean tellNest;

        public void setTellNest(boolean tellNest) { this.tellNest = tellNest; }

        public void setContext(Context context) {
            this.context = context;
        }

        public void setAwayStatus(String away_status) {
            this.away_status = away_status;
        }

        public void setStructureId(String structure_id) {
            this.structure_id = structure_id;
        }

        @Override
        protected String doInBackground(Void... params) {

            HttpClient httpClient = new DefaultHttpClient();
            String url = "https://" + BackendConstants.appEngineHost + "/status";
            HttpPost httpPost = new HttpPost(url);
            try {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");

                String InstallationId = Installation.id(context);
                String tellNestString;
                if (tellNest)
                    tellNestString="true";
                else
                    tellNestString="false";
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("request", "set"));
                nameValuePairs.add(new BasicNameValuePair("installation_id", InstallationId));
                nameValuePairs.add(new BasicNameValuePair("structure_id", structure_id));
                nameValuePairs.add(new BasicNameValuePair("away_status", away_status));
                nameValuePairs.add(new BasicNameValuePair("access_token", access_token));
                nameValuePairs.add(new BasicNameValuePair("tell_nest", tellNestString));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                if (debug) {
                    Log.d(TAG, "parameters = " + nameValuePairs.toString());
                }

                httpPost.setHeader("User-Agent","ComingHome/1.0");
                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (debug) Log.d(TAG, "result=" + result);
            if (debug) {
                if (context != null)
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            }
            if (context!=null) HistoryUpdate.add(context,result);
        }
    }

    public static void getOthers(Context context, Handler handler, String structure_id,
                                 String post_action) {
        if (debug) Log.d(TAG, "getOthers(,," + structure_id + ")");
        if ((structure_id == null) || (structure_id.isEmpty())) return;

        GetOthersAsyncTask getOthersAsyncTask = new GetOthersAsyncTask();
        getOthersAsyncTask.setContext(context);
        getOthersAsyncTask.setHandler(handler);
        getOthersAsyncTask.setStructureId(structure_id);
        getOthersAsyncTask.setPostAction(post_action);
        getOthersAsyncTask.execute();
    }

    private static class GetOthersAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        private String structure_id;

        private Handler handler;

        private String post_action;

        public void setPostAction(String post_action) { this.post_action = post_action; }

        public void setContext(Context context) {
            this.context = context;
        }

        public void setHandler(Handler handler) {
            this.handler = handler;
        }

        public void setStructureId(String structure_id) {
            this.structure_id = structure_id;
        }

        @Override
        protected String doInBackground(Void... params) {

            HttpClient httpClient = new DefaultHttpClient();
            String url = "https://" + BackendConstants.appEngineHost + "/status";
            HttpPost httpPost = new HttpPost(url);
            try {
                String InstallationId = Installation.id(context);

                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("request", "getothers"));
                nameValuePairs.add(new BasicNameValuePair("installation_id", InstallationId));
                nameValuePairs.add(new BasicNameValuePair("structure_id", structure_id));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                if (response.getStatusLine().getStatusCode() == 200) {
                    return EntityUtils.toString(response.getEntity());
                }
                return "Error: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase();

            } catch (ClientProtocolException e) {
                return e.getMessage();
            } catch (IOException e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (debug) Log.d(TAG, "result=" + result);
            if (debug) {
                if (context != null)
                    Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            }
            if ((post_action!=null) && (post_action.equals(POST_ACTION_IF_NOBODY_HOME_SET_AWAY))) {
                if ((result!=null) && (result.equals("No others"))) {
                    SharedPreferences prefs;
                    prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    String access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");

                    if (!access_token.isEmpty()) {
                        NestUtils.sendAwayStatus(context, access_token, null, structure_id, "away");
                    } else {
                        Log.e(TAG,"missing access_token");
                    }
                }
            }
            if (handler != null) {
                Message msg = Message.obtain();
                Bundle b = new Bundle();
                b.putString("type", NestUtils.MSG_GET_OTHERS);
                b.putString("result", result);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        }
    }


}
