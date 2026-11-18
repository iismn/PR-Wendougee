@file:Suppress("TooManyFunctions", "LongParameterList")

package app.geeflow.presentation.feature.device.dashboard.main

import app.geeflow.core.presentation.BaseViewModel
import app.geeflow.core.presentation.launch
import app.geeflow.core.presentation.launchCatching
import app.geeflow.core.presentation.toUserMessage
import app.geeflow.data.brew.model.BrewMode
import app.geeflow.data.brew.model.BrewProfile
import app.geeflow.data.brew.model.BrewSession
import app.geeflow.data.device.model.DeviceCapability
import app.geeflow.data.device.model.DeviceState
import app.geeflow.data.device.model.DeviceState.BoilerType
import app.geeflow.data.device.model.DeviceState.BrewStatus
import app.geeflow.data.device.model.DeviceState.ConnectionStatus
import app.geeflow.data.device.model.isDemo
import app.geeflow.data.user.model.ChartType
import app.geeflow.domain.brew.usecase.GetBrewProfileUseCase
import app.geeflow.domain.brew.usecase.ObserveBrewDataUseCase
import app.geeflow.domain.brew.usecase.ObserveDeviceProfileUseCase
import app.geeflow.domain.brew.usecase.SaveBrewToHistoryUseCase
import app.geeflow.domain.brew.usecase.SaveFreeVariableProfileUseCase
import app.geeflow.domain.device.usecase.ConnectDeviceUseCase
import app.geeflow.domain.device.usecase.GetProfilingCapabilitiesUseCase
import app.geeflow.domain.device.usecase.GetDeviceConstraintsUseCase
import app.geeflow.domain.device.usecase.StartFreeVariableBrewingUseCase
import app.geeflow.domain.device.usecase.StopFreeVariableBrewingUseCase
import app.geeflow.domain.device.usecase.SetFreeBrewPressureUseCase
import app.geeflow.domain.device.usecase.SetFreeBrewFlowUseCase
import androidx.lifecycle.viewModelScope
import app.geeflow.domain.device.usecase.DisconnectDeviceUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceBrewingSettingsUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceCapabilitiesUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceStateUseCase
import app.geeflow.domain.device.usecase.ObserveDeviceUseCase
import app.geeflow.domain.device.usecase.RefreshAccessoriesUseCase
import app.geeflow.domain.device.usecase.SetBoilerSettingsUseCase
import app.geeflow.domain.device.usecase.StartHotWaterUseCase
import app.geeflow.domain.device.usecase.StartManualBrewingUseCase
import app.geeflow.domain.device.usecase.StartProfileBrewingUseCase
import app.geeflow.domain.device.usecase.StopBrewingUseCase
import app.geeflow.domain.exception.DeviceNotConnectedException
import app.geeflow.domain.user.usecase.GetHomeDockOrderUseCase
import app.geeflow.domain.user.usecase.SetHomeDockOrderUseCase
import app.geeflow.domain.user.usecase.GetSelectedUserUseCase
import app.geeflow.domain.user.usecase.GetVisibleChartsUseCase
import app.geeflow.domain.user.usecase.ToggleChartVisibilityUseCase
import app.geeflow.navigation.NavEvent.To
import app.geeflow.navigation.destination.DeviceDashboard
import app.geeflow.navigation.destination.DeviceList
import app.geeflow.navigation.destination.DeviceSettings
import app.geeflow.navigation.destination.FreeControl
import app.geeflow.platform.permissions.DeniedException
import app.geeflow.platform.permissions.PermissionBluetoothConnect
import app.geeflow.platform.permissions.PermissionBluetoothScan
import app.geeflow.platform.permissions.PermissionsController
import app.geeflow.presentation.feature.device.dashboard.ProfileEditor
import app.geeflow.presentation.feature.device.dashboard.QuickMaintenance
import app.geeflow.presentation.feature.device.dashboard.QuickScale
import app.geeflow.presentation.feature.device.dashboard.QuickSettings
import app.geeflow.presentation.feature.device.dashboard.components.BrewButtonState
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.AlarmClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.BrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.BrewDescriptionClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.CleaningClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.SettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectionButtonClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DeviceClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DialogDismissed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.EditProfileClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.FlowControlClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.HistoryBrewSelected
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ManualBrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.OpenSystemSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.PermissionDialogResumed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ProfileSelected
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsLongPressed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.Resumed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.StopBrewClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ToggleChartVisibility
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.UserClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Brew
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.DashboardChartType
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Dialog
import app.geeflow.presentation.feature.device.dashboard.model.displayDescription
import app.geeflow.presentation.feature.device.dashboard.model.toChartData
import app.geeflow.presentation.feature.device.dashboard.model.toDashboard
import app.geeflow.presentation.feature.device.dashboard.model.toTargetData
import co.touchlab.kermit.Logger
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_steam_boiler_off
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_steam_boiler_on
import geeflow.shared.feature.device.dashboard.generated.resources.free_control_profile_saved
import geeflow.shared.feature.device.dashboard.generated.resources.home_manual_recipe_no_recording
import geeflow.shared.feature.device.dashboard.generated.resources.home_manual_recipe_invalid_recording
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.math.pow
import kotlin.math.round
import app.geeflow.data.device.model.Device as Machine

