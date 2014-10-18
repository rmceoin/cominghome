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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * Nest utility functions
 * <p/>
 * Some handy info is at:
 * <p/>
 * http://stackoverflow.com/questions/24601798/acquiring-and-changing-basic-data-on-the-nest-thermostat
 */
public class NestUtils {
    public static final String TAG = NestUtils.class.getSimpleName();
    public static final boolean debug = false;

    public static final String MSG_ETA = "eta";
    public static final String MSG_AWAY = "away";

    public static void getInfo(Context context, String access_token) {
        if (debug) Log.d(TAG, "getInfo()");
        if (context == null) {
            Log.e(TAG, "missing context");
            return;
        }

        // Tag used to cancel the request
        String tag_update_status = "nest_info_req";

        String url = "https://developer-api.nest.com/structures?auth=" + access_token;

        JsonObjectRequest updateStatusReq = new JsonObjectRequest(Request.Method.GET,
                url, null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        if (debug) Log.d(TAG, "response=" + response.toString());

                        // {
                        // "structure_id":"njBTS-gAhF1mJ8_oF23ne7JNDyx1m1hULWixOD6IQWEe-SFA",
                        // "thermostats":["n232323jy8Xr1HVc2OGqICVP45i-Mc"],
                        // "smoke_co_alarms":["pt00ag34grggZchI7ICVPddi-Mc"],
                        // "country_code":"US",
                        // "away":"home"
                        // "name":"Home"
                        // }

                        String structure_id = "";
                        String structure_name = "";
                        String away_status = "";

                        JSONObject structures;
                        try {
                            structures = new JSONObject(response.toString());
                            Iterator<String> keys = structures.keys();
                            while (keys.hasNext()) {
                                String structure = keys.next();
                                JSONObject value = structures.getJSONObject(structure);
                                if (debug) Log.d(TAG, "value=" + value);
                                structure_id = value.getString("structure_id");
                                structure_name = value.getString("name");
                                away_status = value.getString("away");
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "error parsing JSON");
                            return;
                        }

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppController.getInstance().getApplicationContext());
                        SharedPreferences.Editor pref = prefs.edit();
                        pref.putString(MainActivity.PREFS_STRUCTURE_ID, structure_id);
                        pref.putString(MainActivity.PREFS_STRUCTURE_NAME, structure_name);
                        pref.putString(MainActivity.PREFS_LAST_AWAY_STATUS, away_status);
                        pref.apply();

//                        if (handler != null) {
//                            Message msg = Message.obtain();
//                            Bundle b = new Bundle();
//                            b.putString("type", MSG_STRUCTURES);
//                            b.putString("structure_id", structure_id);
//                            b.putString("structure_name", structure_name);
//                            b.putString("away_status", away_status);
//                            msg.setData(b);
//                            handler.sendMessage(msg);
//                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (debug) Log.d(TAG, "getInfo volley error=" + error.getLocalizedMessage());
                Context context = AppController.getInstance().getApplicationContext();
                HistoryUpdate.add(context, error.getLocalizedMessage());
                VolleyLog.d(TAG, "getInfo Error: " + error.getMessage());
            }
        });
        AppController.getInstance().addToRequestQueue(updateStatusReq, tag_update_status);
    }

    public static void sendETA(String access_token, Handler handler, String structure_id,
                               String trip_id, int etaMinutes) {
        if (debug) Log.d(TAG, "sendETA(,,,," + etaMinutes + ")");
        SendETA sendEta = new SendETA();
        sendEta.setHandler(handler);
        sendEta.setStructureId(structure_id);
        sendEta.setTripId(trip_id);
        sendEta.setEtaMinutes(etaMinutes);
        sendEta.execute(access_token);
    }

    public static void sendAwayStatus(Context context, String access_token, Handler handler, String structure_id,
                                      String away_status) {
        if (debug) Log.d(TAG, "sendAwayStatus(,,," + away_status + ")");
        if ((access_token == null) || (access_token.isEmpty())) {
            Log.e(TAG, "missing access_token");
            return;
        }
        if ((structure_id == null) || (structure_id.isEmpty())) {
            Log.e(TAG, "missing structure_id");
            return;
        }

        SendAwayStatus sendAwayStatus = new SendAwayStatus();
        sendAwayStatus.setHandler(handler);
        sendAwayStatus.setStructureId(structure_id);
        sendAwayStatus.setAwayStatus(away_status);
        sendAwayStatus.setContext(context);
        sendAwayStatus.execute(access_token);
    }

    private static void logException(Exception e, String extra) {
        String msg = "Exception";
        if ((e != null) && (e.getLocalizedMessage() != null)) {
            msg = "Exception: " + e.getLocalizedMessage();
        }
        msg += " " + extra;
        Log.e(TAG, msg);

    }

    private static class SendETA extends AsyncTask<String, Integer, Double> {

        private String trip_id;
        private String structure_id;
        private int etaMinutes;

        private Handler handler;
        private boolean error = false;
        private String errorResult = "";

        public void setHandler(Handler handler) {
            this.handler = handler;
        }

        public void setTripId(String trip_id) {
            this.trip_id = trip_id;
        }

        public void setStructureId(String structure_id) {
            this.structure_id = structure_id;
        }

        public void setEtaMinutes(int etaMinutes) {
            this.etaMinutes = etaMinutes;
        }

        @Override
        protected Double doInBackground(String... params) {
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result) {
            if (debug) {
                Log.d(TAG, "onPostExecute");
            }
            Message msg = Message.obtain();
            Bundle b = new Bundle();
            b.putString("type", MSG_ETA);
            String status;
            if (error) {
                status = "error";
            } else {
                status = "ok";
            }
            b.putString("status", status);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        protected void onProgressUpdate(Integer... progress) {
//            pb.setProgress(progress[0]);
        }

        public void postData(String access_token) {
            StringBuilder builder = new StringBuilder();

            // curl -X PUT -d ' {"trip_id":"sample-trip-id","estimated_arrival_window_begin":"2014-07-04T10:48:11+00:00","estimated_arrival_window_end":"2014-07-04T18:48:11+00:00"}'
            //"http://developer-api.nest.com/structures/5af48890-b516-11e3-9eff-123139166438/eta.json?auth=c.VG6bfzyOxAltaih6P4v..."

            String urlString = "https://developer-api.nest.com/structures/" + structure_id + "/eta.json?auth=" + access_token;
            if (debug) {
                Log.d(TAG, "url=" + urlString);
            }

            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("PUT");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0);

                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");
                urlConnection.setRequestProperty("Accept", "application/json");

                JSONObject keyArg = new JSONObject();
                final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs
                Date now = new Date();
                long t = now.getTime();
                Date begin = new Date(t + (etaMinutes * ONE_MINUTE_IN_MILLIS));
                Date end = new Date(t + ((etaMinutes + 5) * ONE_MINUTE_IN_MILLIS));
                // date must be in ISO 8601 format
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00");
                TimeZone tz = TimeZone.getTimeZone("UTC");
                formatter.setTimeZone(tz);
                String beginString = formatter.format(begin);
                String endString = formatter.format(end);
                keyArg.put("estimated_arrival_window_begin", beginString);
                keyArg.put("estimated_arrival_window_end", endString);
                keyArg.put("trip_id", trip_id);

                if (debug) {
                    Log.d(TAG, "estimated_arrival_window_begin=" + beginString);
                    Log.d(TAG, "estimated_arrival_window_end=" + endString);
                }

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(keyArg.toString());
                wr.flush();
                if (debug) {
                    Log.d(TAG, keyArg.toString());
                }

                boolean redirect = false;

                // normally, 3xx is redirect
                int status = urlConnection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == 307    // Temporary redirect
                            || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }

                System.out.println("Response Code ... " + status);

                if (redirect) {

                    // get redirect url from "location" header field
                    String newUrl = urlConnection.getHeaderField("Location");

                    // open the new connnection again
                    urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                    urlConnection.setRequestMethod("PUT");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setChunkedStreamingMode(0);
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");
                    urlConnection.setRequestProperty("Accept", "application/json");

                    System.out.println("Redirect to URL : " + newUrl);

                    wr = new OutputStreamWriter(urlConnection.getOutputStream());
                    wr.write(keyArg.toString());
                    wr.flush();

                }

                int statusCode = urlConnection.getResponseCode();

                if (debug) {
                    Log.d(TAG, "statusCode=" + statusCode);
                }
                if ((statusCode == 200) || (statusCode == 400)) {
                    InputStream response;
                    if (statusCode == 200) {
                        response = urlConnection.getInputStream();
                    } else {
                        response = urlConnection.getErrorStream();
                        error = true;
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    if (debug) Log.d(TAG, "response=" + builder.toString());
                    if (statusCode == 400) {
                        JSONObject object = new JSONObject(builder.toString());
                        Iterator<String> keys = object.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if ((statusCode == 400) && (key.equals("error"))) {
                                errorResult = object.getString("error");
                                if (debug) {
                                    Log.d(TAG, "errorResult=" + errorResult);
                                }
                            }
                        }
                    }
                } else {
                    error = true;
                }

            } catch (ClientProtocolException e) {
                logException(e, "SendETA: ClientProtocol");
            } catch (IOException e) {
                logException(e, "SendETA: IO");
            } catch (Exception e) {
                logException(e, "SendETA");

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

    }

    private static class SendAwayStatus extends AsyncTask<String, Integer, Double> {

        private String structure_id;
        private String away_status;

        private Handler handler;
        private boolean error = false;
        private String errorResult = "";

        private Context context;

        public void setContext(Context context) {
            this.context = context;
        }

        public void setHandler(Handler handler) {
            this.handler = handler;
        }

        public void setStructureId(String structure_id) {
            this.structure_id = structure_id;
        }

        public void setAwayStatus(String away_status) {
            this.away_status = away_status;
        }

        @Override
        protected Double doInBackground(String... params) {
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result) {
            if (debug) {
                Log.d(TAG, "onPostExecute");
            }
            if (context != null) {
                String status;
                if (error) {
                    status = away_status + " errored";
                } else {
                    status = away_status;
                }
                sendNotification(context, status);
            }
            if (handler != null) {
                Message msg = Message.obtain();
                Bundle b = new Bundle();
                b.putString("type", MSG_AWAY);
                String status;
                if (error) {
                    status = "error";
                } else {
                    status = "ok";
                }
                b.putString("status", status);
                msg.setData(b);
                handler.sendMessage(msg);
            }
        }

        protected void onProgressUpdate(Integer... progress) {
//            pb.setProgress(progress[0]);
        }

        public void postData(String access_token) {
            StringBuilder builder = new StringBuilder();


            // curl -v -L -X PUT "https://developer-api.nest.com/structures/uD9iIYonC2ygFs54nyP468bBDsGdF8rRIG0AHPcn4dhimK7g/away?auth=c.bCYw5l7mvN1ogNPVe9ecomAlrht34NTnP7Rb10b01tBnlFUVfiVjF9x4z3CIpPXKaU7nO2owrAIhwb05HZTheRTRMPhV32GshhkWD9HrteWhC1XcfzP02xBL06wyqnSKl2PiKEHAG2ZQmair"
            // -H "Content-Type: application/json" -d '"away"'


            String urlString = "https://developer-api.nest.com/structures/" + structure_id + "/away?auth=" + access_token;
            if (debug) {
                Log.d(TAG, "url=" + urlString);
            }

            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("User-Agent", "ComingHome/1.0");
                urlConnection.setRequestMethod("PUT");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setChunkedStreamingMode(0);

                urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");
//                urlConnection.setRequestProperty("Accept", "application/json");

                String payload = "\"" + away_status + "\"";

                JSONObject keyArg = new JSONObject();
                keyArg.put("away", away_status);

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(payload);
                wr.flush();
                if (debug) {
                    Log.d(TAG, keyArg.toString());
                }

                boolean redirect = false;

                // normally, 3xx is redirect
                int status = urlConnection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP
                            || status == HttpURLConnection.HTTP_MOVED_PERM
                            || status == 307    // Temporary redirect
                            || status == HttpURLConnection.HTTP_SEE_OTHER)
                        redirect = true;
                }

                System.out.println("Response Code ... " + status);

                if (redirect) {

                    // get redirect url from "location" header field
                    String newUrl = urlConnection.getHeaderField("Location");

                    // open the new connnection again
                    urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                    urlConnection.setRequestMethod("PUT");
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setChunkedStreamingMode(0);
                    urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");
                    urlConnection.setRequestProperty("Accept", "application/json");

                    System.out.println("Redirect to URL : " + newUrl);

                    wr = new OutputStreamWriter(urlConnection.getOutputStream());
                    wr.write(payload);
                    wr.flush();

                }

                int statusCode = urlConnection.getResponseCode();

                if (debug)
                    Log.d(TAG, "statusCode=" + statusCode);
                if ((statusCode == 200) || (statusCode == 400)) {
                    InputStream response;
                    if (statusCode == 200) {
                        response = urlConnection.getInputStream();
                    } else {
                        response = urlConnection.getErrorStream();
                        error = true;
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                        if (debug) Log.d(TAG, "response=" + builder.toString());
                        JSONObject object = new JSONObject(builder.toString());
                        Iterator<String> keys = object.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            if (key.equals("error")) {
                                errorResult = object.getString("error");
                                if (debug) {
                                    Log.d(TAG, "errorResult=" + errorResult);
                                }
                            }
                        }
                    }
                } else {
                    error = true;
                }

            } catch (ClientProtocolException e) {
                logException(e, "SendAwayStatus: ClientProtocol");
            } catch (IOException e) {
                logException(e, "SendAwayStatus: IO");
            } catch (Exception e) {
                logException(e, "SendAwayStatus: other");

            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the main Activity.
     *
     * @param transitionType The type of transition that occurred.
     */
    public static void sendNotification(Context context, String transitionType) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notifications = prefs.getBoolean(MainActivity.PREFS_NOTIFICATIONS, true);

        if (!notifications) {
            if (debug) Log.d(TAG, "notifications are turned off");
            return;
        }

        // Create an explicit content Intent that starts the main Activity
        Intent notificationIntent =
                new Intent(context, MainActivity.class);

        // Construct a task stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the main Activity to the task stack as the parent
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Set the notification contents
        builder.setSmallIcon(R.drawable.home)
                .setContentTitle(
                        context.getString(R.string.nest_transition_notification_title,
                                transitionType))
                .setContentText(context.getString(R.string.nest_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


}
