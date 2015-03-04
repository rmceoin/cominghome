package net.mceoin.cominghome;

import android.app.Activity;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

import junit.framework.TestCase;

public class InstallationTest extends ActivityInstrumentationTestCase2<MainActivity> {

    MainActivity mActivity;
    
    public InstallationTest() {
        super(MainActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
    }

    public void testPreconditions() {
        assertNotNull("mActivity is null", mActivity);
    }
    
    public void testId() throws Exception {
        Context context = mActivity.getApplicationContext();
        assertNotNull("context is null", context);
        String id = Installation.id(context);
        assertNotNull("id is null", id);
        assertTrue("id is not greater than 10 characters", id.length() > 10);
    }
}