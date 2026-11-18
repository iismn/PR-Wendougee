package app.geeflow.presentation.feature.device.dashboard.quickmaintenance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import app.geeflow.ui.components.CoffeeMotionEasing
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_cleaning_time
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_rest_time
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_repeat_count
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_previous
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_next
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import app.geeflow.ui.components.GeeFlowDialog
import app.geeflow.ui.components.DotMatrixNumber
import app.geeflow.ui.components.RulerSlider
import app.geeflow.ui.components.CoffeeWorkspaceTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.window.DialogProperties
import app.geeflow.ui.components.CoffeePopup
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardPreviewContent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.data.device.model.CleaningType
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.dashboard.quickmaintenance.QuickMaintenanceViewModelEvent.ShowSnackbar
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowDialogTopBar
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import geeflow.shared.core.ui.generated.resources.common_sec
import geeflow.shared.core.ui.generated.resources.common_settings
import geeflow.shared.core.ui.generated.resources.common_times
import geeflow.shared.core.ui.generated.resources.maintenance_daily
import geeflow.shared.core.ui.generated.resources.maintenance_daily_description
import geeflow.shared.core.ui.generated.resources.maintenance_deep
import geeflow.shared.core.ui.generated.resources.maintenance_deep_description
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_start
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_stop
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_title
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_adjust_hint
import geeflow.shared.feature.device.dashboard.generated.resources.quick_maintenance_info
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

@Composable
internal fun QuickMaintenanceScreen(
    viewModel: QuickMaintenanceViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    EventsDispatcher(viewModel.events) { event ->
        when (event) {
            is ShowSnackbar -> scope.launch { snackbar.showSnackbar(event.message) }
        }
    }
    NavigatorEffect(navigator, viewModel.navEvent)

    Box(Modifier.padding(24.dp)) { QuickMaintenanceContent(
        viewState = viewState,
        snackbar = snackbar,
        onEvent = viewModel::handleEvent,
    ) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickMaintenanceContent(
    viewState: QuickMaintenanceViewState,
    onEvent: (QuickMaintenanceEvent) -> Unit,
    isExpanded: Boolean = isWidthExpanded(),
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) = CoffeeWorkspaceTheme {
    var infoShown by remember { mutableStateOf(false) }
    if (infoShown) MaintenanceInfo(viewState.selectedType, onClose = { infoShown = false })
    Column(
        modifier = Modifier
            .testTag("quick-maintenance-dialog")
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GeeFlowDialogTopBar(
            title = stringResource(Res.string.quick_maintenance_title),
            onCloseClick = { onEvent(QuickMaintenanceEvent.CloseClicked) },
        )
        VerticalSpacer(16.dp)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            CleaningType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = viewState.selectedType == type,
                    enabled = !viewState.isStarting && !viewState.isCleaning,
                    onClick = { onEvent(QuickMaintenanceEvent.TypeSelected(type)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = CleaningType.entries.size,
                        baseShape = MaterialTheme.shapes.large,
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        activeBorderColor = Color.Transparent,
                        inactiveBorderColor = Color.Transparent,
                    ),
                    border = BorderStroke(0.dp, Color.Transparent),
                ) {
                    Text(
                        text = stringResource(
                            if (type == CleaningType.Daily) {
                                CoreRes.string.maintenance_daily
                            } else {
                                CoreRes.string.maintenance_deep
                            },
                        ),
                    )
                }
            }
        }
        VerticalSpacer(16.dp)
        CleaningProgressContent(viewState, onEvent)
        VerticalSpacer(16.dp)
        Buttons(viewState, isExpanded, onEvent, onInfo = { infoShown = true })
        SnackbarHost(snackbar)
    }
}

