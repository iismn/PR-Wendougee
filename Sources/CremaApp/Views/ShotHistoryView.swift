import SwiftUI
import CremaKit

/// Browse + manage past brews. List of `ShotLog` rows with timestamp, profile
/// name snapshot, yield delta, taste tags, and inline comments. Per-row delete
/// via the menu, footer "Clear all" for the nuclear option, and an inline
/// comment editor.
struct ShotHistoryView: View {
    @Bindable var history: ShotHistory
    @Environment(\.dismiss) private var dismiss

    @State private var pendingClearAll = false
    @State private var editingShotID: UUID?

    var body: some View {
        VStack(spacing: 0) {
            header
                .safeAreaPadding(.top)
            ScrollView {
                if history.isEmpty {
                    emptyState
                        .padding(.horizontal, 20)
                        .padding(.top, 24)
                } else {
                    LazyVStack(spacing: 8) {
                        ForEach(history.shots) { shot in
                            ShotHistoryRow(
                                shot: shot,
                                isEditing: editingShotID == shot.id,
                                onTapComments: {
                                    editingShotID = editingShotID == shot.id ? nil : shot.id
                                },
                                onCommitComments: { new in
                                    history.updateComments(shot.id, comments: new)
                                    editingShotID = nil
                                },
                                onDelete: { history.remove(shot.id) }
                            )
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
            }
            if !history.isEmpty {
                footer
            }
        }
        #if os(macOS)
        .frame(minWidth: 480, idealWidth: 560, minHeight: 540, idealHeight: 720)
        #endif
        .background(CremaColor.bg.ignoresSafeArea())
        .confirmationDialog(
            "Clear all shot history?",
            isPresented: $pendingClearAll
        ) {
            Button("Delete \(history.shots.count) shot\(history.shots.count == 1 ? "" : "s")",
                   role: .destructive) {
                history.clearAll()
                pendingClearAll = false
            }
            Button("Cancel", role: .cancel) { pendingClearAll = false }
        } message: {
            Text("This permanently removes every saved shot. The profiles themselves are kept.")
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            Text("Shot history")
                .font(CremaTypography.font(size: 18, weight: .semibold))
                .foregroundStyle(CremaColor.cream)
            Spacer()
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(CremaColor.crema)
                    .frame(width: 44, height: 44)
                    .background(Capsule().fill(CremaColor.surface))
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close brew journal")
            .help("Close brew journal")
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, 12)
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "clock.arrow.circlepath")
                .font(.system(size: 36, weight: .light))
                .foregroundStyle(CremaColor.secondary)
            Text("No saved shots")
                .font(CremaTypography.font(size: 16, weight: .medium))
                .foregroundStyle(CremaColor.cream)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 40)
        .frame(maxWidth: .infinity)
    }

    private var footer: some View {
        HStack {
            Button(role: .destructive, action: { pendingClearAll = true }) {
                Image(systemName: "trash")
                    .font(.system(size: 14, weight: .medium))
                .foregroundStyle(CremaColor.danger)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Delete all saved shots")
            .help("Delete all saved shots")
            Spacer()
            Text("\(history.shots.count) shot\(history.shots.count == 1 ? "" : "s")")
                .font(CremaTypography.font(size: 11, design: .rounded).monospacedDigit())
                .foregroundStyle(CremaColor.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(CremaColor.bg)
    }
}

// MARK: - Row

private struct ShotHistoryRow: View {
    let shot: ShotLog
    let isEditing: Bool
    let onTapComments: () -> Void
    let onCommitComments: (String) -> Void
    let onDelete: () -> Void

    @State private var draftComments: String = ""
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private static let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateStyle = .medium
        df.timeStyle = .short
        return df
    }()

    private var yieldText: String {
        if let m = shot.measuredYieldG {
            return String(format: "%.0f g", m)
        }
        return "—"
    }
    private var yieldAccent: Color {
        guard let m = shot.measuredYieldG, shot.targetYieldG > 0 else { return CremaColor.secondary }
        let pct = abs(m - shot.targetYieldG) / shot.targetYieldG
        if pct < 0.05 { return CremaColor.connected }   // on target
        if pct < 0.15 { return CremaColor.crema }       // close
        return CremaColor.danger                         // way off
    }
    private var tasteText: String {
        var tags: [String] = []
        if shot.taste.acidity <= -0.4 { tags.append("sour") }
        if shot.taste.acidity >=  0.4 { tags.append("bitter") }
        if shot.taste.strength <= -0.4 { tags.append("weak") }
        if shot.taste.strength >=  0.4 { tags.append("strong") }
        return tags.isEmpty ? "balanced" : tags.joined(separator: " · ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: 12) {
                    shotIdentity.frame(minWidth: 128)
                    shotStats.fixedSize(horizontal: true, vertical: false)
                    rowMenu
                }
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .top, spacing: 12) {
                        shotIdentity
                        Spacer(minLength: 0)
                        rowMenu
                    }
                    shotStats
                }
            }

            if isEditing {
                TextField("Add a note for this shot…", text: $draftComments, axis: .vertical)
                    .textFieldStyle(.plain)
                    .font(CremaTypography.font(size: 12, design: .rounded))
                    .foregroundStyle(CremaColor.cream)
                    .lineLimit(2...4)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(CremaColor.bg)
                    )
                HStack {
                    Spacer()
                    Button("Save note") { onCommitComments(draftComments) }
                        .font(CremaTypography.font(size: 12, weight: .semibold, design: .rounded))
                        .foregroundStyle(CremaColor.crema)
                        .padding(.horizontal, 10)
                        .frame(minHeight: 44)
                        .contentShape(Rectangle())
                        .buttonStyle(.plain)
                }
            } else if !shot.comments.isEmpty {
                Text(shot.comments)
                    .font(CremaTypography.font(size: 11))
                    .foregroundStyle(CremaColor.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(CremaColor.surface)
                .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(CremaColor.hairline, lineWidth: 0.5))
        )
        .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: isEditing)
        .onAppear { draftComments = shot.comments }
    }

    private var shotIdentity: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(shot.profileName)
                .font(CremaTypography.font(size: 14, weight: .semibold))
                .foregroundStyle(CremaColor.cream)
                .lineLimit(2)
            Text(Self.dateFormatter.string(from: shot.timestamp))
                .font(CremaTypography.font(size: 10))
                .foregroundStyle(CremaColor.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var shotStats: some View {
        HStack(alignment: .top, spacing: 16) {
            statTile(label: "Yield", value: yieldText,
                     subtitle: "Target \(Int(shot.targetYieldG)) g", accent: yieldAccent)
            statTile(label: "Time", value: "\(Int(shot.actualTimeS)) s",
                     subtitle: nil, accent: CremaColor.cream)
            statTile(label: "Taste", value: tasteText,
                     subtitle: nil, accent: CremaColor.matcha)
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
    }

    private var rowMenu: some View {
        Menu {
            Button("Edit comments", systemImage: "pencil", action: onTapComments)
            Divider()
            Button("Delete", systemImage: "trash", role: .destructive, action: onDelete)
        } label: {
            Image(systemName: "ellipsis")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(CremaColor.secondary)
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .accessibilityLabel("Options for \(shot.profileName)")
    }

    private func statTile(label: String, value: String, subtitle: String?, accent: Color) -> some View {
        VStack(alignment: .trailing, spacing: 4) {
            Text(label.uppercased())
                .font(CremaTypography.font(size: 8, weight: .semibold))
                .foregroundStyle(CremaColor.secondary)
            Text(value)
                .font(CremaTypography.font(size: 12, weight: .semibold).monospacedDigit())
                .foregroundStyle(accent)
                .fixedSize(horizontal: false, vertical: true)
            if let subtitle {
                Text(subtitle)
                    .font(CremaTypography.font(size: 9, design: .rounded).monospacedDigit())
                    .foregroundStyle(CremaColor.secondary)
            }
        }
    }
}
