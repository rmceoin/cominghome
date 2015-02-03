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
        try {
            PreparedQuery pq = datastore.prepare(findStatusQuery);
            for (Entity result : pq.asIterable(FetchOptions.Builder.withLimit(1000))) {
                String installation_ID = (String) result.getProperty("installation_id");
                Date date = (Date) result.getProperty("date");

                Date now = new Date();
                long delta = (now.getTime() - date.getTime()) / 1000;
                log.info("installation_id=" + installation_ID + " date=" + date + " delta=" + delta);

                int hoursOld = 48;
                if (delta > hoursOld * 60 * 60) {
                    log.info("older than " + hoursOld + " hours: deleting " + result.getKey());
                    datastore.delete(result.getKey());
                }

            }
        } catch (Exception e) {
            log.warning("Error: " + e.getLocalizedMessage());
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
