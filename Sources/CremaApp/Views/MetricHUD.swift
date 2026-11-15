import SwiftUI

/// The original four readouts beneath the curve. Pumped volume remains in mL.
struct MetricHUD: View {
    let pressure: Double
    let flow: Double
    let volume: Double
    let elapsed: Double
    var hasReading: Bool = true
    /// Kept for callers that explicitly need a two-column presentation.
    var columns: Int = 0

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @ScaledMetric(relativeTo: .largeTitle) private var numberSize: CGFloat = 32
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var sizeClass
    private var isCompact: Bool { sizeClass == .compact }
    #else
    private var isCompact: Bool { false }
    #endif

    var body: some View {
        Group {
            if dynamicTypeSize.isAccessibilitySize {
                VStack(spacing: 8) {
                    pressureTile
                    flowTile
                    volumeTile
                    timeTile
                }
            } else if isCompact || columns == 2 {
                twoColumns
            } else {
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 10) {
                        pressureTile.frame(minWidth: 128)
                        flowTile.frame(minWidth: 128)
                        volumeTile.frame(minWidth: 128)
                        timeTile.frame(minWidth: 128)
                    }
                    twoColumns
                }
            }
        }
    }

    private var twoColumns: some View {
        VStack(spacing: 8) {
            HStack(spacing: 8) { pressureTile; flowTile }
            HStack(spacing: 8) { volumeTile; timeTile }
        }
    }

    private var pressureTile: some View {
        tile(value: reading(pressure, decimals: 1), unit: "bar",
             caption: "Pressure", symbol: "gauge.with.needle", accent: CremaColor.crema)
    }

    private var flowTile: some View {
        tile(value: reading(flow, decimals: 1), unit: "mL/s",
             caption: "Pump flow", symbol: "drop.fill", accent: CremaColor.matcha)
    }

    private var volumeTile: some View {
        tile(value: reading(volume, decimals: 0), unit: "mL",
             caption: "Pumped", symbol: "waterbottle", accent: CremaColor.cream)
    }

    private var timeTile: some View {
        tile(value: hasReading && elapsed.isFinite ? String(format: "%.1f", max(0, elapsed)) : "—",
             unit: "s", caption: "Time", symbol: "clock", accent: CremaColor.cream)
    }

    private func tile(value: String, unit: String, caption: String, symbol: String, accent: Color) -> some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(accent)
                .frame(height: 14)
                .accessibilityHidden(true)
            ViewThatFits(in: .horizontal) {
                valueGroup(value, unit: unit, accent: accent, size: numberSize)
                valueGroup(value, unit: unit, accent: accent, size: numberSize * 0.875)
                valueGroup(value, unit: unit, accent: accent, size: numberSize * 0.75)
            }
            .frame(maxWidth: .infinity, alignment: .center)
        }
        .frame(maxWidth: .infinity, alignment: .center)
        .padding(.horizontal, 14)
        .padding(.vertical, 14)
        .frame(minHeight: 110)
        .cremaGlass(in: RoundedRectangle(cornerRadius: 16, style: .continuous), tint: accent)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(LinearGradient(colors: [accent.opacity(0), accent.opacity(0.45), accent.opacity(0)],
                                     startPoint: .leading, endPoint: .trailing))
                .frame(height: 1)
                .padding(.horizontal, 14)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(caption))
        .accessibilityValue(Text(value == "—" ? "No reading" : "\(value) \(unit)"))
    }

    private func valueGroup(_ value: String, unit: String, accent: Color, size: CGFloat) -> some View {
        HStack(alignment: value == "—" ? .center : .lastTextBaseline, spacing: 4) {
            CremaDotReadout(text: value, color: accent, size: size)
                .fixedSize()
                .alignmentGuide(.lastTextBaseline) { dimensions in
                    // The reference dots occupy 18.44 of the nominal 20 vertical units.
                    dimensions.height - size * 0.039
                }
            Text(unit)
                .font(CremaTypography.font(size: 11, weight: .medium))
                .foregroundStyle(CremaColor.secondary)
                .fixedSize()
        }
        .fixedSize(horizontal: true, vertical: false)
    }
    private func reading(_ value: Double, decimals: Int) -> String {
        guard hasReading, value.isFinite else { return "—" }
        return String(format: "%.*f", decimals, value)
    }
}
