package app.geeflow.presentation.feature.device.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.MonitorWeight
import geeflow.shared.feature.device.dashboard.generated.resources.profile_list_show_profiles
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.AlarmClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.CleaningClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ConnectionButtonClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.DeviceClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.QuickSettingsLongPressed
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.ScaleClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardEvent.UserClicked
import app.geeflow.presentation.feature.device.dashboard.main.DeviceDashboardViewState.Device
import app.geeflow.ui.components.CoffeeMotionEasing
import app.geeflow.ui.components.GeeFlowUserAvatar
import app.geeflow.ui.components.HorizontalSpacer
import app.geeflow.ui.icons.GeeFlowIcon
import app.geeflow.ui.icons.Pressure
import app.geeflow.ui.icons.Steam
import app.geeflow.ui.icons.Temperature
import app.geeflow.ui.icons.Logo
import app.geeflow.ui.theme.GeeFlowComponentPreview
import app.geeflow.ui.theme.GeeFlowPreviewWrapper
import app.geeflow.ui.theme.coffeeGlass
import geeflow.shared.core.ui.generated.resources.common_settings
import geeflow.shared.feature.device.dashboard.generated.resources.Res
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_alarm
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_clean
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_connected
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_connecting
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_disconnected
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_syncing
import geeflow.shared.feature.device.dashboard.generated.resources.device_dashboard_toggle_steam_boiler
import geeflow.shared.feature.device.dashboard.generated.resources.quick_scale_title
import org.jetbrains.compose.resources.stringResource
import geeflow.shared.core.ui.generated.resources.Res as CoreRes

private val LocalCompactHeader = staticCompositionLocalOf { false }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TopBar(
    device: Device,
    photoFileName: String? = null,
    onEvent: (DeviceDashboardEvent) -> Unit = {},
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onHome: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalCompactHeader provides compact) {
        if (compact) {
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).testTag("home-header-logo").clip(CircleShape).headerClickable { onEvent(UserClicked) }, contentAlignment = Alignment.Center) {
                    Icon(rememberVectorPainter(GeeFlowIcon.Logo), stringResource(CoreRes.string.common_settings),
                        Modifier.size(30.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
                HorizontalSpacer(8.dp)
                Box(Modifier.weight(1f)) { DeviceTile(device, onEvent) }
                HorizontalSpacer(8.dp)
                ActionBar(device, onEvent, onHome = onHome)
            }
        } else {
        FlowRow(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            GeeFlowUserAvatar(
                photoFileName = photoFileName,
                modifier = Modifier.size(if (compact) 32.dp else 48.dp),
                onClick = { onEvent(UserClicked) },
            )
            HorizontalSpacer(8.dp)
            DeviceTile(device, onEvent)
            HorizontalSpacer(8.dp)
            ActionBar(
                device = device,
                onEvent = onEvent,
                modifier = if (compact) Modifier else Modifier.weight(1f, false),
            )
        }
        }
    }
}

