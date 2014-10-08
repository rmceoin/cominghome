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

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import net.mceoin.cominghome.oauth.OAuthFlowApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends FragmentActivity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean debug = true;

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    public static final String PREFS_STRUCTURE_ID = "structure_id";
    public static final String PREFS_STRUCTURE_NAME = "structure_name";
    public static final String PREFS_LAST_AWAY_STATUS = "last_away_status";
    public static final String PREFS_LAST_MAP_LATITUDE = "last_map_latitude";
    public static final String PREFS_LAST_MAP_LONGITUDE = "last_map_longitude";
    public static final String PREFS_NOTIFICATIONS = "notifications";
    public static final String PREFS_TIME_LEFT_WORK = "time_left_work";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Request code for the OAuth activity
     */
//    protected static final int REQUEST_CODE_OAUTH = 2;

    /**
     * Google API client.
     */
//    private GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;

    public static SharedPreferences prefs;
    Button getNestInfo;
    Button connectButton;
    TextView structureNameText;
//    Button sendETAButton;
    TextView awayStatusText;
//    EditText ETAminutes;
    Button atHomeButton;
    Button atWorkButton;

    public static String access_token = "";
    public static String structure_id = "";
    String structure_name = "";
    String trip_id = "";
    String away_status = "";

    LocationClient mLocationClient;
    Location mCurrentLocation;

    GoogleMap map;
    Map<String, Marker> mapMarkers = new HashMap<String, Marker>();
    Map<String, Circle> mapCircles = new HashMap<String, Circle>();

//    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
//    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
//            GEOFENCE_EXPIRATION_IN_HOURS * DateUtils.HOUR_IN_MILLIS;

    public static final String FENCE_HOME = "home";
    public static final String FENCE_WORK = "work";

    float fenceRadius = 100;    // meters

    private SimpleGeofenceStore mGeofenceStorage;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;

    private SimpleGeofence homeGeofence;
    private SimpleGeofence workGeofence;

    // Add geofences handler
    private GeofenceRequester mGeofenceRequester;

    // Store the current request
//    private GeofenceUtils.REQUEST_TYPE mRequestType;


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (debug) {
                Log.d(TAG, "handling stuff");
            }
            Bundle b = msg.getData();
            String msgType = b.getString("type");

            if (msgType.equals(NestUtils.MSG_STRUCTURES)) {
                structure_id = b.getString("structure_id");
                structure_name = b.getString("structure_name");
                away_status = b.getString("away_status");
                if (debug) {
                    Log.d(TAG, "structure_id=" + structure_id);
                    Log.d(TAG, "structure_name=" + structure_name);
                    Log.d(TAG, "away_status=" + away_status);
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREFS_STRUCTURE_ID, structure_id);
                editor.putString(PREFS_STRUCTURE_NAME, structure_name);
                editor.apply();
                structureNameText.setText(structure_name);
                awayStatusText.setText(away_status);
//                sendETAButton.setEnabled(true);
            } else if (msgType.equals(NestUtils.MSG_GET_OTHERS)) {
                if (debug) Log.d(TAG,"message get others");
                String result = b.getString("result");
                if (debug) Log.d(TAG,"result="+result);
            } else {
                Log.e(TAG,"unknown handle message: "+msgType);
            }
        }
    };

    /*
 * Define a request code to send to Google Play services
 * This code is returned in Activity.onActivityResult
 */
//    private final static int
//            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        structure_name = prefs.getString(PREFS_STRUCTURE_NAME, "");
        structure_id = prefs.getString(PREFS_STRUCTURE_ID, "");
        away_status = prefs.getString(PREFS_LAST_AWAY_STATUS, "");

        Installation.id(this);

        setContentView(R.layout.main);

        connectButton = (Button) findViewById(R.id.buttonConnectNest);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                startActivity(new Intent().setClass(arg0.getContext(), OAuthFlowApp.class));
            }
        });

        getNestInfo = (Button) findViewById(R.id.buttonGetNestInfo);
        getNestInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (!access_token.isEmpty()) {
                    NestUtils.getInfo(getApplicationContext(), access_token);
                }
            }
        });
        structureNameText = (TextView) findViewById(R.id.structure_name);
        structureNameText.setText(structure_name);

