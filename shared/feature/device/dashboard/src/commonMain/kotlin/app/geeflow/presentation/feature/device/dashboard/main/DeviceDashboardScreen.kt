package app.geeflow.presentation.feature.device.dashboard.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.platform.ForegroundEffect
import app.geeflow.presentation.feature.device.dashboard.components.BrewCharts
import app.geeflow.presentation.feature.device.dashboard.components.BrewTime
import app.geeflow.presentation.feature.device.dashboard.components.BrewStatistics
import app.geeflow.presentation.feature.device.dashboard.components.DashboardBrewButton
import app.geeflow.presentation.feature.device.dashboard.components.TopBar
import app.geeflow.presentation.feature.device.dashboard.maintenance.MaintenanceReminderHost
import app.geeflow.presentation.feature.device.dashboard.model.chartModel
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileList
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListEvent
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModel
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.SelectProfile
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowHistoryBrew
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.dashboard.profiles.ProfileListViewState
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.GeeFlowInsets
import app.geeflow.ui.isWidthExpanded
import app.geeflow.ui.modifier.geeFlowInsetsPadding
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.disabled
import geeflow.shared.core.ui.generated.resources.Res
import geeflow.shared.core.ui.generated.resources.error_generic
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.feature.device.dashboard.generated.resources.dashboard_tab_details
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_show_profiles
import geeflow.shared.feature.device.dashboard.generated.resources.Res as DashboardRes

@Composable
internal fun DeviceDashboardScreen(
    viewModel: DeviceDashboardViewModel,
    profileListViewModel: ProfileListViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    val profileListViewState by profileListViewModel.viewState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { CompactDashboardPage.entries.size }

    ForegroundEffect(viewModel) {
        viewModel.handleEvent(DeviceDashboardEvent.Resumed)
    }

    DeviceDashboardContent(
        viewState = viewState,
        onEvent = viewModel::handleEvent,
        profileListViewState = profileListViewState,
        snackbarState = snackbarState,
        onProfileListEvent = profileListViewModel::handleEvent,
        pagerState = pagerState,
    )

    EventsDispatcher(profileListViewModel.events) {
        when (it) {
            is SelectProfile -> viewModel.handleEvent(DeviceDashboardEvent.ProfileSelected(it.id))
            is ShowHistoryBrew -> {
                viewModel.handleEvent(
                    DeviceDashboardEvent.HistoryBrewSelected(
                        name = it.name,
                        durationSeconds = it.durationSeconds,
                        data = it.data,
                        targetData = it.targetData,
                        phaseProgram = it.phaseProgram,
                        phaseTransitions = it.phaseTransitions,
                    ),
                )
                coroutineScope.launch { pagerState.animateScrollToPage(CompactDashboardPage.Details.ordinal) }
            }

            is ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    NavigatorEffect(navigator, viewModel.navEvent)
    NavigatorEffect(navigator, profileListViewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is DeviceDashboardViewModelEvent.SwitchToDetails -> coroutineScope.launch {
                pagerState.animateScrollToPage(CompactDashboardPage.Details.ordinal)
            }

            is DeviceDashboardViewModelEvent.SelectProfile ->
                profileListViewModel.handleEvent(ProfileListEvent.ProfileSelected(it.id))

            is DeviceDashboardViewModelEvent.ShowSnackbar -> coroutineScope.launch {
                snackbarState.currentSnackbarData?.dismiss()
                snackbarState.showSnackbar(it.message)
            }
        }
    }

    MaintenanceReminderHost(
        deviceId = viewState.device.id,
        blocked = viewState.dialog != null || navigator.getCurrentDestination() != DeviceDashboard(viewState.device.id),
        onOpen = { viewModel.handleEvent(DeviceDashboardEvent.MaintenanceReminderOpened(it)) },
        onError = { coroutineScope.launch { snackbarState.showSnackbar(getString(Res.string.error_generic)) } },
    )

    viewState.dialog?.let {
        DeviceDashboardDialog(
            model = it,
            error = viewState.device.error,
            onEvent = viewModel::handleEvent,
        )
    }
}

@Composable
private fun DeviceDashboardContent(
    viewState: DeviceDashboardViewState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState = SnackbarHostState(),
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    if (isWidthExpanded()) {
        ExpandedDashboard(
            viewState = viewState,
            profileListViewState = profileListViewState,
            snackbarState = snackbarState,
            onEvent = onEvent,
            onProfileListEvent = onProfileListEvent,
        )
    } else {
        CompactDashboard(
            viewState = viewState,
            snackbarState = snackbarState,
            onEvent = onEvent,
            profileListViewState = profileListViewState,
            onProfileListEvent = onProfileListEvent,
            pagerState = pagerState,
        )
    }
}

@Composable
private fun ExpandedDashboard(
    viewState: DeviceDashboardViewState,
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    onProfileListEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var chartsShown by remember { mutableStateOf(false) }
    if (chartsShown) Box(modifier.fillMaxSize()) {
        ChartDashboard(viewState, profileListViewState, snackbarState, onEvent, onProfileListEvent)
        IconButton(onClick = { chartsShown = false }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Outlined.Apps, stringResource(DashboardRes.string.profile_list_show_profiles))
        }
    } else ProfileHome(viewState, profileListViewState, snackbarState, onEvent, onProfileListEvent, onShowCharts = { chartsShown = true })
}

