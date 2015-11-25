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
package net.mceoin.cominghome.geofence;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import net.mceoin.cominghome.PrefsFragment;

/**
 * Utility functions for Wi-Fi
 */
public class WiFiUtils {
    private static final String TAG = WiFiUtils.class.getSimpleName();
    private static final boolean debug = false;

    /**
     * Get the current Wi-Fi SSID.
     *
     * @param context Application context
     * @return Null if not connected, otherwise SSID of current connected WiFi
     */
    @Nullable
    public static String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // TODO: Move this to getAllNetworks when API is 21 or above
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
        }
        if (debug) Log.d(TAG, "current SSID = " + ssid);
        return ssid;
    }

    /**
     * Save the current Wi-Fi SSID to shared preferences.
     *
     * @param context Application context
     * @return Null or current SSID.
     */
    @Nullable
    public static String saveCurrentSsid(Context context) {
        String currentSsid = WiFiUtils.getCurrentSsid(context);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PrefsFragment.PREFERENCE_HOME_WIFI_SSID, currentSsid);
        editor.apply();
        if (debug) Log.d(TAG, "saved SSID: " + currentSsid);
        return currentSsid;
    }

    public static boolean isCurrentSsidSameAsStored(Context context) {
        String currentSsid = WiFiUtils.getCurrentSsid(context);
        if (currentSsid == null) {
            return false;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useHomeWifi = sp.getBoolean(PrefsFragment.PREFERENCE_USE_HOME_WIFI, true);
        if (!useHomeWifi) {
            return false;
        }

        String storedSsid = sp.getString(PrefsFragment.PREFERENCE_HOME_WIFI_SSID, "");
        return currentSsid.equals(storedSsid);
    }
}
