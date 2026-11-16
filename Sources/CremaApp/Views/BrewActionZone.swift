import SwiftUI
import CremaKit

/// A tactile action dock. Transport and brew state determine every available action.
struct BrewActionZone: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Binding var mode: AppMode
    let liveDriver: LiveDriver
    let replayPlayback: ShotPlayback
    let library: ProfileLibrary
    let history: ShotHistory
    let tipPreferences: TipPreferences
    var onAutoTune: (() -> Void)? = nil

    var body: some View {
        Group {
            if mode == .replay {
                ReplayPlaybackBar(playback: replayPlayback)
            } else {
                LiveBrewBar(driver: liveDriver, library: library,
                            history: history, tipPreferences: tipPreferences,
                            onAutoTune: onAutoTune)
            }
        }
        .padding(16)
        .background {
            RoundedRectangle(cornerRadius: 30, style: .continuous)
                .fill(CremaColor.surface)
                .overlay {
                    RoundedRectangle(cornerRadius: 30, style: .continuous)
                        .strokeBorder(CremaColor.cream.opacity(0.05), lineWidth: 0.5)
                }
        }
        .padding(.horizontal, 20)
        .padding(.bottom, 16)
        .animation(reduceMotion ? nil : .smooth(duration: 0.25), value: mode)
    }
}

private struct LiveBrewBar: View {
    @Bindable var driver: LiveDriver
    let library: ProfileLibrary
    @Bindable var history: ShotHistory
    @Bindable var tipPreferences: TipPreferences
    var onAutoTune: (() -> Void)? = nil
    @State private var showMachines = false
    @State private var showFeedback = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            switch phase {
            case .needsSetup:
                AdaptiveDockRow {
                    DockStatus(text: "Not paired", symbol: "link", tint: CremaColor.secondary,
                               accessibilityText: "Machine not paired")
                } action: {
                    PrimaryPill(label: "Pair machine", systemImage: "antenna.radiowaves.left.and.right",
                                action: { showMachines = true })
                }
            case .primaryDisconnected(let name):
                AdaptiveDockRow {
                    HStack(spacing: 12) {
                        DockStatus(text: name, symbol: "link", tint: CremaColor.secondary,
                                   accessibilityText: "Disconnected, \(name)")
                        Spacer(minLength: 0)
                        SecondaryPill(label: "Machines", systemImage: "slider.horizontal.3",
                                      action: { showMachines = true })
                    }
                } action: {
                    PrimaryPill(label: "Connect to \(name)", systemImage: "antenna.radiowaves.left.and.right",
                                action: { driver.connectToPrimary() })
                }
            case .scanning(let message):
                HStack(spacing: 12) {
                    ProgressView().tint(CremaColor.crema)
                    Text(message)
                        .font(CremaTypography.font(size: 12))
                        .foregroundStyle(CremaColor.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                    Spacer(minLength: 0)
                    SecondaryPill(label: "Cancel connection", systemImage: "xmark", action: { driver.stop() })
                }
                .frame(minHeight: 44)
            case .readyToBrew(let name):
                AdaptiveDockRow {
                    HStack(spacing: 10) {
                        ConnectionChip(name: name, isDimmed: false, onTap: { showMachines = true })
                        Spacer(minLength: 0)
                        BrewToolsMenu(driver: driver, onAutoTune: onAutoTune,
                                      onMachines: { showMachines = true })
                    }
                } action: {
                    PrimaryPill(label: "Brew", systemImage: "drop.fill", action: { driver.brew() })
                }
            case .brewing(let name):
                AdaptiveDockRow {
                    DockStatus(text: name, symbol: "drop.fill", tint: CremaColor.crema,
                               accessibilityText: "Brewing with \(name)")
                } action: {
                    PrimaryPill(label: "Abort shot", systemImage: "stop.fill", isDestructive: true,
                                action: { driver.abort() })
                }
            case .done(let name):
                AdaptiveDockRow {
                    ConnectionChip(name: name, isDimmed: false, onTap: { showMachines = true })
                        .accessibilityValue("Shot complete")
                } action: {
                    HStack(spacing: 8) {
                        SecondaryPill(label: "Discard shot", systemImage: "trash",
                                      action: { prepareNextShot() })
                        SecondaryPill(label: "Brew again", systemImage: "arrow.clockwise",
                                      action: { prepareNextShot(); driver.brew() })
                        PrimaryPill(label: "Save shot and tasting notes", systemImage: "tray.and.arrow.down",
                                    action: { showFeedback = true })
                    }
                }
            case .failed(let reason):
                Label(reason, systemImage: "exclamationmark.triangle.fill")
                    .font(CremaTypography.font(size: 12))
                    .foregroundStyle(CremaColor.danger)
                    .fixedSize(horizontal: false, vertical: true)
                HStack(spacing: 8) {
                    Spacer(minLength: 0)
                    SecondaryPill(label: "Machines", systemImage: "antenna.radiowaves.left.and.right",
                                  action: { showMachines = true })
                    PrimaryPill(label: "Retry connection", systemImage: "arrow.clockwise",
                                action: { driver.connectToPrimary() })
                }
            }
        }
        .sheet(isPresented: $showMachines) {
            MachinesView(driver: driver)
        }
        .sheet(isPresented: $showFeedback) {
            ShotFeedbackSheet(
                profile: driver.playback.profile,
                actualTimeS: driver.playback.t,
                curve: capturedCurve,
                history: history,
                tipPreferences: tipPreferences,
                library: library
            )
        }
    }

    /// Keep completed telemetry tied to its original recipe until explicitly cleared.
    private func prepareNextShot() {
        driver.clearShot()
        driver.playback.profile = library.active
    }

    /// Snapshot the playback's samples as the persisted curve for history.
    private var capturedCurve: [ShotLog.Sample] {
        driver.playback.samples.map {
            ShotLog.Sample(t: $0.t, pressureBar: $0.pressureBar, volumeMl: $0.volumeMl)
        }
    }

    // MARK: phase derivation

    private enum Phase: Hashable {
        case needsSetup
        case primaryDisconnected(String)
        case scanning(String)
        case readyToBrew(String)
        case brewing(String)
        case done(String)
        case failed(String)
    }

    private var phase: Phase {
        if let err = driver.lastError, case .idle = driver.state {
            return .failed(err)
        }
        switch driver.state {
        case .idle:
            if let primary = driver.registry.primary {
                return .primaryDisconnected(primary.displayName)
            }
            return .needsSetup
        case .scanning:
            return .scanning("Scanning for nearby machines…")
        case .discovered(let name, _):
            return .scanning("Found \(name) — connecting…")
        case .connecting(let name):
            return .scanning("Connecting to \(prettyName(name))…")
        case .disconnecting:
            return .scanning("Disconnecting…")
        case .failed(let reason):
            return .failed(reason)
        case .connected(let name):
            let pretty = displayNameForConnected(name)
            switch driver.brewState {
            case .ready:   return .readyToBrew(pretty)
            case .brewing: return .brewing(pretty)
            case .done:    return .done(pretty)
            }
        }
    }

    /// Prefer the registry's nickname/displayName over the raw advertised name
    /// (e.g. "Sunday Lita" instead of "WDG · AB071169…").
    private func displayNameForConnected(_ advertisedName: String) -> String {
        if let m = driver.registry.machines.first(where: { $0.advertisedName == advertisedName }) {
            return m.displayName
        }
        return prettyName(advertisedName)
    }

    private func prettyName(_ s: String) -> String {
        if let after = s.split(separator: "_").last {
            return "WDG · \(after.prefix(8))…"
        }
        return s
    }
}

