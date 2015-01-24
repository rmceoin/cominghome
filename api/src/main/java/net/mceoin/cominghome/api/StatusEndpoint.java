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

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import java.util.Date;
import java.util.logging.Logger;

import javax.inject.Named;

/**
 * Cloud Endpoint to allow the backend to track away/home status for each Android
 * installation.  If two installations share a common structure_id, then it is assumed
 * they belong to the same household.  This allows the backend to automagically know
 * if another member of the household is still at home or not.
 */
@Api(name = "myApi", version = "v1", namespace = @ApiNamespace(ownerDomain = "api.cominghome.mceoin.net", ownerName = "api.cominghome.mceoin.net", packagePath = ""))
public class StatusEndpoint {

    public static final String KIND_EVENT = "Event";
    public static final String KIND_STATUS = "Status";

    private static final Logger log = Logger.getLogger(StatusEndpoint.class.getName());

    /**
     * Let the backend know that user has arrived home.  If set, let Nest know as well.
     *
     * @param InstallationID Unique identifier for device sending the message
     * @param access_token   Token to be used to allow access to Nest
     * @param structure_id   Nest id for the structure where the thermostat is located
     * @param tell_nest      Whether or not to let Nest know
     * @return {@link net.mceoin.cominghome.api.StatusBean} Outcome of the update
     */
    @ApiMethod(name = "arrivedHome")
    public StatusBean arrivedHome(@Named("InstallationID") String InstallationID,
                                  @Named("access_token") String access_token,
                                  @Named("structure_id") String structure_id,
                                  @Named("tell_nest") boolean tell_nest) {
        StatusBean response = new StatusBean();

        response.setSuccess(true);
        log.info("arrived home: tell_nest="+tell_nest);
        logEvent(InstallationID, structure_id, "arrived home");

        if (InstallationID.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("InstallationID is empty");
            return response;
        }

        if (access_token.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("access_token is empty");
            return response;
        }

        if (structure_id.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("structure_id is empty");
            return response;
        }

        saveStatus(InstallationID, structure_id, "home");

        if (tell_nest) {
            String nest_away = NestUtil.getNestAwayStatus(access_token);
            switch (nest_away) {
                case "away":
                case "auto-away":
                    String result = NestUtil.tellNestAwayStatus(access_token, structure_id, "home");
                    response.setMessage("Nest updated");
                    if (result.equals("Success")) {
                        response.setNestSuccess(true);
                        response.setNestUpdated(true);
                    } else {
                        response.setNestSuccess(false);
                        response.setNestUpdated(false);
                        response.setMessage(result);
                    }
                    break;
                case "home":
                    response.setNestSuccess(true);
                    response.setNestUpdated(false);
                    response.setMessage("Nest already home");
                    break;
                default:
                    response.setNestSuccess(false);
                    response.setNestUpdated(false);
                    response.setMessage(nest_away);
                    break;
            }
        } else {
            response.setNestSuccess(true);
            response.setNestUpdated(false);
            response.setMessage("Backend was updated");
        }
        return response;
    }

    @ApiMethod(name = "leftHome")
    public StatusBean leftHome(@Named("InstallationID") String InstallationID,
                               @Named("access_token") String access_token,
                               @Named("structure_id") String structure_id,
                               @Named("tell_nest") boolean tell_nest) {
        StatusBean response = new StatusBean();

        response.setSuccess(true);
        log.info("left home: tell_nest="+tell_nest);
        logEvent(InstallationID, structure_id, "left home");

        if (InstallationID.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("InstallationID is empty");
            return response;
        }

        if (access_token.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("access_token is empty");
            return response;
        }

        if (structure_id.isEmpty()) {
            response.setSuccess(false);
            response.setMessage("structure_id is empty");
            return response;
        }

        saveStatus(InstallationID, structure_id, "away");

        boolean others = checkOthersAtHome(InstallationID, structure_id);
        response.setOthersAtHome(others);

        if (others) {
            response.setNestSuccess(true);
            response.setNestUpdated(false);
            response.setMessage("Others still at home");
        } else if (tell_nest) {
            String nest_away = NestUtil.getNestAwayStatus(access_token);

            switch (nest_away) {
                case "home":
                    String result = NestUtil.tellNestAwayStatus(access_token, structure_id, "away");
                    response.setMessage("Nest updated");
                    if (result.equals("Success")) {
                        response.setNestSuccess(true);
                        response.setNestUpdated(true);
                    } else {
                        response.setNestSuccess(false);
                        response.setMessage(result);
                    }
                    break;
                case "away":
                case "auto-away":
                    response.setNestSuccess(true);
                    response.setNestUpdated(false);
                    response.setMessage("Nest already " + nest_away);
                    break;
                default:
                    response.setNestSuccess(false);
                    response.setNestUpdated(false);
                    response.setMessage(nest_away);
                    break;
            }
        } else {
            response.setNestSuccess(true);
            response.setNestUpdated(false);
            response.setMessage("Backend was updated");
        }
        return response;
    }

    private void logEvent(String installation_id, String structure_id, String event_msg) {
        Date date = new Date();
        Entity event = new Entity(KIND_EVENT);
        event.setProperty("installation_id", installation_id);
        event.setProperty("date", date);
        event.setProperty("structure_id", structure_id);
        event.setProperty("msg", event_msg);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(event);

    }

    private void saveStatus(String installation_id, String structure_id, String away_status) {
        Key statusKey = KeyFactory.createKey(KIND_STATUS, installation_id);
        Date date = new Date();
        Entity status = new Entity(statusKey);
        status.setProperty("installation_id", installation_id);
        status.setProperty("date", date);
        status.setProperty("structure_id", structure_id);
        status.setProperty("away_status", away_status);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(status);
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

        try {
            PreparedQuery pq = datastore.prepare(findStructureQuery);
            for (Entity result : pq.asIterable(FetchOptions.Builder.withLimit(10))) {
                String installation_ID = (String) result.getProperty("installation_id");
                String away_status = (String) result.getProperty("away_status");
                Date date = (Date) result.getProperty("date");

                if ((!installation_ID.equals(installation_id)) && (away_status != null)) {
                    Date now = new Date();
                    long delta_hours = ((now.getTime() - date.getTime()) / 1000) / (60*60);
                    log.info("installation_id=" + installation_ID + " date=" + date + " delta_hours=" + delta_hours);

                    if ((away_status.equals("home")) && (delta_hours < 24)) {
                        //
                        // The other installation is at home and checked in with us in the last 24 hours
                        log.info("found somebody else at home");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Error: "+e.getLocalizedMessage());
        }
        log.info("nobody else home");
        return false;
    }
}
