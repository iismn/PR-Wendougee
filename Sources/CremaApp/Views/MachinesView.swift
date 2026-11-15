import SwiftUI
import CremaKit

/// Paired equipment and Bluetooth discovery, driven by the real transport state.
struct MachinesView: View {
    @Bindable var driver: LiveDriver
    @Environment(\.dismiss) private var dismiss

    @State private var scanning = false
    @State private var renamingMachine: PairedMachine?
    @State private var renameText = ""

    var body: some View {
        VStack(spacing: 0) {
            header.safeAreaPadding(.top)
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    connectionSummary
                    pairedSection
                    discoveredSection
                }
                .padding(.horizontal, 22)
                .padding(.bottom, 24)
            }
            footer
        }
        #if os(macOS)
        .frame(minWidth: 420, minHeight: 520, idealHeight: 660)
        #endif
        .background(CremaColor.bg.ignoresSafeArea())
        .onAppear { startScan() }
        .onDisappear { driver.stopScan() }
        .alert("Rename machine", isPresented: Binding(
            get: { renamingMachine != nil },
            set: { if !$0 { renamingMachine = nil } }
        ), presenting: renamingMachine) { machine in
            #if os(iOS)
            TextField("Nickname", text: $renameText)
                .textInputAutocapitalization(.words)
            #else
            TextField("Nickname", text: $renameText)
            #endif
            Button("Save") {
                driver.registry.rename(machine.id, to: renameText)
                renamingMachine = nil
            }
            Button("Reset to default", role: .destructive) {
                driver.registry.rename(machine.id, to: nil)
                renamingMachine = nil
            }
            Button("Cancel", role: .cancel) { renamingMachine = nil }
        } message: { machine in
            Text(machine.prettyAdvertisedName)
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Machines")
                    .font(CremaTypography.font(size: 30, weight: .medium, design: .rounded))
                    .tracking(-0.8)
                    .foregroundStyle(CremaColor.cream)
            }
            Spacer()
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(CremaColor.cream)
                    .frame(width: 44, height: 44)
                    .background(CremaColor.surface, in: Circle())
                    .overlay(Circle().strokeBorder(CremaColor.hairline, lineWidth: 1))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Done")
            .help("Close machines")
        }
        .padding(.horizontal, 22)
        .padding(.top, 24)
        .padding(.bottom, 22)
    }

    private var connectionSummary: some View {
        let status = connectionStatus
        return HStack(alignment: .center, spacing: 16) {
            Image(systemName: "cup.and.saucer")
                .font(.system(size: 27, weight: .light))
                .foregroundStyle(CremaColor.crema)
                .frame(width: 64, height: 70)
                .background(CremaColor.crema.opacity(0.08), in: RoundedRectangle(cornerRadius: 20))
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 6) {
                    Circle().fill(status.color).frame(width: 6, height: 6)
                    Text(status.title)
                        .font(CremaTypography.font(size: 14, weight: .semibold, design: .rounded))
                        .foregroundStyle(CremaColor.cream)
                }
                Text(status.detail)
                    .font(CremaTypography.font(size: 12, design: .rounded))
                    .foregroundStyle(CremaColor.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(16)
        .background(CremaColor.surface, in: RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 26, style: .continuous)
            .strokeBorder(CremaColor.hairline.opacity(0.8), lineWidth: 1))
    }

    @ViewBuilder
    private var pairedSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionLabel("Saved machines", count: driver.registry.machines.count)
            if driver.registry.machines.isEmpty {
                EmptyHint(title: "No saved machines", text: "Turn on LITA, then pair it from Nearby.",
                          systemImage: "cup.and.saucer")
            } else {
                VStack(spacing: 10) {
                    ForEach(driver.registry.sorted) { machine in
                        PairedRow(
                            machine: machine,
                            isPrimary: machine.id == driver.registry.primaryID,
                            isConnected: isConnected(machine),
                            isConnecting: isConnecting(machine),
                            onTap: { driver.connect(to: machine.id) },
                            onSetPrimary: { driver.registry.setPrimary(machine.id) },
                            onRename: {
                                renameText = machine.nickname ?? ""
                                renamingMachine = machine
                            },
                            onForget: { driver.registry.forget(machine.id) }
                        )
                    }
                }
            }
        }
    }

    private var unpaired: [DiscoveredPeripheral] {
        driver.discovered.filter { peer in
            !driver.registry.machines.contains(where: { $0.id == peer.identifier })
        }
    }

    private var discoveredSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                SectionLabel("Nearby", count: unpaired.count)
                Spacer()
                if scanning {
                    HStack(spacing: 6) {
                        ProgressView().controlSize(.small).tint(CremaColor.crema)
                        Text("Scanning")
                            .font(CremaTypography.font(size: 11, weight: .medium, design: .rounded))
                            .foregroundStyle(CremaColor.secondary)
                    }
                }
            }
            if unpaired.isEmpty {
                EmptyHint(title: scanning ? "Scanning" : "No new machines",
                          text: scanning ? "Keep your LITA powered on and nearby." : "Keep your LITA nearby and scan again.",
                          systemImage: "antenna.radiowaves.left.and.right")
            } else {
                VStack(spacing: 10) {
                    ForEach(unpaired) { peer in
                        DiscoveredRow(peer: peer) {
                            driver.pair(peer, setAsPrimary: driver.registry.isEmpty)
                        }
                    }
                }
            }
        }
    }

    private var footer: some View {
        VStack(spacing: 10) {
            if let error = driver.lastError {
                Label(error, systemImage: "exclamationmark.triangle")
                    .font(CremaTypography.font(size: 12, design: .rounded))
                    .foregroundStyle(CremaColor.danger)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Button(action: { scanning ? stopScan() : startScan() }) {
                HStack(spacing: 8) {
                    Image(systemName: scanning ? "stop.fill" : "arrow.clockwise")
                    Text(scanning ? "Stop scanning" : "Scan for machines")
                }
                .font(CremaTypography.font(size: 14, weight: .semibold, design: .rounded))
                .foregroundStyle(CremaColor.onPrimary)
                .frame(maxWidth: .infinity, minHeight: 52)
                .background(CremaColor.primarySurface, in: Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 22)
        .padding(.vertical, 16)
        .background(CremaColor.surface)
        .overlay(alignment: .top) { CremaColor.hairline.frame(height: 1) }
    }

    private var connectionStatus: (title: String, detail: String, color: Color) {
        switch driver.state {
        case .connected(let name):
            let displayName = driver.registry.machines.first(where: {
                $0.advertisedName == name || $0.id == name
            })?.displayName ?? PairedMachine.prettify(name)
            return ("Connected", displayName, CremaColor.connected)
        case .scanning:
            return ("Scanning", "Keep LITA powered on and nearby.", CremaColor.crema)
        case .discovered(let name, _):
            return ("Machine found", "Connecting to \(PairedMachine.prettify(name))…", CremaColor.crema)
        case .connecting(let name):
            return ("Connecting", "Connecting to \(PairedMachine.prettify(name))…", CremaColor.crema)
        case .disconnecting:
            return ("Disconnecting", "Finishing the current connection.", CremaColor.secondary)
        case .failed(let reason):
            return ("Connection failed", reason, CremaColor.danger)
        case .idle:
            if let error = driver.lastError {
                return ("Connection failed", error, CremaColor.danger)
            }
            return ("Not connected", "Pair a nearby machine or reconnect a saved one.", CremaColor.secondary)
        }
    }

    // MARK: - Existing transport actions

    private func startScan() {
        scanning = true
        driver.startScan(timeout: 12)
        Task {
            for await state in driver.transport.stateChanges() {
                if case .scanning = state { continue }
                self.scanning = false
                return
            }
        }
    }

    private func stopScan() {
        scanning = false
        driver.stopScan()
    }

    private func isConnected(_ machine: PairedMachine) -> Bool {
        if case .connected(let name) = driver.state {
            return name == machine.advertisedName || name == machine.id
        }
        return false
    }

    private func isConnecting(_ machine: PairedMachine) -> Bool {
        if case .connecting(let name) = driver.state {
            return name == machine.advertisedName || name == machine.id
        }
        return false
    }
}

private struct PairedRow: View {
    let machine: PairedMachine
    let isPrimary: Bool
    let isConnected: Bool
    let isConnecting: Bool
    let onTap: () -> Void
    let onSetPrimary: () -> Void
    let onRename: () -> Void
    let onForget: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onTap) {
                HStack(spacing: 12) {
                    Image(systemName: "cup.and.saucer.fill")
                        .font(.system(size: 20, weight: .light))
                        .foregroundStyle(isConnected ? CremaColor.matcha : CremaColor.crema)
                        .frame(width: 42, height: 48)
                        .background(CremaColor.bg, in: RoundedRectangle(cornerRadius: 14))
                    VStack(alignment: .leading, spacing: 5) {
                        Text(machine.displayName)
                            .font(CremaTypography.font(size: 15, weight: .semibold, design: .rounded))
                            .foregroundStyle(CremaColor.cream)
                            .multilineTextAlignment(.leading)
                        HStack(spacing: 5) {
                            if isPrimary {
                                Label("Primary", systemImage: "star.fill")
                                    .foregroundStyle(CremaColor.crema)
                            }
                            if isPrimary { Text("·") }
                            Text(isConnected ? "Connected" : isConnecting ? "Connecting…" : relativeDateText)
                                .foregroundStyle(isConnected ? CremaColor.matcha : CremaColor.secondary)
                        }
                        .font(CremaTypography.font(size: 10, weight: .medium, design: .rounded))
                        .foregroundStyle(CremaColor.secondary)
                        .lineLimit(1)
                        Text(machine.advertisedName)
                            .font(CremaTypography.font(size: 9, design: .monospaced))
                            .foregroundStyle(CremaColor.secondary)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 4)
                    if isConnecting {
                        ProgressView().controlSize(.small).tint(CremaColor.crema)
                    } else {
                        Image(systemName: isConnected ? "checkmark.circle.fill" : "arrow.up.right")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(isConnected ? CremaColor.matcha : CremaColor.secondary)
                    }
                }
                .padding(.leading, 14)
                .padding(.vertical, 16)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityHint(isConnected ? "Reconnect to this machine" : "Connect to this machine")

            Menu {
                Button("Rename", systemImage: "pencil", action: onRename)
                if !isPrimary {
                    Button("Set as primary", systemImage: "star", action: onSetPrimary)
                }
                Divider()
                Button("Forget machine", systemImage: "trash", role: .destructive, action: onForget)
            } label: {
                Image(systemName: "ellipsis")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(CremaColor.secondary)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .menuStyle(.borderlessButton)
            .menuIndicator(.hidden)
            .fixedSize()
            .accessibilityLabel("Options for \(machine.displayName)")
        }
        .padding(.trailing, 4)
        .background(CremaColor.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous)
            .strokeBorder(isConnected ? CremaColor.matcha.opacity(0.35) : CremaColor.hairline.opacity(0.8), lineWidth: 1))
    }

    private var relativeDateText: String {
        guard let lastConnected = machine.lastConnected else { return "Not connected yet" }
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .short
        return formatter.localizedString(for: lastConnected, relativeTo: Date())
    }
}

