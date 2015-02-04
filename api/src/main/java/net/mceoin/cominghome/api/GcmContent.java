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
    private int time_to_live;
    private String collapse_key;

    public List<String> getRegistration_ids() {
        return registration_ids;
    }

    public Map<String, String> getData() {
        return data;
    }

    public int getTime_to_live() {
        return time_to_live;
    }

    public void setTime_to_live(int time_to_live) {
        this.time_to_live = time_to_live;
    }

    public String getCollapse_key() {
        return collapse_key;
    }

    public void setCollapse_key(String collapse_key) {
        this.collapse_key = collapse_key;
    }

    public void addRegId(String regId) {
        if (registration_ids == null)
            registration_ids = new LinkedList<String>();
        if (!registration_ids.contains(regId)) {
            // don't allow duplicates
            registration_ids.add(regId);
        }
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