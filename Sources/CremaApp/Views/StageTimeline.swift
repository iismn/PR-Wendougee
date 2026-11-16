import SwiftUI
import CremaKit

/// Every stage remains visible in a horizontally scrolling progression, including waits.
struct StageTimeline: View {
    let profile: BrewProfile
    let playhead: Double
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    ForEach(Array(profile.stages.enumerated()), id: \.element.id) { index, stage in
                        stagePill(stage, index: index)
                            .id(index)
                    }
                }
                .padding(.vertical, 3)
            }
            .onChange(of: currentIndex) { _, index in
                guard profile.stages.indices.contains(index) else { return }
                withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) {
                    proxy.scrollTo(index, anchor: .center)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Brew stages")
        .animation(reduceMotion ? nil : .smooth(duration: 0.25), value: currentIndex)
    }

    private func stagePill(_ stage: BrewStage, index: Int) -> some View {
        let state = stateFor(index)
        let isCurrent = state == .current
        let isWaiting = isCurrent && playhead >= startTime(for: index) + stage.duration
        let foreground = CremaColor.cream

        return HStack(spacing: 9) {
            ZStack {
                Circle()
                    .fill(isCurrent ? CremaColor.primarySurface : CremaColor.surface)
                if state == .past {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(CremaColor.matcha)
                } else if isWaiting {
                    Image(systemName: "pause.fill")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(CremaColor.onPrimary)
                } else {
                    Text("\(index + 1)")
                        .font(CremaTypography.font(size: 11, weight: .semibold).monospacedDigit())
                        .foregroundStyle(isCurrent ? CremaColor.onPrimary : CremaColor.secondary)
                }
            }
            .frame(width: 26, height: 26)

            VStack(alignment: .leading, spacing: 3) {
                Text(stage.label.isEmpty ? "Stage \(index + 1)" : stage.label)
                    .font(CremaFont.stageLabel)
                    .foregroundStyle(foreground)
                    .lineLimit(1)
                Text(detail(for: stage, index: index, isWaiting: isWaiting))
                    .font(CremaTypography.font(size: 10, weight: .regular).monospacedDigit())
                    .foregroundStyle(CremaColor.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
        .background(
            isCurrent ? CremaColor.surfaceWarm : Color.clear,
            in: RoundedRectangle(cornerRadius: 18, style: .continuous)
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("Stage \(index + 1), \(stage.label.isEmpty ? "unnamed" : stage.label)"))
        .accessibilityValue(Text(accessibilityDetail(for: stage, state: state, isWaiting: isWaiting)))
    }

    private enum StageState { case past, current, future }

    private var currentIndex: Int {
        var cursor = 0.0
        for (index, stage) in profile.stages.enumerated() {
            cursor += stage.duration + stage.waitAfter
            if playhead < cursor { return index }
        }
        return profile.stages.count
    }

    private func stateFor(_ index: Int) -> StageState {
        if index < currentIndex { return .past }
        if index == currentIndex { return .current }
        return .future
    }

    private func startTime(for index: Int) -> Double {
        profile.stages.prefix(index).reduce(0) { $0 + $1.duration + $1.waitAfter }
    }

    private func detail(for stage: BrewStage, index: Int, isWaiting: Bool) -> String {
        if isWaiting {
            let remaining = max(0, startTime(for: index) + stage.duration + stage.waitAfter - playhead)
            return "Wait · \(seconds(remaining))s"
        }
        let wait = stage.waitAfter > 0 ? " · +\(seconds(stage.waitAfter))s wait" : ""
        return "\(seconds(stage.duration))s\(wait)"
    }

    private func accessibilityDetail(for stage: BrewStage, state: StageState, isWaiting: Bool) -> String {
        let status = isWaiting ? "Waiting" : state == .past ? "Complete" : state == .current ? "Current" : "Upcoming"
        let wait = stage.waitAfter > 0 ? ", followed by \(seconds(stage.waitAfter)) seconds of waiting" : ""
        return "\(status), \(seconds(stage.duration)) seconds\(wait)"
    }

    private func seconds(_ value: Double) -> String {
        value == value.rounded() ? String(format: "%.0f", value) : String(format: "%.1f", value)
    }
}
