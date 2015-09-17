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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
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

import net.mceoin.cominghome.gcm.GcmRegister;
import net.mceoin.cominghome.geofence.FenceHandling;
import net.mceoin.cominghome.geofence.GeofenceRegister;
import net.mceoin.cominghome.geofence.SimpleGeofence;
import net.mceoin.cominghome.geofence.SimpleGeofenceStore;
import net.mceoin.cominghome.history.HistoryUpdate;
import net.mceoin.cominghome.oauth.OAuthFlowApp;
import net.mceoin.cominghome.structures.StructuresBean;
import net.mceoin.cominghome.structures.StructuresUpdate;
import net.mceoin.cominghome.structures.StructuresValues;
import net.mceoin.cominghome.wizard.InitialWizardActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean debug = true;

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";
    public static final String PREFS_INITIAL_WIZARD = "initial_wizard";
    public static final String PREFS_STRUCTURE_ID = "structure_id";
    public static final String PREFS_STRUCTURE_NAME = "structure_name";
    public static final String PREFS_LAST_AWAY_STATUS = "last_away_status";
    public static final String PREFS_LAST_MAP_LATITUDE = "last_map_latitude";
    public static final String PREFS_LAST_MAP_LONGITUDE = "last_map_longitude";
    public static final String PREFS_NOTIFICATIONS = "notifications";
    public static final String PREFS_LAST_PERMISSION_REQUEST = "last_permission_request";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    protected static final int REQUEST_PICK_STRUCTURE = 2;

    /**
     * Request code for the OAuth activity
     */
