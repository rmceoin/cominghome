package net.mceoin.cominghome.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * Handle a GCM message from the backend
 * <p/>
 * Inspired from https://developer.android.com/google/gcm/client.html
 */
public class GcmIntentService extends IntentService {
    private static final String TAG = GcmIntentService.class.getSimpleName();
    private static final boolean debug = true;

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
                    break;
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

}
