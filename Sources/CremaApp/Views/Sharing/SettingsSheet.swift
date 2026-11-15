import SwiftUI
import CremaKit

/// Account + backend settings. Lives behind a gear-icon entry in the
/// ProfilesView footer (when signed in).
///
/// Sections:
/// - **Account** — current signed-in user, edit display name, sign out.
/// - **Backend** — the API base URL the app POSTs to. Defaults to
///   `http://localhost:8080`; users can point at their own deployment.
/// - **About** — version + build, link to repo.
///
/// Editing the backend URL takes effect immediately — the underlying
/// `ShareAPIClient` is an actor with an `updateBaseURL(_:)` setter.
struct SettingsSheet: View {
    @Bindable var session: SignedInUser
    let onDismiss: () -> Void

    @State private var editedName: String = ""
    @State private var savingName = false
    @State private var backendURLString: String = ""
    @State private var backendError: String?

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(spacing: 18) {
                    if session.isSignedIn { accountSection } else { signInPrompt }
                    backendSection
                    aboutSection
                }
                .padding(20)
            }
        }
        .background(CremaColor.bg.ignoresSafeArea())
        .onAppear {
            editedName = session.user?.displayName ?? ""
            // Read the current backend URL from defaults — synchronously,
            // since ShareAPIClient is an actor we'd otherwise need a hop.
            backendURLString = UserDefaults.standard.string(
                forKey: "crema.backend.url.override")
                ?? defaultBackendString
        }
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 5) {
                Text("YOUR COFFEE CORNER")
                    .font(CremaTypography.font(size: 9, weight: .semibold))
                    .tracking(1.8)
                    .foregroundStyle(CremaColor.crema)
                Text("Settings")
                    .font(CremaTypography.font(size: 30, weight: .regular, design: .serif))
                    .foregroundStyle(CremaColor.cream)
            }
            Spacer()
            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(CremaColor.secondary)
                    .frame(width: 44, height: 44)
                    .background(Circle().fill(CremaColor.surface))
            }
            .buttonStyle(.plain)
        }
        .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 8)
    }

    // MARK: Account

    private var accountSection: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionLabel("ACCOUNT")
            // Signed-in identity
            HStack(spacing: 12) {
                ZStack {
                    Circle().fill(CremaColor.crema.opacity(0.25))
                        .frame(width: 44, height: 44)
                    Image(systemName: "person.fill")
                        .font(.system(size: 18))
                        .foregroundStyle(CremaColor.crema)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(session.user?.displayName ?? "")
                        .font(CremaTypography.font(size: 15, weight: .semibold, design: .rounded))
                        .foregroundStyle(CremaColor.cream)
                    Text("Signed in")
                        .font(CremaTypography.font(size: 11, design: .rounded))
                        .foregroundStyle(CremaColor.secondary)
                }
                Spacer()
            }
            .padding(12)
            .background(card)

            // Display name editor
            VStack(alignment: .leading, spacing: 6) {
                Text("Display name")
                    .font(CremaTypography.font(size: 11, weight: .semibold, design: .rounded).smallCaps())
                    .tracking(0.6)
                    .foregroundStyle(CremaColor.secondary)
                HStack(spacing: 8) {
                    TextField("Name shown on shared profiles", text: $editedName)
                        .textFieldStyle(.plain)
                        .font(CremaTypography.font(size: 14, design: .rounded))
                        .foregroundStyle(CremaColor.cream)
                        .padding(.horizontal, 10).padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 8)
                            .fill(CremaColor.surfaceWarm))
                    if editedName.trimmingCharacters(in: .whitespaces)
                        != session.user?.displayName {
                        Button(action: saveName) {
                            HStack(spacing: 4) {
                                if savingName {
                                    ProgressView().controlSize(.small).tint(CremaColor.onAccent)
                                } else {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 11, weight: .bold))
                                }
                                Text("Save")
                                    .font(CremaTypography.font(size: 12, weight: .semibold, design: .rounded))
                            }
                            .foregroundStyle(CremaColor.onAccent)
                            .padding(.horizontal, 10).padding(.vertical, 7)
                            .background(Capsule().fill(CremaColor.crema))
                        }
                        .buttonStyle(.plain)
                        .disabled(savingName)
                    }
                }
            }

            // Sign out
            Button(role: .destructive, action: { session.signOut(); onDismiss() }) {
                HStack(spacing: 6) {
                    Image(systemName: "rectangle.portrait.and.arrow.right")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Sign out")
                        .font(CremaTypography.font(size: 13, weight: .semibold, design: .rounded))
                }
                .foregroundStyle(CremaColor.danger)
                .padding(.horizontal, 14).frame(minHeight: 38)
                .background(Capsule().fill(CremaColor.danger.opacity(0.15)))
            }
            .buttonStyle(.plain)
        }
    }

    private var signInPrompt: some View {
        VStack(alignment: .leading, spacing: 8) {
            sectionLabel("ACCOUNT")
            Text("Not signed in. Profile sharing + community features need an account.")
                .font(CremaTypography.font(size: 12, design: .rounded))
                .foregroundStyle(CremaColor.secondary)
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(card)
        }
    }

    // MARK: Backend

    private var backendSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("BACKEND")
            VStack(alignment: .leading, spacing: 6) {
                Text("Base URL")
                    .font(CremaTypography.font(size: 11, weight: .semibold, design: .rounded).smallCaps())
                    .tracking(0.6).foregroundStyle(CremaColor.secondary)
                HStack(spacing: 8) {
                    TextField("https://crema.coffee", text: $backendURLString)
                        .textFieldStyle(.plain)
                        .font(CremaTypography.font(size: 13, design: .rounded).monospacedDigit())
                        .foregroundStyle(CremaColor.cream)
                        #if os(iOS)
                        .keyboardType(.URL).autocapitalization(.none)
                        .disableAutocorrection(true)
                        #endif
                        .padding(.horizontal, 10).padding(.vertical, 8)
                        .background(RoundedRectangle(cornerRadius: 8)
                            .fill(CremaColor.surfaceWarm))
                    Button(action: applyBackendURL) {
                        Text("Apply")
                            .font(CremaTypography.font(size: 12, weight: .semibold, design: .rounded))
                            .foregroundStyle(CremaColor.onAccent)
                            .padding(.horizontal, 12).padding(.vertical, 7)
                            .background(Capsule().fill(CremaColor.matcha))
                    }
                    .buttonStyle(.plain)
                }
                if let backendError {
                    Text(backendError)
                        .font(CremaTypography.font(size: 11, design: .rounded))
                        .foregroundStyle(CremaColor.danger)
                }
                Text("Crema points here for uploads, downloads, and sign-in. Self-host via `docker compose up` in the `backend/` directory of the repo.")
                    .font(CremaTypography.font(size: 11, design: .rounded))
                    .foregroundStyle(CremaColor.secondary)
            }
        }
    }

    // MARK: About

    private var aboutSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            sectionLabel("ABOUT")
            VStack(alignment: .leading, spacing: 6) {
                row("Version", appVersion)
                row("Build", appBuild)
                row("Repository", "github.com/dallonby/Crema")
            }
            .padding(12)
            .background(card)
        }
    }

    private func row(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(CremaTypography.font(size: 12, design: .rounded))
                .foregroundStyle(CremaColor.secondary)
            Spacer()
            Text(value)
                .font(CremaTypography.font(size: 12, weight: .medium, design: .rounded).monospacedDigit())
                .foregroundStyle(CremaColor.cream)
        }
    }

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(CremaTypography.font(size: 11, weight: .semibold))
            .tracking(1.5)
            .foregroundStyle(CremaColor.crema)
    }

    private var card: some View {
        RoundedRectangle(cornerRadius: 20, style: .continuous)
            .fill(CremaColor.surface)
            .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous)
                .strokeBorder(CremaColor.hairline, lineWidth: 0.5))
    }

    // MARK: Actions

    private func saveName() {
        let trimmed = editedName.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        savingName = true
        Task {
            await session.updateDisplayName(trimmed)
            savingName = false
        }
    }

    private func applyBackendURL() {
        let trimmed = backendURLString.trimmingCharacters(in: .whitespaces)
        guard let url = URL(string: trimmed), url.scheme?.hasPrefix("http") == true else {
            backendError = "Needs to be a valid http(s) URL"
            return
        }
        backendError = nil
        UserDefaults.standard.set(trimmed, forKey: "crema.backend.url.override")
        Task { await session.client.updateBaseURL(url) }
    }

    // MARK: Helpers

    private var defaultBackendString: String {
        // Match the CremaApp default. Hardcoded mirror — keeps this view
        // self-contained.
        "https://api.caffecremalabs.com"
    }

    private var appVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
    }

    private var appBuild: String {
        Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
    }
}
