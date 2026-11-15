import SwiftUI
import CremaKit

/// Crema's original order: profile header, shot chart, measurements, stages, actions.
/// Recipe authoring and the content lists remain in their original presentations.
struct LiveShotView: View {
    @Binding var mode: AppMode
    @Bindable var replayPlayback: ShotPlayback
    @Bindable var liveDriver: LiveDriver
    @Bindable var library: ProfileLibrary
    @Bindable var history: ShotHistory
    @Bindable var tipPreferences: TipPreferences
    @Bindable var session: SignedInUser
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var showLibrary = false
    @State private var showHistory = false
    @State private var showMachines = false
    @State private var autoTuneSession: AutoTuneSession?
    @State private var lastTick: Date = .now
    @State private var appeared = false
    @State private var sectionHeights: [CremaMainSection: CGFloat] = [:]

    private var playback: ShotPlayback { mode == .replay ? replayPlayback : liveDriver.playback }
    private var isBrewing: Bool { liveDriver.brewState == .brewing }
    private var profileName: String { mode == .replay ? "Demo" : playback.profile.name }

    var body: some View {
        TimelineView(.periodic(from: .now, by: mode == .replay && replayPlayback.isPlaying ? 1.0 / 60.0 : 30)) { timeline in
            let date = timeline.date
            DispatchQueue.main.async { advance(to: date) }
            return content
        }
    }

