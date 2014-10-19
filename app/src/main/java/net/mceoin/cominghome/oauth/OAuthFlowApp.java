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
package net.mceoin.cominghome.oauth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.mceoin.cominghome.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OAuthFlowApp extends Activity {
    public final String TAG = getClass().getName();
    public static final boolean debug = false;

    private SharedPreferences prefs;
    public static final String PREF_ACCESS_TOKEN = "access_token";
    public static final String PREF_EXPIRES_IN = "expires_in";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauth);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        EditText editPincode = (EditText) findViewById(R.id.editPincode);
        editPincode.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Button usePincode = (Button) findViewById(R.id.btn_use_pincode);
                if (s.length() == 8) {
                    usePincode.setEnabled(true);
                } else {
                    usePincode.setEnabled(false);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        Button usePincode = (Button) findViewById(R.id.btn_use_pincode);
        usePincode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editPincode = (EditText) findViewById(R.id.editPincode);
                String pincode = editPincode.getText().toString();
                if (debug) Log.d(TAG, "pincode=" + pincode);
                new RequestAccessToken().execute(pincode);
            }
        });

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(false);

        try {
            webView.loadUrl(Constants.AUTHORIZE_URL);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
        }

    }

    private class RequestAccessToken extends AsyncTask<String, Integer, Double> {

        String access_token = "";

        @Override
        protected Double doInBackground(String... params) {
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result) {
            if (!access_token.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Success!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_LONG).show();
            }
        }

        protected void onProgressUpdate(Integer... progress) {
//            pb.setProgress(progress[0]);
        }

        public void postData(String pincode) {
            StringBuilder builder = new StringBuilder();

            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            String url = "https://api.home.nest.com/oauth2/access_token";
            HttpPost httppost = new HttpPost(url);

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("code", pincode));
                nameValuePairs.add(new BasicNameValuePair("client_id", Constants.CLIENT_ID));
                nameValuePairs.add(new BasicNameValuePair("client_secret", Constants.CLIENT_SECRET));
                nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (debug) Log.d(TAG, "statusCode=" + statusCode);
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    JSONObject object = new JSONObject(builder.toString());
                    access_token = object.getString("access_token");
                    // expires_in = token expires in this number of seconds
                    // The values I've seen have been 10 years out
                    long expires_in = object.getLong("expires_in");
                    long expires_in_hours = expires_in / (60 * 60);

                    if (debug) Log.d(TAG, "access_token=" + access_token);
                    if (debug) Log.d(TAG, "expires_in_hours=" + expires_in_hours);

                    Editor pref = prefs.edit();
                    pref.putString(PREF_ACCESS_TOKEN, access_token);
                    pref.putLong(PREF_EXPIRES_IN, expires_in);
                    pref.apply();
                }

            } catch (ClientProtocolException e) {
                Log.e(TAG, e.getLocalizedMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getLocalizedMessage());
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

    }

}