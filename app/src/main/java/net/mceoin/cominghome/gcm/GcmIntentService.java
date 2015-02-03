package net.mceoin.cominghome.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.location.LocationServices;

import net.mceoin.cominghome.LocationUtils;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.geofence.FenceHandling;
import net.mceoin.cominghome.geofence.SimpleGeofence;
import net.mceoin.cominghome.geofence.SimpleGeofenceStore;

/**
 * Handle a GCM message from the backend
 * <p/>
 * Inspired from https://developer.android.com/google/gcm/client.html
 */
public class GcmIntentService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    
    private static final String TAG = GcmIntentService.class.getSimpleName();
    private static final boolean debug = true;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    private SimpleGeofenceStore mGeofenceStorage;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            switch (messageType) {
                case GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR:
                    if (debug) Log.d(TAG, "Send error: " + extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_DELETED:
                    if (debug) Log.d(TAG, "Deleted messages on server: " +
                            extras.toString());
                    break;
                case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE:
                    // If it's a regular GCM message, do some work.
                    Log.i(TAG, "Received: " + extras.toString());
                    processMessage(extras);
                    break;
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    protected void processMessage(Bundle extras) {
        String command = extras.getString("command", "none");
        String message = extras.getString("message", "none");

        if (debug) Log.d(TAG, "command=" + command + " message=" + message);
        switch (command) {
            case "check-in":
                if (debug) Log.d(TAG, "need to check in");
                checkIn();
                break;
            case "none":
            default:
                Log.w(TAG, "unknown command: " + command);
                break;
        }
    }

    protected void checkIn() {
        if (mGoogleApiClient==null) {
            buildGoogleApiClient();
        }
        if (mGoogleApiClient!=null) {
            mGoogleApiClient.connect();
        }
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
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());

    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        if (debug) Log.d(TAG, "GoogleApiClient connected");
        Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mCurrentLocation == null) {
            return;
        }
        if (debug) Log.d(TAG, mCurrentLocation.toString());
        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();
        
        mGoogleApiClient.disconnect();

        mGeofenceStorage = new SimpleGeofenceStore(getApplicationContext());
        SimpleGeofence homeGeofence = mGeofenceStorage.getGeofence(MainActivity.FENCE_HOME);

        if (homeGeofence != null) {
            float distFromHome = LocationUtils.distFrom(latitude, longitude,
                    homeGeofence.getLatitude(), homeGeofence.getLongitude());
            if (distFromHome <= homeGeofence.getRadius()) {
                if (debug) Log.d(TAG, "we're at home");
                FenceHandling.arrivedHome(getApplicationContext());
            }
        }
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }
}
