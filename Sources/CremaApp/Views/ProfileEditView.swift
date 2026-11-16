import SwiftUI
import CremaKit

/// Multi-stage profile editor. Top: live chart preview. Middle: recipe basics
/// (name, dose, yield, ratio). Bottom: an ordered list of stage cards — each
/// collapsible, each independently flow- or pressure-priority, each with its
/// own duration / setpoint / wait-after / label.
///
/// Stages can be added, deleted, and reordered via card menus. The chart on
/// top rebuilds on every edit so the curve reshapes in real time.
struct ProfileEditView: View {
    @Bindable var editing: EditableProfile
    let onSave: (BrewProfile) -> Void
    let onCancel: () -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    enum Mode: String, Hashable { case visual, list }
    @State private var mode: Mode = .visual
    @State private var selectedStageID: UUID?

    var body: some View {
        GeometryReader { geo in
            // Wide enough for a side-by-side layout? Triggers on iPad landscape
            // and macOS, falls back to stacked on iPhone / iPad portrait.
            let isWide = geo.size.width > 820 && mode == .visual
            VStack(spacing: 0) {
                header
                    .padding(.horizontal, 24)
                    .padding(.top, 16)
                    .padding(.bottom, 8)
                    .safeAreaPadding(.top)

                if isWide {
                    splitLayout
                } else {
                    stackedLayout
                }
            }
            .background(CremaColor.bg.ignoresSafeArea())
        }
        #if os(macOS)
        .frame(minWidth: 980, idealWidth: 1180, minHeight: 640, idealHeight: 760)
        #endif
        .onAppear {
            if selectedStageID == nil { selectedStageID = editing.stages.first?.id }
        }
    }

