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
/*
 * Got this from:
 *
 * http://android-developers.blogspot.com/2011/03/identifying-app-installations.html
 *
 */
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Handle creating a unique identifier for the specific installation of the application
 * that the end user is running.  This is used by the backend to differentiate one
 * phone/tablet from another.
 */
public class Installation {
    public static final String TAG = "Installation";
    public static final boolean debug = true;

    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    /**
     * Retrieve a unique identifier.
     *
     * @param context
     * @return String unique identifier
     */
    public synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
                if (sID.length()<5) {
                    // something wrong happened, sID is too short, recreate it
                    writeInstallationFile(installation);
                    sID = readInstallationFile(installation);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }
}