@Composable
private fun ActionBar(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    onHome: (() -> Unit)? = null,
) {
    val compact = LocalCompactHeader.current
    Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically) {
        if (device.alarm) {
            ErrorButton(onClick = { onEvent(AlarmClicked) })
            HorizontalSpacer(8.dp)
        }
        Row(Modifier.testTag("home-header-actions").coffeeGlass(MaterialTheme.shapes.large).clip(MaterialTheme.shapes.large)) {
            SettingsButton(onClick = { onEvent(QuickSettingsClicked) }, onLongClick = { onEvent(QuickSettingsLongPressed) })
            VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
            CleaningButton(onClick = { onEvent(CleaningClicked) })
            VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
            ConnectivityButton(device.smartScaleConnected, device.singleDoseGrinderConnected,
                onClick = { onEvent(ScaleClicked) })
            if (compact) {
                VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                ConnectionStatusButton(device.connectionStatus, onEvent, Modifier.fillMaxHeight(), grouped = true)
                if (onHome != null) {
                    VerticalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
                    Box(Modifier.testTag("home-header-home").headerClickable(onHome)
                        .height(44.dp).widthIn(min = 44.dp).padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Apps, stringResource(Res.string.profile_list_show_profiles),
                            Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (!compact) {
            HorizontalSpacer(16.dp)
            ConnectionStatusButton(device.connectionStatus, onEvent, Modifier.fillMaxHeight())
        }
    }
}

/** Keep the glass group fixed while its individual control gives immediate press feedback. */
@Composable
private fun Modifier.headerPress(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .96f else 1f,
        tween(if (pressed) 90 else 180, easing = CoffeeMotionEasing), label = "header-button-press")
    return scale(scale)
}

@Composable
private fun Modifier.headerClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return headerPress(interactionSource).clickable(interactionSource = interactionSource,
        indication = LocalIndication.current, onClick = onClick)
}

@Composable
private fun CleaningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .testTag("home-header-cleaning")
        .headerClickable(onClick = onClick)
        .height(if (LocalCompactHeader.current) 44.dp else 48.dp)
        .widthIn(min = 44.dp)
        .padding(horizontal = if (LocalCompactHeader.current) 10.dp else 16.dp),
    contentAlignment = Alignment.Center,
) {
    Icon(
        painter = rememberVectorPainter(Icons.Filled.AutoAwesome),
        contentDescription = stringResource(Res.string.device_dashboard_clean),
        modifier = Modifier.size(if (LocalCompactHeader.current) 16.dp else 20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorButton(onClick: () -> Unit) {
    val animation = rememberInfiniteTransition("warningSizeAnimation")
    val scale by animation.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(WarningAnimationMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .headerClickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.errorContainer)
            .size(if (LocalCompactHeader.current) 44.dp else 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Filled.Error),
            contentDescription = stringResource(Res.string.device_dashboard_alarm),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(if (LocalCompactHeader.current) 16.dp else 20.dp).scale(scale),
        )
    }
}

@Composable
private fun SettingsButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .testTag("home-header-boiler")
            .headerPress(interactionSource)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClickLabel = stringResource(Res.string.device_dashboard_toggle_steam_boiler),
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .height(if (LocalCompactHeader.current) 44.dp else 48.dp)
            .widthIn(min = 44.dp)
            .padding(horizontal = if (LocalCompactHeader.current) 10.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = rememberVectorPainter(Icons.Filled.Tune),
            contentDescription = stringResource(CoreRes.string.common_settings),
            modifier = Modifier.size(if (LocalCompactHeader.current) 16.dp else 20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConnectivityButton(
    smartScaleConnected: Boolean,
    singleDoseGrinderConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Box(
    modifier = modifier
        .testTag("home-header-peripherals")
        .headerClickable(onClick = onClick)
        .height(if (LocalCompactHeader.current) 44.dp else 48.dp)
        .widthIn(min = 44.dp)
        .padding(horizontal = if (LocalCompactHeader.current) 10.dp else 16.dp),
    contentAlignment = Alignment.Center,
) {
    if (singleDoseGrinderConnected) {
        Box(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                .size(4.dp),
        )
    }
    if (smartScaleConnected) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp, end = 10.dp)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(0.5.dp))
                .size(4.dp),
        )
    }
    Icon(
        imageVector = Icons.Outlined.MonitorWeight,
        modifier = Modifier.size(if (LocalCompactHeader.current) 16.dp else 20.dp),
        contentDescription = stringResource(Res.string.quick_scale_title),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ConnectionStatusButton(
    connectionStatus: Device.ConnectionStatus,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
    grouped: Boolean = false,
) {
    val containerColor by animateColorAsState(
        when (connectionStatus) {
            Device.ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.errorContainer
            Device.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.surfaceContainer
            Device.ConnectionStatus.Synchronizing -> MaterialTheme.colorScheme.secondaryContainer
            Device.ConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
        },
    )

    val text = when (connectionStatus) {
        Device.ConnectionStatus.Disconnected -> stringResource(Res.string.device_dashboard_disconnected)
        Device.ConnectionStatus.Connecting -> stringResource(Res.string.device_dashboard_connecting)
        Device.ConnectionStatus.Synchronizing -> stringResource(Res.string.device_dashboard_syncing)
        Device.ConnectionStatus.Connected -> stringResource(Res.string.device_dashboard_connected)
    }

    val contentColor by animateColorAsState(
        when (connectionStatus) {
            Device.ConnectionStatus.Disconnected -> MaterialTheme.colorScheme.onErrorContainer
            Device.ConnectionStatus.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
            Device.ConnectionStatus.Synchronizing -> MaterialTheme.colorScheme.onSecondaryContainer
            Device.ConnectionStatus.Connected -> MaterialTheme.colorScheme.onPrimary
        },
    )

    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        modifier = modifier.testTag("home-header-connection").headerPress(interactionSource)
            .height(if (LocalCompactHeader.current) 44.dp else 48.dp),
        interactionSource = interactionSource,
        contentPadding = if (LocalCompactHeader.current) PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            else ButtonDefaults.TextButtonContentPadding,
        onClick = { onEvent(ConnectionButtonClicked) },
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        shape = if (grouped) RoundedCornerShape(0.dp) else MaterialTheme.shapes.large,
    ) {
        if (connectionStatus == Device.ConnectionStatus.Connecting || connectionStatus == Device.ConnectionStatus.Synchronizing) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (LocalCompactHeader.current) 14.dp else 20.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                modifier = Modifier.size(if (LocalCompactHeader.current) 14.dp else 20.dp),
                painter = rememberVectorPainter(
                    when (connectionStatus) {
                        Device.ConnectionStatus.Connected -> Icons.Filled.BluetoothConnected
                        Device.ConnectionStatus.Connecting -> Icons.Filled.BluetoothAudio
                        Device.ConnectionStatus.Synchronizing -> Icons.Filled.BluetoothAudio
                        Device.ConnectionStatus.Disconnected -> Icons.Filled.BluetoothDisabled
                    },
                ),
                contentDescription = null,
            )
        }
        Text(
            text = text,
            style = if (LocalCompactHeader.current) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                else MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = if (LocalCompactHeader.current) 4.dp else 8.dp),
        )
    }
}

