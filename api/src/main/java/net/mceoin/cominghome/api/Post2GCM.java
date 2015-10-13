/*
 * Copyright 2015 Randy McEoin
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
package net.mceoin.cominghome.api;

import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

/**
 * Post to Google Cloud Messaging
 */
public class Post2GCM {

    private static final Logger log = Logger.getLogger(Post2GCM.class.getName());

    private static final int REGISTRATION_IDS_MAX = 1000;

    public static void post(GcmContent content) {

        if (content == null) return;

        List<String> registration_ids = content.getRegistration_ids();
        if (registration_ids != null) {
            log.info("count of RegId: " + registration_ids.size());
            if (registration_ids.size() > REGISTRATION_IDS_MAX) {
                log.warning("too many registration ids");
            }
        }
        log.info("content: " + content.toString());
        try {

            URL url = new URL("https://android.googleapis.com/gcm/send");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "key=" + CloudConstants.GCM_API_KEY);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setChunkedStreamingMode(0);

            ObjectMapper mapper = new ObjectMapper();
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            mapper.writeValue(wr, content);

            int responseCode = conn.getResponseCode();
            log.info("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            log.info("response: " + response.toString());

        } catch (IOException e) {
            log.warning("IOException: " + e.getLocalizedMessage());
        }
    }
}