@Composable
private fun MaintenanceInfo(type: CleaningType, onClose: () -> Unit) {
    GeeFlowDialog(onDismissRequest = onClose) {
        Column(Modifier.testTag("maintenance-info-dialog").padding(24.dp), verticalArrangement = spacedBy(16.dp)) {
            GeeFlowDialogTopBar(stringResource(Res.string.quick_maintenance_info), onCloseClick = onClose)
            Text(stringResource(if (type == CleaningType.Daily) CoreRes.string.maintenance_daily else CoreRes.string.maintenance_deep),
                style = MaterialTheme.typography.titleMedium)
            Text(stringResource(if (type == CleaningType.Daily) CoreRes.string.maintenance_daily_description else CoreRes.string.maintenance_deep_description))
            Text(stringResource(Res.string.quick_maintenance_adjust_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CleaningProgressContent(viewState: QuickMaintenanceViewState, onEvent: (QuickMaintenanceEvent) -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    var direction by remember { mutableStateOf(1) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { direction = -1; page = (page + 2) % 3 },
            modifier = Modifier.testTag("maintenance-previous")) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(Res.string.quick_maintenance_previous))
        }
        AnimatedContent(page, modifier = Modifier.weight(1f), transitionSpec = {
            val sign = direction
            (slideInHorizontally(tween(300, easing = CoffeeMotionEasing)) { sign * it / 5 } +
                fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(220, easing = CoffeeMotionEasing)) { -sign * it / 5 } + fadeOut(tween(160)))
        }, label = "maintenance-parameter") { selected ->
            val progress = when (selected) { 0 -> viewState.flushProgress; 1 -> viewState.restProgress; else -> viewState.cycleProgress }
            val range = when (selected) { 0 -> viewState.flushRange; 1 -> viewState.restRange; else -> viewState.cycleRange }
            val label = stringResource(when (selected) {
                0 -> Res.string.quick_maintenance_cleaning_time
                1 -> Res.string.quick_maintenance_rest_time
                else -> Res.string.quick_maintenance_repeat_count
            })
            ProgressItem(label = label, current = progress.current, target = progress.target,
                unit = stringResource(if (selected == 2) CoreRes.string.common_times else CoreRes.string.common_sec),
                range = range, editing = !viewState.isStarting && !viewState.isCleaning,
                isCleaning = viewState.isCleaning,
                tag = when (selected) { 0 -> "maintenance-flush"; 1 -> "maintenance-rest"; else -> "maintenance-cycles" },
                onChange = { value -> onEvent(when (selected) {
                    0 -> QuickMaintenanceEvent.FlushChanged(value)
                    1 -> QuickMaintenanceEvent.RestChanged(value)
                    else -> QuickMaintenanceEvent.CyclesChanged(value)
                }) }, modifier = Modifier.fillMaxWidth())
        }
        IconButton(onClick = { direction = 1; page = (page + 1) % 3 },
            modifier = Modifier.testTag("maintenance-next")) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(Res.string.quick_maintenance_next))
        }
    }
}

@Composable
private fun ProgressItem(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    range: IntRange,
    editing: Boolean,
    isCleaning: Boolean,
    tag: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(12.dp)
        DotMatrixNumber(if (isCleaning) current else target, MaterialTheme.colorScheme.onSurface,
            Modifier.height(42.dp).testTag("$tag-value"))
        VerticalSpacer(8.dp)
        Text(
            text = if (isCleaning) "/ $target $unit" else unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(20.dp)
        RulerSlider(
            value = target, range = range, label = label, onChange = onChange, enabled = editing,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

@Composable
private fun Buttons(
    viewState: QuickMaintenanceViewState,
    isExpanded: Boolean,
    onEvent: (QuickMaintenanceEvent) -> Unit,
    onInfo: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = spacedBy(16.dp, alignment = Alignment.End),
) {
    IconButton(onClick = onInfo, modifier = Modifier.testTag("maintenance-info")) {
        Icon(Icons.Outlined.ErrorOutline, stringResource(Res.string.quick_maintenance_info))
    }
    TextButton(
        enabled = !viewState.isStarting,
        onClick = { onEvent(QuickMaintenanceEvent.MoreSettingsClicked(isExpanded)) },
        content = { Text(stringResource(CoreRes.string.common_settings)) },
    )
    val buttonColor by animateColorAsState(
        if (viewState.isCleaning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primary,
    )
    val textColor by animateColorAsState(
        if (viewState.isCleaning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimary,
    )
    Button(
        onClick = { onEvent(QuickMaintenanceEvent.ToggleCleaningClicked) },
        enabled = !viewState.isStarting && (viewState.isCleaning || viewState.canStart),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = textColor,
        ),
    ) {
        Text(
            stringResource(
                if (viewState.isCleaning) Res.string.quick_maintenance_stop else Res.string.quick_maintenance_start,
            ),
        )
    }
}

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun QuickMaintenanceDesignPreview(cleaning: Boolean = false) {
    var state by remember { mutableStateOf(QuickMaintenanceViewState(
        isCleaning = cleaning, canStart = true,
        flushProgress = QuickMaintenanceViewState.Progress(if (cleaning) 3 else 0, 5),
        restProgress = QuickMaintenanceViewState.Progress(0, 5),
        cycleProgress = QuickMaintenanceViewState.Progress(if (cleaning) 1 else 0, 3),
        flushRange = 1..60, restRange = 0..60, cycleRange = 1..20,
    )) }
    Box(Modifier.fillMaxSize()) {
    DeviceDashboardPreviewContent()
    CoffeePopup(onDismissRequest = {}, properties = DialogProperties(usePlatformDefaultWidth = false), modifier = Modifier.padding(24.dp)) {
    QuickMaintenanceContent(
        viewState = state,
        onEvent = { event -> state = when (event) {
            is QuickMaintenanceEvent.FlushChanged -> state.copy(flushProgress = state.flushProgress.copy(target = event.seconds))
            is QuickMaintenanceEvent.RestChanged -> state.copy(restProgress = state.restProgress.copy(target = event.seconds))
            is QuickMaintenanceEvent.CyclesChanged -> state.copy(cycleProgress = state.cycleProgress.copy(target = event.count))
            is QuickMaintenanceEvent.TypeSelected -> state.copy(selectedType = event.type)
            else -> state
        } },
    )
    }
    }
}
