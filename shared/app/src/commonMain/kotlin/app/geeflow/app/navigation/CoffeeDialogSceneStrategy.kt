package app.geeflow.app.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.*
import app.geeflow.ui.components.*

/** Dialog overlays participate in the same entrance, exit and background blur. */
internal class CoffeeDialogSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        val properties = entry.metadata[DialogSceneStrategy.Companion.DialogKey] ?: return null
        return CoffeeDialogScene(entry, entries.dropLast(1), properties, onBack)
    }
}

private class CoffeeDialogScene<T : Any>(
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    private val properties: DialogProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val key: Any = entry.contentKey
    override val entries = listOf(entry)
    override val overlaidEntries = previousEntries
    private val progress = Animatable(0f)
    override val content: @Composable () -> Unit = {
        val owner = rememberLifecycleOwner()
        LaunchedEffect(Unit) { progress.animateTo(1f, tween(CoffeeEnterMillis, easing = CoffeeMotionEasing)) }
        CoffeePopup(onDismissRequest = onBack, properties = properties, progress = progress) {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                entry.Content()
            }
        }
    }
    override suspend fun onRemove() {
        progress.animateTo(0f, tween(CoffeeExitMillis, easing = CoffeeMotionEasing))
    }
    override fun equals(other: Any?): Boolean = other is CoffeeDialogScene<*> &&
        entry == other.entry && previousEntries == other.previousEntries && properties == other.properties
    override fun hashCode(): Int = 31 * (31 * entry.hashCode() + previousEntries.hashCode()) + properties.hashCode()
}
