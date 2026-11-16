package app.geeflow.presentation.feature.device.dashboard

import androidx.compose.runtime.Composable
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardPreviewContent
import app.geeflow.presentation.feature.device.dashboard.quicksettings.QuickSettingsReviewContent

/** Actual production components with local fixtures, used by simulator/JVM review captures. */
@Composable
fun DeviceDesignReviewContent(scene: String) {
    when (scene) {
        "temperature" -> QuickSettingsReviewContent()
        "temperature-applying" -> QuickSettingsReviewContent(applying = true)
        "profile-editor" -> app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorPreviewContent()
        "step-editor" -> app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorPreviewContent()
        "charts" -> app.geeflow.presentation.feature.device.dashboard.main.DashboardChartsDesignPreview()
        "history" -> app.geeflow.presentation.feature.device.dashboard.main.DashboardChartsDesignPreview(history = true)
        "profile-details", "finish-target", "single-dose" -> app.geeflow.presentation.feature.device.dashboard.profileeditor.ProfileEditorDialogDesignPreview(scene)
        "step-rename", "step-value" -> app.geeflow.presentation.feature.device.dashboard.profileeditor.StepEditorPreviewContent(scene)
        else -> DeviceDashboardPreviewContent()
    }
}
