package net.mceoin.cominghome.api;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Handle cron tasks
 */
public class CronServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(CronServlet.class.getName());

    /**
     * Ping after this many hours
     */
    private static final int PING_AFTER = 12;

    /**
     * Purge status entry after this many hours
     */
    private static final int PURGE_AFTER = 48;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Cron complete.");

        log.info("got get");
        purgeOldStatus();
        purgeOldEvents();
    }

    private void purgeOldStatus() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query findStatusQuery = new Query(StatusEndpointV1.KIND_STATUS);
        findStatusQuery.addSort("date", Query.SortDirection.ASCENDING);
        GcmContent content = null;
        try {
            PreparedQuery pq = datastore.prepare(findStatusQuery);
            for (Entity result : pq.asIterable(FetchOptions.Builder.withLimit(1000))) {
                String installation_ID = (String) result.getProperty("installation_id");
                Date date = (Date) result.getProperty("date");
                String away_status = (String) result.getProperty("away_status");
                String gcm_reg_id = (String) result.getProperty("gcm_reg_id");

                Date now = new Date();
                long delta = (now.getTime() - date.getTime()) / 1000;
                log.info("installation_id=" + installation_ID + " date=" + date + " delta=" + delta);


                if (delta > (PURGE_AFTER * 60 * 60)) {
                    log.info("older than " + PURGE_AFTER + " hours: deleting " + result.getKey());
                    datastore.delete(result.getKey());
                } else if ((away_status.equals("home")) && (delta > (PING_AFTER * 60 * 60))) {
                    log.info("at home and older than " + PING_AFTER + " hours: ping " + result.getKey());
                    if (!gcm_reg_id.isEmpty()) {
                        if (gcm_reg_id.length() > 10) {
                            // basic sanity check on registration_id, make sure it's at least
                            // 10 characters long
                            if (content == null) {
                                content = new GcmContent();
                            }
                            content.addRegId(gcm_reg_id);
                            log.info("added reg_id: " + gcm_reg_id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Error: " + e.getLocalizedMessage());
        }
        if (content != null) {
            content.createData("check-in", "Still at home?");
            content.setTime_to_live( 60*60 );   // live for one hour
            content.setCollapse_key("check-in");
            Post2GCM.post(content);
        }
    }

    private void purgeOldEvents() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        int logDaysOld = 7;

        Query findEventQuery = new Query(StatusEndpointV1.KIND_EVENT);

        Date now = new Date();
        Date earlier = new Date();
        earlier.setTime(now.getTime() - (long) logDaysOld * 1000 * 60 * 60 * 24);

        Query.Filter dateFilter =
                new Query.FilterPredicate("date",
                        Query.FilterOperator.LESS_THAN,
                        earlier);
        findEventQuery.setFilter(dateFilter);
        log.info("earlier=" + earlier.getTime());

        try {
            PreparedQuery pq = datastore.prepare(findEventQuery);

            int count = 0;
            for (Entity result : pq.asIterable()) {

                String installation_ID = (String) result.getProperty("installation_id");
                Date date = (Date) result.getProperty("date");

                long delta = (now.getTime() - date.getTime()) / 1000;
                log.info("installation_id=" + installation_ID + " date=" + date + " " + date.getTime() + " delta=" + delta);

                if (delta > logDaysOld * 24 * 60 * 60) {
                    log.info("older than " + logDaysOld + " days: deleting " + result.getKey());
                    datastore.delete(result.getKey());
                    count++;
                }

            }
            log.info("deleted " + count + " events older than " + logDaysOld + " days");
        } catch (Exception e) {
            log.warning("Error: " + e.getLocalizedMessage());
        }

        log.info("done");
    }
}
