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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import net.mceoin.cominghome.R;
import net.mceoin.cominghome.history.HistoryUpdate;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class OAuthFlowApp extends Activity {
    private final String TAG = getClass().getName();
    private static final boolean debug = false;

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
        editPincode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (v.length() == 8) {
                        usePincode();
                        return true;
                    }
                }
                return false;
            }
        });

        Button usePincode = (Button) findViewById(R.id.btn_use_pincode);
        usePincode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                usePincode();
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

    private void usePincode() {
        EditText editPincode = (EditText) findViewById(R.id.editPincode);
        String pincode = editPincode.getText().toString();
        if (debug) Log.d(TAG, "pincode=" + pincode);
        new RequestAccessToken().execute(pincode);
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
                Toast.makeText(getApplicationContext(), getString(R.string.oauth_success), Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.oauth_failed), Toast.LENGTH_LONG).show();
            }
        }

        protected void onProgressUpdate(Integer... progress) {
//            pb.setProgress(progress[0]);
        }

        public void postData(String pincode) {
            StringBuilder builder = new StringBuilder();

            String nestAccessToken = "https://api.home.nest.com/oauth2/access_token";

            try {
                String urlParameters = "code=" + URLEncoder.encode(pincode, "UTF-8") +
                        "&client_id=" + URLEncoder.encode(Constants.CLIENT_ID, "UTF-8") +
                        "&client_secret=" + URLEncoder.encode(Constants.CLIENT_SECRET, "UTF-8") +
                        "&grant_type=" + URLEncoder.encode("authorization_code", "UTF-8");

                URL url = new URL(nestAccessToken + "?" + urlParameters);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");

                int httpResult = urlConnection.getResponseCode();
                if (debug) Log.d(TAG, "httpResult=" + httpResult);
                if (httpResult == HttpsURLConnection.HTTP_OK) {

                    InputStream content = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    long expires_in = 0;
                    try {
                        JSONObject object = new JSONObject(builder.toString());
                        access_token = object.getString("access_token");
                        // expires_in = token expires in this number of seconds
                        // The values I've seen have been 10 years out
                        expires_in = object.getLong("expires_in");
                    } catch (Exception e) {
                        Log.e(TAG, e.getLocalizedMessage());
                    }

                    if (debug) Log.d(TAG, "access_token=" + access_token);
                    if (debug) {
                        long expires_in_hours = expires_in / (60 * 60);
                        Log.d(TAG, "expires_in_hours=" + expires_in_hours);
                    }

                    Editor pref = prefs.edit();
                    pref.putString(PREF_ACCESS_TOKEN, access_token);
                    pref.putLong(PREF_EXPIRES_IN, expires_in);
                    pref.apply();
                } else if (httpResult == HttpsURLConnection.HTTP_BAD_REQUEST) {
                    // if user types wrong pincode, then Nest returns 400 with this as body:
                    // {"error":"oauth2_error","error_description":"authorization code not found"}

                    InputStream content = urlConnection.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                    String error = "";
                    String error_description;

                    try {
                        JSONObject object = new JSONObject(builder.toString());
                        error = object.getString("error");
                        error_description = object.getString("error_description");
                    } catch (Exception e) {
                        error_description = getString(R.string.oauth_missing_error);
                    }
                    HistoryUpdate.add(getApplicationContext(), getString(R.string.oauth_connect_issue) +
                            ": " + error + " - " + error_description);

                } else {
                    HistoryUpdate.add(getApplicationContext(), getString(R.string.oauth_connect_issue) +
                            ": Did not get OK during auth: " + httpResult);
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage());
                HistoryUpdate.add(getApplicationContext(), getString(R.string.oauth_connect_issue) +
                        ": " + e.getLocalizedMessage());
            }
        }

    }

}