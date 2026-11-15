import SwiftUI
import CremaKit

/// Visual node editor — the centerpiece chart with one draggable node per stage.
/// Drag horizontally to change a stage's duration. Drag vertically to change its
/// setpoint (interpreted as pressure or flow per the stage's priority). Tap an
/// empty area of the chart to add a new stage. Tap a node to select it and
/// reveal the per-stage controls (priority toggle, pause-after, delete).
///
/// Lives alongside the list editor; both bind to the same `EditableProfile` so
/// flipping between them is instant.
struct ProfileNodeEditor: View {
    @Bindable var editing: EditableProfile
    @Binding var selectedStageID: UUID?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var draggingStageID: UUID?
    /// Per-stage drag origin captured ONCE at gesture start. Includes the
    /// starting duration so we don't read mutating state during the drag —
    /// reading `stage.duration` mid-drag created a positive-feedback loop
    /// where every tick re-inflated the next (10 px of slow drag could
    /// snowball into 45 s of movement).
    @State private var dragOrigin: (duration: Double, value: Double)?
    /// Locked `totalDuration` snapshot taken at drag start. Used to keep the
    /// chart's horizontal scale STABLE during a drag — otherwise the chart
    /// rescales under your finger as the dragged stage grows/shrinks, which
    /// feels chaotic ("expand and contract far too quickly").
    @State private var dragLockedTotalT: Double?

    /// Axis ceilings — fixed so dragging doesn't get "chased" by a rescaling
    /// axis underneath.
    private let maxPressure: Double = 9.0
    private let maxFlow: Double = 8.0
    private let nodeRadius: CGFloat = 11
    /// Gesture damping. 1.0 = 1:1 — finger movement maps directly to time/
    /// value change in the chart's coordinate scale. Earlier values were
    /// lower to compensate for a compounding-feedback bug (each tick read
    /// the just-updated stage.duration to back-calculate priorTime, which
    /// re-inflated the next tick); with that bug fixed in onChanged a
    /// straight 1:1 mapping feels tactile and predictable.
    private let dragGain: Double = 1.0

