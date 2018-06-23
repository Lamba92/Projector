package it.unito.lamba.tests

import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.contrib.DrawerMatchers.isClosed
import android.support.test.espresso.contrib.NavigationViewActions
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingRegistry
import android.support.test.espresso.IdlingResource
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.Gravity
import it.unito.lamba.projector.fragments.MAIN_RV_ID
import it.unito.lamba.projector.MainActivity
import it.unito.lamba.projector.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class FragmentLaunchingTest{

    @Rule
    @JvmField
    val mActivityRule = ActivityTestRule(MainActivity::class.java)

    private var mIdlingResource: IdlingResource? = null

    // Registers any resource that needs to be synchronized with Espresso before the test is run.
    @Before
    fun registerIdlingResource() {
        mIdlingResource = mActivityRule.activity.getUserIdlingResource()
        IdlingRegistry.getInstance().register(mIdlingResource)
    }

    @Test
    fun testDrawer(){
        HashMap<Int, Int>().apply {
            this[R.id.nav_profile] = R.id.profile_cover
            this[R.id.nav_projects] = MAIN_RV_ID
            this[R.id.nav_my_project] = if(mActivityRule.activity.userProjectID == null) R.id.project_title else R.id.project_title_textview
            this[R.id.nav_about] = R.id.textView14
        }.forEach { key, value ->
            onView(withId(R.id.drawer_layout))
                    .check(matches(isClosed(Gravity.LEFT))) // Left Drawer should be closed.
                    .perform(DrawerActions.open()) // Open Drawer
            Thread.sleep(1000)
            // Start the screen of your activity.
            onView(withId(R.id.nav_view))
                    .perform(NavigationViewActions.navigateTo(key))
            Thread.sleep(1000)
            onView(withId(value)).check(matches(isDisplayed()))
        }
    }

    @After
    fun releaseResources(){
        IdlingRegistry.getInstance().unregister(mIdlingResource)
    }
}