@Composable
internal fun DeviceTile(
    device: Device,
    onEvent: (DeviceDashboardEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compact = LocalCompactHeader.current
    val tileModifier = modifier.testTag("home-header-machine")
        .clip(MaterialTheme.shapes.medium).headerClickable { onEvent(DeviceClicked) }
    if (compact) {
        Row(tileModifier.height(36.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(device.name, modifier = Modifier.testTag("home-header-machine-name"),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    } else {
        Column(tileModifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(Modifier.padding(start = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(device.name, modifier = Modifier.testTag("home-header-machine-name"), style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.animateContentSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ParameterItem(device.brewBoilerTemp, rememberVectorPainter(GeeFlowIcon.Temperature))
                ParameterItem(device.steamBoilerTemp, rememberVectorPainter(GeeFlowIcon.Steam))
                ParameterItem(device.pressure, rememberVectorPainter(GeeFlowIcon.Pressure))
            }
        }
    }
}

@Composable
private fun ParameterItem(
    value: String?,
    painter: Painter,
    modifier: Modifier = Modifier,
) = Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(if (LocalCompactHeader.current) 3.dp else 4.dp),
) {
    Icon(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.size(if (LocalCompactHeader.current) 11.dp else 16.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val textMeasurer = rememberTextMeasurer()
    val style = if (LocalCompactHeader.current) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp)
        else MaterialTheme.typography.labelLarge
    val textWidth = LocalDensity.current.run {
        textMeasurer.measure(text = "100.0", style = style).size.width.toDp()
    }

    Text(
        text = value ?: "  —",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = style,
        modifier = Modifier.widthIn(min = textWidth),
    )
}

private const val WarningAnimationMs = 500

@PreviewWrapper(GeeFlowPreviewWrapper::class)
@Composable
@GeeFlowComponentPreview
private fun Preview() {
    var alarmOn by remember { mutableStateOf(false) }
    TopBar(
        device = Device(
            name = "Wendougee Data-S",
            brewBoilerTemp = "93°",
            steamBoilerTemp = "125°",
            pressure = "9.0",
            connectionStatus = Device.ConnectionStatus.Connected,
            smartScaleConnected = true,
            singleDoseGrinderConnected = true,
            alarm = alarmOn,
        ),
        modifier = Modifier.padding(16.dp),
        onEvent = {
            alarmOn = !alarmOn
        },
    )
}