    var body: some View {
        GeometryReader { geo in
            let plot = CGRect(x: 4, y: 12, width: geo.size.width - 8, height: geo.size.height - 28)
            // While dragging, freeze the horizontal scale so the chart geometry
            // doesn't shift under the user's finger.
            let totalT = max(dragLockedTotalT ?? editing.totalDuration, 1)

            ZStack {
                // Backdrop: faint grid + axis ticks (no labels — keep the
                // surface clean since the user is interacting with shapes).
                Canvas { ctx, _ in
                    drawGrid(ctx: ctx, in: plot)
                    drawWaitGaps(ctx: ctx, in: plot, totalT: totalT)
                    drawCurve(ctx: ctx, in: plot, totalT: totalT)
                }
                .allowsHitTesting(false)

                // Background catches taps to *deselect* only — adding stages
                // is an explicit action via the "Add stage" button (tap-to-add
                // on dead space was too easy to trigger accidentally).
                Color.clear
                    .contentShape(Rectangle())
                    .onTapGesture { _ in
                        if selectedStageID != nil { selectedStageID = nil }
                    }

                // Stage nodes — one per stage, positioned at the stage's end
                // (cumulative time, setpoint y) coordinate.
                ForEach(stagePositions(plot: plot, totalT: totalT), id: \.stage.id) { item in
                    StageNode(
                        priority: item.stage.priority,
                        isSelected: item.stage.id == selectedStageID,
                        isDragging: item.stage.id == draggingStageID,
                        radius: nodeRadius
                    )
                    .position(item.point)
                    .gesture(dragGesture(for: item.stage, plot: plot, totalT: totalT))
                    .onTapGesture {
                        selectedStageID = (selectedStageID == item.stage.id) ? nil : item.stage.id
                    }
                }

                // Floating value pill above the active node — only when dragging
                // or selected.
                if let item = activeStageItem(plot: plot, totalT: totalT) {
                    ValuePill(stage: item.stage)
                        .position(x: item.point.x, y: max(28, item.point.y - 30))
                        .allowsHitTesting(false)
                        .transition(reduceMotion ? .opacity : .opacity.combined(with: .scale(scale: 0.85)))
                }
            }
            .animation(reduceMotion ? nil : .smooth(duration: 0.25), value: selectedStageID)
            .animation(reduceMotion ? nil : .smooth(duration: 0.18), value: editing.stages.map(\.id))
            // Clip so a stage dragged past the chart's right edge can't bleed
            // visually into the recipe pane next to us in the landscape split.
            // Matches the editor card's outer corner radius (set in
            // ProfileEditView.visualEditor).
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
    }

    // MARK: - Geometry

    private struct Positioned {
        let stage: EditableStage
        let point: CGPoint
    }

    /// For each stage, its node's `(x, y)` position — x at the cumulative
    /// time at the stage's *end*, y at the stage's active setpoint.
    private func stagePositions(plot: CGRect, totalT: Double) -> [Positioned] {
        var cursor: Double = 0
        return editing.stages.map { s in
            let end = cursor + s.duration
            let x = plot.minX + plot.width * CGFloat(end / max(totalT, 0.001))
            let y = yFor(stage: s, plot: plot)
            cursor = end + s.waitAfter
            return Positioned(stage: s, point: CGPoint(x: x, y: y))
        }
    }

    private func yFor(stage: EditableStage, plot: CGRect) -> CGFloat {
        switch stage.priority {
        case .pressure:
            let f = max(0, min(1, stage.pressureBar / maxPressure))
            return plot.maxY - plot.height * CGFloat(f)
        case .flow:
            let f = max(0, min(1, stage.flowMlPerSec / maxFlow))
            return plot.maxY - plot.height * CGFloat(f)
        }
    }

    private func activeStageItem(plot: CGRect, totalT: Double) -> Positioned? {
        let id = draggingStageID ?? selectedStageID
        guard let id else { return nil }
        return stagePositions(plot: plot, totalT: totalT).first(where: { $0.stage.id == id })
    }

    // MARK: - Drag gesture

    private func dragGesture(for stage: EditableStage, plot: CGRect, totalT: Double) -> some Gesture {
        // CRITICAL: use `.global` coordinate space. The StageNode this gesture
        // is attached to is positioned via `.position(item.point)` driven by
        // the stage's data — when we update stage.duration in onChanged, the
        // node moves, and a default-coordinate-space gesture measures
        // translation relative to the moving view, creating a positive-feedback
        // loop. Global coords are anchored to the screen so translation stays
        // stable regardless of how the view underneath moves.
        //
        // Higher minimumDistance prevents micro-jitter from registering as a
        // drag (8pt ≈ a thumb-rest twitch on iPad).
        DragGesture(minimumDistance: 8, coordinateSpace: .global)
            .onChanged { value in
                if draggingStageID != stage.id {
                    draggingStageID = stage.id
                    selectedStageID = stage.id
                    // Capture starting STATE once. All deltas are computed
                    // from these stable values + the gesture's cumulative
                    // translation. Never re-read stage.* during the drag —
                    // that's what created the runaway feedback loop.
                    dragOrigin = (
                        duration: stage.duration,
                        value: stage.priority == .pressure ? stage.pressureBar : stage.flowMlPerSec
                    )
                    dragLockedTotalT = max(editing.totalDuration, 1)
                }
                guard let origin = dragOrigin else { return }
                let lockedT = dragLockedTotalT ?? totalT

                // Vertical drag → setpoint. dragGain dampens twitchiness.
                let dy = value.translation.height
                let valueRange = stage.priority == .pressure ? maxPressure : maxFlow
                let valueDelta = -Double(dy) / Double(plot.height) * valueRange * dragGain
                var newValue = origin.value + valueDelta
                // Coarser snap = more tactile, less jittery.
                let snap = stage.priority == .pressure ? 0.25 : 0.25
                newValue = (newValue / snap).rounded() * snap
                newValue = max(0, min(valueRange, newValue))

                // Horizontal drag → duration. cumulative translation from
                // gesture start (.global coord space below) → time, added to
                // captured starting duration. No reading of stage.duration.
                let dx = value.translation.width
                let timeDelta = Double(dx) / Double(plot.width) * lockedT * dragGain
                var newDuration = origin.duration + timeDelta
                // 1.0s snap reads as deliberate without feeling sticky.
                newDuration = max(0.5, min(60, (newDuration / 1.0).rounded() * 1.0))

                // Apply.
                stage.duration = newDuration
                if stage.priority == .pressure {
                    stage.pressureBar = newValue
                } else {
                    stage.flowMlPerSec = newValue
                }
            }
            .onEnded { _ in
                draggingStageID = nil
                dragOrigin = nil
                dragLockedTotalT = nil
            }
    }

    private func cumulativeTimeAtEnd(of stage: EditableStage) -> Double {
        var t: Double = 0
        for s in editing.stages {
            t += s.duration
            if s.id == stage.id { return t }
            t += s.waitAfter
        }
        return t
    }

    // MARK: - Canvas drawing helpers

    private func drawGrid(ctx: GraphicsContext, in r: CGRect) {
        var path = Path()
        // Horizontal: 4 divisions
        for i in 0...4 {
            let y = r.minY + r.height * CGFloat(i) / 4
            path.move(to: CGPoint(x: r.minX, y: y))
            path.addLine(to: CGPoint(x: r.maxX, y: y))
        }
        ctx.stroke(path, with: .color(CremaColor.hairline.opacity(0.35)),
                   style: StrokeStyle(lineWidth: 0.5))
    }

    private func drawWaitGaps(ctx: GraphicsContext, in r: CGRect, totalT: Double) {
        var cursor: Double = 0
        for stage in editing.stages {
            cursor += stage.duration
            if stage.waitAfter > 0 {
                let x0 = r.minX + r.width * CGFloat(cursor / totalT)
                let x1 = r.minX + r.width * CGFloat((cursor + stage.waitAfter) / totalT)
                let rect = CGRect(x: x0, y: r.minY, width: x1 - x0, height: r.height)
                ctx.fill(Path(rect), with: .color(CremaColor.waitGray.opacity(0.18)))
            }
            cursor += stage.waitAfter
        }
    }

    /// Draws the profile curve as horizontal segments per stage, in the stage's
    /// priority color. Same "ghost target" rendering as ShotChart's drawGhostProfile.
    private func drawCurve(ctx: GraphicsContext, in r: CGRect, totalT: Double) {
        var cursor: Double = 0
        for stage in editing.stages {
            let endT = cursor + stage.duration
            let x0 = r.minX + r.width * CGFloat(cursor / totalT)
            let x1 = r.minX + r.width * CGFloat(endT / totalT)
            let y = yFor(stage: stage, plot: r)
            var p = Path()
            p.move(to: CGPoint(x: x0, y: y))
            p.addLine(to: CGPoint(x: x1, y: y))
            let color = stage.priority == .pressure ? CremaColor.crema : CremaColor.matcha
            ctx.stroke(p, with: .color(color.opacity(0.85)),
                       style: StrokeStyle(lineWidth: 5, lineCap: .round, lineJoin: .round))
            cursor = endT + stage.waitAfter
        }
    }
}

// MARK: - Node handle

private struct StageNode: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let priority: BrewStage.Priority
    let isSelected: Bool
    let isDragging: Bool
    let radius: CGFloat