@KoinViewModel
internal class DeviceDashboardViewModel(
    @InjectedParam private val args: DeviceDashboard,
    @InjectedParam val permissionsController: PermissionsController,
    private val observeDevice: ObserveDeviceUseCase,
    private val observeDeviceState: ObserveDeviceStateUseCase,
    private val connectDevice: ConnectDeviceUseCase,
    private val disconnectDevice: DisconnectDeviceUseCase,
    private val refreshAccessories: RefreshAccessoriesUseCase,
    private val startHotWater: StartHotWaterUseCase,
    private val observeDeviceCapabilities: ObserveDeviceCapabilitiesUseCase,
    private val startManualBrewing: StartManualBrewingUseCase,
    private val startProfileBrewing: StartProfileBrewingUseCase,
    private val stopBrewing: StopBrewingUseCase,
    private val observeBrewData: ObserveBrewDataUseCase,
    private val observeDeviceProfile: ObserveDeviceProfileUseCase,
    private val getVisibleCharts: GetVisibleChartsUseCase,
    private val toggleChartVisibility: ToggleChartVisibilityUseCase,
    private val getBrewProfileUseCase: GetBrewProfileUseCase,
    private val getSelectedUser: GetSelectedUserUseCase,
    private val getHomeDockOrder: GetHomeDockOrderUseCase,
    private val setHomeDockOrder: SetHomeDockOrderUseCase,
    private val saveBrewToHistory: SaveBrewToHistoryUseCase,
    private val saveFreeVariableProfile: SaveFreeVariableProfileUseCase,
    private val observeDeviceBrewingSettings: ObserveDeviceBrewingSettingsUseCase,
    private val setBoilerSettings: SetBoilerSettingsUseCase,
    getProfilingCapabilities: GetProfilingCapabilitiesUseCase,
    getDeviceConstraints: GetDeviceConstraintsUseCase,
    startFreeVariableBrewing: StartFreeVariableBrewingUseCase,
    stopFreeVariableBrewing: StopFreeVariableBrewingUseCase,
    setFreeBrewPressure: SetFreeBrewPressureUseCase,
    setFreeBrewFlow: SetFreeBrewFlowUseCase,
) : BaseViewModel<DeviceDashboardViewState, DeviceDashboardViewModelEvent>(DeviceDashboardViewState()) {

    private var selectedProfileId: String? = null
    private var selectedProfileName: String? = null
    private var selectedProfileDescription: String? = null
    private var machine: Machine? = null
    private var machineBusy = false
    private var deviceConfig: DeviceState.Config? = null
    private var brewInProgress = false
    private var pendingAppProfile: BrewProfile? = null
    private var activeBrewProfile: BrewProfile? = null
    private var skipManualBrews = true
    private var lastBrewSession: BrewSession? = null
    private var missingManualRecording: Job? = null
    private val manualRecipeCapture = HomeManualRecipeCapture()
    private var manualRecipePromptEnabled = true
    private val manualRecipeSave = HomeManualRecipeSave(viewModelScope,
        persist = { session, name, description ->
            check(getSelectedUser().first()?.id == session.userId) { "The selected user changed after extraction" }
            saveFreeVariableProfile(session, name, description)
        }, errorMessage = { it.toUserMessage() },
        onSaved = { emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar(getString(Res.string.free_control_profile_saved))) })
    private val manualControl = HomeManualControl(viewModelScope,
        start = { startFreeVariableBrewing(args.deviceId, it == ManualPriority.Flow) },
        setTarget = { mode, value ->
            if (mode == ManualPriority.Pressure) setFreeBrewPressure(args.deviceId, value)
            else setFreeBrewFlow(args.deviceId, value)
        }, stop = { stopFreeVariableBrewing(args.deviceId) }, onError = ::onError,
        onStartFailed = { manualRecipeCapture.abandon(); missingManualRecording?.cancel(); missingManualRecording = null })

    init {
        launch { manualRecipeSave.draft.collect { draft ->
            modify { copy(dialog = if (draft != null || dialog is Dialog.ManualRecipe) draft else dialog) }
        } }
        launch { manualControl.state.collect { manual -> modify { copy(manual = manual) } } }
        launchCatching(::onError) { manualControl.configure(getProfilingCapabilities(args.deviceId), getDeviceConstraints(args.deviceId)) }
        launch {
            observeDevice(args.deviceId).collect {
                machine = it
                it?.let {
                    modify {
                        copy(device = device.copy(id = args.deviceId, name = machine?.name ?: args.deviceId.toString()))
                    }
                }
            }
        }
        launch {
            observeDeviceState(args.deviceId).combine(observeDeviceCapabilities(args.deviceId)) { state, capabilities ->
                state to (DeviceCapability.HotWaterDispensing in capabilities)
            }.collect { (state, hotWaterSupported) -> updateMachineStateUi(state, hotWaterSupported) }
        }
        launch { getVisibleCharts().collect(::chartsVisibilityChanged) }
        launch { observeBrewData(args.deviceId).collect(::brewSessionDataChanged) }
        launch { getSelectedUser().collect { u ->
            if (manualRecipeCapture.ownerId?.let { it != u?.id } == true) {
                manualRecipeCapture.abandon()
                missingManualRecording?.cancel()
                missingManualRecording = null
            }
            if (manualRecipeSave.ownerId?.let { it != u?.id } == true) manualRecipeSave.cancel()
            modify { copy(user = user.copy(id = u?.id?.toString().orEmpty(), photoFileName = u?.photoUri)) }
        } }
        launch { getHomeDockOrder().collect { order -> modify { copy(dockOrder = order) } } }
        launch { observeDeviceBrewingSettings(args.deviceId).collect { skipManualBrews = it.treatManualAsFlush } }
        launch {
            observeDeviceBrewingSettings.manualRecipePrompt(args.deviceId).collect { enabled ->
                manualRecipePromptEnabled = enabled
                if (!enabled) {
                    manualRecipeCapture.abandon()
                    missingManualRecording?.cancel()
                    missingManualRecording = null
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun handleEvent(event: DeviceDashboardEvent) = when (event) {
        is DeviceDashboardEvent.DockOrderChanged -> {
            modify { copy(dockOrder = event.order) }
            launchCatching(::onError) { setHomeDockOrder(event.order) }
        }
        is ToggleChartVisibility -> launch { toggleChartVisibility(event.type.toDomain()) }
        is ConnectionButtonClicked -> toggleConnection()
        is DeviceClicked -> navigate(To(DeviceList))
        is QuickSettingsClicked -> withDeviceConnected { navigate(To(QuickSettings(args.deviceId))) }
        is QuickSettingsLongPressed -> withDeviceConnected { toggleSteamBoiler() }
        is UserClicked, is SettingsClicked -> navigate(To(DeviceSettings(args.deviceId)))

        is CleaningClicked -> withDeviceConnected { navigate(To(QuickMaintenance(args.deviceId))) }
        is DeviceDashboardEvent.MaintenanceReminderOpened -> withDeviceConnected {
            navigate(To(QuickMaintenance(args.deviceId, event.type)))
        }

        is BrewDescriptionClicked -> modify {
            copy(dialog = Dialog.BrewDescription(brew.name, brew.description))
        }

        is EditProfileClicked -> editSelectedProfile()
        is DialogDismissed -> {
            if (viewState.value.dialog is Dialog.ManualRecipe) manualRecipeSave.cancel()
            else modify { copy(dialog = null) }
        }
        is DeviceDashboardEvent.ManualRecipeNameChanged -> manualRecipeSave.nameChanged(event.value)
        is DeviceDashboardEvent.ManualRecipeDescriptionChanged -> manualRecipeSave.descriptionChanged(event.value)
        is DeviceDashboardEvent.ManualRecipeSaveClicked -> manualRecipeSave.save()
        is DeviceDashboardEvent.ManualRecipeCancelled -> manualRecipeSave.cancel().let { Unit }
        is OpenSystemSettingsClicked -> permissionsController.openAppSettings()
        is DeviceDashboardEvent.HotWaterClicked -> launchCatching(::onError) { startHotWater(args.deviceId) }
        is ManualBrewClicked -> launchCatching(::onError) { startManualBrewing(args.deviceId) }
        is StopBrewClicked -> launchCatching(::onError) { stopBrewing(args.deviceId) }
        is FlowControlClicked -> navigate(To(FreeControl(args.deviceId)))
        is DeviceDashboardEvent.ScaleClicked -> withDeviceConnected { navigate(To(QuickScale(args.deviceId))) }
        is DeviceDashboardEvent.DialModeChanged -> manualControl.select(event.mode)
        is DeviceDashboardEvent.DialTargetChanged -> manualControl.turn(event.value)
        is DeviceDashboardEvent.DialStartClicked -> startManualRecipeShot()
        is DeviceDashboardEvent.DialStopClicked -> manualControl.stopShot()
        is BrewClicked -> startProfile()
        is PermissionDialogResumed -> withBluetoothPermissions { modify { copy(dialog = null) } }
        is ProfileSelected -> onProfileSelected(event.id)
        is HistoryBrewSelected -> modify {
            copy(
                brew = Brew(
                    name = event.name,
                    time = event.durationSeconds,
                    data = event.data,
                    historyTarget = event.targetData,
                    phaseProgram = event.phaseProgram,
                    phaseTransitions = event.phaseTransitions,
                ),
            )
        }

        is Resumed -> connect()
        is AlarmClicked -> modify { copy(dialog = Dialog.Errors) }
    }

    private fun startManualRecipeShot() {
        if (!manualControl.state.value.canStart || manualRecipeCapture.pending || manualRecipeSave.pending) return
        val userId = viewState.value.user.id.toLongOrNull() ?: return
        if (manualRecipePromptEnabled) {
            manualRecipeCapture.arm(manualControl.state.value.mode, userId, lastBrewSession)
        }
        manualControl.startShot()
    }

    private fun offerManualRecipe(session: BrewSession) {
        missingManualRecording?.cancel()
        missingManualRecording = null
        if (!manualRecipePromptEnabled) return
        val problem = manualRecipeProblem(session)
        manualRecipeSave.offer(session, canSave = problem == null)
        if (problem != null) launch {
            val message = when {
                session.recording == null -> getString(Res.string.home_manual_recipe_no_recording)
                problem is IllegalArgumentException || problem is IllegalStateException -> getString(Res.string.home_manual_recipe_invalid_recording)
                else -> problem.toUserMessage()
            }
            manualRecipeSave.setValidationMessage(message)
        }
    }

    private fun toggleConnection() {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            launchCatching { disconnectDevice(args.deviceId) }
        } else {
            withBluetoothPermissions {
                launchCatching { connectDevice(args.deviceId) }
            }
        }
    }

    private fun connect() {
        withBluetoothPermissions {
            when (viewState.value.device.connectionStatus) {
                Device.ConnectionStatus.Disconnected -> launchCatching { connectDevice(args.deviceId) }
                Device.ConnectionStatus.Connected -> launchCatching(
                    onError = { Logger.w(it) { "Could not refresh accessories" } },
                ) { refreshAccessories(args.deviceId) }

                else -> Unit
            }
        }
    }

    private fun editSelectedProfile() {
        val profileId = selectedProfileId?.toLongOrNull() ?: return
        modify { copy(dialog = null) }
        navigate(To(ProfileEditor(args.deviceId, profileId)))
    }

    private fun startProfile() = launchCatching(::onError) {
        selectedProfileId
            ?.toLongOrNull()
            ?.let { getBrewProfileUseCase(it) }
            ?.let { profile ->
                var started = false
                try {
                    modify { copy(brewButtonState = BrewButtonState.Syncing) }
                    pendingAppProfile = profile
                    startProfileBrewing(args.deviceId, profile)
                    started = true
                } finally {
                    if (!started) pendingAppProfile = null
                    modify {
                        copy(
                            brewButtonState = if (machineBusy) {
                                BrewButtonState.Busy
                            } else if (device.isBrewing || device.dispensingHotWater) {
                                BrewButtonState.Brewing
                            } else {
                                BrewButtonState.Idle
                            },
                        )
                    }
                }
                emitEvent(DeviceDashboardViewModelEvent.SwitchToDetails)
            }
    }

    private fun onProfileSelected(id: String?) = launchCatching(::onError) {
        selectedProfileId = id
        val profileId = id?.toLongOrNull() ?: return@launchCatching
        val profile = getBrewProfileUseCase(profileId) ?: return@launchCatching
        selectProfile(profile)
    }

    private suspend fun selectProfile(profile: BrewProfile) {
        selectedProfileId = profile.id.toString()
        selectedProfileName = profile.name
        selectedProfileDescription = profile.displayDescription()
        modify { copy(brew = Brew(name = profile.name, description = selectedProfileDescription.orEmpty())) }
    }

    private fun updateMachineStateUi(state: DeviceState, hotWaterSupported: Boolean) {
        manualRecipeCapture.updateDevice(state)?.let(::offerManualRecipe)
        if (state.connectionStatus != ConnectionStatus.Connected || state.brewStatus != BrewStatus.Idle || !manualRecipeCapture.pending) {
            missingManualRecording?.cancel()
            missingManualRecording = null
        } else if (missingManualRecording == null) {
            missingManualRecording = launch {
                // ObserveBrewData waits up to one second for final totals before freezing its recording.
                delay(2500)
                val missing = manualRecipeCapture.takeMissingRecording()
                missingManualRecording = null
                missing?.let(::offerManualRecipe)
            }
        }
        manualControl.updateDevice(state)
        deviceConfig = state.config
        machineBusy = state.brewStatus == BrewStatus.Cleaning ||
            (state.brewStatus == BrewStatus.WaterFlow && !hotWaterSupported)
        modify {
            val newBrewStatus = when (state.brewStatus) {
                BrewStatus.Manual -> Device.BrewStatus.Manual
                BrewStatus.Profile -> Device.BrewStatus.Profile
                BrewStatus.FreeVariable -> Device.BrewStatus.Profile
                else -> Device.BrewStatus.Idle
            }

            val currentButtonState = if (machineBusy) {
                BrewButtonState.Busy
            } else if (state.brewStatus == BrewStatus.WaterFlow) {
                BrewButtonState.Brewing
            } else if (this.brewButtonState == BrewButtonState.Syncing) {
                BrewButtonState.Syncing
            } else if (newBrewStatus != Device.BrewStatus.Idle) {
                BrewButtonState.Brewing
            } else {
                BrewButtonState.Idle
            }

            copy(
                device = device.copy(
                    brewBoilerTemp = state.brewBoilerTemp?.roundDecimalsTo(1)?.toString(),
                    steamBoilerTemp = state.steamBoilerTemp?.roundDecimalsTo(1)?.toString(),
                    pressure = state.pressure?.roundDecimalsTo(1)?.toString(),
                    flowRate = state.flowRate?.roundDecimalsTo(1)?.toString(),
                    connectionStatus = when (state.connectionStatus) {
                        ConnectionStatus.Disconnected -> Device.ConnectionStatus.Disconnected
                        ConnectionStatus.Connecting -> Device.ConnectionStatus.Connecting
                        ConnectionStatus.Synchronizing -> Device.ConnectionStatus.Synchronizing
                        ConnectionStatus.Connected -> Device.ConnectionStatus.Connected
                    },
                    brewStatus = newBrewStatus,
                    dispensingHotWater = state.brewStatus == BrewStatus.WaterFlow,
                    smartScaleConnected = state.smartScale?.isConnected == true,
                    singleDoseGrinderConnected = state.singleDoseGrinder?.isConnected == true,
                    alarm = state.hasError,
                    error = state.error,
                ),
                hotWaterSupported = hotWaterSupported,
                brewButtonState = currentButtonState,
            )
        }
    }

    /**
     * Keeps the machine's own target temperature — the long press is a power toggle, not a way to
     * change how hot the steam boiler runs.
     */
    private fun toggleSteamBoiler() {
        val config = deviceConfig ?: return
        val enabled = !config.steamBoilerEnabled
        launchCatching(::onError) {
            setBoilerSettings(
                deviceId = args.deviceId,
                boilerType = BoilerType.Steam,
                enabled = enabled,
                temp = config.targetSteamTemp.toInt(),
            )
            val message = if (enabled) {
                Res.string.device_dashboard_steam_boiler_on
            } else {
                Res.string.device_dashboard_steam_boiler_off
            }
            emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar(getString(message)))
        }
    }

    private suspend fun brewSessionDataChanged(session: BrewSession) {
        lastBrewSession = session
        manualRecipeCapture.updateSession(session)?.let(::offerManualRecipe)
        val wasBrewing = brewInProgress
        brewInProgress = session.inProgress

        if (session.mode == BrewMode.Manual && skipManualBrews) return

        if (!wasBrewing && session.inProgress) {
            selectBrewSessionProfile(session)
            val description = activeBrewProfile?.displayDescription().orEmpty()
            modify {
                copy(
                    brew = Brew(
                        name = activeBrewProfile?.name.orEmpty(),
                        description = description,
                    ),
                )
            }
        }
        modify {
            copy(
                brew = brew.copy(
                    time = session.elapsedSeconds,
                    data = session.toChartData(),
                    historyTarget = session.executionTrace?.let { trace ->
                        if (session.inProgress) trace.profile.program.toTargetData(trace.transitions) else trace.toTargetData()
                    } ?: brew.historyTarget,
                    phaseProgram = session.executionTrace?.profile?.program,
                    phaseTransitions = session.executionTrace?.transitions.orEmpty(),
                ),
            )
        }
        if (wasBrewing && !session.inProgress) recordBrew(session)
    }

    /**
     * Selects the started profile, using the bound profile for brews started on the machine.
     * Opens its details and remembers it for brew history.
     */
    private suspend fun selectBrewSessionProfile(session: BrewSession) {
        val appProfile = pendingAppProfile
        pendingAppProfile = null
        activeBrewProfile = session.executionTrace?.profile ?: appProfile
            ?: if (session.mode == BrewMode.Profile) observeDeviceProfile(args.deviceId).first() else null
        activeBrewProfile?.let { profile ->
            selectProfile(profile)
            emitEvent(DeviceDashboardViewModelEvent.SelectProfile(profile.id.toString()))
            emitEvent(DeviceDashboardViewModelEvent.SwitchToDetails)
        }
    }

    /** Only profile brews carry a profile — manual and freehand shots are labelled from their mode. */
    private fun recordBrew(session: BrewSession) {
        val profile = activeBrewProfile
        launchCatching(::onError) {
            saveBrewToHistory(
                deviceId = args.deviceId,
                session = session,
                profileId = profile?.id,
                profileName = profile?.name,
                profileSteps = profile?.steps.orEmpty(),
                profileRecording = profile?.recording,
            )
        }
    }

    private fun chartsVisibilityChanged(charts: Set<ChartType>) = modify {
        copy(visibleCharts = charts.toDashboard())
    }

    private fun withBluetoothPermissions(block: suspend () -> Unit) = launch {
        try {
            if (machine?.isDemo == false) {
                permissionsController.providePermission(PermissionBluetoothScan)
                permissionsController.providePermission(PermissionBluetoothConnect)
            }
            block()
        } catch (_: DeniedException) {
            showBluetoothPermissionMissingDialog()
        }
    }

    private fun withDeviceConnected(block: () -> Unit) {
        if (viewState.value.device.connectionStatus == Device.ConnectionStatus.Connected) {
            block()
        } else {
            onError(DeviceNotConnectedException(args.deviceId))
        }
    }

    private fun showBluetoothPermissionMissingDialog() {
        modify { copy(dialog = Dialog.BluetoothPermissionMissing) }
    }

    private fun onError(throwable: Throwable) {
        Logger.e(throwable = throwable) { "${this::class.simpleName}" }
        launch { emitEvent(DeviceDashboardViewModelEvent.ShowSnackbar(throwable.toUserMessage())) }
    }

    private fun DashboardChartType.toDomain() = when (this) {
        DashboardChartType.Pressure -> ChartType.PRESSURE
        DashboardChartType.FlowRate -> ChartType.FLOW_RATE
        DashboardChartType.WeightRate -> ChartType.WEIGHT_RATE
        DashboardChartType.Volume -> ChartType.VOLUME
        DashboardChartType.Weight -> ChartType.WEIGHT
    }
}

private fun Float.roundDecimalsTo(decimals: Int): Float {
    val factor = 10f.pow(decimals)
    return round(this * factor) / factor
}
