package net.mceoin.cominghome;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

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

    public static void updateStatus(Context context, Handler handler, String structure_id,
                                    String away_status) {
        if (debug) Log.d(TAG, "updateStatus(,,," + away_status + ")");
        if ((away_status==null) || (away_status.isEmpty())) return;

        UpdateStatusAsyncTask updateStatusAsyncTask = new UpdateStatusAsyncTask();
        updateStatusAsyncTask.setContext(context);
        updateStatusAsyncTask.setHandler(handler);
        updateStatusAsyncTask.setStructureId(structure_id);
        updateStatusAsyncTask.setAwayStatus(away_status);
        updateStatusAsyncTask.execute();
    }

    private static class UpdateStatusAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        private String structure_id;
        private String away_status;

        private Handler handler;

        public void setContext(Context context) {
            this.context = context;
        }

        public void setHandler(Handler handler) {
            this.handler = handler;
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
            HttpPost httpPost = new HttpPost("https://coming-home-666.appspot.com/status"); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("request", "set"));
                nameValuePairs.add(new BasicNameValuePair("installation_id", MainActivity.InstallationId));
                nameValuePairs.add(new BasicNameValuePair("structure_id", structure_id));
                nameValuePairs.add(new BasicNameValuePair("away_status", away_status));
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
        }
    }

    public static void getOthers(Context context, Handler handler, String structure_id
                                    ) {
        if (debug) Log.d(TAG, "getOthers(,," + structure_id + ")");
        if ((structure_id==null) || (structure_id.isEmpty())) return;

        GetOthersAsyncTask getOthersAsyncTask = new GetOthersAsyncTask();
        getOthersAsyncTask.setContext(context);
        getOthersAsyncTask.setHandler(handler);
        getOthersAsyncTask.setStructureId(structure_id);
        getOthersAsyncTask.execute();
    }

    private static class GetOthersAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        private String structure_id;

        private Handler handler;

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
            HttpPost httpPost = new HttpPost("https://coming-home-666.appspot.com/status"); // 10.0.2.2 is localhost's IP address in Android emulator
            try {
                // Add name data to request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("request", "getothers"));
                nameValuePairs.add(new BasicNameValuePair("installation_id", MainActivity.InstallationId));
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
            if (handler!=null) {
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