// MARK: - Replay controls

private struct ReplayPlaybackBar: View {
    @Bindable var playback: ShotPlayback

    var body: some View {
        HStack(spacing: 12) {
            Button(action: { playback.reset() }) {
                Image(systemName: "arrow.counterclockwise")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(CremaColor.cream)
                    .frame(width: 44, height: 44)
                    .background(CremaColor.bg, in: Circle())
            }
            .buttonStyle(PressDownStyle())
            .accessibilityLabel("Reset playback")
            .help("Reset playback")

            VStack(spacing: 0) {
                Text(String(format: "%.1f / %.1f s", playback.t, playback.totalDuration))
                    .font(CremaTypography.font(size: 10, weight: .medium))
                    .foregroundStyle(CremaColor.secondary)
                    .monospacedDigit()
                    .frame(maxWidth: .infinity, alignment: .trailing)
                ReplayScrubber(playback: playback)
            }

            Menu {
                ForEach([0.5, 1.0, 1.5, 2.0, 4.0], id: \.self) { speed in
                    Button(speedLabel(speed)) { playback.speed = speed }
                }
            } label: {
                Text(speedLabel(playback.speed))
                    .font(CremaTypography.font(size: 12, weight: .semibold))
                    .monospacedDigit()
                    .foregroundStyle(CremaColor.cream)
                    .frame(minWidth: 44, minHeight: 44)
                    .background(CremaColor.bg, in: Capsule())
            }
            .menuStyle(.borderlessButton)
            .menuIndicator(.hidden)
            .fixedSize()
            .accessibilityLabel("Playback speed")
            .accessibilityValue(speedLabel(playback.speed))
            .help("Playback speed")
            playButton
        }
    }

