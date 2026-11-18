package app.geeflow.presentation.feature.user.settings.appearance.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.geeflow.domain.ClockThemeSchedule
import app.geeflow.domain.ClockThemeStage
import app.geeflow.domain.MinutesPerDay
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.theme.CoffeeAmbientPreview
import app.geeflow.ui.theme.coffeeClockAppearanceAt
import app.geeflow.ui.theme.coffeeColorScheme
import geeflow.shared.core.ui.generated.resources.Res as CoreRes
import geeflow.shared.core.ui.generated.resources.common_cancel
import geeflow.shared.core.ui.generated.resources.common_confirm
import geeflow.shared.feature.user.settings.generated.resources.Res
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_blackout
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_dawn
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_evening
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_midday
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_morning
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_preview
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_reset
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_schedule
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_sunset
import geeflow.shared.feature.user.settings.generated.resources.user_settings_clock_time_order
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ClockThemeScheduleDialog(
    schedule: ClockThemeSchedule,
    onApply: (ClockThemeSchedule) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(schedule) { mutableStateOf(schedule) }
    var previewMinute by remember(schedule) { mutableIntStateOf(schedule[ClockThemeStage.Morning]) }
    var editingStage by remember { mutableStateOf<ClockThemeStage?>(null) }
    GeeFlowDialog(onDismissRequest = onDismiss) {
        AnimatedContent(editingStage, label = "clock-theme-edit") { stage ->
            if (stage == null) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp).testTag("clock-theme-dialog"),
                ) {
                    GeeFlowDialogTopBar(
                        title = stringResource(Res.string.user_settings_clock_schedule),
                        onCloseClick = onDismiss,
                    )
                    val appearance = remember(previewMinute, draft) { coffeeClockAppearanceAt(previewMinute.toFloat(), draft) }
                    val ink = coffeeColorScheme(appearance.phase).onBackground
                    CoffeeAmbientPreview(
                        appearance.ambient,
                        Modifier.fillMaxWidth().padding(top = 12.dp).height(96.dp).clip(MaterialTheme.shapes.large),
                    ) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(stringResource(Res.string.user_settings_clock_preview), color = ink)
                            Text(previewMinute.asClockTime(), style = MaterialTheme.typography.headlineMedium, color = ink)
                        }
                    }
                    Slider(
                        value = previewMinute.toFloat(),
                        onValueChange = { previewMinute = it.roundToInt() },
                        valueRange = 0f..(MinutesPerDay - 1).toFloat(),
                        modifier = Modifier.fillMaxWidth().testTag("clock-preview-time"),
                    )
                    for (point in ClockThemeStage.entries) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(point.title(), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { editingStage = point }) {
                                Text(draft[point].asClockTime())
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = {
                            draft = ClockThemeSchedule.Default
                            previewMinute = draft[ClockThemeStage.Morning]
                        }) { Text(stringResource(Res.string.user_settings_clock_reset)) }
                        Button(onClick = { onApply(draft) }) { Text(stringResource(CoreRes.string.common_confirm)) }
                    }
                }
            } else {
                ClockThemeTimeEditor(
                    stage = stage,
                    schedule = draft,
                    onCancel = { editingStage = null },
                    onConfirm = { next ->
                        draft = next
                        previewMinute = next[stage]
                        editingStage = null
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockThemeTimeEditor(
    stage: ClockThemeStage,
    schedule: ClockThemeSchedule,
    onCancel: () -> Unit,
    onConfirm: (ClockThemeSchedule) -> Unit,
) {
    val current = schedule[stage]
    val time = rememberTimePickerState(initialHour = current / 60, initialMinute = current % 60, is24Hour = true)
    val next = schedule.withTime(stage, time.hour * 60 + time.minute)
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stage.title(), style = MaterialTheme.typography.titleLarge)
        TimeInput(time)
        if (next == null) {
            Text(
                stringResource(Res.string.user_settings_clock_time_order),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text(stringResource(CoreRes.string.common_cancel)) }
            Button(
                enabled = next != null,
                onClick = { next?.let(onConfirm) },
            ) { Text(stringResource(CoreRes.string.common_confirm)) }
        }
    }
}

@Composable
private fun ClockThemeStage.title() = stringResource(when (this) {
    ClockThemeStage.Dawn -> Res.string.user_settings_clock_dawn
    ClockThemeStage.Morning -> Res.string.user_settings_clock_morning
    ClockThemeStage.Midday -> Res.string.user_settings_clock_midday
    ClockThemeStage.Sunset -> Res.string.user_settings_clock_sunset
    ClockThemeStage.Evening -> Res.string.user_settings_clock_evening
    ClockThemeStage.Blackout -> Res.string.user_settings_clock_blackout
})

private fun Int.asClockTime() = (this / 60).toString().padStart(2, '0') + ":" + (this % 60).toString().padStart(2, '0')
