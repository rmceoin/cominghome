/*
 * Copyright 2016 Randy McEoin
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
/*
 * Got this from:
 *
 * http://android-developers.blogspot.com/2011/03/identifying-app-installations.html
 *
 */

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Handle creating a unique identifier for the specific installation of the application
 * that the end user is running.  This is used by the backend to differentiate one
 * phone/tablet from another.
 * <p/>
 * This is used to allow multiple processes to read/write the correct values.
 * This is because
 * {@link Context#getSharedPreferences(String, int)}
 * deprecated
 * {@link Context#MODE_MULTI_PROCESS}.  Their suggestion upon deprecation was to
 * use a ContentProvider, which seemed a bit heavy for one shared value.
 */
public class AlarmTime {
    public static final String TAG = "AlarmTime";
    public static final boolean debug = true;

    private static final String INSTALLATION = "AlarmTime";

    /**
     * Retrieve when timer was set.
     *
     * @param context Context of app
     * @return long of when timer was set
     */
    public synchronized static long getTime(@NonNull Context context) {
        File alarmtime = new File(context.getFilesDir(), INSTALLATION);
        try {
            if (alarmtime.exists())
                return readAlarmTimeFile(alarmtime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public synchronized static void setTime(@NonNull Context context, long time) {
        File alarmtime = new File(context.getFilesDir(), INSTALLATION);
        try {
            writeAlarmTimeFile(alarmtime, time);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static long readAlarmTimeFile(@NonNull File alarmTime) throws IOException {
        RandomAccessFile f = new RandomAccessFile(alarmTime, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        long time;
        try {
            time = Long.parseLong(new String(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (debug) Log.d(TAG, "readAlarmTimeFile: time=" + time);
        return time;
    }

    private static void writeAlarmTimeFile(@NonNull File alarmTimeFile, long time) throws IOException {
        if (debug) Log.d(TAG, "writeAlarmTimeFile: time=" + time);
        FileOutputStream out = new FileOutputStream(alarmTimeFile);
        String id = String.valueOf(time);
        out.write(id.getBytes());
        out.close();
    }
}