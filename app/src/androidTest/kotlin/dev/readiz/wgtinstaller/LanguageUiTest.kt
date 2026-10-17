// SPDX-License-Identifier: GPL-3.0-only
package dev.readiz.wgtinstaller

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.LocaleList
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import dev.readiz.wgtinstaller.core.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.Locale

class LanguageUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    @Before fun setUp() {
        InstallState.finish()
        InstallState::class.java.getDeclaredField("snapshot").apply { isAccessible = true }.set(null, InstallState.Snapshot())
        context.getSharedPreferences("setup", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("install-journal", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit().putString("language", "ko").commit()
        instrumentation.waitForIdleSync()
    }
    @After fun tearDown() { InstallState.finish() }

    @Test fun englishOnlyEvenWithKoreanPhoneAndSavedLanguage() {
        val config = Configuration(context.resources.configuration).apply { setLocales(LocaleList(Locale.KOREAN)) }
        val english = AppLanguage.wrap(context.createConfigurationContext(config))
        assertEquals("en", english.resources.configuration.locales[0].language)
        assertEquals("Install", english.getString(R.string.install))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val views = descendants(activity.window.decorView)
                assertFalse(views.any { it is Spinner || it is EditText || it is RadioButton || it is CheckBox })
                assertTrue(views.filterIsInstance<ImageButton>().single().contentDescription == "About")
                listOf("1. TV select", "2. TV setup", "3. Select app version", "4. Install").forEach { heading ->
                    assertTrue(views.filterIsInstance<TextView>().any { it.text.toString() == heading })
                }
                assertTrue(views.filterIsInstance<TextView>().any { it.text.toString() == "Years refer to the release year of your Tizen TV." })
                val install = button(activity, R.string.install)
                assertFalse(install.isEnabled)
                assertTrue(generateSequence(install.parent) { it.parent }.any { it is ScrollView })
                assertFalse(views.filterIsInstance<TextView>().any { it.text == activity.getString(R.string.manual_ip) })
                seed(activity)
                button(activity, R.string.youtube).performClick()
                requireNotNull(dialog(activity)).listView.performItemClick(null, 0, 0)
                assertTrue(button(activity, R.string.install).isEnabled)
            }
            val monitor = instrumentation.addMonitor(SetupGuideActivity::class.java.name, null, false)
            try {
                scenario.onActivity { button(it, R.string.setup_title).performClick() }
                val guide = requireNotNull(instrumentation.waitForMonitorWithTimeout(monitor, 5000))
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync {
                    descendants(guide.window.decorView).filterIsInstance<ImageButton>().single().performClick()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    assertEquals("192.168.1.42", field(activity, "host"))
                    assertNotNull(field(activity, "selected"))
                    assertTrue(button(activity, R.string.install).isEnabled)
                }
            } finally { instrumentation.removeMonitor(monitor) }
        }
    }

    @Test fun singleAndMultipleTvsAlwaysOfferChoiceAndManualIp() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                invoke(activity, "acceptTelevisions", listOf(Television("192.168.1.20", "Living room", "")), null)
                val chooser = requireNotNull(dialog(activity))
                assertEquals(1, chooser.listView.adapter.count)
                assertEquals("", field(activity, "host"))
                assertTrue(chooser.getButton(AlertDialog.BUTTON_NEUTRAL).isEnabled)
                assertEquals("Enter another TV IP", chooser.getButton(AlertDialog.BUTTON_NEUTRAL).text.toString())
            }
            scenario.recreate(); instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val chooser = requireNotNull(dialog(activity))
                assertEquals(1, chooser.listView.adapter.count)
                chooser.getButton(AlertDialog.BUTTON_NEUTRAL).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val manual = requireNotNull(dialog(activity))
                descendants(manual.window!!.decorView).filterIsInstance<EditText>().single().setText("192.168.1.55")
                manual.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals("192.168.1.55", field(activity, "host"))
                invoke(activity, "acceptTelevisions", listOf(Television("192.168.1.20", "Living room", "")), null)
                requireNotNull(dialog(activity)).listView.performItemClick(null, 0, 0)
                assertEquals("192.168.1.20", field(activity, "host"))
                invoke(activity, "acceptTelevisions", listOf(Television("192.168.1.21", "Bedroom", ""), Television("192.168.1.20", "Living room", "")), null)
                val chooser = requireNotNull(dialog(activity))
                assertEquals(2, chooser.listView.adapter.count)
                assertEquals("Enter another TV IP", chooser.getButton(AlertDialog.BUTTON_NEUTRAL).text.toString())
                chooser.listView.performItemClick(null, 0, 0)
                assertEquals("192.168.1.21", field(activity, "host"))
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("192.168.1.21", field(activity, "host"))
                assertTrue(descendants(activity.window.decorView).filterIsInstance<TextView>().any { it.text.contains("Bedroom") })
            }
        }
    }

    @Test fun missingTvOffersManualIpWithValidationAndDraftRestoration() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                invoke(activity, "acceptTelevisions", emptyList<Television>(), null)
                requireNotNull(dialog(activity)).getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val manual = requireNotNull(dialog(activity))
                val input = descendants(manual.window!!.decorView).filterIsInstance<EditText>().single()
                input.setText("8.8.8.8")
                manual.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertTrue(manual.isShowing); assertNotNull(input.error)
                assertEquals("", field(activity, "host"))
                input.setText("192.168.1.42")
            }
            scenario.recreate(); instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val manual = requireNotNull(dialog(activity))
                assertEquals("192.168.1.42", descendants(manual.window!!.decorView).filterIsInstance<EditText>().single().text.toString())
                manual.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                assertEquals("192.168.1.42", field(activity, "host"))
            }
        }
    }

    @Test fun versionPopupSelectsPackageAndInstallRequiresFreshConfirmation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                seed(activity)
                button(activity, R.string.youtube).performClick()
                val picker = requireNotNull(dialog(activity))
                assertEquals("Tizen 5.5 · 2020 or newer", picker.listView.adapter.getItem(0))
                assertEquals("Tizen 5.0 · 2019", picker.listView.adapter.getItem(1))
                picker.listView.performItemClick(null, 0, 0)
                assertEquals("Selected: Tizen 5.5 · 2020 or newer · v1.4.0", (field(activity, "selection") as TextView).text.toString())
                assertTrue(button(activity, R.string.install).isEnabled)
                assertFalse(InstallState.snapshot.running)
                button(activity, R.string.install).performClick()
                val confirm = requireNotNull(dialog(activity))
                val message = confirm.findViewById<TextView>(android.R.id.message).text.toString()
                assertTrue(message.contains("SushyDev/tizen-youtube")); assertTrue(message.contains("192.168.1.42"))
                assertTrue(message.contains("tizen-youtube-1.4.0-tizen-5.5.wgt")); assertTrue(message.contains("trust"))
                assertFalse(InstallState.snapshot.running)
            }
            scenario.recreate(); instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val confirm = requireNotNull(dialog(activity))
                assertFalse(InstallState.snapshot.running)
                confirm.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                assertFalse(InstallState.snapshot.running)
                assertTrue(button(activity, R.string.install).isEnabled)
            }
        }
    }

    @Test fun unverifiedPackagesCannotBeSelectedAndCustomShowsFullNames() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                seed(activity, unverifiedFirst = true)
                invoke(activity, "showVersions")
                val first = requireNotNull(dialog(activity))
                assertFalse((first.listView.adapter as ListAdapter).isEnabled(0))
                first.listView.performItemClick(null, 0, 0)
                assertNull(field(activity, "selected")); assertFalse(button(activity, R.string.install).isEnabled)
                first.dismiss()
                setField(activity, "customSource", true)
                invoke(activity, "showVersions")
                val custom = requireNotNull(dialog(activity))
                assertTrue(custom.listView.adapter.getItem(1).toString().contains(".wgt"))
            }
        }
    }

    @Test fun customPopupHasNoRepositoryInputAndOffersLocalPicker() {
        val monitor = instrumentation.addMonitor(
            android.content.IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); addDataType("*/*")
            }, android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null), true)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    seed(activity)
                    setField(activity, "selected", (field(activity, "assets") as List<*>).first())
                    button(activity, R.string.short_apps).performClick()
                    val views = descendants(requireNotNull(dialog(activity)).window!!.decorView)
                    assertFalse(views.any { it is EditText })
                    assertTrue(views.filterIsInstance<Button>().any { it.text == "Find app" })
                    assertTrue(views.filterIsInstance<Button>().any { it.text == "Custom" })
                }
                scenario.recreate(); instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val views = descendants(requireNotNull(dialog(activity)).window!!.decorView)
                    assertFalse(views.any { it is EditText })
                    views.filterIsInstance<Button>().single { it.text == "Custom" }.performClick()
                }
                instrumentation.waitForIdleSync()
                assertEquals(1, monitor.hits)
                scenario.onActivity { activity ->
                    assertNull(dialog(activity))
                    assertNotNull(field(activity, "selected"))
                    assertFalse(descendants(activity.window.decorView).any { it is EditText })
                }
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun customRepositoryValidatesInputAndRestoresDraftWithoutClearingSelection() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                seed(activity)
                button(activity, R.string.youtube).performClick()
                requireNotNull(dialog(activity)).listView.performItemClick(null, 0, 0)
                val selected = field(activity, "selected")
                button(activity, R.string.short_apps).performClick()
                descendants(requireNotNull(dialog(activity)).window!!.decorView).filterIsInstance<Button>()
                    .single { it.text == "Find app" }.performClick()
                val repository = requireNotNull(dialog(activity))
                val input = descendants(repository.window!!.decorView).filterIsInstance<EditText>().single()
                assertEquals("", input.text.toString())
                for (invalid in listOf("", "https://example.com/owner/repo", "owner/repo/releases")) {
                    input.setText(invalid)
                    repository.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
                    assertTrue(repository.isShowing); assertNotNull(input.error)
                    assertEquals(false, field(activity, "busy"))
                    assertEquals(selected, field(activity, "selected"))
                }
                input.setText("https://github.com/reisxd/TizenBrew.git")
            }
            scenario.recreate(); instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val repository = requireNotNull(dialog(activity))
                assertEquals("repository", field(activity, "activePopup"))
                assertEquals("https://github.com/reisxd/TizenBrew.git",
                    descendants(repository.window!!.decorView).filterIsInstance<EditText>().single().text.toString())
                repository.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
                assertNotNull(field(activity, "selected"))
                assertTrue(button(activity, R.string.install).isEnabled)
                assertFalse(descendants(activity.window.decorView).any { it is EditText })
                button(activity, R.string.youtube).performClick()
                assertEquals("Tizen 5.5 · 2020 or newer", requireNotNull(dialog(activity)).listView.adapter.getItem(0))
            }
        }
    }

    @Test fun localDocumentReplacesSelectionAndSurvivesRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                seed(activity)
                setField(activity, "selected", (field(activity, "assets") as List<*>).first())
                invoke(activity, "render", InstallState.snapshot)
                invoke(activity, "onActivityResult", 21, android.app.Activity.RESULT_OK,
                    Intent().setData(Uri.parse("content://dev.readiz.wgtinstaller.test.documents/fixture.wgt")))
                assertNull(field(activity, "selected")); assertFalse(button(activity, R.string.install).isEnabled)
            }
            val deadline = System.currentTimeMillis() + 5000
            var loaded = false
            while (!loaded && System.currentTimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                scenario.onActivity { loaded = field(it, "local") != null }
                if (!loaded) Thread.sleep(50)
            }
            assertTrue(loaded)
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("fixture.wgt", (field(activity, "local") as LocalWidget).name)
                assertEquals("Selected: fixture.wgt", (field(activity, "selection") as TextView).text.toString())
                button(activity, R.string.install).performClick()
                val message = requireNotNull(dialog(activity)).findViewById<TextView>(android.R.id.message).text.toString()
                assertTrue(message.contains("WGT from this phone")); assertFalse(message.contains("SushyDev"))
                assertFalse(InstallState.snapshot.running)
            }
        }
    }

    @Test fun runningInstallDisablesAllSelectionActions() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                seed(activity)
                setField(activity, "selected", (field(activity, "assets") as List<*>).first())
                assertTrue(InstallState.begin("192.168.1.42"))
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                for (name in listOf("scan", "youtube", "custom", "install")) assertFalse((field(activity, name) as Button).isEnabled)
            }
        }
    }

    @Test fun escapedTvNamesRenderInSavedSummaryAndChooser() {
        assertEquals("85\" QLED", TvNames.display("85&quot; QLED"))
        assertEquals("85\" QLED", TvNames.display("85&#34; QLED"))
        assertEquals("85\" QLED", TvNames.display("85&#x22; QLED"))
        assertEquals("Room <TV> & John's", TvNames.display("Room <TV> &amp; John&apos;s"))
        context.getSharedPreferences("setup", Context.MODE_PRIVATE).edit()
            .putString("ip", "192.168.1.42").putString("tvName", "85&quot; QLED").commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Selected: 85\" QLED · 192.168.1.42", (field(activity, "tvSummary") as TextView).text.toString())
                invoke(activity, "acceptTelevisions", listOf(Television("192.168.1.42", "85&quot; QLED", "")), null)
                val chooser = requireNotNull(dialog(activity))
                val row = chooser.listView.adapter.getView(0, null, chooser.listView)
                assertEquals("85\" QLED, 192.168.1.42", row.contentDescription.toString())
                chooser.listView.performItemClick(null, 0, 0)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("Selected: 85\" QLED · 192.168.1.42", (field(activity, "tvSummary") as TextView).text.toString())
            }
        }
    }

    @Test fun progressAndCancellationStayWithTheirStep() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val body = (field(activity, "scan") as Button).parent as LinearLayout
                val tv = field(activity, "tvActivity") as LinearLayout
                val version = field(activity, "versionActivity") as LinearLayout
                val install = field(activity, "activitySection") as LinearLayout
                assertEquals(body.indexOfChild(field(activity, "scan") as View) + 1, body.indexOfChild(tv))
                assertEquals(body.indexOfChild(field(activity, "youtube") as View) + 1, body.indexOfChild(version))
                assertEquals(body.indexOfChild(field(activity, "install") as View) + 1, body.indexOfChild(install))
                for (searching in listOf(true, false)) {
                    setField(activity, "token", Cancellation())
                    setField(activity, "busy", true); setField(activity, "searching", searching)
                    invoke(activity, "render", InstallState.snapshot)
                    assertEquals(if (searching) View.VISIBLE else View.GONE, tv.visibility)
                    assertEquals(if (searching) View.GONE else View.VISIBLE, version.visibility)
                    assertEquals(View.GONE, install.visibility)
                    val active = if (searching) tv else version
                    assertTrue(descendants(active).filterIsInstance<ProgressBar>().single().isIndeterminate)
                    descendants(active).filterIsInstance<Button>().single().performClick()
                    assertTrue(runCatching { (field(activity, "token") as Cancellation).check() }.isFailure)
                }
                setField(activity, "importing", true)
                invoke(activity, "render", InstallState.snapshot)
                assertEquals("Reading WGT…", (field(activity, "youtube") as Button).text.toString())
                setField(activity, "busy", false); setField(activity, "importing", false)
                invoke(activity, "render", InstallState.snapshot)
                assertEquals(View.GONE, tv.visibility); assertEquals(View.GONE, version.visibility)
                assertTrue(InstallState.begin("192.168.1.42"))
                InstallState.publish(Update(Phase.DOWNLOADING, "", 45))
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertEquals(View.GONE, (field(activity, "tvActivity") as View).visibility)
                assertEquals(View.GONE, (field(activity, "versionActivity") as View).visibility)
                assertEquals(View.VISIBLE, (field(activity, "activitySection") as View).visibility)
                assertEquals(45, (field(activity, "progress") as ProgressBar).progress)
                assertEquals("Downloading and checking the app", (field(activity, "status") as TextView).text.toString())
            }
        }
    }

    @Test fun capturePlainScreensForReview() {
        if (InstrumentationRegistry.getArguments().getString("screenshots") != "true") return
        fun capture(name: String) {
            instrumentation.waitForIdleSync(); Thread.sleep(500)
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            java.io.File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }; bitmap.recycle()
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            capture("plain-home")
            scenario.onActivity { activity ->
                invoke(activity, "acceptTelevisions", listOf(Television("192.168.1.20", "Living room", "")), null)
            }; capture("plain-single-tv")
            scenario.onActivity { activity ->
                invoke(activity, "acceptTelevisions", listOf(Television("192.168.1.20", "Living room", ""), Television("192.168.1.21", "Bedroom", "")), null)
            }; capture("plain-tv-picker")
            scenario.onActivity { requireNotNull(dialog(it)).getButton(AlertDialog.BUTTON_NEUTRAL).performClick() }
            capture("plain-manual-ip")
            scenario.onActivity { activity ->
                requireNotNull(dialog(activity)).dismiss(); seed(activity); button(activity, R.string.youtube).performClick()
            }; capture("plain-versions")
            scenario.onActivity { requireNotNull(dialog(it)).listView.performItemClick(null, 0, 0) }
            capture("plain-ready")
            scenario.onActivity { button(it, R.string.install).performClick() }; capture("plain-confirm")
            scenario.onActivity { activity ->
                requireNotNull(dialog(activity)).dismiss(); button(activity, R.string.short_apps).performClick()
            }; capture("plain-custom")
            scenario.onActivity { activity ->
                descendants(requireNotNull(dialog(activity)).window!!.decorView).filterIsInstance<Button>()
                    .single { it.text == "Find app" }.performClick()
            }; capture("plain-repository")
            scenario.onActivity { activity ->
                invoke(activity, "showAbout")
            }; capture("plain-about")
            scenario.onActivity { activity ->
                invoke(activity, "acceptTelevisions", emptyList<Television>(), null)
            }; capture("plain-no-tv")
            scenario.onActivity { activity ->
                requireNotNull(dialog(activity)).dismiss()
                invoke(activity, "selectTelevision", Television("192.168.1.42", "85&quot; QLED", ""))
            }; capture("plain-tv-name")
            scenario.onActivity { activity ->
                setField(activity, "busy", true); setField(activity, "searching", true)
                invoke(activity, "render", InstallState.snapshot)
            }; capture("plain-tv-search")
            scenario.onActivity { activity ->
                setField(activity, "searching", false); invoke(activity, "render", InstallState.snapshot)
            }; capture("plain-version-search")
            scenario.onActivity { activity ->
                setField(activity, "busy", false)
                InstallState.begin("192.168.1.42"); InstallState.publish(Update(Phase.DOWNLOADING, "", 45))
            }; capture("plain-install-progress")
        }
    }

    private fun seed(activity: MainActivity, unverifiedFirst: Boolean = false) {
        val source = RepoRef("SushyDev", "tizen-youtube")
        val assets = listOf("5.5", "5.0").mapIndexed { index, version ->
            val name = "tizen-youtube-1.4.0-tizen-$version.wgt"
            ReleaseAsset(source, 1, "v1.4.0", index + 1L, name, 1024,
                "https://github.com/${source.fullName}/releases/download/v1.4.0/$name", if (unverifiedFirst && index == 0) null else "a".repeat(64))
        }
        setField(activity, "assets", assets); setField(activity, "customSource", false)
        invoke(activity, "selectTelevision", Television("192.168.1.42", "Living room", ""))
        invoke(activity, "render", InstallState.snapshot)
    }
    private fun dialog(activity: MainActivity) = field(activity, "pickerDialog") as? AlertDialog
    private fun field(activity: MainActivity, name: String) = MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(activity)
    private fun setField(activity: MainActivity, name: String, value: Any?) = MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.set(activity, value)
    private fun invoke(activity: MainActivity, name: String, vararg args: Any?) = MainActivity::class.java.declaredMethods
        .single { it.name == name && it.parameterCount == args.size }.apply { isAccessible = true }.invoke(activity, *args)
    private fun button(activity: MainActivity, label: Int) = descendants(activity.window.decorView).filterIsInstance<Button>().single { it.text == activity.getString(label) }
    private fun descendants(view: View): List<View> = listOf(view) + if (view is ViewGroup) (0 until view.childCount).flatMap { descendants(view.getChildAt(it)) } else emptyList()
}