@Composable
private fun ChartDashboard(
    viewState: DeviceDashboardViewState,
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    onProfileListEvent: (ProfileListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .geeFlowInsetsPadding(),
    ) {
        val libraryWidth = (maxWidth * 0.24f).coerceIn(232.dp, 320.dp)
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            TopBar(
                device = viewState.device,
                photoFileName = viewState.user.photoFileName,
                onEvent = onEvent,
                compact = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(start = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrewCharts(
                    model = viewState.chartModel(profileListViewState),
                    visibleCharts = viewState.visibleCharts,
                    modifier = Modifier.weight(1f),
                )
                ProfileList(
                    viewState = profileListViewState,
                    onEvent = onProfileListEvent,
                    modifier = Modifier.width(libraryWidth).fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                BrewStatistics(
                    brew = viewState.brew,
                    isBrewing = viewState.device.isBrewing,
                    visibleCharts = viewState.visibleCharts,
                    onToggle = { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                    modifier = Modifier.widthIn(max = 540.dp),
                )
                Spacer(Modifier.weight(1f))
                BrewTime(viewState.brew.time, Modifier.padding(end = 12.dp))
                DashboardBrewButton(viewState = viewState, onEvent = onEvent)
            }
        }
        SnackbarHost(
            hostState = snackbarState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun CompactDashboard(
    viewState: DeviceDashboardViewState,
    profileListViewState: ProfileListViewState,
    snackbarState: SnackbarHostState,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    onProfileListEvent: (ProfileListEvent) -> Unit,
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        contentWindowInsets = GeeFlowInsets.content,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .geeFlowInsetsPadding(),
        topBar = {
            TopBar(
                device = viewState.device,
                photoFileName = viewState.user.photoFileName,
                onEvent = onEvent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 16.dp, end = 24.dp),
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrewTime(viewState.brew.time, Modifier.weight(1f))
                DashboardBrewButton(viewState = viewState, onEvent = onEvent)
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarState, modifier = Modifier.padding(horizontal = 16.dp)) },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 24.dp),
                onSelected = { index ->
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
            )
            HorizontalPager(
                state = pagerState,
                key = { it },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    CompactDashboardPage.Details.ordinal -> Column(modifier = Modifier.fillMaxSize()) {
                        BrewCharts(
                            model = viewState.chartModel(profileListViewState),
                            visibleCharts = viewState.visibleCharts,
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                                .padding(start = 24.dp, end = 24.dp),
                        )
                        BrewStatistics(
                            brew = viewState.brew,
                            isBrewing = viewState.device.isBrewing,
                            visibleCharts = viewState.visibleCharts,
                            onToggle = { onEvent(DeviceDashboardEvent.ToggleChartVisibility(it)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 16.dp, end = 24.dp),
                        )
                    }

                    CompactDashboardPage.Profiles.ordinal -> ProfileList(
                        viewState = profileListViewState,
                        onEvent = onProfileListEvent,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                    )

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit = {},
) = Row(modifier = modifier) {
    CompactDashboardPage.entries.forEachIndexed { index, page ->
        val color by animateColorAsState(
            targetValue = if (index == selectedTabIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.primary.disabled()
            },
        )
        Text(
            text = when (page) {
                CompactDashboardPage.Details -> stringResource(DashboardRes.string.dashboard_tab_details)
                CompactDashboardPage.Profiles -> stringResource(DashboardRes.string.profile_list_show_profiles)
            },
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { onSelected(index) }
                .padding(vertical = 12.dp)
                .fillMaxWidth()
                .weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = color,
        )
    }
}

private enum class CompactDashboardPage {
    Details,
    Profiles
}


@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
internal fun DeviceDashboardPreviewContent() {
    val state = remember { mutableStateOf(getMockDeviceDashboardViewState()) }
    val pagerState = rememberPagerState { CompactDashboardPage.entries.size }
    DeviceDashboardContent(
        viewState = state.value,
        profileListViewState = getMockProfileListViewState(),
        onProfileListEvent = {},
        pagerState = pagerState,
    )
}

@Composable
internal fun DashboardChartsDesignPreview(history: Boolean = false) {
    val profiles = getMockProfileListViewState().copy(showHistory = history, history = listOf(
        ProfileListViewState.HistoryBrew("1", "Preinfusion", "40 s · 36.7 g", selected = true),
        ProfileListViewState.HistoryBrew("2", "Blooming Espresso", "35 s · 42.0 g"),
        ProfileListViewState.HistoryBrew("3", "Gentle & Sweet", "32 s · 36.0 g"),
    ))
    ChartDashboard(getMockDeviceDashboardViewState(), profiles, remember { SnackbarHostState() }, onProfileListEvent = {})
}