    private var content: some View {
        GeometryReader { geometry in
            let wide = geometry.size.width >= 820
            let usedHeight = (sectionHeights[.header] ?? 52)
                + (sectionHeights[.metrics] ?? 110)
                + (sectionHeights[.stages] ?? 56)
                + (sectionHeights[.actions] ?? 102)
                + 136 // content padding, three gaps, and chart legend/padding
            let chartHeight = max(210, geometry.size.height - usedHeight)
            ScrollView {
                VStack(spacing: 16) {
                    header
                        .cremaMeasureHeight(.header)
                    chart(height: chartHeight)
                    MetricHUD(pressure: playback.currentSample?.pressureBar ?? 0,
                              flow: playback.currentSample?.flowMlPerSec ?? 0,
                              volume: playback.currentSample?.volumeMl ?? 0,
                              elapsed: playback.t,
                              hasReading: playback.currentSample != nil)
                        .cremaMeasureHeight(.metrics)
                    StageTimeline(profile: playback.profile, playhead: playback.t)
                        .frame(height: 56)
                        .cremaMeasureHeight(.stages)
                }
                .padding(.horizontal, wide ? 24 : 16)
                .padding(.top, 16)
                .padding(.bottom, 16)
                .frame(maxWidth: 1400)
                .frame(maxWidth: .infinity)
                .opacity(appeared ? 1 : 0)
                .offset(y: appeared ? 0 : 5)
            }
            .scrollIndicators(.hidden)
            .safeAreaInset(edge: .bottom, spacing: 0) {
                BrewActionZone(mode: $mode, liveDriver: liveDriver, replayPlayback: replayPlayback,
                               library: library, history: history, tipPreferences: tipPreferences,
                               onAutoTune: { startAutoTune() })
                    .padding(.top, 8)
                    .frame(maxWidth: 1400)
                    .frame(maxWidth: .infinity)
                    .background(CremaColor.bg.opacity(0.98))
                    .cremaMeasureHeight(.actions)
            }
            .background(CremaColor.bg.ignoresSafeArea())
        }
        .foregroundStyle(CremaColor.cream)
        .onPreferenceChange(CremaMainHeightsKey.self) { heights in
            Task { @MainActor in
                if sectionHeights != heights { sectionHeights = heights }
            }
        }
        .tint(CremaColor.crema)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.5), value: CremaAppearance.shared.snapshot)
        .onAppear {
            withAnimation(reduceMotion ? nil : .easeOut(duration: 0.22)) { appeared = true }
        }
        .onChange(of: library.active) { _, profile in
            if liveDriver.brewState == .ready && liveDriver.tempProfileOverride == nil {
                liveDriver.playback.profile = profile
            }
        }
        .onChange(of: liveDriver.brewState) { _, state in
            if state == .ready && liveDriver.tempProfileOverride == nil {
                liveDriver.playback.profile = library.active
            }
        }
        .onChange(of: replayPlayback.isPlaying) { _, _ in lastTick = .now }
        .onChange(of: mode) { _, _ in lastTick = .now }
        .sheet(isPresented: $showLibrary) { ProfilesView(library: library, session: session) }
        .sheet(isPresented: $showHistory) { ShotHistoryView(history: history) }
        .sheet(isPresented: $showMachines) { MachinesView(driver: liveDriver) }
        #if os(iOS)
        .fullScreenCover(item: $autoTuneSession) { tune in
            AutoTuneSheet(session: tune, onClose: { autoTuneSession = nil })
        }
        #else
        .sheet(item: $autoTuneSession) { tune in
            AutoTuneSheet(session: tune, onClose: { autoTuneSession = nil })
                .frame(minWidth: 700, minHeight: 720)
        }
        #endif
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 18) {
                profileSelector.fixedSize(horizontal: true, vertical: false)
                Spacer(minLength: 12)
                headerTools
            }
            VStack(alignment: .leading, spacing: 8) {
                profileSelector
                HStack { Spacer(minLength: 0); headerTools }
            }
        }
    }

    private var profileSelector: some View {
        Button { showLibrary = true } label: {
            HStack(spacing: 10) {
                Circle().fill(connectionColor).frame(width: 6, height: 6)
                Text(profileName)
                    .font(CremaTypography.font(size: 23, weight: .medium))
                    .tracking(-0.5).lineLimit(1)
                Image(systemName: "chevron.down").font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(CremaColor.secondary)
            }
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(CremaHeaderPressStyle())
        .accessibilityLabel("Recipes, \(profileName)").help("Recipes")
    }

    private var headerTools: some View {
        HStack(spacing: 7) {
            utility("clock.arrow.circlepath", label: "Journal", availableWhileBrewing: true) { showHistory = true }
            utility("antenna.radiowaves.left.and.right", label: statusLabel) { showMachines = true }
            appearanceMenu
            modePill
        }
    }

    private func utility(_ symbol: String, label: String, availableWhileBrewing: Bool = false,
                         action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 16))
                .foregroundStyle(CremaColor.secondary)
                .frame(width: 44, height: 44)
                .contentShape(Circle())
        }
        .buttonStyle(CremaHeaderPressStyle()).disabled(isBrewing && !availableWhileBrewing)
        .accessibilityLabel(label).help(label)
    }

    private var appearanceMenu: some View {
        let appearance = CremaAppearance.shared.snapshot
        return Menu {
            ForEach(CremaAppearanceMode.allCases, id: \.self) { choice in
                Button { CremaAppearance.shared.setMode(choice) } label: {
                    Label(choice.title, systemImage: choice.symbol)
                }
            }
        } label: {
            Image(systemName: appearance.phase.symbol).font(.system(size: 16))
                .foregroundStyle(CremaColor.secondary)
                .frame(width: 44, height: 44)
        }
        .menuStyle(.borderlessButton)
        .accessibilityLabel("Time appearance, \(appearance.phase.title)")
        .help(appearance.mode == .automatic ? "Auto · \(appearance.phase.title)" : appearance.phase.title)
    }

    private var modePill: some View {
        HStack(spacing: 2) {
            modeChip("Live", symbol: "waveform.path.ecg", active: mode == .live) {
                mode = .live
                replayPlayback.pause()
                liveDriver.bootstrap()
            }
            modeChip("Demo", symbol: "play", active: mode == .replay) {
                mode = .replay
                liveDriver.stop()
                replayPlayback.reset()
                replayPlayback.play()
            }
        }
        .padding(3)
        .background(CremaColor.surface, in: Capsule())
        .disabled(isBrewing)
    }

    private func modeChip(_ title: String, symbol: String, active: Bool,
                          action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: symbol).font(.system(size: 13, weight: .medium))
                .foregroundStyle(active ? CremaColor.onPrimary : CremaColor.secondary)
                .frame(width: 44, height: 44)
                .background(active ? CremaColor.primarySurface : .clear, in: Capsule())
        }
        .buttonStyle(CremaHeaderPressStyle())
        .accessibilityLabel(title).help(title)
        .accessibilityAddTraits(active ? .isSelected : [])
    }

    private func chart(height: CGFloat) -> some View {
        VStack(spacing: 12) {
            HStack(spacing: 16) {
                chartKey("gauge.with.needle", unit: "bar", color: CremaColor.crema)
                chartKey("drop", unit: "mL/s", color: CremaColor.matcha)
                chartKey("waterbottle", unit: "mL", color: CremaColor.cream)
                Spacer(minLength: 0)
            }
            ShotChart(samples: playback.emitted, profile: playback.profile,
                      playhead: playback.t, totalDuration: playback.totalDuration)
                .frame(height: height)
        }
        .padding(16)
        .background(CremaColor.surface, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
    }

    private func chartKey(_ symbol: String, unit: String, color: Color) -> some View {
        HStack(spacing: 5) {
            Image(systemName: symbol).font(.system(size: 11))
            Text(unit).font(CremaTypography.font(size: 10))
        }
        .foregroundStyle(color)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(unit == "bar" ? "Pressure, bar" : unit == "mL/s" ? "Pump flow, milliliters per second" : "Pumped water, milliliters")
    }

    private var connectionColor: Color {
        if mode == .replay { return CremaColor.crema }
        switch liveDriver.state {
        case .connected: return CremaColor.connected
        case .failed: return CremaColor.danger
        default: return CremaColor.secondary
        }
    }

    private var statusLabel: String {
        switch liveDriver.state {
        case .connected(let name):
            return liveDriver.registry.machines.first(where: {
                $0.advertisedName == name || $0.id == name
            })?.displayName ?? PairedMachine.prettify(name)
        case .connecting: return "Connecting…"
        case .scanning, .discovered: return "Finding machine…"
        case .disconnecting: return "Disconnecting…"
        case .failed: return "Connection needs attention"
        case .idle: return "Pair your machine"
        }
    }

    @MainActor private func advance(to date: Date) {
        let dt = date.timeIntervalSince(lastTick)
        lastTick = date
        if playback.isPlaying, dt > 0, dt < 0.5 { playback.advance(by: dt) }
    }

    @MainActor private func startAutoTune() {
        #if targetEnvironment(simulator)
        let scale: ScaleTransport = StubScale(targetG: 36, durationS: 28)
        #else
        let scale: ScaleTransport = ManualScale()
        #endif
        autoTuneSession = AutoTuneSession(liveDriver: liveDriver, library: library, scale: scale)
    }
}

private enum CremaMainSection: Hashable, Sendable {
    case header, metrics, stages, actions
}

private struct CremaMainHeightsKey: PreferenceKey {
    static let defaultValue: [CremaMainSection: CGFloat] = [:]
    static func reduce(value: inout [CremaMainSection: CGFloat],
                       nextValue: () -> [CremaMainSection: CGFloat]) {
        value.merge(nextValue(), uniquingKeysWith: { _, new in new })
    }
}

private extension View {
    func cremaMeasureHeight(_ section: CremaMainSection) -> some View {
        background {
            GeometryReader { geometry in
                Color.clear.preference(key: CremaMainHeightsKey.self,
                                       value: [section: geometry.size.height])
            }
        }
    }
}

private struct CremaHeaderPressStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.98 : 1)
            .opacity(configuration.isPressed ? 0.72 : 1)
            .animation(reduceMotion ? nil : .easeOut(duration: 0.13), value: configuration.isPressed)
    }
}
