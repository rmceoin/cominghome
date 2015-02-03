package net.mceoin.cominghome.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * What to send to Google Cloud Messaging
 */
public class GcmContent implements Serializable {

    private List<String> registration_ids;
    private Map<String, String> data;

    public List<String> getRegistration_ids() {
        return registration_ids;
    }
    
    public Map<String, String> getData() {
        return data;
    }
    
    public void addRegId(String regId) {
        if (registration_ids == null)
            registration_ids = new LinkedList<String>();
        registration_ids.add(regId);
    }

    public void createData(String command, String message) {
        if (data == null)
            data = new HashMap<String, String>();

        data.put("command", command);
        data.put("message", message);
    }

    @Override
    public String toString() {
        return "{registration_ids=" + registration_ids +
                "data=" + data + "}";
    }
}