    private var playButton: some View {
        Button(action: { playback.toggle() }) {
            Image(systemName: playback.isPlaying ? "pause.fill" : "play.fill")
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(CremaColor.onPrimary)
                .offset(x: playback.isPlaying ? 0 : 1)
                .frame(width: 54, height: 54)
                .background(CremaColor.primarySurface, in: Circle())
                .contentTransition(.symbolEffect(.replace))
        }
        .buttonStyle(PressDownStyle())
        .accessibilityLabel(playback.isPlaying ? "Pause playback" : "Play recording")
        .help(playback.isPlaying ? "Pause playback" : "Play recording")
    }

    private func speedLabel(_ speed: Double) -> String {
        speed.truncatingRemainder(dividingBy: 1) == 0
            ? "\(Int(speed))×" : String(format: "%.1f×", speed)
    }
}

private struct ReplayScrubber: View {
    @Bindable var playback: ShotPlayback
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @FocusState private var hasKeyboardFocus: Bool
    @State private var isScrubbing = false

    var body: some View {
        GeometryReader { geometry in
            let fraction = min(1, max(0, playback.t / max(0.001, playback.totalDuration)))
            let trackWidth = max(0, geometry.size.width - 18)
            let duration = playback.totalDuration.isFinite ? max(0.001, playback.totalDuration) : 0.001
            ZStack(alignment: .topLeading) {
                Canvas { context, _ in
                    let minorInterval = trackWidth / CGFloat(duration) >= 14 ? 0.5 : 1.0
                    let minorCount = Int(duration / minorInterval)
                    let baseline = Path { path in
                        path.move(to: CGPoint(x: 9, y: 19))
                        path.addLine(to: CGPoint(x: 9 + trackWidth, y: 19))
                    }
                    context.stroke(baseline, with: .color(CremaColor.hairline.opacity(0.7)), lineWidth: 0.7)
                    let played = CGRect(x: 9, y: 17, width: trackWidth * CGFloat(fraction), height: 3)
                    context.fill(Path(roundedRect: played, cornerRadius: 1.5),
                                 with: .color(CremaColor.crema.opacity(0.2)))

                    for index in 0...minorCount {
                        let second = Double(index) * minorInterval
                        let isMajor = abs(second / 5 - (second / 5).rounded()) < 0.001
                        let x = 9 + trackWidth * CGFloat(second / duration)
                        let tick = Path { path in
                            path.move(to: CGPoint(x: x, y: isMajor ? 5 : 12))
                            path.addLine(to: CGPoint(x: x, y: 19))
                        }
                        context.stroke(tick, with: .color(CremaColor.secondary.opacity(isMajor ? 0.75 : 0.35)),
                                       lineWidth: isMajor ? 1 : 0.7)
                        if isMajor {
                            context.draw(Text("\(Int(second))")
                                .font(CremaTypography.font(size: 8, weight: .medium))
                                .foregroundStyle(CremaColor.secondary),
                                         at: CGPoint(x: x, y: 33))
                        }
                    }
                }
                .accessibilityHidden(true)

                VStack(spacing: -1) {
                    Circle().fill(CremaColor.surface)
                        .frame(width: 10, height: 10)
                        .overlay(Circle().strokeBorder(CremaColor.crema, lineWidth: 2))
                    Capsule().fill(CremaColor.crema).frame(width: 2, height: 12)
                }
                .scaleEffect(isScrubbing && !reduceMotion ? 1.1 : 1, anchor: .top)
                .animation(reduceMotion ? nil : .spring(response: 0.22, dampingFraction: 0.8), value: isScrubbing)
                .offset(x: 4 + trackWidth * CGFloat(fraction), y: 1)
                .accessibilityHidden(true)
            }
            .frame(height: 44)
            .contentShape(Rectangle())
            .gesture(DragGesture(minimumDistance: 0)
                .onChanged { value in
                    hasKeyboardFocus = true
                    isScrubbing = true
                    let fraction = max(0, min(1, (value.location.x - 9) / max(1, trackWidth)))
                    playback.scrub(to: fraction * playback.totalDuration)
                }
                .onEnded { _ in isScrubbing = false }
            )
        }
        .frame(minWidth: 40, minHeight: 44, maxHeight: 44)
        .focusable()
        .focused($hasKeyboardFocus)
        .onKeyPress(.leftArrow) {
            playback.scrub(to: max(0, playback.t - 1))
            return .handled
        }
        .onKeyPress(.rightArrow) {
            playback.scrub(to: min(playback.totalDuration, playback.t + 1))
            return .handled
        }
        .accessibilityElement()
        .accessibilityLabel("Playback position in seconds")
        .help("Seek playback")
        .accessibilityValue(String(format: "%.1f seconds", playback.t))
        .accessibilityHint("Drag the ruler, or use the left and right arrow keys to seek by one second.")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: playback.scrub(to: min(playback.totalDuration, playback.t + 1))
            case .decrement: playback.scrub(to: max(0, playback.t - 1))
            @unknown default: break
            }
        }
    }
}

// MARK: - Dock components

