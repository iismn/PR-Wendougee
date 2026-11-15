import SwiftUI
import CremaKit

/// Every stage remains visible in a horizontally scrolling progression, including waits.
struct StageTimeline: View {
    let profile: BrewProfile
    let playhead: Double

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(Array(profile.stages.enumerated()), id: \.element.id) { index, stage in
                        stagePill(stage, index: index)
                            .id(index)
                    }
                }
                .padding(.vertical, 3)
            }
            .onChange(of: currentIndex) { _, index in
                guard profile.stages.indices.contains(index) else { return }
                proxy.scrollTo(index, anchor: .center)
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Brew stages")
    }

    private func stagePill(_ stage: BrewStage, index: Int) -> some View {
        let state = stateFor(index)
        let isCurrent = state == .current
        let isWaiting = isCurrent && playhead >= startTime(for: index) + stage.duration
        let foreground = isCurrent ? CremaColor.onPrimary : CremaColor.cream

        return HStack(spacing: 9) {
            ZStack {
                Circle()
                    .fill(isCurrent ? CremaColor.amber : CremaColor.surfaceWarm)
                if state == .past {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .semibold))
                        .foregroundStyle(CremaColor.matcha)
                } else if isWaiting {
                    Image(systemName: "pause.fill")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(CremaColor.espresso)
                } else {
                    Text("\(index + 1)")
                        .font(CremaTypography.font(size: 11, weight: .semibold).monospacedDigit())
                        .foregroundStyle(isCurrent ? CremaColor.espresso : CremaColor.secondary)
                }
            }
            .frame(width: 26, height: 26)

            VStack(alignment: .leading, spacing: 3) {
                Text(stage.label.isEmpty ? "Stage \(index + 1)" : stage.label)
                    .font(CremaFont.stageLabel)
                    .foregroundStyle(foreground)
                    .lineLimit(1)
                Text(detail(for: stage, index: index, isWaiting: isWaiting))
                    .font(CremaTypography.font(size: 9, weight: .regular).monospacedDigit())
                    .foregroundStyle(isCurrent ? CremaColor.onPrimary.opacity(0.65) : CremaColor.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.horizontal, 11)
        .padding(.vertical, 9)
        .background(
            isCurrent ? CremaColor.primarySurface : CremaColor.surface.opacity(state == .past ? 0.65 : 1),
            in: Capsule()
        )
        .overlay {
            Capsule().strokeBorder(
                isCurrent ? Color.clear : CremaColor.hairline.opacity(0.7),
                lineWidth: 0.75
            )
        }
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
