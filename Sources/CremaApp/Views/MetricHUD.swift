import SwiftUI

/// Crema's four measurements, with pressure and flow carrying the visual emphasis.
struct MetricHUD: View {
    let pressure: Double
    let flow: Double
    let volume: Double
    let elapsed: Double
    var hasReading: Bool = true
    var columns: Int = 0
    var landscape: Bool = false
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var numberSize: CGFloat = 38
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var sizeClass
    private var isCompact: Bool { sizeClass == .compact }
    #else
    private var isCompact: Bool { false }
    #endif

    var body: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                VStack(spacing: 12) { pressureTile; flowTile; volumeTile; timeTile }
            } else if isCompact || columns == 2 {
                twoColumns
            } else {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 12) {
                        pressureTile.frame(minWidth: 150)
                        flowTile.frame(minWidth: 150)
                        volumeTile.frame(minWidth: 150)
                        timeTile.frame(minWidth: 150)
                    }
                    twoColumns
                }
            }
        }
    }
    private var twoColumns: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) { pressureTile; flowTile }
            HStack(spacing: 12) { volumeTile; timeTile }
        }
    }
    private var pressureTile: some View {
        tile(value: reading(pressure, decimals: 1), unit: "bar", caption: "Pressure",
             symbol: "gauge.with.needle", colors: [CremaColor.pressureTileStart, CremaColor.pressureTileEnd], fraction: pressure / 12)
    }
    private var flowTile: some View {
        tile(value: reading(flow, decimals: 1), unit: "mL/s", caption: "Pump flow",
             symbol: "drop", colors: [CremaColor.flowTileStart, CremaColor.flowTileEnd], fraction: flow / 8)
    }
    private var volumeTile: some View {
        tile(value: reading(volume, decimals: 0), unit: "mL", caption: "Pumped water", symbol: "waterbottle")
    }
    private var timeTile: some View {
        tile(value: reading(max(0, elapsed), decimals: 1), unit: "s", caption: "Time", symbol: "timer")
    }
    private func tile(value: String, unit: String, caption: String, symbol: String,
                      colors: [Color] = [CremaColor.surface, CremaColor.surface], fraction: Double? = nil) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                Image(systemName: symbol).font(.system(size: 16, weight: .regular))
                Spacer(minLength: 0)
                if let fraction {
                    CremaMetricScale(fraction: hasReading ? fraction : 0)
                        .frame(width: 64, height: 18)
                }
            }
            .foregroundStyle(CremaColor.cream.opacity(0.65))
            .accessibilityHidden(true)
            Spacer(minLength: 23)
            ViewThatFits(in: .horizontal) {
                valueGroup(value, unit: unit, size: numberSize)
                valueGroup(value, unit: unit, size: numberSize * 0.82)
                valueGroup(value, unit: unit, size: numberSize * 0.68)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(landscape ? 18 : 20)
        .frame(maxWidth: .infinity, minHeight: landscape ? 128 : 144, alignment: .leading)
        .background(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing),
                    in: RoundedRectangle(cornerRadius: 26, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 26, style: .continuous)
                .strokeBorder(CremaColor.cream.opacity(0.035), lineWidth: 0.6)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(caption))
        .accessibilityValue(Text(value == "—" ? "No reading" : "\(value) \(unit)"))
        .help(caption)
    }
    private func valueGroup(_ value: String, unit: String, size: CGFloat) -> some View {
        HStack(alignment: value == "—" ? .center : .lastTextBaseline, spacing: 6) {
            CremaDotReadout(text: value, color: CremaColor.cream, size: size)
                .fixedSize()
                .alignmentGuide(.lastTextBaseline) { $0.height - size * 0.039 }
            Text(unit).font(CremaTypography.font(size: 12))
                .foregroundStyle(CremaColor.cream.opacity(0.64)).fixedSize()
        }
        .fixedSize(horizontal: true, vertical: false)
    }
    private func reading(_ value: Double, decimals: Int) -> String {
        guard hasReading, value.isFinite else { return "—" }
        return String(format: "%.*f", decimals, value)
    }
}

/// This graduated scale is driven by the same sample as its number.
private struct CremaMetricScale: View {
    let fraction: Double
    var body: some View {
        Canvas { context, size in
            let level = fraction.isFinite ? min(1, max(0, fraction)) : 0
            for tick in 0..<17 {
                let x = CGFloat(tick) / 16 * (size.width - 2) + 1
                let height: CGFloat = tick % 4 == 0 ? 16 : 9
                var mark = Path()
                mark.move(to: CGPoint(x: x, y: (size.height - height) / 2))
                mark.addLine(to: CGPoint(x: x, y: (size.height + height) / 2))
                context.stroke(mark, with: .color(CremaColor.cream.opacity(Double(tick) / 16 < level ? 0.65 : 0.16)), lineWidth: 1)
            }
        }
    }
}