private struct DockStatus: View {
    let text: String
    let symbol: String
    let tint: Color
    let accessibilityText: String

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: symbol).font(.system(size: 13, weight: .medium))
                .foregroundStyle(tint)
            Text(text)
                .font(CremaTypography.font(size: 12, weight: .medium))
                .foregroundStyle(CremaColor.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
    }
}
private struct PrimaryPill: View {
    let label: String
    let systemImage: String
    var isDestructive = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 19, weight: .semibold))
                .foregroundStyle(isDestructive ? CremaColor.onDanger : CremaColor.onPrimary)
                .frame(width: 54, height: 54)
                .background(isDestructive ? CremaColor.danger : CremaColor.primarySurface, in: Circle())
                .contentShape(Circle())
        }
        .buttonStyle(PressDownStyle())
        .accessibilityLabel(label)
        .help(label)
    }
}

private struct SecondaryPill: View {
    let label: String
    let systemImage: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(CremaColor.cream)
                .frame(width: 44, height: 44)
                .background(CremaColor.bg, in: Circle())
                .overlay(Circle().strokeBorder(CremaColor.hairline.opacity(0.7), lineWidth: 1))
                .contentShape(Circle())
        }
        .buttonStyle(PressDownStyle())
        .accessibilityLabel(label)
        .help(label)
    }
}
private struct ConnectionChip: View {
    let name: String
    let isDimmed: Bool
    let onTap: (() -> Void)?

    private var label: some View {
        HStack(spacing: 7) {
            Circle().fill(CremaColor.connected).frame(width: 6, height: 6)
            Text(name)
                .font(CremaTypography.font(size: 11, weight: .medium, design: .rounded))
                .lineLimit(1)
            if onTap != nil {
                Image(systemName: "chevron.down").font(.system(size: 9, weight: .semibold))
            }
        }
        .foregroundStyle(CremaColor.matcha)
        .padding(.horizontal, 12)
        .frame(minHeight: 44)
        .background(CremaColor.matcha.opacity(0.08), in: Capsule())
        .opacity(isDimmed ? 0.6 : 1)
        .accessibilityLabel("Connected to \(name)")
    }

    var body: some View {
        if let onTap {
            Button(action: onTap) { label }.buttonStyle(PressDownStyle())
                .accessibilityLabel("Connected to \(name)")
                .accessibilityHint("Manage machine")
                .help("Manage \(name)")
        } else {
            label
        }
    }
}

/// A single equipment menu keeps preparation actions within reach without growing the dock.
private struct BrewToolsMenu: View {
    let driver: LiveDriver
    let onAutoTune: (() -> Void)?
    let onMachines: () -> Void
    @State private var justSent = false

    var body: some View {
        Menu {
            if let onAutoTune {
                Button("Auto-tune", systemImage: "wand.and.stars", action: onAutoTune)
            }
            if driver.playback.profile.grinder != nil {
                Button("Set grinder", systemImage: "circle.hexagongrid.fill") {
                    driver.sendGrinderSettings()
                    justSent = true
                    Task {
                        try? await Task.sleep(nanoseconds: 1_400_000_000)
                        justSent = false
                    }
                }
            }
            if onAutoTune != nil || driver.playback.profile.grinder != nil { Divider() }
            Button("Machines", systemImage: "antenna.radiowaves.left.and.right", action: onMachines)
        } label: {
            Image(systemName: justSent ? "checkmark" : "slider.horizontal.3")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(justSent ? CremaColor.matcha : CremaColor.cream)
                .frame(width: 44, height: 44)
                .background(CremaColor.bg, in: Circle())
                .overlay(Circle().strokeBorder(CremaColor.hairline.opacity(0.7), lineWidth: 1))
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .accessibilityLabel(justSent ? "Grinder settings sent" : "Brew tools")
        .help(justSent ? "Grinder settings sent" : "Auto-tune, grinder and machines")
    }
}

/// A single compact row when space permits; a full-width action on smaller screens.
private struct AdaptiveDockRow<Leading: View, Action: View>: View {
    @ViewBuilder let leading: () -> Leading
    @ViewBuilder let action: () -> Action

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 16) {
                leading().fixedSize(horizontal: true, vertical: false)
                Spacer(minLength: 4)
                action().fixedSize(horizontal: true, vertical: false)
            }
            VStack(alignment: .leading, spacing: 12) {
                leading().frame(maxWidth: .infinity, alignment: .leading)
                HStack { Spacer(minLength: 0); action() }
            }
        }
    }
}
private struct PressDownStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(reduceMotion ? 1 : configuration.isPressed ? 0.98 : 1)
            .opacity(configuration.isPressed ? 0.9 : 1)
            .animation(reduceMotion ? nil : .spring(response: 0.22, dampingFraction: 0.8),
                       value: configuration.isPressed)
    }
}