/*
        sendETAButton = (Button) findViewById(R.id.buttonSendETA);
        sendETAButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (trip_id.isEmpty()) {
                    trip_id = UUID.randomUUID().toString();
                }
                int minutes = Integer.parseInt(ETAminutes.getText().toString());

                NestUtils.sendETA(access_token, handler, structure_id, trip_id, minutes);
            }
        });
*/
        awayStatusText = (TextView) findViewById(R.id.away_status);

        if (away_status.isEmpty()) {
            awayStatusText.setText("Checking status");
        } else {
            awayStatusText.setText(away_status);
        }

        playServicesConnected();

        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        if (map!=null) {
            map.setMyLocationEnabled(true);

            float lastLatitude=prefs.getFloat(PREFS_LAST_MAP_LATITUDE,0);
            float lastLongitude=prefs.getFloat(PREFS_LAST_MAP_LONGITUDE,0);
            if ((lastLatitude!=0) && (lastLongitude!=0)) {
                LatLng current = new LatLng(lastLatitude, lastLongitude);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 13));
            }
        }

        mLocationClient = new LocationClient(this, this, this);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LocationService.LOCATION_CHANGED));

        atHomeButton = (Button) findViewById(R.id.buttonSetAtHome);
        atHomeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                homeGeofence = updateGeofenceLocation(FENCE_HOME);
                if (homeGeofence!=null) updateGeofences();
            }
        });

        atWorkButton = (Button) findViewById(R.id.buttonSetAtWork);
        atWorkButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                workGeofence = updateGeofenceLocation(FENCE_WORK);
                if (workGeofence!=null) updateGeofences();
            }
        });
        // Instantiate a Geofence requester
        mGeofenceRequester = new GeofenceRequester(this);

        mGeofenceStorage = new SimpleGeofenceStore(getApplicationContext());
        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<Geofence>();

        loadFences();
    }

    /**
     * Call using isMyServiceRunning(MyService.class)
     * http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
     */
    public static boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void loadFences() {

        homeGeofence = mGeofenceStorage.getGeofence(FENCE_HOME);
        if (homeGeofence!=null) {
            updateMarker(homeGeofence.getLatitude(),homeGeofence.getLongitude(),FENCE_HOME,false);
            mCurrentGeofences.add(homeGeofence.toGeofence());
        }
        workGeofence = mGeofenceStorage.getGeofence(FENCE_WORK);
        if (workGeofence!=null) {
            updateMarker(workGeofence.getLatitude(),workGeofence.getLongitude(),FENCE_WORK,false);
            mCurrentGeofences.add(workGeofence.toGeofence());
        }

        if (!mCurrentGeofences.isEmpty()) {
            updateGeofences();
        }

    }

    private SimpleGeofence updateGeofenceLocation(String geofenceId) {
        if (mLocationClient == null) {
            return null;
        }
        mCurrentLocation = mLocationClient.getLastLocation();
        if (debug) Log.d(TAG, mCurrentLocation.toString());
        if (mCurrentLocation == null) {
            return null;
        }
        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();

        SimpleGeofence newFence = new SimpleGeofence(
                geofenceId,
                latitude,
                longitude,
                fenceRadius,
                Geofence.NEVER_EXPIRE,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);

        SimpleGeofence oldFence = mGeofenceStorage.getGeofence(geofenceId);
        if (oldFence != null) {
            mGeofenceStorage.clearGeofence(geofenceId);
        }
        // Store this flat version
        mGeofenceStorage.setGeofence(geofenceId, newFence);

        for (Geofence fence : mCurrentGeofences) {
            if (fence.getRequestId().equals(geofenceId)) {
                mCurrentGeofences.remove(fence);
            }
        }
        mCurrentGeofences.add(newFence.toGeofence());

        updateMarker(latitude, longitude, geofenceId, true);

        return newFence;
    }

    private void updateMarker(double latitude, double longitude, String geofenceId, boolean move) {
        LatLng current = new LatLng(latitude, longitude);
        Marker marker;
        Circle circle;
        if (mapMarkers.containsKey(geofenceId)) {
            marker = mapMarkers.get(geofenceId);
            marker.setPosition(current);

            circle = mapCircles.get(geofenceId);
            if (circle!=null) {
                circle.setCenter(current);
            } else {
                Log.e(TAG,"missing circle for "+geofenceId);
            }
        } else {
            int iconId;
            if (geofenceId.equals(FENCE_HOME)) {
                iconId = R.drawable.home;
            } else {
                iconId = R.drawable.my_briefcase;
            }
            if (map!=null) {

                CircleOptions circleOptions = new CircleOptions()
                        .center(current)
                        .radius(fenceRadius); // In meters
                circle = map.addCircle(circleOptions);
                mapCircles.put(geofenceId, circle);

                marker = map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(iconId))
                        .anchor(0.5f, 0.5f)
                        .title(geofenceId)
                        .position(current));
                mapMarkers.put(geofenceId, marker);
            }
        }
        if ((move)&&(map!=null)) map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 13));
    }

    private void updateGeofences() {

        // Start the request. Fail if there's already a request in progress
        try {
            // Try to add geofences
            mGeofenceRequester.addGeofences(mCurrentGeofences);
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.add_geofences_already_requested_error,
                    Toast.LENGTH_LONG).show();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            LatLng current = new LatLng(latitude, longitude);
            if (debug)
                Log.d(TAG, "Got location update: " + latitude + ", " + longitude);
            if (map != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 13));
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fake_arrived:
                if ((structure_id!=null) && (!structure_id.isEmpty())) {
                    FenceHandling.arrivedHome(getApplicationContext());
                }
                return true;
            case R.id.fake_left:
                if ((structure_id!=null) && (!structure_id.isEmpty())) {
                    FenceHandling.leftHome(getApplicationContext());
                }
                return true;
            case R.id.fake_left_work:
                if ((structure_id!=null) && (!structure_id.isEmpty())) {
                    FenceHandling.leftWork(getApplicationContext());
                }
                return true;
            case R.id.stop_tracking:
                LocationService.sendTrackingStop(getApplicationContext());
                return true;
            case R.id.settings:
                startActivity(new Intent(this,SettingsActivity.class));
                return true;
            case R.id.history:
                startActivity(new Intent(this,HistoryList.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
/*
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
*/
        mLocationClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        if (access_token.isEmpty()) {
            connectButton.setEnabled(true);
            connectButton.setVisibility(View.VISIBLE);
            getNestInfo.setEnabled(false);
            structureNameText.setText("");
        } else {
            connectButton.setEnabled(false);
            connectButton.setVisibility(View.GONE);
            getNestInfo.setEnabled(true);
        }
        structure_id = prefs.getString(PREFS_STRUCTURE_ID, "");
        structure_name = prefs.getString(PREFS_STRUCTURE_NAME, "");
/*
        if (structure_id.isEmpty()) {
            sendETAButton.setEnabled(false);
        } else {
            sendETAButton.setEnabled(true);
        }
*/
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (map!=null) {
            SharedPreferences.Editor editor = prefs.edit();

            CameraPosition cameraPosition=map.getCameraPosition();
            editor.putFloat(PREFS_LAST_MAP_LATITUDE,(float)cameraPosition.target.latitude);
            editor.putFloat(PREFS_LAST_MAP_LONGITUDE, (float) cameraPosition.target.longitude);
            editor.apply();
        }
    }
    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
//        if (mGoogleApiClient != null) {
//            mGoogleApiClient.disconnect();
//        }
        mLocationClient.disconnect();
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, mIsInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_RESOLUTION:
                retryConnecting();
                break;
        }
    }

    private void retryConnecting() {
        mIsInResolution = false;
//        if ((mGoogleApiClient!=null) && (!mGoogleApiClient.isConnecting())) {
//            mGoogleApiClient.connect();
//        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        // TODO: Start making API requests.
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
/*
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }
*/

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            retryConnecting();
                        }
                    }
            ).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (mIsInResolution) {
            return;
        }
        mIsInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean playServicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            int v=0;
            try {
                v = getPackageManager().getPackageInfo("com.google.android.gms", 0).versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (debug) Log.d(TAG, "Google Play services available: client "+
                    GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE + " package "+v);
            return true;

            // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), TAG);
            }
            return false;
        }
    }
}
