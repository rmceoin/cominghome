package net.mceoin.cominghome.wizard.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.codepond.wizardroid.WizardStep;
import net.mceoin.cominghome.R;

public class InitialStep2 extends WizardStep {

    //You must have an empty constructor for every step
    public InitialStep2() {
    }

    //Set your layout here
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.initial_step, container, false);
        TextView tv = (TextView) v.findViewById(R.id.textView);
        tv.setText(getString(R.string.step2_description));

        return v;
    }
}