    private var color: Color {
        priority == .pressure ? CremaColor.crema : CremaColor.matcha
    }

    var body: some View {
        ZStack {
            // A quiet selection ring keeps the editing handle distinct on porcelain.
            Circle()
                .fill(color.opacity(isSelected || isDragging ? 0.16 : 0.06))
                .frame(width: (isSelected || isDragging) ? radius * 4 : radius * 2.4,
                       height: (isSelected || isDragging) ? radius * 4 : radius * 2.4)
                .blur(radius: isSelected || isDragging ? 3 : 0)
            // Outer ring
            Circle()
                .stroke(color.opacity(isSelected ? 1.0 : 0.7), lineWidth: isSelected ? 2 : 1)
                .frame(width: radius * 2.2, height: radius * 2.2)
            // A surface-colored center separates the handle from the curve behind it.
            Circle()
                .fill(CremaColor.surface)
                .frame(width: radius * 1.6, height: radius * 1.6)
            Circle()
                .fill(color)
                .frame(width: radius, height: radius)
        }
        .scaleEffect(isDragging && !reduceMotion ? 1.15 : 1.0)
        .animation(reduceMotion ? nil : .snappy(duration: 0.18), value: isSelected)
        .animation(reduceMotion ? nil : .snappy(duration: 0.12), value: isDragging)
        .frame(width: radius * 4, height: radius * 4)        // tap target
        .contentShape(Circle())
    }
}

