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
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

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
        synchronizedContent
            .foregroundStyle(CremaColor.cream)
            .tint(CremaColor.crema)
            .animation(reduceMotion ? nil : .easeInOut(duration: 0.5), value: CremaAppearance.shared.snapshot)
            .onAppear {
                withAnimation(reduceMotion ? nil : .easeOut(duration: 0.22)) { appeared = true }
            }
            .onPreferenceChange(CremaMainHeightsKey.self) { heights in
                Task { @MainActor in
                    if sectionHeights != heights { sectionHeights = heights }
                }
            }
    }

    private var synchronizedContent: some View {
        autoTunePresentation
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
    }

    private var autoTunePresentation: some View {
        #if os(iOS)
        libraryPresentations
            .fullScreenCover(item: $autoTuneSession) { tune in
                AutoTuneSheet(session: tune, onClose: { autoTuneSession = nil })
            }
        #else
        libraryPresentations
            .sheet(item: $autoTuneSession) { tune in
                AutoTuneSheet(session: tune, onClose: { autoTuneSession = nil })
                    .frame(minWidth: 700, minHeight: 720)
            }
        #endif
    }

    private var libraryPresentations: some View {
        responsiveLayout
            .sheet(isPresented: $showLibrary) { ProfilesView(library: library, session: session) }
            .sheet(isPresented: $showHistory) { ShotHistoryView(history: history) }
            .sheet(isPresented: $showMachines) { MachinesView(driver: liveDriver) }
    }

    private var responsiveLayout: some View {
        GeometryReader { geometry in
            if geometry.size.width >= 1000 && geometry.size.width > geometry.size.height
                && !dynamicTypeSize.isAccessibilitySize {
                HStack(spacing: 0) {
                    shotScrollView(size: CGSize(width: geometry.size.width - 304, height: geometry.size.height),
                                   wide: true, besideLibrary: true)
                    recipeRail
                        .frame(width: 280)
                        .padding(.trailing, 24)
                        .padding(.vertical, 16)
                }
                .background(CremaColor.bg.ignoresSafeArea())
            } else {
                shotScrollView(size: geometry.size, wide: geometry.size.width >= 820)
            }
        }
    }

    private func shotScrollView(size: CGSize, wide: Bool, besideLibrary: Bool = false) -> some View {
        ScrollView {
            shotSections(size: size, wide: wide, besideLibrary: besideLibrary)
        }
        .scrollIndicators(.hidden)
        .safeAreaInset(edge: .bottom, spacing: 0) { actions }
        .background(CremaColor.bg.ignoresSafeArea())
    }

    private func shotSections(size: CGSize, wide: Bool, besideLibrary: Bool) -> some View {
        VStack(spacing: 16) {
            header(wide: wide).cremaMeasureHeight(.header)
            chart(height: chartHeight(in: size))
            measurements(wide: wide).cremaMeasureHeight(.metrics)
            StageTimeline(profile: playback.profile, playhead: playback.t)
                .frame(height: 56)
                .cremaMeasureHeight(.stages)
        }
        .padding(.horizontal, besideLibrary ? 24 : wide ? 36 : 20)
        .padding(.top, 16)
        .padding(.bottom, 8)
        .frame(maxWidth: 1180)
        .frame(maxWidth: .infinity)
        .opacity(appeared ? 1 : 0)
        .offset(y: appeared ? 0 : 5)
    }

    // The same library is visible beside the shot on a landscape iPad.
    // Authoring, sharing, community browsing and deletion keep their original sheet.
    private var recipeRail: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) {
                Image(systemName: "square.stack.3d.up")
                    .font(.system(size: 18))
                Text("\(library.profiles.count)")
                    .font(CremaTypography.font(size: 12).monospacedDigit())
                    .foregroundStyle(CremaColor.secondary)
                Spacer()
                Button { showLibrary = true } label: {
                    Image(systemName: "arrow.up.right")
                        .font(.system(size: 16))
                        .frame(width: 44, height: 44)
                        .background(CremaColor.surface, in: Circle())
                }
                .buttonStyle(CremaHeaderPressStyle())
                .accessibilityLabel("Open recipe library").help("Recipe library")
            }
            .padding(.horizontal, 16)
            .padding(.top, 12)
            ScrollView {
                LazyVStack(spacing: 8) {
                    ForEach(Array(library.profiles.enumerated()), id: \.element.id) { index, profile in
                        railRecipe(profile, number: index + 1)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 12)
            }
            .scrollIndicators(.hidden)
        }
        .background(CremaColor.surfaceWarm.opacity(0.6),
                    in: RoundedRectangle(cornerRadius: 30, style: .continuous))
    }

    private func railRecipe(_ profile: BrewProfile, number: Int) -> some View {
        let selected = mode == .live && playback.profile.id == profile.id
        return Button {
            guard liveDriver.brewState == .ready else { return }
            library.setActive(profile.id)
            liveDriver.setTempProfile(nil)
            liveDriver.playback.profile = profile
            if mode == .replay {
                replayPlayback.pause()
                mode = .live
                liveDriver.bootstrap()
            }
        } label: {
            VStack(alignment: .leading, spacing: 16) {
                HStack(alignment: .top, spacing: 10) {
                    Text(String(format: "%02d", number))
                        .font(CremaTypography.font(size: 11, weight: .medium).monospacedDigit())
                        .frame(width: 30, height: 30)
                        .foregroundStyle(selected ? CremaColor.onPrimary : CremaColor.secondary)
                        .background(selected ? CremaColor.primarySurface : CremaColor.bg, in: Circle())
                    VStack(alignment: .leading, spacing: 5) {
                        Text(profile.name).font(CremaTypography.font(size: 16, weight: .medium))
                            .lineLimit(2).multilineTextAlignment(.leading)
                        HStack(spacing: 8) {
                            if profile.target == .weight, let target = profile.targetWeightG {
                                Text("\(target) g")
                            } else if let target = profile.targetVolumeMl {
                                Text("\(target) mL")
                            }
                            Text(String(format: "%.0f s", profile.totalDuration))
                        }
                        .font(CremaTypography.font(size: 11).monospacedDigit())
                        .foregroundStyle(CremaColor.secondary)
                    }
                    Spacer(minLength: 0)
                }
                ShotChart(samples: [][...], profile: profile, playhead: -1,
                          totalDuration: max(profile.totalDuration, 1), compact: true)
                    .frame(height: 46)
                    .clipShape(RoundedRectangle(cornerRadius: 10))
                    .accessibilityHidden(true)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(CremaColor.surface, in: RoundedRectangle(cornerRadius: 22))
            .overlay {
                RoundedRectangle(cornerRadius: 22)
                    .strokeBorder(selected ? CremaColor.matcha.opacity(0.65) : .clear, lineWidth: 1)
            }
            .contentShape(RoundedRectangle(cornerRadius: 22))
        }
        .buttonStyle(CremaHeaderPressStyle())
        .disabled(liveDriver.brewState != .ready)
        .accessibilityLabel("Select \(profile.name)")
        .accessibilityAddTraits(selected ? .isSelected : [])
    }

    private func measurements(wide: Bool) -> some View {
        MetricHUD(pressure: playback.currentSample?.pressureBar ?? 0,
                  flow: playback.currentSample?.flowMlPerSec ?? 0,
                  volume: playback.currentSample?.volumeMl ?? 0,
                  elapsed: playback.t,
                  hasReading: playback.currentSample != nil,
                  landscape: wide)
    }

    private var actions: some View {
        BrewActionZone(mode: $mode, liveDriver: liveDriver, replayPlayback: replayPlayback,
                       library: library, history: history, tipPreferences: tipPreferences,
                       onAutoTune: { startAutoTune() })
            .padding(.top, 8)
            .frame(maxWidth: 1180)
            .frame(maxWidth: .infinity)
            .background(CremaColor.bg.opacity(0.98))
            .cremaMeasureHeight(.actions)
    }

    private func chartHeight(in size: CGSize) -> CGFloat {
        let headerHeight: CGFloat = sectionHeights[.header] ?? 72
        let metricHeight: CGFloat = sectionHeights[.metrics] ?? 128
        let stageHeight: CGFloat = sectionHeights[.stages] ?? 56
        let actionHeight: CGFloat = sectionHeights[.actions] ?? 102
        // Content padding, three gaps, and chart legend/padding.
        let usedHeight: CGFloat = headerHeight + metricHeight + stageHeight + actionHeight + 132
        return max(210, size.height - usedHeight)
    }

    @ViewBuilder
    private func header(wide: Bool) -> some View {
        if wide {
            HStack(alignment: .center, spacing: 24) {
                VStack(alignment: .leading, spacing: 4) {
                    wordmark
                    profileSelector
                }
                Spacer(minLength: 12)
                recipeSummary
                headerTools
            }
        } else {
            compactHeader
        }
    }

    private var wordmark: some View {
        Text("crema")
            .font(CremaTypography.font(size: 19, weight: .semibold))
            .tracking(-0.8)
    }

    private var compactHeader: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 8) {
                wordmark
                Spacer(minLength: 8)
                headerTools
            }
            HStack(alignment: .bottom, spacing: 14) {
                profileSelector
                Spacer(minLength: 0)
                recipeSummary
            }
        }
        .padding(.bottom, 6)
    }

    private var recipeSummary: some View {
        VStack(alignment: .trailing, spacing: 6) {
            if playback.profile.target == .weight, let weight = playback.profile.targetWeightG {
                Text("\(weight) g")
            } else if let volume = playback.profile.targetVolumeMl {
                Text("\(volume) mL")
            }
            Text(String(format: "%.0f s", playback.profile.totalDuration))
        }
        .font(CremaTypography.font(size: 12).monospacedDigit())
        .foregroundStyle(CremaColor.secondary)
        .fixedSize()
        .accessibilityLabel("Recipe target and duration")
    }

    private var profileSelector: some View {
        Button { showLibrary = true } label: {
            HStack(spacing: 10) {
                Text(profileName)
                    .font(CremaTypography.font(size: 34, weight: .regular))
                    .tracking(-1.3).lineLimit(2).minimumScaleFactor(0.7)
                    .multilineTextAlignment(.leading)
                Image(systemName: "chevron.down").font(.system(size: 12, weight: .medium))
                    .foregroundStyle(CremaColor.secondary)
            }
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(CremaHeaderPressStyle())
        .accessibilityLabel("Recipes, \(profileName)").help("Recipes")
    }

    private var headerTools: some View {
        HStack(spacing: 4) {
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
                .background(CremaColor.surface, in: Circle())
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
                .background(CremaColor.surface, in: Circle())
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
        .padding(18)
        .background(CremaColor.surface, in: RoundedRectangle(cornerRadius: 30, style: .continuous))
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
