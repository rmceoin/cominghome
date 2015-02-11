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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import net.mceoin.cominghome.AppController;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.wizard.steps.InitialStep1;
import net.mceoin.cominghome.wizard.steps.InitialStep2;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.layouts.BasicWizardLayout;

/**
 * Used to show a series of screens upon first run of the application.
 */
public class InitialWizard extends BasicWizardLayout {

    /**
     * Note that we inherit from {@link android.support.v4.app.Fragment} and therefore must have an empty constructor
     */
    public InitialWizard() {
        super();
    }

    /*
        You must override this method and create a wizard flow by
        using WizardFlow.Builder as shown in this example
     */
    @Override
    public WizardFlow onSetup() {
        return new WizardFlow.Builder()
                .addStep(InitialStep1.class)           //Add your steps in the order you want them
                .addStep(InitialStep2.class)           //to appear and eventually call create()
                .create();                              //to create the wizard flow.
    }

    /*
        You'd normally override onWizardComplete to access the wizard context and/or close the wizard
     */
    @Override
    public void onWizardComplete() {
        super.onWizardComplete();   //Make sure to first call the super method before anything else

        Context context = AppController.getInstance().getApplicationContext();
        SharedPreferences prefs;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(MainActivity.PREFS_INITIAL_WIZARD, true);
        editor.apply();

        startActivity(new Intent().setClass(context, MainActivity.class));

        getActivity().finish();     //Terminate the wizard
    }
}