// MARK: - Floating value pill

private struct ValuePill: View {
    let stage: EditableStage

    private var text: String {
        let value = stage.priority == .pressure
            ? String(format: "%.1f bar", stage.pressureBar)
            : String(format: "%.1f mL/s", stage.flowMlPerSec)
        return "\(value)  ·  \(stage.duration.formatted(.number.precision(.fractionLength(0...1)))) s"
    }
    private var accent: Color {
        stage.priority == .pressure ? CremaColor.crema : CremaColor.matcha
    }

    var body: some View {
        Text(text)
            .font(CremaTypography.font(size: 11, weight: .semibold, design: .rounded).monospacedDigit())
            .foregroundStyle(CremaColor.onAccent)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Capsule().fill(accent))
            .overlay(Capsule().strokeBorder(CremaColor.cream.opacity(0.15), lineWidth: 0.5))
            .shadow(color: accent.opacity(0.15), radius: 5, x: 0, y: 2)
    }
}

// MARK: - Selected-stage action bar (priority toggle / delete)

/// Floating bar that appears under the chart when a node is selected. Holds
/// the priority toggle + pause-after slider + delete button — the actions
/// that don't map directly to a drag gesture.
struct SelectedStageBar: View {
    @Bindable var stage: EditableStage
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    let canDelete: Bool
    let onDelete: () -> Void
    let onDeselect: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 10) {
                Text(stage.label.isEmpty ? "Stage" : stage.label)
                    .font(CremaTypography.font(size: 13, weight: .semibold, design: .rounded))
                    .foregroundStyle(CremaColor.cream)
                Spacer()
                Picker("Priority", selection: $stage.priority) {
                    Text("Pressure").tag(BrewStage.Priority.pressure)
                    Text("Flow").tag(BrewStage.Priority.flow)
                }
                .pickerStyle(.segmented)
                .frame(maxWidth: 200)
            }

            ViewThatFits(in: .horizontal) {
                HStack(alignment: .top, spacing: 12) {
                    setpointDial.frame(width: 190)
                    timingRulers.frame(minWidth: 190)
                }
                VStack(spacing: 12) {
                    setpointDial
                    timingRulers
                }
            }

            HStack(spacing: 10) {
                if canDelete {
                    Button(role: .destructive, action: onDelete) {
                        Image(systemName: "trash")
                            .font(.system(size: 14, weight: .medium))
                            .frame(width: 44, height: 44)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(CremaColor.danger)
                    .accessibilityLabel("Delete stage")
                    .help("Delete stage")
                }
                Spacer()
                Button(action: onDeselect) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(CremaColor.crema)
                        .frame(width: 44, height: 44)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Finish editing stage")
                .help("Finish editing stage")
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(CremaColor.surface)
                .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(CremaColor.crema.opacity(0.18), lineWidth: 0.8))
        )
        .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity))
    }

    @ViewBuilder private var setpointDial: some View {
        if stage.priority == .pressure {
            CremaDialControl(value: $stage.pressureBar, range: 0...9, step: 0.1,
                             label: "Pressure", unit: "bar", accent: CremaColor.crema)
        } else {
            CremaDialControl(value: $stage.flowMlPerSec, range: 0.1...8, step: 0.1,
                             label: "Pump flow", unit: "mL/s", accent: CremaColor.matcha)
        }
    }

    private var timingRulers: some View {
        VStack(spacing: 12) {
            CremaRulerControl(value: $stage.duration, range: 1...60, step: 0.5,
                              label: "Duration", unit: "s")
            CremaRulerControl(value: $stage.waitAfter, range: 0...30, step: 0.5,
                              label: "Pause after", unit: "s", accent: CremaColor.matcha)
        }
    }
}


