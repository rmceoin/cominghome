/* 
 * Copyright (C) 2015 Randy McEoin
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
 *
 * Grabbed from OpenIntents OI Safe project
 */
package net.mceoin.cominghome.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.R;

public class ServiceNotification {
    private static final boolean debug = true;
    private static String TAG = "ServiceNotification";

    private static final int NOTIFICATION_ID = 1;

    static NotificationManager mNotifyManager;
    static Builder notificationCompat;

    public static void setNotification(Context context) {

        if (debug) Log.d(TAG, "setNotification()" );
        // look up the notification manager service
        mNotifyManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);


        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent cancelIntent = new Intent(context, MainActivity.class);
        cancelIntent.putExtra("foo_bar_extra_key", "foo_bar_extra_value");
        PendingIntent cancelPendingIntent = PendingIntent.getActivity(
                context, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Intent awayIntent = new Intent(context, MainActivity.class);
        awayIntent.putExtra("foo_bar_extra_key", "foo_bar_extra_value");
        PendingIntent awayPendingIntent = PendingIntent.getActivity(
                context, 0, awayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        notificationCompat = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.delay_until_away))
                .setSmallIcon(R.drawable.home).setOngoing(true)
                .setOngoing(true)
                .setProgress(100, 0, false)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
                .addAction(android.R.drawable.ic_menu_send, "Away Now", awayPendingIntent)
                .setContentIntent(pi);

        mNotifyManager.notify(NOTIFICATION_ID, notificationCompat.build());
    }

    public static void clearNotification(Context context) {

        if (debug) Log.d(TAG, "clearNotification()" );
        // look up the notification manager service
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    /**
     * Update the existing notification progress bar.
     *
     * @param max Maximum value
     * @param progress A value between 0 and the max
     */
    public static void updateProgress(int max, int progress) {
        if (debug) Log.d(TAG, "updateProgress("+max+", "+progress+")" );
        if (progress > max) { progress = max; }
        notificationCompat.setProgress(max, progress, false);
        mNotifyManager.notify(NOTIFICATION_ID, notificationCompat.build());
    }
}
