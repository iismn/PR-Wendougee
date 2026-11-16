import SwiftUI
import CremaKit

/// Profile library browser — list of user profiles with mini curve previews.
/// Tap to activate, swipe to delete, edit to open the guided editor, +
/// button to create a new one.
struct ProfilesView: View {
    @Bindable var library: ProfileLibrary
    /// Optional — when present, enables Share button per row + Browse button
    /// in the footer. Omitted on screens where sharing isn't relevant
    /// (currently always passed, but the optionality keeps tests / previews
    /// simple).
    var session: SignedInUser?
    @Environment(\.dismiss) private var dismiss

    @State private var editingProfile: EditableProfile?
    @State private var newProfile: EditableProfile?
    @State private var pendingDelete: BrewProfile?
    @State private var sharingProfile: BrewProfile?
    @State private var showBrowse = false
    @State private var showSettings = false

    var body: some View {
        VStack(spacing: 0) {
            header
                .safeAreaPadding(.top)   // iPhone Dynamic Island / notch
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(library.profiles) { profile in
                        ProfileRow(
                            profile: profile,
                            isActive: profile.id == library.activeProfileID,
                            canShare: session != nil,
                            onTap: {
                                library.setActive(profile.id)
                                dismiss()
                            },
                            onEdit: {
                                editingProfile = EditableProfile.from(profile)
                            },
                            onShare: session == nil ? nil : {
                                sharingProfile = profile
                            },
                            onDelete: library.profiles.count > 1 ? {
                                pendingDelete = profile
                            } : nil
                        )
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
            }
            footer
        }
        #if os(macOS)
        .frame(minWidth: 460, minHeight: 540, idealHeight: 720)
        #endif
        .background(CremaColor.bg.ignoresSafeArea())
        .modifier(EditorPresentation(item: $editingProfile) { editing in
            ProfileEditView(
                editing: editing,
                onSave: { library.update($0) },
                onCancel: {}
            )
        })
        .modifier(EditorPresentation(item: $newProfile) { editing in
            ProfileEditView(
                editing: editing,
                onSave: { library.add($0, setActive: true) },
                onCancel: {}
            )
        })
        .sheet(item: $sharingProfile) { profile in
            if let session {
                ShareProfileSheet(profile: profile, session: session,
                                   onDismiss: { sharingProfile = nil })
            }
        }
        .sheet(isPresented: $showBrowse) {
            if let session {
                BrowseCommunitySheet(session: session, library: library,
                                       onDismiss: { showBrowse = false })
            }
        }
        .sheet(isPresented: $showSettings) {
            if let session {
                SettingsSheet(session: session,
                                onDismiss: { showSettings = false })
            }
        }
        .confirmationDialog(
            "Delete this profile?",
            isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } }
            ),
            presenting: pendingDelete
        ) { profile in
            Button("Delete \"\(profile.name)\"", role: .destructive) {
                library.remove(profile.id)
                pendingDelete = nil
            }
            Button("Cancel", role: .cancel) { pendingDelete = nil }
        }
    }

    // MARK: - Header / footer

    private var header: some View {
        HStack(spacing: 12) {
            Text("Profiles")
                .font(CremaTypography.font(size: 28, weight: .regular))
                .tracking(-0.8)
                .foregroundStyle(CremaColor.cream)
            Spacer()
            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundStyle(CremaColor.cream)
                    .frame(width: 44, height: 44)
                    .background(Capsule().fill(CremaColor.surface))
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Close recipe library")
            .help("Close recipe library")
        }
        .padding(.horizontal, 24)
        .padding(.top, 24)
        .padding(.bottom, 12)
    }

    private var footer: some View {
        HStack(spacing: 10) {
            Button(action: { newProfile = EditableProfile() }) {
                Image(systemName: "plus")
                    .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(CremaColor.onPrimary)
                .frame(width: 44, height: 44)
                .background(
                    Capsule().fill(CremaColor.primarySurface)
                )
            }
            .buttonStyle(.plain)
            .accessibilityLabel("New recipe")
            .help("New recipe")
            Spacer()
            if session != nil {
                Button(action: { showBrowse = true }) {
                    Image(systemName: "globe")
                        .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(CremaColor.matcha)
                    .frame(width: 44, height: 44)
                    .background(
                        Capsule().fill(CremaColor.matcha.opacity(0.16))
                            .overlay(Capsule().strokeBorder(CremaColor.matcha.opacity(0.35), lineWidth: 0.5))
                    )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Community recipes")
                .help("Community recipes")
                Button(action: { showSettings = true }) {
                    Image(systemName: "gearshape.fill")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(CremaColor.secondary)
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Settings")
                .help("Settings")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(CremaColor.bg)
    }
}

// MARK: - Editor presentation

/// Presents the profile editor full-screen on iOS (the landscape split layout
/// needs the full screen width — a form sheet caps at ~540pt and boxes the
/// chart in) and as a regular sheet on macOS.
private struct EditorPresentation<Item: Identifiable, Sheet: View>: ViewModifier {
    @Binding var item: Item?
    @ViewBuilder var sheetContent: (Item) -> Sheet

    func body(content: Content) -> some View {
        #if os(iOS)
        content.fullScreenCover(item: $item, content: sheetContent)
        #else
        content.sheet(item: $item, content: sheetContent)
        #endif
    }
}

// MARK: - Row

private struct ProfileRow: View {
    let profile: BrewProfile
    let isActive: Bool
    let canShare: Bool
    let onTap: () -> Void
    let onEdit: () -> Void
    let onShare: (() -> Void)?
    let onDelete: (() -> Void)?

    var body: some View {
        HStack(spacing: 8) {
            Button(action: onTap) {
                HStack(spacing: 12) {
                    ProfilePreviewMini(profile: profile)
                        .frame(width: 86, height: 60)
                        .background(CremaColor.bg,
                                    in: RoundedRectangle(cornerRadius: 15))
                        .accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 4) {
                        HStack(spacing: 6) {
                            if isActive {
                                Image(systemName: "checkmark.circle.fill")
                                    .font(.system(size: 12))
                                    .foregroundStyle(CremaColor.crema)
                                    .accessibilityHidden(true)
                            }
                            Text(profile.name)
                                .font(CremaTypography.font(size: 16, weight: .medium))
                                .foregroundStyle(CremaColor.cream)
                                .lineLimit(2)
                        }
                        Text(metaLine)
                            .font(CremaTypography.font(size: 11).monospacedDigit())
                            .foregroundStyle(CremaColor.secondary)
                            .lineLimit(3)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(ProfilePressStyle())
            .accessibilityLabel("\(profile.name), \(metaLine)\(isActive ? ", selected" : "")")
            .accessibilityAddTraits(isActive ? .isSelected : [])

            Button(action: onEdit) {
                Image(systemName: "slider.horizontal.3")
                    .font(.system(size: 15, weight: .medium))
                    .foregroundStyle(CremaColor.secondary)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(ProfilePressStyle())
            .accessibilityLabel("Edit \(profile.name)")
            .help("Edit profile")
            rowMenu
        }
        .padding(14)
        .frame(minHeight: 110)
        .background(RoundedRectangle(cornerRadius: 24).fill(CremaColor.surface))
        .overlay(RoundedRectangle(cornerRadius: 24)
            .strokeBorder(isActive ? CremaColor.matcha.opacity(0.5) : Color.clear, lineWidth: 1))
    }

    private var rowMenu: some View {
        Menu {
            Button("Edit", systemImage: "slider.horizontal.3", action: onEdit)
            if let onShare {
                Button("Share…", systemImage: "square.and.arrow.up", action: onShare)
            }
            if let onDelete {
                Button("Delete", systemImage: "trash", role: .destructive, action: onDelete)
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(CremaColor.secondary)
                .frame(width: 44, height: 44)
                .background(Circle().fill(CremaColor.bg))
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .accessibilityLabel("Recipe options")
    }

    private var metaLine: String {
        var bits: [String] = []
        if profile.target == .weight, let target = profile.targetWeightG {
            bits.append("\(target) g target")
        } else if let target = profile.targetVolumeMl {
            bits.append("\(target) mL target")
        }
        bits.append(String(format: "%.0f s", profile.totalDuration))
        bits.append("\(profile.stages.count) stage\(profile.stages.count == 1 ? "" : "s")")
        return bits.joined(separator: " · ")
    }
}

private struct ProfilePressStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.75 : 1)
            .scaleEffect(reduceMotion ? 1 : (configuration.isPressed ? 0.99 : 1))
            .animation(reduceMotion ? nil : .easeOut(duration: 0.15), value: configuration.isPressed)
    }
}

/// Tiny ghost-only chart preview — same `ShotChart` view in compact mode
/// (no axis labels, no target text, no margins) so the curves fill the frame
/// cleanly even at ~110×64.
private struct ProfilePreviewMini: View {
    let profile: BrewProfile

    var body: some View {
        ShotChart(
            samples: [][...],
            profile: profile,
            playhead: -1,
            totalDuration: max(profile.totalDuration, 1),
            compact: true
        )
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }
}