private struct CremaRulerScale: View, Animatable {
    var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let pitch: CGFloat

    var animatableData: Double {
        get { value }
        set { value = newValue }
    }

    var body: some View {
        Canvas { context, size in
            let verticalScale = size.height / 68
            let position = (value - range.lowerBound) / step
            let center = Int(position.rounded())
            let last = Int(((range.upperBound - range.lowerBound) / step).rounded(.down))
            let firstTick = max(0, center - 72)
            let lastTick = min(last, center + 72)
            if firstTick <= lastTick {
                for tick in firstTick...lastTick {
                    let x = size.width / 2 + CGFloat(Double(tick) - position) * pitch
                    guard x >= -8, x <= size.width + 8 else { continue }
                    let major = tick % 6 == 0
                    let distance = Double(abs(x - size.width / 2) / max(1, size.width / 2))
                    var line = Path()
                    line.move(to: CGPoint(x: x, y: (major ? 10 : 23) * verticalScale))
                    line.addLine(to: CGPoint(x: x, y: 42 * verticalScale))
                    context.stroke(line, with: .color(CremaColor.secondary.opacity(max(0.12, 0.65 - distance * 0.45))),
                                   lineWidth: major ? 1.5 : 0.8)
                    if major {
                        let number = range.lowerBound + Double(tick) * step
                        let text = Text(InstrumentMath.formatted(number, step: step))
                            .font(CremaTypography.font(size: 8).monospacedDigit())
                            .foregroundColor(CremaColor.secondary)
                        context.draw(text, at: CGPoint(x: x, y: 56 * verticalScale))
                    }
                }
            }
        }
    }
}

// MARK: - Tactile instruments
// The scale moves beneath a fixed marker, as in the local Lofree Hub instruments.
// Bounds and snapped values remain the editor's existing machine-profile values.

struct CremaRulerControl: View {
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let label: String
    let unit: String
    var accent: Color = CremaColor.crema
    var compact: Bool = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @FocusState private var isFocused: Bool
    @State private var dragOrigin: Double?
    @State private var rawValue: Double?
    private let pitch: CGFloat = 8

    private var displayedValue: Double {
        InstrumentMath.magnetic(rawValue ?? value, range: range, step: step)
    }
    private var stepIndex: Int { Int(((value - range.lowerBound) / step).rounded()) }

