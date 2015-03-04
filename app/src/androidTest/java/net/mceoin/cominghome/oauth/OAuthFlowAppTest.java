package net.mceoin.cominghome.oauth;

import android.test.ActivityInstrumentationTestCase2;

import net.mceoin.cominghome.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.not;

public class OAuthFlowAppTest extends ActivityInstrumentationTestCase2<OAuthFlowApp> {

    OAuthFlowApp mActivity;

    public OAuthFlowAppTest() {
        super(OAuthFlowApp.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    public void testOAuth_ConnectToNest() {

        onView(withId(R.id.btn_use_pincode)).check(matches(withText(("Go"))));
        onView(withId(R.id.btn_use_pincode)).check(matches(not(isEnabled())));

        final String STRING_TO_BE_TYPED = "Espresso";
        
        onView(withId(R.id.editPincode))
                .perform(typeText(STRING_TO_BE_TYPED), closeSoftKeyboard());

        onView(withId(R.id.btn_use_pincode)).check(matches(isEnabled()));

    }
}