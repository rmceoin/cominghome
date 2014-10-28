package net.mceoin.cominghome.wizard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;

import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.R;

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