    var body: some View {
        VStack(alignment: .leading, spacing: compact ? 4 : 6) {
            HStack(alignment: .firstTextBaseline) {
                Text(label)
                    .font(CremaTypography.font(size: compact ? 10 : 11, weight: .medium))
                    .foregroundStyle(CremaColor.secondary)
                Spacer()
                CremaDotReadout(text: InstrumentMath.formatted(value, step: step),
                                color: CremaColor.cream, size: compact ? 18 : 22)
                    .fixedSize()
                    .alignmentGuide(.firstTextBaseline) { dimensions in
                        dimensions.height - (compact ? 18 : 22) * 0.039
                    }
                    .accessibilityHidden(true)
                Text(unit)
                    .font(CremaTypography.font(size: 10))
                    .foregroundStyle(CremaColor.secondary)
            }
            GeometryReader { geometry in
                let width = geometry.size.width
                let height: CGFloat = compact ? 60 : 68
                let verticalScale = height / 68
                ZStack {
                    CremaRulerScale(value: displayedValue, range: range, step: step, pitch: pitch)
                        .accessibilityHidden(true)
                    Capsule().fill(accent).frame(width: 2, height: 39 * verticalScale)
                        .position(x: width / 2, y: 24 * verticalScale)
                        .shadow(color: accent.opacity(0.2), radius: 5)
                        .accessibilityHidden(true)
                    Circle().fill(CremaColor.surface).frame(width: 15, height: 15)
                        .overlay(Circle().strokeBorder(CremaColor.hairline, lineWidth: 1))
                        .overlay(Circle().fill(accent).frame(width: 4, height: 4))
                        .position(x: width / 2, y: 44 * verticalScale)
                        .accessibilityHidden(true)
                }
                .frame(width: width, height: height)
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 2, coordinateSpace: .global)
                        .onChanged { gesture in
                            isFocused = true
                            if dragOrigin == nil { dragOrigin = value }
                            let next = (dragOrigin ?? value) - Double(gesture.translation.width / pitch) * step
                            rawValue = InstrumentMath.clamped(next, range: range)
                            value = InstrumentMath.snapped(next, range: range, step: step)
                        }
                        .onEnded { _ in dragOrigin = nil; rawValue = nil }
                )
            }
            .frame(height: compact ? 60 : 68)
        }
        .padding(compact ? 10 : 14)
        .background(RoundedRectangle(cornerRadius: 18).fill(compact ? Color.clear : CremaColor.bg))
        .overlay(RoundedRectangle(cornerRadius: 18)
            .strokeBorder(isFocused ? accent : CremaColor.hairline.opacity(0.7),
                          lineWidth: isFocused ? 1.5 : (compact ? 0 : 0.7)))
        .focusable()
        .focused($isFocused)
        .onKeyPress(phases: [.down, .repeat]) { press in
            guard let next = InstrumentMath.keyboardValue(press, value: value, range: range, step: step) else { return .ignored }
            dragOrigin = nil
            rawValue = nil
            value = next
            return .handled
        }
        .simultaneousGesture(TapGesture().onEnded { isFocused = true })
        .animation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 1), value: displayedValue)
        .sensoryFeedback(.selection, trigger: stepIndex)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(label))
        .accessibilityValue(Text("\(InstrumentMath.formatted(value, step: step)) \(unit)"))
        .accessibilityHint("Swipe up or down, or use the arrow keys, to adjust by \(InstrumentMath.formatted(step, step: step)) \(unit)")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: value = InstrumentMath.snapped(value + step, range: range, step: step)
            case .decrement: value = InstrumentMath.snapped(value - step, range: range, step: step)
            @unknown default: break
            }
        }
    }
}

struct CremaDialControl: View {
    @Binding var value: Double
    let range: ClosedRange<Double>
    let step: Double
    let label: String
    let unit: String
    var accent: Color = CremaColor.crema
    var compact: Bool = false

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @FocusState private var isFocused: Bool
    @State private var lastAngle: Double?
    @State private var rawValue: Double?

    private var displayedValue: Double {
        InstrumentMath.magnetic(rawValue ?? value, range: range, step: step)
    }
    private var ratio: Double {
        (displayedValue - range.lowerBound) / max(step, range.upperBound - range.lowerBound)
    }
    private var stepIndex: Int { Int(((value - range.lowerBound) / step).rounded()) }