//    protected static final int REQUEST_CODE_OAUTH = 2;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean mIsInResolution;

    public static SharedPreferences prefs;
    Button connectButton;
    TextView structureNameText;
    TextView awayStatusText;
    Button atHomeButton;

    public static String access_token = "";
    public static String structure_id = "";
    String structure_name = "";
    String away_status = "";

    long last_info_check = 0;

    Location mCurrentLocation;

    GoogleMap map;
    Map<String, Marker> mapMarkers = new HashMap<>();
    Map<String, Circle> mapCircles = new HashMap<>();

    public static final String FENCE_HOME = "home";

    float fenceRadius;      // meters
    float fenceRadiusExit;  // meters

    private SimpleGeofenceStore mGeofenceStorage;

    // Store a list of geofences to add
    List<Geofence> mCurrentGeofences;

    private SimpleGeofence homeGeofence;

    // Add geofences handler
    private GeofenceRegister mGeofenceRegister;

    private Toolbar toolbar;

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
        @NonNull
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
        structure_id = prefs.getString(PREFS_STRUCTURE_ID, "");

        fenceRadius = prefs.getInt(PrefsFragment.PREFERENCE_GEOFENCE_RADIUS,
                getResources().getInteger(R.integer.geofence_radius_default));
        boolean sameRadius = prefs.getBoolean(PrefsFragment.PREFERENCE_GEOFENCE_SAME_RADIUS, true);
        if (sameRadius) {
            fenceRadiusExit = fenceRadius;
        } else {
            fenceRadiusExit = prefs.getInt(PrefsFragment.PREFERENCE_GEOFENCE_RADIUS_EXIT,
                    getResources().getInteger(R.integer.geofence_radius_exit_default));
        }

        Installation.id(this);

        setContentView(R.layout.main);

        connectButton = (Button) findViewById(R.id.buttonConnectNest);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                startActivity(new Intent().setClass(arg0.getContext(), OAuthFlowApp.class));
            }
        });

        structureNameText = (TextView) findViewById(R.id.structure_name);
        awayStatusText = (TextView) findViewById(R.id.away_status);

        playServicesConnected();

        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        if (map != null) {
            map.setMyLocationEnabled(true);
            map.getUiSettings().setZoomControlsEnabled(true);

            float lastLatitude = prefs.getFloat(PREFS_LAST_MAP_LATITUDE, 0);
            float lastLongitude = prefs.getFloat(PREFS_LAST_MAP_LONGITUDE, 0);
            if ((lastLatitude != 0) && (lastLongitude != 0)) {
                LatLng current = new LatLng(lastLatitude, lastLongitude);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 13));
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(NestUtils.GOT_INFO));

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(NestUtils.LOST_AUTH));

        atHomeButton = (Button) findViewById(R.id.buttonSetAtHome);
        atHomeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                updateHome(false, false);
                HistoryUpdate.add(getApplicationContext(), "Updated home");
            }
        });

        // Instantiate a Geofence requester
        mGeofenceRegister = new GeofenceRegister(this);

        mGeofenceStorage = new SimpleGeofenceStore(getApplicationContext());
        // Instantiate the current List of geofences
        mCurrentGeofences = new ArrayList<>();

        loadFences();
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar!=null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
        setActionBarIcon(R.drawable.home);

        buildGoogleApiClient();

        // setup Google Cloud Messaging
        GcmRegister gcmRegister = new GcmRegister();
        gcmRegister.register(getApplicationContext());

    }

    private static final int PERMISSIONS_REQUEST_LOCATION = 1;

    private void checkPermissions() {
        //
        // This is a stupid hack.  I can't seem to figure out how to use
        // the pre-defined Manifest.permission.{anything}
        //
        final String PERMISSIONS_LOCATION = "android.permission.ACCESS_FINE_LOCATION";

        if (Build.VERSION.SDK_INT < 23) {
            // if earlier than Marshmellow, then don't bother
            return;
        }

        if (checkSelfPermission(PERMISSIONS_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ImageView locationPermission = (ImageView) findViewById(R.id.location_permission);
            locationPermission.setVisibility(View.VISIBLE);

            long lastPermissionRequest = prefs.getLong(PREFS_LAST_PERMISSION_REQUEST, 0);
            long currentTime = System.currentTimeMillis();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(PREFS_LAST_PERMISSION_REQUEST, currentTime);
            editor.apply();

            final long PERMISSION_REQUEST_WAIT = 10 * 60 * 1000;
            long timeDelta = currentTime - lastPermissionRequest;
            if (debug) Log.d(TAG, "timeDelta = " + currentTime +
                    " - " + lastPermissionRequest + " = " + timeDelta);
            if ((timeDelta) < PERMISSION_REQUEST_WAIT){
                // don't bother asking for permission if it's been less than the wait time
                if (debug) Log.d(TAG, "already requested permission recently");
                return;
            }

            if (debug) Log.d(TAG, "requesting permission for location");
            requestPermissions(new String[]{PERMISSIONS_LOCATION}, PERMISSIONS_REQUEST_LOCATION);

        } else {
            if (debug) Log.d(TAG, "we have permission for location");
            ImageView locationPermission = (ImageView) findViewById(R.id.location_permission);
            locationPermission.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (debug) Log.d(TAG, "permission request was allowed");
                    ImageView locationPermission = (ImageView) findViewById(R.id.location_permission);
                    locationPermission.setVisibility(View.INVISIBLE);
                    if (mGoogleApiClient!=null) {
                        mGoogleApiClient.reconnect();
                    }
                } else {
                    if (debug) Log.d(TAG, "permission request was denied");
                    ImageView locationPermission = (ImageView) findViewById(R.id.location_permission);
                    locationPermission.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void updateHome(boolean keepOldFence, boolean confirmed) {
        if (!keepOldFence) {
            if (!mGoogleApiClient.isConnected()) {

                if (debug) Log.d(TAG, "mGoogleApiClient not connected");
                return;
            }
        }

        SimpleGeofence returnGeofence = updateGeofenceLocation(FENCE_HOME, keepOldFence, confirmed);
        if (returnGeofence != null) {
            homeGeofence = returnGeofence;
            updateGeofences();
        }
    }

    protected void setActionBarIcon(int iconRes) {
        toolbar.setNavigationIcon(iconRes);
    }

    private void loadFences() {

        homeGeofence = mGeofenceStorage.getGeofence(FENCE_HOME);
        if (homeGeofence != null) {
            updateMarker(homeGeofence.getLatitude(), homeGeofence.getLongitude(), FENCE_HOME, false);
            mCurrentGeofences.add(homeGeofence.toGeofence());
        }

        if (!mCurrentGeofences.isEmpty()) {
            updateGeofences();
        }

    }

    private SimpleGeofence updateGeofenceLocation(String geofenceId, boolean keepOldLatLong,
                                                  boolean confirmed) {

        double latitude;
        double longitude;
        SimpleGeofence oldFence = mGeofenceStorage.getGeofence(geofenceId);

        if (keepOldLatLong) {
            if (oldFence == null) {
                return null;
            }
            latitude = oldFence.getLatitude();
            longitude = oldFence.getLongitude();
        } else {
            if ((!mGoogleApiClient.isConnected())) {
                return null;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation == null) {
                Log.i(TAG, "updateGeofenceLocation mCurrentLocation is null");
                return null;
            }
            if (debug)
                Log.d(TAG, "updateGeofenceLocation: mCurrentLocation=" + mCurrentLocation.toString());

            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
            Log.i(TAG, "updateGeofenceLocation latitude=" + latitude);

            if (!confirmed && (oldFence != null)) {

                float distFromOldFence = LocationUtils.distFrom(oldFence.getLatitude(), oldFence.getLongitude(),
                        latitude, longitude);

                // only confirm if greater than 200 meters from old fence
                if (distFromOldFence > 200) {
                    showAtHomeConfirmation();
                    return null;
                }
            }
        }

        SimpleGeofence newFence = new SimpleGeofence(
                geofenceId,
                latitude,
                longitude,
                fenceRadius,
                Geofence.NEVER_EXPIRE,
                Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT);

        String geofenceExitId = geofenceId + "-Exit";
        SimpleGeofence newFenceExit = new SimpleGeofence(
                geofenceExitId,
                latitude,
                longitude,
                fenceRadiusExit,
                Geofence.NEVER_EXPIRE,
                Geofence.GEOFENCE_TRANSITION_EXIT);

        if (oldFence != null) {
            mGeofenceStorage.clearGeofence(geofenceId);
        }
        // Store this flat version
        mGeofenceStorage.setGeofence(geofenceId, newFence);
        mGeofenceStorage.setGeofence(geofenceExitId, newFenceExit);

        Geofence prevFence = null;
        Geofence prevFenceExit = null;
        for (Geofence fence : mCurrentGeofences) {
            if (fence.getRequestId().equals(geofenceId)) {
                prevFence = fence;
            } else if (fence.getRequestId().equals(geofenceExitId)) {
                prevFenceExit = fence;
            }
        }
        if (prevFence != null) {
            mCurrentGeofences.remove(prevFence);
        }
        if (prevFenceExit != null) {
            mCurrentGeofences.remove(prevFenceExit);
        }
        mCurrentGeofences.add(newFence.toGeofence());
        mCurrentGeofences.add(newFenceExit.toGeofence());

        updateMarker(latitude, longitude, geofenceId, !keepOldLatLong);

        return newFence;
    }

    private void showAtHomeConfirmation() {

        AlertDialog.Builder builder;

        builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_move_at_home));
        builder.setIcon(android.R.drawable.ic_dialog_map);
        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                updateHome(false, true);
            }
        });
        builder.setMessage(R.string.msg_move_at_home);

        AlertDialog moveAtHomeDialog = builder.create();
        moveAtHomeDialog.show();
    }

    private void updateMarker(double latitude, double longitude, String geofenceId, boolean move) {
        String geofenceExitId = geofenceId + "-exit";
        LatLng current = new LatLng(latitude, longitude);
        Marker marker;
        Circle circle;
        if (mapMarkers.containsKey(geofenceId)) {
            marker = mapMarkers.get(geofenceId);
            marker.setPosition(current);

            circle = mapCircles.get(geofenceId);
            if (circle != null) {
                circle.setCenter(current);
                circle.setRadius(fenceRadius);
            } else {
                Log.e(TAG, "missing circle for " + geofenceId);
            }
            circle = mapCircles.get(geofenceExitId);
            if (circle != null) {
                circle.setCenter(current);
                circle.setRadius(fenceRadiusExit);
            } else {
                Log.e(TAG, "missing circle for " + geofenceExitId);
            }
        } else {
            int iconId;
            if (geofenceId.equals(FENCE_HOME)) {
                iconId = R.drawable.home;
            } else {
                iconId = R.drawable.my_briefcase;
            }
            if (map != null) {

                CircleOptions circleOptions = new CircleOptions()
                        .center(current)
                        .radius(fenceRadius); // In meters

                CircleOptions circleExitOptions = new CircleOptions()
                        .center(current)
                        .strokeColor(Color.BLUE)
                        .radius(fenceRadiusExit); // In meters

                circle = map.addCircle(circleOptions);
                mapCircles.put(geofenceId, circle);

                circle = map.addCircle(circleExitOptions);
                mapCircles.put(geofenceExitId, circle);

                marker = map.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromResource(iconId))
                        .anchor(0.5f, 0.5f)
                        .title(geofenceId)
                        .position(current));
                mapMarkers.put(geofenceId, marker);
            }
        }
        if ((move) && (map != null)) map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 14));
    }

    private void updateGeofences() {

        // Start the request. Fail if there's already a request in progress
        try {
            // Try to add geofences
            mGeofenceRegister.populateGeofenceList();
            mGeofenceRegister.addGeofences();
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.add_geofences_already_requested_error,
                    Toast.LENGTH_LONG).show();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(NestUtils.GOT_INFO)) {
                String got_structure_name = intent.getStringExtra("structure_name");
                String got_away_status = intent.getStringExtra("away_status");

                if (structureNameText != null) {
                    structureNameText.setText(got_structure_name);
                }
                if (awayStatusText != null) {
                    awayStatusText.setText(got_away_status);
                }
            } else if (intent.getAction().equals(NestUtils.LOST_AUTH)) {
                if (structureNameText != null) {
                    structureNameText.setText("");
                }
                if (awayStatusText != null) {
                    awayStatusText.setText("");
                }
                connectButton.setEnabled(true);
                connectButton.setVisibility(View.VISIBLE);
                Toast.makeText(context, R.string.lost_auth, Toast.LENGTH_LONG).show();

            } else {
                double latitude = intent.getDoubleExtra("latitude", 0);
                double longitude = intent.getDoubleExtra("longitude", 0);
                LatLng current = new LatLng(latitude, longitude);
                if (debug)
                    Log.d(TAG, "Got location update: " + latitude + ", " + longitude);
                if (map != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 13));
                }
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem fake_arrived = menu.findItem(R.id.fake_arrived);
        fake_arrived.setVisible(debug);
        MenuItem fake_left = menu.findItem(R.id.fake_left);
        fake_left.setVisible(debug);

        MenuItem select_structures = menu.findItem(R.id.select_structure);
        int structures = StructuresUpdate.countStructureIds(getApplicationContext());
        if (structures > 1) {
            select_structures.setVisible(true);
        } else {
            select_structures.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fake_arrived:
                if ((structure_id != null) && (!structure_id.isEmpty())) {
                    FenceHandling.arrivedHome(getApplicationContext());
                }
                return true;
            case R.id.fake_left:
                if ((structure_id != null) && (!structure_id.isEmpty())) {
                    FenceHandling.leftHome(getApplicationContext());
                }
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.select_structure:
                Intent pickContactIntent = new Intent(Intent.ACTION_PICK, StructuresValues.Structures.CONTENT_URI);
                startActivityForResult(pickContactIntent, REQUEST_PICK_STRUCTURE);
                return true;
            case R.id.history:
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            case R.id.about_menu:
                showAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void showAbout() {
        // Inflate the about message contents
        @SuppressLint("InflateParams") View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        PackageInfo pi;
        String version = "";
        try {
            PackageManager pm = this.getPackageManager();
            String pn = this.getPackageName();
            if (pm == null) {
                version = "unknown";
            } else {
                pi = pm.getPackageInfo(pn, 0);
                version = pi.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TextView versionView = (TextView) messageView.findViewById(R.id.about_version);
        versionView.setText(version);

        // When linking text, force to always use default color. This works
        // around a pressed color state bug.
        TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.home);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
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

        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean initialWizardRan = prefs.getBoolean(PREFS_INITIAL_WIZARD, false);
        if (!initialWizardRan) {
            startActivity(new Intent().setClass(getApplicationContext(), InitialWizardActivity.class));
            finish();
        }

        structure_id = prefs.getString(PREFS_STRUCTURE_ID, "");
        structure_name = prefs.getString(PREFS_STRUCTURE_NAME, "");
        away_status = prefs.getString(PREFS_LAST_AWAY_STATUS, "");

        structureNameText.setText(structure_name);
        awayStatusText.setText(away_status);

        float previousFenceRadius = fenceRadius;
        float previousFenceRadiusExit = fenceRadiusExit;
        fenceRadius = prefs.getInt(PrefsFragment.PREFERENCE_GEOFENCE_RADIUS,
                getResources().getInteger(R.integer.geofence_radius_default));
        boolean sameRadius = prefs.getBoolean(PrefsFragment.PREFERENCE_GEOFENCE_SAME_RADIUS, true);
        if (sameRadius) {
            fenceRadiusExit = fenceRadius;
        } else {
            fenceRadiusExit = prefs.getInt(PrefsFragment.PREFERENCE_GEOFENCE_RADIUS_EXIT,
                    getResources().getInteger(R.integer.geofence_radius_exit_default));
        }
        if ((fenceRadius != previousFenceRadius) || (fenceRadiusExit != previousFenceRadiusExit)) {
            // it must have been changed by settings
            updateHome(true, true);
        }

        access_token = prefs.getString(OAuthFlowApp.PREF_ACCESS_TOKEN, "");
        if (access_token.isEmpty()) {
            connectButton.setEnabled(true);
            connectButton.setVisibility(View.VISIBLE);
        } else {
            connectButton.setEnabled(false);
            connectButton.setVisibility(View.GONE);
            long currentTime = System.currentTimeMillis();
            // make sure it's been at least 60 seconds since last time we got info
            if (currentTime > (last_info_check + 60 * 1000)) {
                NestUtils.getInfo(getApplicationContext(), access_token, null);
                last_info_check = System.currentTimeMillis();
            }
        }
        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
        checkPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (map != null) {
            SharedPreferences.Editor editor = prefs.edit();

            CameraPosition cameraPosition = map.getCameraPosition();
            editor.putFloat(PREFS_LAST_MAP_LATITUDE, (float) cameraPosition.target.latitude);
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
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
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
            case REQUEST_PICK_STRUCTURE:
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    Uri structureUri = data.getData();
                    StructuresBean structuresBean = StructuresUpdate.getStructureId(this, structureUri);
                    if (debug) Log.d(TAG, "structureUri=" + structureUri.toString() +
                            " structureBean=" + structuresBean.toString());
                    structure_id = structuresBean.getId();
                    structure_name = structuresBean.getName();
                    away_status = structuresBean.getAway();

                    structureNameText.setText(structure_name);
                    awayStatusText.setText(away_status);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PREFS_STRUCTURE_ID, structure_id);
                    editor.putString(PREFS_STRUCTURE_NAME, structure_name);
                    editor.putString(PREFS_LAST_AWAY_STATUS, away_status);
                    editor.apply();
                }
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
        if (debug) Log.d(TAG, "GoogleApiClient connected");
        if (map != null) {
            //
            // The very first time we run, the map will be at 0,0 (or very near)
            // Check that we have a map and a location, if so, zoom to it
            //
            CameraPosition cameraPosition = map.getCameraPosition();
            float distFrom0 = LocationUtils.distFrom(cameraPosition.target.latitude, cameraPosition.target.longitude, 0, 0);
            if (debug) Log.d(TAG, "cameraPosition lat=" + cameraPosition.target.latitude +
                    " long=" + cameraPosition.target.longitude + " distFrom0=" + distFrom0);
            if (distFrom0 < 10000) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mCurrentLocation == null) {
                    return;
                }
                if (debug) Log.d(TAG, mCurrentLocation.toString());
                double latitude = mCurrentLocation.getLatitude();
                double longitude = mCurrentLocation.getLongitude();

                LatLng current = new LatLng(latitude, longitude);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(current,
                        13));
            }
        }

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

            if (debug) {
                int v = 0;
                try {
                    v = getPackageManager().getPackageInfo("com.google.android.gms", 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "Google Play services available: client " +
                        GooglePlayServicesUtil.GOOGLE_PLAY_SERVICES_VERSION_CODE + " package " + v);
            }
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

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }
}
