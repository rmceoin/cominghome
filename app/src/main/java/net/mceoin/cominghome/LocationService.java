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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;

import java.util.List;

public class LocationService extends Service implements GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {
    private static final String TAG = LocationService.class.getSimpleName();
    private static final boolean debug = true;

    private LocationManager locationManager = null;

    private long minUpdateTime;
    private static final long MAX_UPDATE_TIME = 120 * 60 * 1000L;
    private float minUpdateDistance;

    private Location myLocation;

    LocationManager lm;
    LocationHelper loc;

    long timeAtLastUpdate = 0;

    int secondsToSleep = 30;
    boolean runBackgroundThread;
    boolean checkinRequested = false;
    private static final int MAXSLEEP_WHILE_NOT_MOVING = 15 * 60;
    private static final int MAXSLEEP_WHILE_MOVING = 5 * 60;

    public static final String LOCATION_CHANGED = "net.mceoin.cominghome.LocationService.LocationChanged";

    LocationClient mLocationClient;

    public static boolean isRunning(Context context) {
        return MainActivity.isMyServiceRunning(context, LocationService.class);
    }

    public LocationService() {
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (debug) Log.d(TAG, "onConnected()");
        mLocationClient.connect();
    }

    @Override
    public void onDisconnected() {
        if (debug) Log.d(TAG, "onDisconnected()");
        mLocationClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (debug) Log.d(TAG, "onConnectionFailed()");

    }

    public class LocationBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    private final IBinder mBinder = new LocationBinder();

    @Override
    public IBinder onBind(Intent intent) {
        if (debug) Log.d(TAG, "onBind()");
        servicesConnected();
        return mBinder;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (debug) Log.d(TAG, "onStartCommand()");
        startBackgroundTask();
        mLocationClient = new LocationClient(getApplicationContext(), this, this);
//        mLocationClient.connect();
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        runBackgroundThread = false;
        if (backgroundThread != null) {
            backgroundThread.interrupt();
        }

    }

    Thread backgroundThread = null;

    private void startBackgroundTask() {
        runBackgroundThread = true;
        // Start a background thread and begin the processing.
        // This moves the time consuming operation to a child thread.
        backgroundThread = new Thread(null, doBackgroundThreadProcessing,
                "LocationChecker");
        backgroundThread.start();

    }

    //Runnable that executes the background processing method.
    private Runnable doBackgroundThreadProcessing = new Runnable() {
        public void run() {
            backgroundThreadProcessing();
        }
    };

    private void backgroundThreadProcessing() {
        secondsToSleep = 30;
        while (runBackgroundThread) {
            if (locationManager == null) {
                initLocationManager();
            }
            try {
                if (debug) Log.d(TAG, "sleeping " + secondsToSleep + " seconds");
                Thread.sleep(secondsToSleep * 1000);
                if (secondsToSleep < MAXSLEEP_WHILE_NOT_MOVING) {
                    secondsToSleep += 15;
                }
            } catch (InterruptedException e) {
//                e.printStackTrace();
                if (debug) Log.d(TAG, "wake up!");
            }
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = locationManager.getBestProvider(criteria, true);

            provider = LocationManager.GPS_PROVIDER;
            Location location = locationManager.getLastKnownLocation(provider);
            if (mLocationClient.isConnected()) {
                location = mLocationClient.getLastLocation();
            }
            if (debug) Log.d(TAG, "location=" + location);
            if (myLocation == null) {
                myLocation = location;
                broadcastLocationChanged(myLocation);
            } else {
                float dist = distFrom(myLocation, location);
                if (debug) Log.d(TAG, "dist=" + dist);
                if ((checkinRequested) || (dist > 20)) {
                    myLocation = location;
                    broadcastLocationChanged(myLocation);
                    checkinRequested = false;
                    if (secondsToSleep > MAXSLEEP_WHILE_MOVING) {
                        // if we're moving, don't sleep too much
                        secondsToSleep = MAXSLEEP_WHILE_MOVING;
                    }
                }
            }

        }
    }

    public class TrackLocation extends Thread {
//        String latString, lngString = null;

        public TrackLocation(Context context) {
            loc = new LocationHelper();
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            setName("TrackLocation");
        }

        public void run() {
            Looper.prepare();
            minUpdateTime = 30 * 1000L;
            /*
             * minimum distance between location updates, in meters
             */
            minUpdateDistance = 200.0f;
//            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minUpdateTime, minUpdateDistance, loc);
            timeAtLastUpdate = System.currentTimeMillis();
            Looper.loop();
        }
    }

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

    public static float distFrom(Location location1, Location location2) {
        return distFrom(location1.getLatitude(), location1.getLongitude(),
                location2.getLatitude(), location2.getLongitude());
    }

    public class LocationHelper implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            if (debug) Log.d(TAG, "onLocationChanged(" + location + ")");
            if (location != null) {
                if (myLocation != null) {
                    float distance = distFrom(myLocation.getLatitude(), myLocation.getLongitude(),
                            location.getLatitude(), location.getLongitude());
                    if (debug) Log.d(TAG, "distance=" + distance);
                }
                myLocation = location;
                long minTimeBetweenUpdate = 50 * 1000L;
                if (System.currentTimeMillis() > (timeAtLastUpdate + minTimeBetweenUpdate)) {
                    broadcastLocationChanged(myLocation);
                }
                if (minUpdateTime < MAX_UPDATE_TIME) {
                    minUpdateTime += 30 * 1000L;
                    if (debug) Log.d(TAG, "increase minUpdateTime to " + minUpdateTime);
                    lm.removeUpdates(loc);
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minUpdateTime, minUpdateDistance, loc);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    }

    private void broadcastLocationChanged(Location location) {
        if (debug) Log.d(TAG, "broadcastLocationChanged(" + location + ")");
        if (location == null) return;
        timeAtLastUpdate = System.currentTimeMillis();
        Intent intent = new Intent(LOCATION_CHANGED);
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void initLocationManager() {
        Context context = getApplicationContext();
        if (context == null) {
            if (debug) Log.d(TAG, "no context!");
            return;
        }
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        if (debug) Log.d(TAG, "providers=" + providers);
        new TrackLocation(getApplicationContext()).start();

    }

    void checkin() {
        if (debug) Log.d(TAG, "checkin()");
        if (backgroundThread != null) {
            if (debug) Log.d(TAG, "interrupting background thread");
            secondsToSleep = 30;
            checkinRequested = true;
            backgroundThread.interrupt();
        }
    }


    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            if (debug) Log.d(TAG,
                    "Google Play services is available.");
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            if (debug) Log.d(TAG,
                    "Google Play services is not available.");
            return false;
        }
    }
}