    /// Stacked vertical scroll — iPhone, iPad portrait, and List mode.
    private var stackedLayout: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                modePicker
                if mode == .visual {
                    visualEditor(showControls: true)
                } else {
                    preview
                }
                recipeSection
                grinderSection
                if mode == .list {
                    stagesSection
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 24)
        }
    }

    /// Landscape split — chart fills the left ~60%, controls scroll on the
    /// right ~40%. Only used in Visual mode where the chart is the hero.
    private var splitLayout: some View {
        HStack(alignment: .top, spacing: 24) {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    modePicker
                    visualEditor(showControls: false)
                    recipeSection
                    grinderSection
                }
                .padding(.leading, 16)
                .padding(.bottom, 24)
            }
            .frame(maxWidth: .infinity)

            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    stageSelector
                    selectedStageControls(compact: true)
                }
                .padding(.trailing, 16)
                .padding(.bottom, 24)
            }
            .frame(width: 400)
        }
    }

    private var stageSelector: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(editing.stages.enumerated()), id: \.element.id) { index, stage in
                    Button {
                        withAnimation(reduceMotion ? nil : .smooth(duration: 0.2)) { selectedStageID = stage.id }
                    } label: {
                        Text(String(format: "%02d", index + 1))
                            .font(CremaTypography.font(size: 12, weight: .medium))
                            .foregroundStyle(selectedStageID == stage.id ? CremaColor.onPrimary : CremaColor.secondary)
                            .frame(width: 48, height: 48)
                            .background(selectedStageID == stage.id ? CremaColor.primarySurface : CremaColor.surface,
                                        in: Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(stage.label.isEmpty ? "Stage \(index + 1)" : stage.label)
                    .accessibilityAddTraits(selectedStageID == stage.id ? .isSelected : [])
                }
            }
        }
    }

    private var modePicker: some View {
        Picker("Mode", selection: $mode) {
            Image(systemName: "chart.xyaxis.line")
                .accessibilityLabel("Visual editor").tag(Mode.visual)
            Image(systemName: "list.bullet")
                .accessibilityLabel("List editor").tag(Mode.list)
        }
        .pickerStyle(.segmented)
    }

    @ViewBuilder
    private func visualEditor(showControls: Bool) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 12) {
                Spacer()
                Button(action: {
                    editing.addStage()
                    selectedStageID = editing.stages.last?.id
                }) {
                    Image(systemName: "plus")
                        .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(CremaColor.crema)
                    .frame(width: 44, height: 44)
                    .background(
                        Capsule().fill(CremaColor.crema.opacity(0.16))
                            .overlay(Capsule().strokeBorder(CremaColor.crema.opacity(0.35), lineWidth: 0.5))
                    )
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Add stage")
                .help("Add stage")
            }
            ProfileNodeEditor(editing: editing, selectedStageID: $selectedStageID)
                .frame(height: showControls ? 280 : 320)
                .background(
                    RoundedRectangle(cornerRadius: 26, style: .continuous)
                        .fill(CremaColor.surface)
                        .overlay(RoundedRectangle(cornerRadius: 26, style: .continuous)
                            .strokeBorder(CremaColor.hairline.opacity(0.5), lineWidth: 0.5))
                )

            HStack(spacing: 14) {
                Label("Pressure", systemImage: "circle.fill").foregroundStyle(CremaColor.crema)
                Label("Flow", systemImage: "circle.fill").foregroundStyle(CremaColor.matcha)
                Spacer()
            }
            .font(CremaTypography.font(size: 10, weight: .medium))
            .padding(.horizontal, 6)

            if showControls {
                selectedStageControls(compact: false)
            }
        }
        .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: selectedStageID)
    }

    @ViewBuilder
    private func selectedStageControls(compact: Bool) -> some View {
        if let selected = editing.stages.first(where: { $0.id == selectedStageID }) {
            SelectedStageBar(stage: selected, canDelete: editing.canRemoveAnyStage,
                             onDelete: {
                                 editing.remove(stageID: selected.id)
                                 selectedStageID = editing.stages.first?.id
                             }, onDeselect: { selectedStageID = nil }, compact: compact)
        }
    }

    // MARK: - Header / preview / recipe

    private var header: some View {
        HStack {
            sheetHeaderButton("Cancel", primary: false) {
                onCancel(); dismiss()
            }
            Spacer()
            Text("Edit profile")
                .font(CremaTypography.font(size: 16, weight: .semibold))
                .foregroundStyle(CremaColor.cream)
            Spacer()
            sheetHeaderButton("Save", primary: true) {
                onSave(editing.compile())
                dismiss()
            }
        }
    }

    /// 44pt-tall hit target so taps reliably land — bare-text buttons in the
    /// header were "super flakey" (user report).
    @ViewBuilder
    private func sheetHeaderButton(_ label: String, primary: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: primary ? "checkmark" : "xmark")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(primary ? CremaColor.bg : CremaColor.secondary)
                .frame(width: 44, height: 44)
                .background(Capsule().fill(primary ? CremaColor.crema : CremaColor.surface))
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .help(label)
    }

    private var preview: some View {
        let compiled = editing.compile()
        return VStack(alignment: .leading, spacing: 6) {
            HStack {
                Spacer()
                Text(String(format: "%.0f s · %.0f mL target · %d stage\(compiled.stages.count == 1 ? "" : "s")",
                            compiled.totalDuration,
                            Double(compiled.targetVolumeMl ?? 0),
                            compiled.stages.count))
                    .font(CremaTypography.font(size: 11, design: .rounded).monospacedDigit())
                    .foregroundStyle(CremaColor.secondary)
            }
            ShotChart(
                samples: [][...],
                profile: compiled,
                playhead: -1,
                totalDuration: max(compiled.totalDuration, 1)
            )
            .frame(height: 200)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(CremaColor.surface)
                    .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .strokeBorder(CremaColor.hairline.opacity(0.5), lineWidth: 0.5))
            )
        }
    }

    private var recipeSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader("Recipe")
            EditorRow(label: "Name") {
                TextField("Profile name", text: $editing.name)
                    .textFieldStyle(.plain)
                    .font(CremaTypography.font(size: 15, weight: .medium, design: .rounded))
                    .foregroundStyle(CremaColor.cream)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 9)
                    .background(
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(CremaColor.surfaceWarm)
                    )
            }
            SliderRow(label: "Dose", value: $editing.doseG, range: 12...22, step: 0.5,
                      format: { String(format: "%.1f g", $0) }, accent: CremaColor.cream)
            // Yield range scales with dose: 1:1 ristretto to 1:10 long shot.
            // Dynamic max so heavier doses can reach correspondingly heavier yields
            // (22 g dose → up to 220 g yield) without hitting an arbitrary cap.
            SliderRow(label: "Target yield", value: $editing.targetYieldG,
                      range: editing.doseG ... editing.doseG * 10, step: 1,
                      format: { String(format: "%.0f g", $0) }, accent: CremaColor.cream)
                // Auto-clamp yield into the new range when dose changes — otherwise
                // a previously-set yield can sit outside the slider's bounds, leaving
                // the thumb visually pegged until the user touches it.
                .onChange(of: editing.doseG) { _, newDose in
                    let lo = newDose
                    let hi = newDose * 10
                    editing.targetYieldG = min(hi, max(lo, editing.targetYieldG))
                }
            Text(String(format: "Ratio 1 : %.2f", editing.targetYieldG / max(editing.doseG, 1)))
                .font(CremaTypography.font(size: 11, design: .rounded).monospacedDigit())
                .foregroundStyle(CremaColor.secondary)
        }
    }

    // MARK: - Grinder section

    private var grinderSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                SectionHeader("Grinder")
                Spacer()
                Toggle("", isOn: $editing.grinderEnabled)
                    .labelsHidden()
                    .tint(CremaColor.crema)
                    .accessibilityLabel("Save grinder settings")
            }
            if editing.grinderEnabled {
                VStack(alignment: .leading, spacing: 12) {
                    SliderRow(label: "Grind size",
                              value: $editing.grinderSizeMicrons,
                              range: 30...500, step: 1,
                              format: { String(format: "%.0f µm", $0) },
                              accent: CremaColor.crema)
                    SliderRow(label: "Motor speed",
                              value: $editing.grinderRPM,
                              range: 200...1200, step: 10,
                              format: { String(format: "%.0f rpm", $0) },
                              accent: CremaColor.matcha)
                    HStack {
                        Toggle("Single-dose mode", isOn: $editing.grinderSingleDose)
                            .font(CremaTypography.font(size: 13, weight: .medium, design: .rounded))
                            .foregroundStyle(CremaColor.cream)
                            .tint(CremaColor.crema)
                        Spacer()
                    }
                }
                .transition(reduceMotion ? .opacity : .opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    // MARK: - Stages section

    @ViewBuilder
    private var stagesSection: some View {
        HStack {
            SectionHeader("Stages")
            Spacer()
            Button(action: { editing.addStage() }) {
                HStack(spacing: 4) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 12, weight: .semibold))
                    Text("Add stage")
                        .font(CremaTypography.font(size: 12, weight: .semibold, design: .rounded))
                }
                .foregroundStyle(CremaColor.crema)
            }
            .buttonStyle(.plain)
        }

        VStack(spacing: 8) {
            ForEach(Array(editing.stages.enumerated()), id: \.element.id) { index, stage in
                StageCard(
                    stage: stage,
                    index: index,
                    stageCount: editing.stages.count,
                    canDelete: editing.canRemoveAnyStage,
                    onMoveUp:   { editing.moveStage(id: stage.id, by: -1) },
                    onMoveDown: { editing.moveStage(id: stage.id, by:  1) },
                    onDelete:   { editing.remove(stageID: stage.id) }
                )
            }
        }
    }
}

