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

    public static void post(GcmContent content) {

        if (content == null) return;

        List<String> registration_ids = content.getRegistration_ids();
        if (registration_ids != null) {
            log.info("count of RegId: " + registration_ids.size());
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

//            wr.flush();
//            wr.close();


            int responseCode = conn.getResponseCode();
            //System.out.println("\nSending 'POST' request to URL : " + url);
            log.info("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // 7. Print result
            //System.out.println(response.toString());
            log.info( "response: " + response.toString());

        } catch (IOException e) {
            log.warning("IOException: " + e.getLocalizedMessage());
        }
    }
}