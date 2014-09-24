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
package net.mceoin.cominghome.api;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Utilities for calling Nest
 */
public class NestUtil {

    private static final Logger log = Logger.getLogger(NestUtil.class.getName());

    /**
     * Make HTTP/JSON call to Nest and set away status.
     *
     * @param access_token OAuth token to allow access to Nest
     * @param structure_id ID of structure with thermostate
     * @param away_status Either "home" or "away"
     * @return Equal to "Success" if succesful, otherwise it contains a hint on the error.
     */
    public static String tellNestAwayStatus(String access_token, String structure_id, String away_status) {

        String urlString = "https://developer-api.nest.com/structures/" + structure_id + "/away?auth=" + access_token;
        log.info("url=" + urlString);

        StringBuilder builder = new StringBuilder();
        boolean error = false;
        String errorResult = "";

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "ComingHomeBackend/1.0");
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");

            String payload = "\"" + away_status + "\"";

            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(payload);
            wr.flush();
            log.info(payload);

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

//            System.out.println("Response Code ... " + status);

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

//                System.out.println("Redirect to URL : " + newUrl);

                wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(payload);
                wr.flush();

            }

            int statusCode = urlConnection.getResponseCode();

            log.info("statusCode=" + statusCode);
            if ((statusCode == HttpURLConnection.HTTP_OK)) {
                error = false;
            } else if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // bad auth
                error = true;
                errorResult = "Unauthorized";
            } else if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                error = true;
                InputStream response;
                response = urlConnection.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                log.info("response=" + builder.toString());
                JSONObject object = new JSONObject(builder.toString());

                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.equals("error")) {
                        // error = Internal Error on bad structure_id
                        errorResult = object.getString("error");
                        log.info("errorResult=" + errorResult);
                    }
                }
            } else {
                error = true;
                errorResult = Integer.toString(statusCode);
            }

        } catch (IOException e) {
            errorResult = e.getLocalizedMessage();
            log.warning("IOException: " + errorResult);
        } catch (Exception e) {
            errorResult = e.getLocalizedMessage();
            log.warning("Exception: " + errorResult);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        if (error) {
            return "Error: " + errorResult;
        } else {
            return "Success";
        }
    }

    public static String getNestAwayStatus(String access_token) {

        String away_status = "";

        String urlString = "https://developer-api.nest.com/structures?auth=" + access_token;
        log.info("url=" + urlString);

        StringBuilder builder = new StringBuilder();
        boolean error = false;
        String errorResult = "";

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "ComingHomeBackend/1.0");
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
//            urlConnection.setChunkedStreamingMode(0);

//            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");

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

//            System.out.println("Response Code ... " + status);

            if (redirect) {

                // get redirect url from "location" header field
                String newUrl = urlConnection.getHeaderField("Location");

                // open the new connnection again
                urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                urlConnection.setRequestMethod("PUT");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
//                urlConnection.setChunkedStreamingMode(0);

//                System.out.println("Redirect to URL : " + newUrl);

            }

            int statusCode = urlConnection.getResponseCode();

            log.info("statusCode=" + statusCode);
            if ((statusCode == HttpURLConnection.HTTP_OK)) {
                error = false;

                InputStream response;
                response = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                log.info("response=" + builder.toString());
                JSONObject object = new JSONObject(builder.toString());

                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    JSONObject structure = object.getJSONObject(key);

                    if (structure.has("away")) {
                        away_status = structure.getString("away");
                    } else {
                        log.info("missing away");
                    }
                }

            } else if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                // bad auth
                error = true;
                errorResult = "Unauthorized";
            } else if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                error = true;
                InputStream response;
                response = urlConnection.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                log.info("response=" + builder.toString());
                JSONObject object = new JSONObject(builder.toString());

                Iterator keys = object.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (key.equals("error")) {
                        // error = Internal Error on bad structure_id
                        errorResult = object.getString("error");
                        log.info("errorResult=" + errorResult);
                    }
                }
            } else {
                error = true;
                errorResult = Integer.toString(statusCode);
            }

        } catch (IOException e) {
            error = true;
            errorResult = e.getLocalizedMessage();
            log.warning("IOException: " + errorResult);
        } catch (Exception e) {
            error = true;
            errorResult = e.getLocalizedMessage();
            log.warning("Exception: " + errorResult);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        if (error) away_status = "Error: "+errorResult;
        return away_status;
    }

}
