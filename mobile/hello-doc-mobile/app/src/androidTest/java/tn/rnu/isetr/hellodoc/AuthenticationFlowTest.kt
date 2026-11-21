package tn.rnu.isetr.hellodoc

import android.view.View
import android.graphics.Rect
import tn.rnu.isetr.hellodoc.ui.home.HomeActivity
import tn.rnu.isetr.hellodoc.ui.auth.LoginActivity
import tn.rnu.isetr.hellodoc.ui.auth.RegisterActivity
import tn.rnu.isetr.hellodoc.network.RetrofitClient
import org.junit.Assert.*
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.Before
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthenticationFlowTest {
    @Before fun dismissEmulatorNotificationShade() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        if (android.os.Build.VERSION.SDK_INT >= 37) instrumentation.uiAutomation.grantRuntimePermission("tn.rnu.isetr.hellodoc", "android.permission.ACCESS_LOCAL_NETWORK")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("cmd statusbar collapse")).use { it.readBytes() }
        RetrofitClient.cookies.clear()
        instrumentation.waitForIdleSync()
    }
    private fun capture(name: String) {
        val i = InstrumentationRegistry.getInstrumentation()
        i.waitForIdleSync()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/hellodoc-$name.png")).use { it.readBytes() }
    }
    private fun scrollToStable() = object : ViewAction {
        override fun getConstraints(): Matcher<View> = org.hamcrest.Matchers.any(View::class.java)
        override fun getDescription() = "Scroll after keyboard resize has settled"
        override fun perform(controller: UiController, view: View) {
            repeat(20) {
                view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
                controller.loopMainThreadForAtLeast(200)
                if (isDisplayingAtLeast(90).matches(view)) return
            }
            throw AssertionError("The requested form control could not be scrolled into view")
        }
    }
    private fun waitFor(id: Int, timeout: Long = 15000) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()
        override fun getDescription() = "Wait for visible enabled view $id"
        override fun perform(controller: UiController, view: View) {
            val deadline = android.os.SystemClock.uptimeMillis() + timeout
            do {
                val ready = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).any {
                    val target = it.findViewById<View>(id)
                    target != null && target.isShown && target.isEnabled
                }
                if (ready) return
                controller.loopMainThreadForAtLeast(100)
            } while (android.os.SystemClock.uptimeMillis() < deadline)
            throw AssertionError("View $id was not ready within $timeout ms")
        }
    }
    @Test fun invalidEmailIsRejectedBeforeLogin() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(isRoot()).perform(waitFor(R.id.btnLogin))
            onView(withId(R.id.etEmail)).perform(replaceText("invalid-email"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(replaceText("password-123"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(scrollTo(), click())
            onView(withText("Saisissez une adresse e-mail valide.")).check(matches(isDisplayed()))
        }
    }
    @Test fun phoneRejectsPastedLettersAndDoctorHasProofField() {
        ActivityScenario.launch(RegisterActivity::class.java).use {
            onView(isRoot()).perform(waitFor(R.id.etPhone))
            capture("register-patient")
            onView(withId(R.id.etPhone)).perform(scrollToStable(), replaceText("22ABC456"), closeSoftKeyboard())
            onView(withId(R.id.etPhone)).check(matches(withText("22456")))
            onView(withId(R.id.rbDoctor)).perform(scrollTo(), click())
            onView(withId(R.id.btnDocument)).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.etLicenseNumber)).perform(scrollTo()).check(matches(isDisplayed()))
            capture("register-doctor")
        }
    }
    /** Run with the disposable Spring fixture on port 8081. */
    @Test fun patientCanRegisterLoginAndLogoutAgainstBackend() {
        assumeTrue("Requires the disposable QA fixture", InstrumentationRegistry.getArguments().getString("hellodocQa") == "true")
        val email = "android-ui-${UUID.randomUUID().toString().take(8)}@qa.example"
        val password = "OnlyForHelloDocQA2026"
        ActivityScenario.launch(RegisterActivity::class.java).use {
            onView(isRoot()).perform(waitFor(R.id.etFirstName))
            val fields = listOf(R.id.etFirstName to "Android", R.id.etLastName to "Validation", R.id.etEmail to email,
                R.id.etPhone to "22123456", R.id.etAddress to "Adresse QA", R.id.etPassword to password, R.id.etConfirmPassword to password)
            fields.forEach { (id, value) -> onView(withId(id)).perform(scrollToStable(), replaceText(value), closeSoftKeyboard()) }
            onView(withId(R.id.cbConsent)).perform(scrollTo(), click())
            onView(withId(R.id.btnRegister)).perform(scrollToStable(), object : ViewAction {
                override fun getConstraints(): Matcher<View> = isEnabled()
                override fun getDescription() = "Double tap must not submit twice"
                override fun perform(controller: UiController, view: View) {
                    view.performClick()
                    assertFalse(view.isEnabled)
                    view.performClick()
                }
            })
            onView(isRoot()).perform(waitFor(R.id.btnLogin))
            onView(withId(R.id.tvNotice)).check(matches(withText("Compte patient créé. Vous pouvez maintenant vous connecter.")))
            onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText(email), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(scrollTo(), replaceText(password), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(scrollTo(), click())
            onView(isRoot()).perform(waitFor(R.id.btnLogout))
            onView(withId(R.id.tvAccountRole)).check(matches(withText("Espace patient")))
            onView(withId(R.id.tvAccountEmail)).check(matches(withText(email)))
            capture("account-patient")
            onView(withId(R.id.btnLogout)).perform(scrollTo(), click())
            onView(isRoot()).perform(waitFor(R.id.btnWelcomeLogin))
            onView(withId(R.id.btnWelcomeLogin)).check(matches(isDisplayed()))
        }
    }
    @Test fun homeOpensLoginActivity() {
        ActivityScenario.launch(HomeActivity::class.java).use {
            capture("home")
            onView(withId(R.id.btnWelcomeLogin)).perform(scrollToStable(), click())
            onView(withId(R.id.etEmail)).check(matches(isDisplayed()))
            capture("login")
        }
    }
    @Test fun registrationDoctorRoleSurvivesRecreation() {
        ActivityScenario.launch(RegisterActivity::class.java).use { scenario ->
            onView(withId(R.id.rbDoctor)).perform(scrollToStable(), click())
            scenario.recreate()
            onView(withId(R.id.rbDoctor)).check(matches(isChecked()))
            onView(withId(R.id.btnDocument)).perform(scrollToStable()).check(matches(isDisplayed()))
        }
    }
    @Test fun doctorCanLoginAndLogoutAgainstBackend() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("hellodocQa") == "true")
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etEmail)).perform(replaceText("doctor@qa.example"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(replaceText("OnlyForHelloDocQA2026"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(scrollToStable(), click())
            onView(isRoot()).perform(waitFor(R.id.btnLogout))
            onView(withId(R.id.tvAccountRole)).check(matches(withText("Espace médecin")))
            capture("account-doctor")
            onView(withId(R.id.btnLogout)).perform(scrollToStable(), click())
            onView(isRoot()).perform(waitFor(R.id.btnWelcomeLogin))
        }
    }
    @Test fun wrongPasswordPreservesFormAndAllowsRetry() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("hellodocQa") == "true")
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.etEmail)).perform(replaceText("patient@qa.example"), closeSoftKeyboard())
            onView(withId(R.id.etPassword)).perform(replaceText("incorrect-password"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(scrollToStable(), click())
            onView(isRoot()).perform(waitFor(R.id.tvError))
            onView(withId(R.id.etEmail)).check(matches(withText("patient@qa.example")))
            onView(withId(R.id.btnLogin)).check(matches(isEnabled()))
            onView(withId(R.id.etPassword)).perform(scrollToStable(), replaceText("OnlyForHelloDocQA2026"), closeSoftKeyboard())
            onView(withId(R.id.btnLogin)).perform(scrollToStable(), click())
            onView(isRoot()).perform(waitFor(R.id.btnLogout))
            onView(withId(R.id.btnLogout)).perform(scrollToStable(), click())
            onView(isRoot()).perform(waitFor(R.id.btnWelcomeLogin))
        }
    }

}
