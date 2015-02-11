/*
 * Copyright 2015 Randy McEoin
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
package net.mceoin.cominghome.wizard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.R;

/**
 * Upon first run of the application, present the user with a series of screens that help
 * describe how to get started using the app.
 */
public class InitialWizardActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.initial_wizard);
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean initialWizardRan = prefs.getBoolean(MainActivity.PREFS_INITIAL_WIZARD, false);
        if (initialWizardRan) {
            finish();
        }
    }
}