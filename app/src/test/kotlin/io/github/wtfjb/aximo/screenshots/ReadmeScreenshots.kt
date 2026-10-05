package io.github.wtfjb.aximo.screenshots

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import io.github.wtfjb.aximo.MainActivity
import io.github.wtfjb.aximo.domain.devdata.DevDataRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real app with the sample data and saves PNGs for the README.
 * Runs only with `-Pscreenshots` (see app/build.gradle.kts); a normal `test` skips it.
 * Screen size roughly matches a Pixel 9 Pro, language German.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "de-w412dp-h915dp-xxhdpi", application = ScreenshotApplication::class)
class ReadmeScreenshots {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val outputDir: String? = System.getProperty("aximo.screenshots.dir")

    @Before
    fun loadSampleData() {
        assumeTrue("Run with -Pscreenshots", !outputDir.isNullOrBlank())
        runBlocking { GlobalContext.get().get<DevDataRepository>().loadTestData() }
    }

    @After
    fun tearDown() {
        // Robolectric creates a new Application per test; Koin is global.
        stopKoin()
    }

    @Test
    fun lightTheme() {
        // The hero card appears once the sample data and the next routine are loaded.
        waitForText("NÄCHSTES WORKOUT")
        save("today")

        click("Pläne")
        waitForText("Freies Training")
        save("plans")

        click("Statistik")
        waitForText("Statistik")
        compose.waitForIdle()
        save("stats")

        click("Heute")
        click("Workout starten")
        waitForText("Beenden")
        save("workout")
    }

    @Test
    @Config(qualifiers = "+night")
    fun darkTheme() {
        waitForText("NÄCHSTES WORKOUT")
        save("today-dark")

        click("Workout starten")
        waitForText("Beenden")
        save("workout-dark")
    }

    private fun click(text: String) {
        waitForText(text)
        compose.onAllNodesWithText(text).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 30_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun save(name: String) {
        // Let charts and lists settle after the data arrived.
        Thread.sleep(500)
        compose.waitForIdle()
        val full = compose.onRoot().captureToImage().asAndroidBitmap()
        // Half size is sharp enough for the README and keeps the repo small.
        val bitmap = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
        val file = File(outputDir, "$name.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
