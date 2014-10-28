package net.mceoin.cominghome.wizard;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.layouts.BasicWizardLayout;

import net.mceoin.cominghome.AppController;
import net.mceoin.cominghome.MainActivity;
import net.mceoin.cominghome.wizard.steps.InitialStep1;
import net.mceoin.cominghome.wizard.steps.InitialStep2;

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
        editor.commit();

        startActivity(new Intent().setClass(context, MainActivity.class));

        getActivity().finish();     //Terminate the wizard
    }
}