package net.mceoin.cominghome;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
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

import android.support.test.runner.AndroidJUnit4;
import android.support.test.runner.AndroidJUnitRunner;
import android.test.ActivityInstrumentationTestCase2;

import junit.framework.TestCase;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HistoryActivityTest extends ActivityInstrumentationTestCase2<HistoryActivity> {

    HistoryActivity mActivity;

    public HistoryActivityTest() {
        super(HistoryActivity.class);
    }
    
    public void setUp() throws Exception {
        super.setUp();

        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        mActivity = getActivity();

    }
    
    public void testHistory() {
/*
        onView(ViewMatchers.withId(R.id.list))
                .check(matches(withText("Lost")));
*/

    }

/*    public static Matcher<Object> withItemContent(String expectedText) {
        checkNotNull(expectedText);
        return withItemContent(equalTo(expectedText));
    }

    @SuppressWarnings("rawtypes")
    public static Matcher<Object> withItemContent(final Matcher<String> itemTextMatcher) {
        checkNotNull(itemTextMatcher);
        return new BoundedMatcher<Object, IconRow>(IconRow.class) {
            @Override
            public boolean matchesSafely(IconRow iconRow) {
                return itemTextMatcher.matches(iconRow.getText());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with item content: ");
                itemTextMatcher.describeTo(description);
            }
        };
    }*/
}