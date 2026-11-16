package app.geeflow.presentation.feature.device.dashboard

import androidx.compose.runtime.Composable
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardPreviewContent
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsReviewContent

/** Actual production components with local fixtures, used by simulator/JVM review captures. */
@Composable
fun DeviceDesignReviewContent(scene: String) {
    when (scene) {
        "temperature" -> QuickSettingsReviewContent()
        else -> DeviceDashboardPreviewContent()
    }
}