private struct DiscoveredRow: View {
    let peer: DiscoveredPeripheral
    let onPair: () -> Void

    var body: some View {
        Button(action: onPair) {
            HStack(spacing: 12) {
                Image(systemName: "antenna.radiowaves.left.and.right")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(CremaColor.crema)
                    .frame(width: 42, height: 48)
                    .background(CremaColor.crema.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
                VStack(alignment: .leading, spacing: 5) {
                    Text(PairedMachine.prettify(peer.name))
                        .font(CremaTypography.font(size: 14, weight: .semibold, design: .rounded))
                        .foregroundStyle(CremaColor.cream)
                    HStack(spacing: 6) {
                        Text(peer.name)
                            .font(CremaTypography.font(size: 9, design: .monospaced))
                            .lineLimit(1)
                        if let rssi = peer.rssi {
                            SignalBars(rssi: rssi)
                        }
                    }
                    .foregroundStyle(CremaColor.secondary)
                }
                Spacer(minLength: 4)
                Text("Pair")
                    .font(CremaTypography.font(size: 12, weight: .semibold, design: .rounded))
                    .foregroundStyle(CremaColor.onPrimary)
                    .padding(.horizontal, 16)
                    .frame(minHeight: 44)
                    .background(CremaColor.primarySurface, in: Capsule())
            }
            .padding(14)
            .background(CremaColor.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous)
                .strokeBorder(CremaColor.hairline.opacity(0.8), lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 22))
        }
        .buttonStyle(.plain)
        .accessibilityHint("Save and connect to this machine")
    }
}

