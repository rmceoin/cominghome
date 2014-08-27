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
package net.mceoin.cominghome.backend;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.http.*;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import org.json.JSONObject;

/**
 * This originally was based on:
 * <p/>
 * https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/HelloWorld
 */
public class MyServlet extends HttpServlet {

    public static final String KIND_STATUS = "Status";

    private static final Logger log = Logger.getLogger(MyServlet.class.getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Please use the form to POST to this url");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String installation_id = req.getParameter("installation_id");
        String structure_id = req.getParameter("structure_id");

        log.info("installation_id=" + installation_id + " structure_id=" + structure_id);

        if ((installation_id == null) || (installation_id.isEmpty())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing installation_id");
            return;
        }
        if ((structure_id == null) || (structure_id.isEmpty())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing structure_id");
            return;
        }

        String request = req.getParameter("request");
        if ((request == null) || (request.isEmpty())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing request");
            return;
        }

        log.info("request = " + request);

        if (request.equals("set")) {
            String away_status = req.getParameter("away_status");
            String access_token = req.getParameter("access_token");
            String tell_nest = req.getParameter("tell_nest");

            log.info("set with away_status = " + away_status + " access_token = " + access_token +
                    " tell_nest = " + tell_nest);

            if ((away_status == null) || (away_status.isEmpty())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing away_status");
                return;
            }


            Key statusKey = KeyFactory.createKey("status", installation_id);
            Date date = new Date();
            Entity status = new Entity(statusKey);
            status.setProperty("installation_id", installation_id);
            status.setProperty("date", date);
            status.setProperty("structure_id", structure_id);
            status.setProperty("away_status", away_status);

            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            datastore.put(status);

            String nestResult = "";
            if (tell_nest.equals("true")) {
                if (!access_token.isEmpty()) {
                    boolean doit=true;
                    if (away_status.equals("away")) {
                        // only check for others still at home if we're setting to away
                        boolean others = checkOthersAtHome(installation_id, structure_id);
                        if (others) {
                            doit=false;
                        }
                    }
                    if (doit) {
                        boolean nestError = tellNestAwayStatus(access_token, structure_id, away_status);
                        if (nestError) {
                            nestResult = " with Nest error";
                        } else {
                            nestResult = " with Nest";
                        }
                    }
                }
            }

            resp.setContentType("text/plain");
            resp.getWriter().println("Updated to " + away_status + nestResult);

        } else if (request.equals("getothers")) {

            boolean others = checkOthersAtHome(installation_id, structure_id);

            resp.setContentType("text/plain");
            if (others) {
                resp.getWriter().println("Others at home");
            } else {
                resp.getWriter().println("No others at home");
            }

        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
        }
    }

    /**
     * Check for anybody else still at home
     *
     * @param installation_id Nest installation_id of user
     * @param structure_id Nest structure_id of user
     * @return true if somebody else is still home
     */
    private boolean checkOthersAtHome(String installation_id, String structure_id) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query findStructureQuery = new Query(KIND_STATUS);

        Query.Filter structureFilter =
                new Query.FilterPredicate("structure_id",
                        Query.FilterOperator.EQUAL,
                        structure_id);
        findStructureQuery.setFilter(structureFilter);
        findStructureQuery.addSort("date", Query.SortDirection.DESCENDING);

        PreparedQuery pq = datastore.prepare(findStructureQuery);
        for (Entity result : pq.asIterable(FetchOptions.Builder.withLimit(10))) {
            String installation_ID = (String) result.getProperty("installation_id");
            String away_status = (String) result.getProperty("away_status");
//            Date date = (Date) result.getProperty("date");

            if ((!installation_ID.equals(installation_id)) && (away_status != null)) {
                if (away_status.equals("home")) {
                    log.info("found somebody else at home");
                    return true;
                }
            }
        }
        log.info("nobody else home");
        return false;
    }

    private boolean tellNestAwayStatus(String access_token, String structure_id, String away_status) {

        String urlString = "https://developer-api.nest.com/structures/" + structure_id + "/away?auth=" + access_token;
        log.info("url=" + urlString);

        StringBuilder builder = new StringBuilder();
        boolean error = false;

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent","ComingHomeBackend/1.0");
            urlConnection.setRequestMethod("PUT");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf8");

            String payload = "\"" + away_status + "\"";

//            JSONObject keyArg = new JSONObject();
//            keyArg.put("away", away_status);

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
            if ((statusCode == 200)) {
                error = false;
            } else if (statusCode == 400) {
                error = true;
                InputStream response;
                response = urlConnection.getErrorStream();
                error = true;
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
                        String errorResult = object.getString("error");
                        log.info("errorResult=" + errorResult);
                    }
                }
            } else {
                error = true;
            }

        } catch (IOException e) {
            log.warning("IO");
        } catch (Exception e) {
            log.warning("other");

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return error;
    }
}