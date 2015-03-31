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
package net.mceoin.cominghome.cloud;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;

import net.mceoin.cominghome.R;

/**
 * Cloud utility functions
 */
public class CloudUtil {
    /**
     * Checks for network connectivity via WiFi or Mobile.
     *
     * @param context Context of application
     * @return A concatenated string with two fields separated by a colon.  The first field is Wifi
     * and second field is Mobile.  If wifi is up, there will be an uppercase 'W', otherwise
     * lowercase 'w'.
     * If mobile is up, there will be an uppercase 'M', otherwise lowercsae 'm'.
     * <p/>
     * For example, if WiFi is up, but Mobile is down, the return string will be W:m
     */
    public static String getNetworkStatus(@NonNull Context context) {

        String result = "";

        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
        boolean wifiConnected;
        boolean mobileConnected;
        if (activeInfo != null && activeInfo.isConnected()) {
            wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
            mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            wifiConnected = false;
            mobileConnected = false;
        }

        if (wifiConnected) {
            result += "W";
        } else {
            result += "w";
        }
        result += ":";
        if (mobileConnected) {
            result += "M";
        } else {
            result += "m";
        }
        return result;
    }

    /**
     * Get our application name along with version code.  Useful for when making an HTTP call
     * as a UserAgent.
     *
     * @param context Context of application
     * @return String in the format of {AppName}/{VersionCode}
     */
    public static String getApplicationName(Context context) {
        String name = context.getString(R.string.app_name).replace(" ","-");
        int version;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            version = 1;
        }
        return name + "/" + version;
    }
}
