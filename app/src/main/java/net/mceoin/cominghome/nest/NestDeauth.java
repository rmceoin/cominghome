/*
 * Copyright 2016 Randy McEoin
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
package net.mceoin.cominghome.nest;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.mceoin.cominghome.cloud.CloudUtil;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Performs a Nest Deauthorization.  Best effort only.  If currently no network or
 * {@link HttpsURLConnection} returns anything other than a
 * {@link HttpsURLConnection#HTTP_NO_CONTENT}, no retry will be attempted.
 * <p/>
 * https://developer.nest.com/documentation/cloud/deauthorization-overview
 * <p/>
 * <pre>
 * curl -v -X DELETE "https://api.home.nest.com/oauth2/access_tokens/c.bQBC1A..."
 * </pre>
 */
public class NestDeauth extends AsyncTask<String, Void, Void> {
    private static final String TAG = NestDeauth.class.getSimpleName();
    private static final boolean debug = false;

    private Context context;

    public NestDeauth(Context context) {
        if (debug) Log.d(TAG, "NestDeauth()");
        this.context = context;
    }

    /**
     * @param params Must be the access_token to deauthorize.
     * @return null
     */
    @Override
    protected Void doInBackground(String... params) {
        if (!CloudUtil.isNetworkConnected(context)) {
            if (debug) Log.d(TAG, "no network, cannot deauth");
            return null;
        }

        String access_token = params[0];
        if ((access_token == null) || (access_token.length() < 1)) {
            return null;
        }
        if (debug) Log.d(TAG, "access_token=" + access_token);
        String nestAccessToken = "https://api.home.nest.com/oauth2/access_tokens";
        try {
            URL url = new URL(nestAccessToken + "/" + access_token);
            if (debug) Log.d(TAG, "url=" + url);
            HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setDoOutput(false);
            urlConnection.setRequestMethod("DELETE");
            int httpResult = urlConnection.getResponseCode();
            if (httpResult == HttpsURLConnection.HTTP_NO_CONTENT) {
                if (debug) Log.d(TAG, "HTTP_NO_CONTENT as expected");
            } else {
                if (debug) Log.d(TAG, "unexpected httpResult=" + httpResult);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return null;
    }
}
