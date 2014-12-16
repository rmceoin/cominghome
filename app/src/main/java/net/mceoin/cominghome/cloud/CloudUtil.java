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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Cloud utility functions
 */
public class CloudUtil {
    /**
     * Checks for network connectivity via WiFi or Mobile.
     *
     * @param context Context of application
     * @return A concatenated string with two fields separated by a colon.  The first field is Wifi
     * and second field is Mobile.  If wifi is up, there will be a 'w', otherwise no character.
     * If mobile is up, there will be a 'm', otherwise no character.  If neither is up, then the
     * string will only be the colon.
     */
    public static String getNetworkStatus(Context context) {

        String result="";

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

        if (wifiConnected) result+="w";
        result+=":";
        if (mobileConnected) result+="m";
        return result;
    }
}
