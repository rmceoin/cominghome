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

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;

import net.mceoin.cominghome.geofence.SimpleGeofence;
import net.mceoin.cominghome.geofence.SimpleGeofenceStore;

public class LocationUtils {

    /**
     * Calculate distance in meters between two points.
     *
     * @param lat1 Latitude for point 1
     * @param lng1 Longitude for point 1
     * @param lat2 Latitude for point 2
     * @param lng2 Longitude for point 2
     * @return distance in meters
     */
    public static float distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;

        int meterConversion = 1609;

        return (float) (dist * meterConversion);
    }

    /**
     * Calculate the distance from home in meters.
     *
     * @param context  Application context
     * @param location Location to compare to home location
     * @return Distance in meters
     */
    public static float distFromHome(@NonNull Context context, @NonNull Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        SimpleGeofenceStore mGeofenceStorage;
        mGeofenceStorage = new SimpleGeofenceStore(context);
        SimpleGeofence geofence = mGeofenceStorage.getGeofence(MainActivity.FENCE_HOME);

        if (geofence == null) {
            return -1;
        }

        return LocationUtils.distFrom(latitude, longitude, geofence.getLatitude(),
                geofence.getLongitude());
    }
}
