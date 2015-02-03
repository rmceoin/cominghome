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

/**
 * Use this as the base class for V1 and V2
 */
public class StatusEndpoint {

    private static final Logger log = Logger.getLogger(StatusEndpoint.class.getName());

    public static final String KIND_EVENT = "Event";
    public static final String KIND_STATUS = "Status";

    protected void logEvent(String installation_id, String structure_id, String event_msg) {
        Date date = new Date();
        Entity event = new Entity(KIND_EVENT);
        event.setProperty("installation_id", installation_id);
        event.setProperty("date", date);
        event.setProperty("structure_id", structure_id);
        event.setProperty("msg", event_msg);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(event);
    }

    protected void saveStatus(String installation_id, String structure_id, String away_status) {
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

    protected void saveStatus(String installation_id, String structure_id, String away_status,
                              String Gcm_reg_id) {
        Key statusKey = KeyFactory.createKey(KIND_STATUS, installation_id);
        Date date = new Date();
        Entity status = new Entity(statusKey);
        status.setProperty("installation_id", installation_id);
        status.setProperty("date", date);
        status.setProperty("structure_id", structure_id);
        status.setProperty("away_status", away_status);
        status.setProperty("gcm_reg_id", Gcm_reg_id);

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
    protected boolean checkOthersAtHome(String installation_id, String structure_id) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query findStructureQuery = new Query(KIND_STATUS);

        Query.Filter structureFilter =
                new Query.FilterPredicate("structure_id",
                        Query.FilterOperator.EQUAL,
                        structure_id);
        findStructureQuery.setFilter(structureFilter);
        findStructureQuery.addSort("date", Query.SortDirection.DESCENDING);

        boolean somebodyAtHome = false;
        GcmContent content = null;
        try {
            PreparedQuery pq = datastore.prepare(findStructureQuery);
            for (Entity result : pq.asIterable(FetchOptions.Builder.withLimit(10))) {
                String installation_ID = (String) result.getProperty("installation_id");
                String away_status = (String) result.getProperty("away_status");
                String gcm_reg_id = (String) result.getProperty("gcm_reg_id");
                Date date = (Date) result.getProperty("date");

                if ((!installation_ID.equals(installation_id)) && (away_status != null)) {
                    Date now = new Date();
                    long delta_hours = ((now.getTime() - date.getTime()) / 1000) / (60 * 60);
                    log.info("installation_id=" + installation_ID + " date=" + date + " delta_hours=" + delta_hours);

                    if ((away_status.equals("home")) && (delta_hours < 24)) {
                        //
                        // The other installation is at home and checked in with us in the last 24 hours
                        log.info("found somebody else at home");
                        somebodyAtHome = true;
                        if (!gcm_reg_id.isEmpty()) {
                            if (content == null) {
                                content = new GcmContent();
                            }
                            if (gcm_reg_id.length() > 10) {
                                // basic sanity check on registration_id, make sure it's at least
                                // 10 characters long
                                content.addRegId(gcm_reg_id);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Error: " + e.getLocalizedMessage());
        }
        if (content != null) {
            content.createData("check-in", "Somebody else left home");
            Post2GCM.post(content);
        }
        log.info("somebodyAtHome: " + somebodyAtHome);
        return somebodyAtHome;
    }

}
