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
package net.mceoin.cominghome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class PrefsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    public final static String TAG = PrefsFragment.class.getSimpleName();
    public final static boolean debug = false;

    public static final String key_tell_nest_on_arrival_home = "tell_nest_on_arrival_home";
    public static final String key_tell_nest_on_leaving_home = "tell_nest_on_leaving_home";

    public static final String PREFERENCE_HISTORY_ENTRIES = "history_entries";
    public static final String PREFERENCE_STRUCTURES_ENTRIES = "structures_entries";
    public static final String PREFERENCE_GEOFENCE_SAME_RADIUS = "geofence_same_radius";
    public static final String PREFERENCE_GEOFENCE_RADIUS = "geofence_radius";
    public static final String PREFERENCE_GEOFENCE_RADIUS_EXIT = "geofence_radius_exit";

    public static final String PREFERENCE_AWAY_DELAY = "away_delay";
    public static final String PREFERENCE_SHOW_AWAY_DELAY_NOTIFICATION = "show_away_delay_notification";

    public static final String PREFERENCE_USE_HOME_WIFI = "use_home_wifi";
    public static final String PREFERENCE_HOME_WIFI_SSID = "home_wifi_ssid";

    public static final String PREFERENCE_HISTORY_SHOW_LOCATION = "history_show_location";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        SharedPreferences sp = getPreferenceManager().getSharedPreferences();
        boolean sameRadius = sp.getBoolean(PREFERENCE_GEOFENCE_SAME_RADIUS, true);
        getPreferenceScreen().findPreference(PREFERENCE_GEOFENCE_RADIUS_EXIT).setEnabled(!sameRadius);
        if (sameRadius) {
            //
            // check for situations like an upgrade where previously we only had the enter radius
            //
            int fenceRadius = sp.getInt(PREFERENCE_GEOFENCE_RADIUS,
                    getResources().getInteger(R.integer.geofence_radius_default));
            int exitRadius = sp.getInt(PREFERENCE_GEOFENCE_RADIUS_EXIT, -1);
            if (exitRadius != fenceRadius) {
                SharedPreferences.Editor editor = sp.edit();
                editor.putInt(PREFERENCE_GEOFENCE_RADIUS_EXIT, fenceRadius);
                editor.apply();
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        boolean sameRadius = sharedPreferences.getBoolean(PREFERENCE_GEOFENCE_SAME_RADIUS, true);
        getPreferenceScreen().findPreference(PREFERENCE_GEOFENCE_RADIUS_EXIT).setEnabled(!sameRadius);

        // should we use the same radius for both entering and exiting?
        if (sameRadius) {
            //
            // if so, then force the exit radius to be the same
            //
            int fenceRadius = sharedPreferences.getInt(PREFERENCE_GEOFENCE_RADIUS,
                    getResources().getInteger(R.integer.geofence_radius_default));
            int exitRadius = sharedPreferences.getInt(PREFERENCE_GEOFENCE_RADIUS_EXIT,
                    getResources().getInteger(R.integer.geofence_radius_exit_default));
            if (exitRadius != fenceRadius) {
                //
                // they're not the same, so update the exit radius to match
                //
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREFERENCE_GEOFENCE_RADIUS_EXIT, fenceRadius);
                editor.apply();
                getPreferenceScreen().findPreference(PREFERENCE_GEOFENCE_RADIUS_EXIT).setSummary(fenceRadius
                        + " " + getString(R.string.summary_geofence_radius_exit_preference));
            }
        }
    }
}