private struct SignalBars: View {
    let rssi: Int

    private var filledBars: Int {
        switch rssi {
        case (-50)...: return 4
        case (-65)...: return 3
        case (-80)...: return 2
        default: return 1
        }
    }

    var body: some View {
        HStack(alignment: .bottom, spacing: 2) {
            ForEach(0..<4) { index in
                Capsule()
                    .fill(index < filledBars ? CremaColor.matcha : CremaColor.hairline)
                    .frame(width: 3, height: 3 + CGFloat(index) * 2)
            }
        }
        .frame(height: 9, alignment: .bottom)
        .accessibilityLabel("Bluetooth signal strength")
        .accessibilityValue("\(filledBars) of 4 bars")
    }
}

private struct SectionLabel: View {
    let text: String
    let count: Int
    init(_ text: String, count: Int) { self.text = text; self.count = count }

    var body: some View {
        HStack(spacing: 8) {
            Text(text)
                .font(CremaTypography.font(size: 13, weight: .semibold, design: .rounded))
                .foregroundStyle(CremaColor.cream)
            Text("\(count)")
                .font(CremaTypography.font(size: 10, weight: .semibold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(CremaColor.secondary)
                .padding(.horizontal, 7)
                .padding(.vertical, 3)
                .background(CremaColor.hairline.opacity(0.5), in: Capsule())
        }
    }
}

private struct EmptyHint: View {
    let title: String
    let text: String
    let systemImage: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: systemImage)
                .font(.system(size: 17, weight: .light))
                .foregroundStyle(CremaColor.secondary)
                .frame(width: 26)
            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(CremaTypography.font(size: 13, weight: .medium, design: .rounded))
                    .foregroundStyle(CremaColor.cream)
                Text(text)
                    .font(CremaTypography.font(size: 12, design: .rounded))
                    .foregroundStyle(CremaColor.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(CremaColor.surface.opacity(0.55), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous)
            .strokeBorder(CremaColor.hairline, style: StrokeStyle(lineWidth: 1, dash: [4, 4])))
    }
}
