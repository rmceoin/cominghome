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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

        String request_id = req.getParameter("request_id");
        if ((request_id == null) || (request_id.isEmpty())) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing request_id");
            return;
        }

        log.info("request = " + request + " request_id = " + request_id);

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

            String nest_result = "Not told";
            String others_status = "Not checked";
            if (tell_nest.equals("true")) {
                if (!access_token.isEmpty()) {
                    boolean doit = true;
                    if (away_status.equals("away")) {
                        // only check for others still at home if we're setting to away
                        boolean others = checkOthersAtHome(installation_id, structure_id);
                        if (others) {
                            others_status = "True";
                            doit = false;
                        } else {
                            others_status = "False";
                        }
                    }
                    if (doit) {
                        nest_result = tellNestAwayStatus(access_token, structure_id, away_status);
                    }
                }
            }

            resp.setContentType("application/json");

            Map<String, String> params = new HashMap<String, String>();
            params.put("result", "success");
            params.put("nest_result", nest_result);
            params.put("others_status", others_status);
            JSONObject jsonResponse = new JSONObject(params);

            resp.getWriter().print(jsonResponse.toString());

        } else if (request.equals("getothers")) {

            boolean others = checkOthersAtHome(installation_id, structure_id);
            String othersResult;

            if (others) {
                othersResult = "Others at home";
            } else {
                othersResult = "No others at home";
            }

            resp.setContentType("application/json");

            Map<String, String> params = new HashMap<String, String>();
            params.put("result", "success");
            params.put("others_result", othersResult);
            JSONObject jsonResponse = new JSONObject(params);

            resp.getWriter().print(jsonResponse.toString());

        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
        }
    }

    /**
     * Check for anybody else still at home
     *
     * @param installation_id Nest installation_id of user
     * @param structure_id    Nest structure_id of user
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

    private String tellNestAwayStatus(String access_token, String structure_id, String away_status) {

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
}