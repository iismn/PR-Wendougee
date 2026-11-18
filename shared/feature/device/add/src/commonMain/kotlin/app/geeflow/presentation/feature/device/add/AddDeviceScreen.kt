package app.geeflow.presentation.feature.device.add

import androidx.compose.animation.AnimatedContent
import app.geeflow.ui.animations.ForwardTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.geeflow.navigation.Navigator
import app.geeflow.navigation.NavigatorEffect
import app.geeflow.presentation.feature.device.add.AddDeviceEvent.BackClicked
import app.geeflow.presentation.feature.device.add.AddDeviceEvent.NearbyDeviceClicked
import app.geeflow.presentation.feature.device.add.AddDeviceEvent.QrCodeScanned
import app.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowNearbyDevicesClicked
import app.geeflow.presentation.feature.device.add.AddDeviceEvent.ShowQrCodeScannerClicked
import app.geeflow.presentation.feature.device.add.AddDeviceViewModelEvent.ShowSnackbar
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.NearbyDevices
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.QrCodeScanner
import app.geeflow.presentation.feature.device.add.AddDeviceViewState.Method.ManualInput
import app.geeflow.ui.EventsDispatcher
import app.geeflow.ui.components.GeeFlowScaffold
import app.geeflow.ui.components.VerticalSpacer
import app.geeflow.ui.components.GeeFlowOutlinedTextField
import app.geeflow.ui.components.LocalNativeDeviceScanner
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.GeeFlowScreenPreview
import app.geeflow.ui.theme.GeeFlowTheme
import app.geeflow.ui.theme.isPreview
import geeflow.shared.core.ui.generated.resources.common_open_settings
import geeflow.shared.core.ui.generated.resources.common_close
import geeflow.shared.core.ui.generated.resources.permission_bluetooth_missing
import geeflow.shared.feature.device.add.generated.resources.Res
import geeflow.shared.feature.device.add.generated.resources.add_device_demo
import geeflow.shared.feature.device.add.generated.resources.add_device_screen_description
import geeflow.shared.feature.device.add.generated.resources.add_device_screen_empty
import geeflow.shared.feature.device.add.generated.resources.add_device_screen_no_camera_permission
import geeflow.shared.feature.device.add.generated.resources.add_device_screen_scan_qr
import geeflow.shared.feature.device.add.generated.resources.add_device_screen_show_nearby
import geeflow.shared.feature.device.add.generated.resources.add_device_screen_title
import geeflow.shared.feature.device.add.generated.resources.add_device_manual
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_label
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_hint
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_error
import geeflow.shared.feature.device.add.generated.resources.add_device_manual_register
import geeflow.shared.feature.device.add.generated.resources.add_device_camera_description
import geeflow.shared.feature.device.add.generated.resources.add_device_camera_open
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.publicvalue.multiplatform.qrcode.CameraPosition
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions
import geeflow.shared.core.ui.generated.resources.Res as UiRes

