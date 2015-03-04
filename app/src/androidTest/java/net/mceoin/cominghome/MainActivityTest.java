package net.mceoin.cominghome;

import android.app.Application;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;

import static org.hamcrest.Matchers.not;

import android.test.ActivityInstrumentationTestCase2;
import android.test.ApplicationTestCase;
import android.test.suitebuilder.annotation.LargeTest;

/**
 * Under 'Settings->Developer options' disable the following 3 settings and restart the device:
 * o Window Animation Scale
 * o Transition Animation Scale
 * o Animator Duration Scale
 */
@LargeTest
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    public void testConnectToNest() {
        final String STRING_CONNECT_TO_NEST = "Connect to Nest";
        
        onView(withId(R.id.buttonConnectNest)).check(matches(withText(STRING_CONNECT_TO_NEST)));

        onView(withId(R.id.buttonConnectNest)).perform(click());

        // moves forward to OAuthFlowApp activity
        onView(withId(R.id.btn_use_pincode)).check(matches(withText(("Go"))));
        onView(withId(R.id.btn_use_pincode)).check(matches(not(isEnabled())));
        
        // Going back to the previous activity
        pressBack();

        onView(withId(R.id.buttonConnectNest)).check(matches(withText(STRING_CONNECT_TO_NEST)));

    }
}