// MARK: - Stage card

private struct StageCard: View {
    @Bindable var stage: EditableStage
    let index: Int
    let stageCount: Int
    let canDelete: Bool
    let onMoveUp: () -> Void
    let onMoveDown: () -> Void
    let onDelete: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var accent: Color {
        stage.priority == .pressure ? CremaColor.crema : CremaColor.matcha
    }

    private var summaryText: String {
        let value = stage.priority == .pressure
            ? String(format: "%.1f bar", stage.pressureBar)
            : String(format: "%.1f mL/s", stage.flowMlPerSec)
        let main = "\(value) · \(stage.duration.formatted(.number.precision(.fractionLength(0...1)))) s"
        if stage.waitAfter > 0 {
            return main + " + \(stage.waitAfter.formatted(.number.precision(.fractionLength(0...1)))) s wait"
        }
        return main
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            collapsedHeader
            if stage.isExpanded {
                Divider()
                    .overlay(CremaColor.hairline.opacity(0.5))
                expandedBody
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(CremaColor.surface)
        )
        .overlay(alignment: .leading) {
            // Colored left edge indicates priority at a glance.
            RoundedRectangle(cornerRadius: 2, style: .continuous)
                .fill(accent)
                .frame(width: 4)
                .padding(.vertical, 6)
                .padding(.leading, 2)
        }
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .strokeBorder(CremaColor.hairline.opacity(0.5), lineWidth: 0.5)
        )
        .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: stage.isExpanded)
    }

    private var collapsedHeader: some View {
        // Not a Button — using onTapGesture so the inner Menu can capture
        // its own taps without nested-Button hit-test conflicts (the Menu
        // would swallow the row's tap otherwise on iOS).
        HStack(spacing: 12) {
            Text("\(index + 1)")
                .font(CremaTypography.font(size: 10, weight: .semibold, design: .rounded).monospacedDigit())
                .foregroundStyle(CremaColor.secondary)
                .frame(width: 16)
                .padding(.leading, 8)

            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(stage.label.isEmpty ? "Stage \(index + 1)" : stage.label)
                        .font(CremaTypography.font(size: 14, weight: .semibold, design: .rounded))
                        .foregroundStyle(CremaColor.cream)
                        .lineLimit(1)
                    Text(stage.priority == .pressure ? "PRESSURE" : "FLOW")
                        .font(CremaTypography.font(size: 9, weight: .semibold, design: .rounded).smallCaps())
                        .tracking(0.6)
                        .foregroundStyle(accent)
                }
                Text(summaryText)
                    .font(CremaTypography.font(size: 11, design: .rounded).monospacedDigit())
                    .foregroundStyle(CremaColor.secondary)
                    .lineLimit(1)
            }
            .contentShape(Rectangle())
            .onTapGesture { stage.isExpanded.toggle() }

            Spacer(minLength: 0)
                .contentShape(Rectangle())
                .onTapGesture { stage.isExpanded.toggle() }

            Menu {
                if index > 0 {
                    Button("Move up", systemImage: "arrow.up", action: onMoveUp)
                }
                if index < stageCount - 1 {
                    Button("Move down", systemImage: "arrow.down", action: onMoveDown)
                }
                if canDelete {
                    Divider()
                    Button("Delete stage", systemImage: "trash", role: .destructive, action: onDelete)
                }
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
            .accessibilityLabel("Options for stage \(index + 1)")

            Image(systemName: "chevron.right")
                .font(.system(size: 11, weight: .semibold))
                .foregroundStyle(CremaColor.secondary)
                .rotationEffect(.degrees(stage.isExpanded ? 90 : 0))
                .frame(width: 32, height: 44)
                .contentShape(Rectangle())
                .onTapGesture { stage.isExpanded.toggle() }
                .animation(reduceMotion ? nil : .easeOut(duration: 0.18), value: stage.isExpanded)
        }
        .padding(.vertical, 8)
        .padding(.trailing, 4)
    }

    private var expandedBody: some View {
        VStack(alignment: .leading, spacing: 14) {
            // Priority segmented picker — one of the two key controls.
            Picker("Priority", selection: $stage.priority) {
                Text("Pressure").tag(BrewStage.Priority.pressure)
                Text("Flow").tag(BrewStage.Priority.flow)
            }
            .pickerStyle(.segmented)

            // The rotary setpoint follows the stage's active control axis.
            // We always render the active one so the row doesn't reshape on
            // priority toggle.
            if stage.priority == .pressure {
                CremaDialControl(value: $stage.pressureBar, range: 0...9, step: 0.1,
                                 label: "Pressure", unit: "bar", accent: CremaColor.crema)
            } else {
                CremaDialControl(value: $stage.flowMlPerSec, range: 0.1...8, step: 0.1,
                                 label: "Pump flow", unit: "mL/s", accent: CremaColor.matcha)
            }

            CremaRulerControl(value: $stage.duration, range: 1...60, step: 0.5,
                              label: "Duration", unit: "s")

            CremaRulerControl(value: $stage.waitAfter, range: 0...30, step: 0.5,
                              label: "Pause after", unit: "s", accent: CremaColor.matcha)

            EditorRow(label: "Label") {
                TextField("Stage label", text: $stage.label)
                    .textFieldStyle(.plain)
                    .font(CremaTypography.font(size: 14, design: .rounded))
                    .foregroundStyle(CremaColor.cream)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 7)
                    .background(
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(CremaColor.surfaceWarm)
                    )
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }
}

// MARK: - Reusable bits

private struct SectionHeader: View {
    let text: String
    init(_ text: String) { self.text = text }
    var body: some View {
        Text(text)
            .font(CremaTypography.font(size: 20, weight: .regular, design: .serif))
            .foregroundStyle(CremaColor.cream)
            .padding(.top, 4)
    }
}

private struct SliderRow: View {
    let label: String
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let format: (Double) -> String
    let accent: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(label)
                    .font(CremaTypography.font(size: 13, weight: .medium, design: .rounded))
                    .foregroundStyle(CremaColor.cream)
                Spacer()
                Text(format(value))
                    .font(CremaTypography.font(size: 17, weight: .medium, design: .rounded).monospacedDigit())
                    .foregroundStyle(accent)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(CremaColor.bg))
            }
            Slider(value: $value, in: range, step: step)
                .tint(accent)
        }
    }
}

private struct EditorRow<Content: View>: View {
    let label: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label)
                .font(CremaTypography.font(size: 11, weight: .semibold, design: .rounded).smallCaps())
                .tracking(0.6)
                .foregroundStyle(CremaColor.secondary)
            content()
        }
    }
}