@Composable
internal fun AddDeviceScreen(
    viewModel: AddDeviceViewModel,
    navigator: Navigator,
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    AddDeviceContent(
        viewState = viewState,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::handleEvent,
    )

    LifecycleResumeEffect(Unit) {
        viewModel.handleEvent(AddDeviceEvent.Resumed)
        onPauseOrDispose {}
    }

    NavigatorEffect(navigator, viewModel.navEvent)

    EventsDispatcher(viewModel.events) {
        when (it) {
            is ShowSnackbar -> coroutineScope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(it.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDeviceContent(
    viewState: AddDeviceViewState,
    onEvent: (AddDeviceEvent) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    LaunchedEffect(viewState.method) {
        if (viewState.method is QrCodeScanner) topAppBarState.heightOffset = 0f
    }
    GeeFlowScaffold(
        title = stringResource(Res.string.add_device_screen_title),
        subtitle = stringResource(Res.string.add_device_screen_description),
        navIconClick = { onEvent(BackClicked) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        scrollBehavior = scrollBehavior,
        floatingActionButton = {
            FloatingActionButton(viewState = viewState, onEvent = onEvent)
        },
        floatingActionButtonPosition = FabPosition.Center,
        content = {
            Content(
                viewState = viewState,
                onEvent = onEvent,
                contentPadding = it,
            )
        },
    )

    AddDeviceDialogs(
        dialog = viewState.dialog,
        onEvent = onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    viewState: AddDeviceViewState,
    onEvent: (AddDeviceEvent) -> Unit,
    contentPadding: PaddingValues,
) {
    val modifier = Modifier

    AnimatedContent(
        targetState = viewState.method,
        contentKey = { it::class },
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { ForwardTransition },
        label = "device-add-method",
    ) { method ->
        when (method) {
            is NearbyDevices -> NearbyDevicesList(
                model = method,
                onEvent = onEvent,
                contentPadding = contentPadding,
                modifier = modifier,
            )

            is QrCodeScanner -> Scanner(
                model = method,
                onEvent = onEvent,
                contentPadding = contentPadding,
            )
            is ManualInput -> ManualCodeInput(method, onEvent, contentPadding)
        }
    }
}

@Composable
private fun FloatingActionButton(
    viewState: AddDeviceViewState,
    onEvent: (AddDeviceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (viewState.method !is NearbyDevices) {
            OutlinedButton(onClick = { onEvent(ShowNearbyDevicesClicked) }) {
                Text(stringResource(Res.string.add_device_screen_show_nearby))
            }
        }
        if (viewState.method.changeMethodButtonVisible && viewState.method !is QrCodeScanner) {
            Button(onClick = { onEvent(ShowQrCodeScannerClicked) }) {
                Text(stringResource(Res.string.add_device_screen_scan_qr))
            }
        }
        if (viewState.method !is ManualInput) {
            OutlinedButton(onClick = { onEvent(AddDeviceEvent.ShowManualInputClicked) },
                modifier = Modifier.testTag("device-manual-entry")) {
                Text(stringResource(Res.string.add_device_manual))
            }
        }
    }
}

@Composable
private fun ManualCodeInput(model: ManualInput, onEvent: (AddDeviceEvent) -> Unit, contentPadding: PaddingValues) {
    val code = rememberTextFieldState()
    val submit = { if (code.text.isNotBlank()) onEvent(AddDeviceEvent.ManualCodeSubmitted(code.text.toString())) }
    Box(Modifier.fillMaxSize().padding(contentPadding).padding(24.dp).imePadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.add_device_manual), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(Res.string.add_device_manual_hint), style = MaterialTheme.typography.bodyMedium)
            GeeFlowOutlinedTextField(
                state = code,
                modifier = Modifier.fillMaxWidth().testTag("device-manual-code"),
                label = { Text(stringResource(Res.string.add_device_manual_label)) },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                onKeyboardAction = { submit() },
                autoFocus = true,
            )
            if (model.invalidCode) Text(stringResource(Res.string.add_device_manual_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = submit, enabled = code.text.isNotBlank(), modifier = Modifier.align(Alignment.End).testTag("device-manual-register")) {
                Text(stringResource(Res.string.add_device_manual_register))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NearbyDevicesList(
    model: NearbyDevices,
    onEvent: (AddDeviceEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(
            horizontal = GeeFlowTheme.spacing.contentHorizontal,
            vertical = GeeFlowTheme.spacing.contentVertical,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            model.showMissingPermissionMessage -> item {
                MissingPermissionsMessage(
                    onEvent = onEvent,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }

            model.devices.isEmpty() -> item {
                EmptyListMessage(
                    onEvent = onEvent,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        }
        items(model.devices, key = { it.id }) {
            DeviceItem(it, onEvent)
        }
        if (model.devices.isNotEmpty()) {
            item { AddDemoDeviceButton(onEvent) }
        }
    }
}

@Composable
private fun DeviceItem(
    model: AddDeviceViewState.DeviceItem,
    onEvent: (AddDeviceEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = { onEvent(NearbyDeviceClicked(model.id)) })
            .padding(start = 24.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
    ) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = model.id,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Scanner(
    model: QrCodeScanner,
    onEvent: (AddDeviceEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val nativeScanner = LocalNativeDeviceScanner.current
    if (nativeScanner != null && !isPreview) {
        val title = stringResource(Res.string.add_device_screen_scan_qr)
        val closeTitle = stringResource(UiRes.string.common_close)
        LaunchedEffect(nativeScanner, model.requestId) {
            nativeScanner.scanDeviceCode(title, closeTitle,
                onScanned = { onEvent(QrCodeScanned(it)) },
                onClosed = { onEvent(ShowNearbyDevicesClicked) },
                onError = { onEvent(AddDeviceEvent.NativeScannerFailed(it)) })
        }
        Column(Modifier.fillMaxSize().padding(contentPadding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(model.nativeError ?: stringResource(Res.string.add_device_camera_description), textAlign = TextAlign.Center)
            VerticalSpacer(16.dp)
            Button(onClick = { onEvent(ShowQrCodeScannerClicked) }) { Text(stringResource(Res.string.add_device_camera_open)) }
            if (model.nativeError != null) {
                OutlinedButton(onClick = { onEvent(AddDeviceEvent.OpenSystemSettingsClicked) }) { Text(stringResource(UiRes.string.common_open_settings)) }
            }
        }
        return
    }
    val shape = MaterialTheme.shapes.extraLarge
    val scannerModifier = modifier
        .fillMaxHeight()
        .padding(contentPadding)
        .padding(48.dp)
        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        .clip(shape)
        .padding(2.dp)
        .background(Color.Black, shape)
        .fillMaxSize()

    if (isPreview) {
        Box(modifier = scannerModifier)
    } else {
        ScannerWithPermissions(
            modifier = scannerModifier.clipToBounds(),
            onScanned = {
                onEvent(QrCodeScanned(it))
                !model.scanningEnabled
            },
            types = listOf(CodeType.QR),
            cameraPosition = CameraPosition.BACK,
            enableTorch = false,
            permissionDeniedContent = { permissionState ->
                Column(
                    modifier = scannerModifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VerticalSpacer(1f)
                    Text(
                        modifier = Modifier.padding(6.dp),
                        text = stringResource(Res.string.add_device_screen_no_camera_permission),
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )
                    Button(onClick = { permissionState.goToSettings() }) {
                        Text(text = stringResource(UiRes.string.common_open_settings))
                    }
                    VerticalSpacer(1f)
                }
            },
        )
    }
}

@Composable
private fun MissingPermissionsMessage(
    onEvent: (AddDeviceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(UiRes.string.permission_bluetooth_missing),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(24.dp)
        OutlinedButton(
            onClick = { onEvent(AddDeviceEvent.OpenSystemSettingsClicked) },
        ) {
            Text(text = stringResource(UiRes.string.common_open_settings))
        }
        VerticalSpacer(16.dp)
        AddDemoDeviceButton(onEvent)
    }
}

@Composable
private fun EmptyListMessage(
    onEvent: (AddDeviceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.add_device_screen_empty),
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(16.dp)
        AddDemoDeviceButton(onEvent)
    }
}

@Composable
private fun AddDemoDeviceButton(
    onEvent: (AddDeviceEvent) -> Unit,
) = OutlinedButton(
    modifier = Modifier.padding(horizontal = GeeFlowTheme.spacing.contentHorizontal),
    onClick = { onEvent(AddDeviceEvent.AddDemoDeviceClicked) },
    content = { Text(text = stringResource(Res.string.add_device_demo)) },
)

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowScreenPreview
fun AddDeviceDesignPreview(found: Boolean = false) {
    AddDeviceContent(
        viewState = AddDeviceViewState(
            method = NearbyDevices(
                changeMethodButtonVisible = true,
                devices = if (found) listOf(AddDeviceViewState.DeviceItem("review-device", "Wendougee LITA")) else emptyList(),
            ),
        ),
    )
}