    var body: some View {
        VStack(spacing: compact ? 6 : 8) {
            Text(label.uppercased())
                .font(CremaTypography.font(size: compact ? 9 : 10, weight: .semibold))
                .tracking(1.3)
                .foregroundStyle(CremaColor.secondary)
            GeometryReader { geometry in
                let size = min(geometry.size.width, geometry.size.height)
                let center = CGPoint(x: geometry.size.width / 2, y: geometry.size.height / 2)
                ZStack {
                    Circle()
                        .fill(CremaColor.surface)
                        .shadow(color: CremaColor.espresso.opacity(0.08), radius: 12, x: 0, y: 7)
                        .padding(size * 0.16)
                    Canvas { context, canvasSize in
                        let radius = min(canvasSize.width, canvasSize.height) / 2
                        let mid = CGPoint(x: canvasSize.width / 2, y: canvasSize.height / 2)
                        for tick in 0...80 {
                            let angle = (-140 + 280 * Double(tick) / 80) * .pi / 180
                            let major = tick % 5 == 0
                            let inner = radius * (major ? 0.82 : 0.87)
                            let outer = radius * 0.92
                            var path = Path()
                            path.move(to: CGPoint(x: mid.x + CGFloat(sin(angle)) * inner, y: mid.y - CGFloat(cos(angle)) * inner))
                            path.addLine(to: CGPoint(x: mid.x + CGFloat(sin(angle)) * outer, y: mid.y - CGFloat(cos(angle)) * outer))
                            context.stroke(path, with: .color(CremaColor.secondary.opacity(major ? 0.7 : 0.36)),
                                           lineWidth: major ? 1.5 : 0.8)
                        }
                    }
                    .rotationEffect(.degrees(140 - 280 * ratio))
                    .accessibilityHidden(true)

                    HStack(alignment: .lastTextBaseline, spacing: 4) {
                        CremaDotReadout(text: InstrumentMath.formatted(value, step: step),
                                        color: CremaColor.cream, size: compact ? 28 : 32)
                            .fixedSize()
                            .alignmentGuide(.lastTextBaseline) { dimensions in
                                dimensions.height - (compact ? 28 : 32) * 0.039
                            }
                            .accessibilityHidden(true)
                        Text(unit)
                            .font(CremaTypography.font(size: 11))
                            .foregroundStyle(CremaColor.secondary)
                            .fixedSize()
                    }
                    .fixedSize()
                    .position(x: size / 2, y: size / 2)
                    .allowsHitTesting(false)

                    Circle().fill(CremaColor.surface).frame(width: 21, height: 21)
                        .overlay(Circle().strokeBorder(CremaColor.hairline, lineWidth: 1))
                        .overlay(Circle().fill(accent).frame(width: 5, height: 5))
                        .position(x: size / 2, y: size * 0.11)
                        .shadow(color: accent.opacity(0.2), radius: 6)
                        .allowsHitTesting(false)
                        .accessibilityHidden(true)
                }
                .frame(width: size, height: size)
                .position(center)
                .frame(width: geometry.size.width, height: geometry.size.height)
                .contentShape(Circle())
                .gesture(
                    DragGesture(minimumDistance: 2)
                        .onChanged { gesture in
                            isFocused = true
                            let angle = atan2(Double(gesture.location.x - center.x),
                                              Double(center.y - gesture.location.y)) * 180 / .pi
                            guard hypot(gesture.location.x - center.x, gesture.location.y - center.y) > size * 0.2 else { return }
                            if let previous = lastAngle {
                                var delta = angle - previous
                                while delta > 180 { delta -= 360 }
                                while delta < -180 { delta += 360 }
                                let next = (rawValue ?? value) + delta / 280 * (range.upperBound - range.lowerBound)
                                rawValue = InstrumentMath.clamped(next, range: range)
                                value = InstrumentMath.snapped(next, range: range, step: step)
                            } else {
                                rawValue = value
                            }
                            lastAngle = angle
                        }
                        .onEnded { _ in lastAngle = nil; rawValue = nil }
                )
            }
            .frame(height: compact ? 150 : 190)

            HStack {
                instrumentButton("minus", description: "Decrease \(label)", delta: -step)
                Spacer()
                Text("\(InstrumentMath.formatted(range.lowerBound, step: step))–\(InstrumentMath.formatted(range.upperBound, step: step)) \(unit)")
                    .font(CremaTypography.font(size: 9).monospacedDigit())
                    .foregroundStyle(CremaColor.secondary)
                Spacer()
                instrumentButton("plus", description: "Increase \(label)", delta: step)
            }
        }
        .padding(compact ? 10 : 14)
        .background(RoundedRectangle(cornerRadius: 22).fill(compact ? Color.clear : CremaColor.bg))
        .overlay(RoundedRectangle(cornerRadius: 22)
            .strokeBorder(isFocused ? accent : CremaColor.hairline.opacity(0.7),
                          lineWidth: isFocused ? 1.5 : (compact ? 0 : 0.7)))
        .focusable()
        .focused($isFocused)
        .onKeyPress(phases: [.down, .repeat]) { press in
            guard let next = InstrumentMath.keyboardValue(press, value: value, range: range, step: step) else { return .ignored }
            lastAngle = nil
            rawValue = nil
            value = next
            return .handled
        }
        .simultaneousGesture(TapGesture().onEnded { isFocused = true })
        .animation(reduceMotion ? nil : .spring(response: 0.3, dampingFraction: 1), value: ratio)
        .sensoryFeedback(.selection, trigger: stepIndex)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(Text(label))
        .accessibilityValue(Text("\(InstrumentMath.formatted(value, step: step)) \(unit)"))
        .accessibilityHint("Drag around the dial or use the arrow keys to adjust")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment: value = InstrumentMath.snapped(value + step, range: range, step: step)
            case .decrement: value = InstrumentMath.snapped(value - step, range: range, step: step)
            @unknown default: break
            }
        }
    }

    private func instrumentButton(_ symbol: String, description: String, delta: Double) -> some View {
        Button {
            value = InstrumentMath.snapped(value + delta, range: range, step: step)
        } label: {
            Image(systemName: symbol).font(.system(size: 12, weight: .semibold))
                .foregroundStyle(accent)
                .frame(width: 44, height: 44)
                .background(CremaColor.surface, in: Circle())
        }
        .buttonStyle(InstrumentPressStyle())
        .accessibilityLabel(description)
        .accessibilityValue("\(InstrumentMath.formatted(value, step: step)) \(unit)")
    }
}

