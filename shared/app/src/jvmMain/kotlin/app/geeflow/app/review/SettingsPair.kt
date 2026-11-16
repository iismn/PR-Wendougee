package app.geeflow.app.review

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.listPane
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy.Companion.detailPane
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import app.geeflow.presentation.feature.device.settings.main.DeviceSettingsDesignPreview
import app.geeflow.presentation.feature.user.settings.main.UserSettingsDesignPreview

private enum class ReviewPane : NavKey { List, Detail }

/** Uses the same adaptive scene strategy as RootNavigation, without view models or device services. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun SettingsPair(machine: Boolean, selected: Int, detail: @Composable () -> Unit) {
    NavDisplay(
        backStack = listOf(ReviewPane.List, ReviewPane.Detail),
        onBack = {},
        sceneStrategies = listOf(rememberListDetailSceneStrategy<ReviewPane>()),
        entryProvider = { key ->
            NavEntry(key, metadata = if (key == ReviewPane.List) listPane() else detailPane()) {
                if (key == ReviewPane.Detail) detail()
                else if (machine) DeviceSettingsDesignPreview(selected)
                else UserSettingsDesignPreview(selected)
            }
        },
    )
}
