package app.geeflow.app.review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import app.geeflow.ui.components.DialogCaptureHost
import app.geeflow.ui.theme.CoffeeDayPhase
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.coffeeColorScheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.Locale
import java.util.concurrent.FutureTask
import javax.swing.SwingUtilities

/** Render production composables and fixture states; no Bluetooth, persistence or cloud services. */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    Locale.setDefault(Locale.KOREAN)
    val directory = File(args.firstOrNull() ?: "docs/review/all-ui").apply { mkdirs() }
    val filter = args.getOrNull(1)?.split(',')?.toSet()
    val selected = reviewScenes.filter { filter == null || it.id in filter }
    for (screen in selected) for (phase in listOf(CoffeeDayPhase.Day, CoffeeDayPhase.Night)) {
        val name = "${screen.id}-${if (phase.dark) "night" else "day"}.png"
        val scene = onUiThread { ImageComposeScene(2388, 1668, Density(2f)) {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                GeeFlowTheme(darkMode = phase.dark, colorScheme = coffeeColorScheme(phase), phase = phase) {
                    DialogCaptureHost { screen.content() }
                }
            }
        } }
        try {
            onUiThread { scene.render() }
            Thread.sleep(600)
            onUiThread { scene.render(0L) }
            Thread.sleep(100)
            onUiThread { scene.render(1_500_000_000L) }
            Thread.sleep(100)
            val data = onUiThread { scene.render(1_600_000_000L).encodeToData(EncodedImageFormat.PNG) }
                ?: error("Could not encode $name")
            File(directory, name).writeBytes(data.bytes)
        } finally {
            onUiThread { scene.close() }
        }
        println("Rendered $name")
    }
    // Titles and identifiers are controlled catalog strings, never user data.
    File(directory, "manifest.json").writeText("""{
        "runtime":"JVM Compose", "nativeGlassCaptured":false, "width":2388, "height":1668,
        "generatedAt":"${java.time.Instant.now()}",
        "screens":[${reviewScenes.joinToString(",") { """{"id":"${it.id}","title":"${it.title}","category":"${it.category}"}""" }}]
    }""")
}

private fun <T> onUiThread(action: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return action()
    val task = FutureTask(action)
    SwingUtilities.invokeAndWait(task)
    return task.get()
}