private struct InstrumentPressStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(reduceMotion ? 1 : (configuration.isPressed ? 0.92 : 1))
            .opacity(configuration.isPressed ? 0.8 : 1)
            .animation(reduceMotion ? nil : .spring(response: 0.22, dampingFraction: 0.85),
                       value: configuration.isPressed)
    }
}

private enum InstrumentMath {
    static func keyboardValue(_ press: KeyPress, value: Double, range: ClosedRange<Double>, step: Double) -> Double? {
        guard press.modifiers.intersection([.command, .control, .option]).isEmpty else { return nil }
        switch press.key {
        case .leftArrow, .downArrow, "-": return snapped(value - step, range: range, step: step)
        case .rightArrow, .upArrow, "+", "=": return snapped(value + step, range: range, step: step)
        case .home: return range.lowerBound
        case .end: return range.upperBound
        default: return nil
        }
    }

    static func clamped(_ value: Double, range: ClosedRange<Double>) -> Double {
        min(range.upperBound, max(range.lowerBound, value))
    }
    static func snapped(_ value: Double, range: ClosedRange<Double>, step: Double) -> Double {
        let next = range.lowerBound + ((value - range.lowerBound) / step).rounded() * step
        return clamped((next * 1_000_000).rounded() / 1_000_000, range: range)
    }
    static func magnetic(_ value: Double, range: ClosedRange<Double>, step: Double) -> Double {
        let raw = clamped(value, range: range)
        let nearest = snapped(raw, range: range, step: step)
        let distance = abs(raw - nearest) / step
        let attraction = pow(1 - min(1, max(0, (distance - 0.08) / 0.42)), 2) * 0.38
        return raw + (nearest - raw) * attraction
    }
    static func formatted(_ value: Double, step: Double) -> String {
        step >= 1 ? String(format: "%.0f", value) : String(format: "%.1f", value)
    }
}
