package net.mceoin.cominghome.backend;

import java.io.IOException;
import java.util.Date;
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

        log.info("installation_id="+installation_id+" structure_id="+structure_id);

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

        log.info("request = "+request);

        if (request.equals("set")) {
            String away_status = req.getParameter("away_status");

            log.info("set with away_status = "+away_status);

            if ((away_status == null) || (away_status.isEmpty())) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing away_status");
                return;
            }

            resp.setContentType("text/plain");
            resp.getWriter().println("Updated to " + away_status);

            Key statusKey = KeyFactory.createKey("status", installation_id);
            Date date = new Date();
            Entity status = new Entity(statusKey);
            status.setProperty("installation_id", installation_id);
            status.setProperty("date", date);
            status.setProperty("structure_id", structure_id);
            status.setProperty("away_status", away_status);

            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            datastore.put(status);
        } else if (request.equals("getothers")) {

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
                Date date = (Date) result.getProperty("date");

                if ((!installation_ID.equals(installation_id)) && (away_status!=null)) {
                    if (away_status.equals("home")) {
                        resp.setContentType("text/plain");
                        resp.getWriter().println("At home " + installation_ID + " " + date);
                        return;
                    }
                }
            }

            resp.setContentType("text/plain");
            resp.getWriter().println("No others");

        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request");
       }
    }
}