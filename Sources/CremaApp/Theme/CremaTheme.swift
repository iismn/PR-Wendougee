import SwiftUI
import Observation
import Foundation

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }
}

enum CremaAppearanceMode: String, CaseIterable, Identifiable, Sendable {
    case automatic = "auto", morning, day, evening, night
    var id: String { rawValue }
    var title: String {
        switch self {
        case .automatic: return "Auto"
        case .morning: return "Morning"
        case .day: return "Day"
        case .evening: return "Evening"
        case .night: return "Night"
        }
    }
    var symbol: String {
        switch self {
        case .automatic: return "clock"
        case .morning: return "sunrise"
        case .day: return "sun.max"
        case .evening: return "sunset"
        case .night: return "moon.stars"
        }
    }
}

enum CremaDayPhase: String, CaseIterable, Sendable {
    case morning, day, evening, night
    var title: String { rawValue.capitalized }
    var symbol: String {
        switch self {
        case .morning: return "sunrise"
        case .day: return "sun.max"
        case .evening: return "sunset"
        case .night: return "moon.stars"
        }
    }
    var isDark: Bool { self == .evening || self == .night }
}

/// The same local-time boundaries and smooth warmth curve as MonitorShade.
enum CremaAppearanceSchedule {
    static func phase(at hour: Double) -> CremaDayPhase {
        let h = normalized(hour)
        if h >= 6 && h < 12 { return .morning }
        if h >= 12 && h < 18 { return .day }
        if h >= 18 && h < 23 { return .evening }
        return .night
    }

    static func warmth(at hour: Double) -> Double {
        let h = normalized(hour)
        func smooth(_ value: Double) -> Double {
            let t = min(1, max(0, value))
            return t * t * (3 - 2 * t)
        }
        if h < 5 || h >= 20 { return 1 }
        if h < 9 { return 1 - smooth((h - 5) / 4) }
        return smooth((h - 14) / 6)
    }

    static func hour(at date: Date, calendar: Calendar = .current) -> Double {
        let components = calendar.dateComponents([.hour, .minute], from: date)
        return Double(components.hour ?? 0) + Double(components.minute ?? 0) / 60
    }

    private static func normalized(_ hour: Double) -> Double {
        guard hour.isFinite else { return 0 }
        return (hour.truncatingRemainder(dividingBy: 24) + 24)
            .truncatingRemainder(dividingBy: 24)
    }
}

struct CremaAppearanceSnapshot: Sendable, Equatable {
    let mode: CremaAppearanceMode
    let phase: CremaDayPhase
    let hour: Double
    let warmth: Double

    var colorScheme: ColorScheme { phase.isDark ? .dark : .light }
    var palette: CremaPalette { CremaPalette.forPhase(phase) }

    static func resolve(at date: Date, mode: CremaAppearanceMode,
                        calendar: Calendar = .current) -> Self {
        let hour: Double
        switch mode {
        case .automatic: hour = CremaAppearanceSchedule.hour(at: date, calendar: calendar)
        case .morning: hour = 9
        case .day: hour = 14
        case .evening: hour = 20
        case .night: hour = 0
        }
        return Self(mode: mode, phase: CremaAppearanceSchedule.phase(at: hour),
                    hour: hour, warmth: CremaAppearanceSchedule.warmth(at: hour))
    }
}

/// Reads are safe from Canvas render closures. Observable access is registered before
/// reading the immutable snapshot; writes happen on the main actor and under the lock.
@Observable
final class CremaAppearance: @unchecked Sendable {
    static let shared = CremaAppearance()
    static let preferenceKey = "crema.appearance.mode"

    @ObservationIgnored private let lock = NSLock()
    @ObservationIgnored private var storedSnapshot: CremaAppearanceSnapshot

    var snapshot: CremaAppearanceSnapshot {
        access(keyPath: \.snapshot)
        return lock.withLock { storedSnapshot }
    }

    private init() {
        let raw = UserDefaults.standard.string(forKey: Self.preferenceKey) ?? "auto"
        let mode = CremaAppearanceMode(rawValue: raw) ?? .automatic
        storedSnapshot = .resolve(at: .now, mode: mode)
    }

    @MainActor
    func refresh(at date: Date = .now) {
        let raw = UserDefaults.standard.string(forKey: Self.preferenceKey) ?? "auto"
        let mode = CremaAppearanceMode(rawValue: raw) ?? .automatic
        let next = CremaAppearanceSnapshot.resolve(at: date, mode: mode)
        guard lock.withLock({ storedSnapshot != next }) else { return }
        withMutation(keyPath: \.snapshot) {
            lock.withLock { storedSnapshot = next }
        }
    }

    @MainActor
    func setMode(_ mode: CremaAppearanceMode) {
        UserDefaults.standard.set(mode.rawValue, forKey: Self.preferenceKey)
        refresh()
    }
}

/// Raw sRGB values also make the native palette reproducible in the design preview.
struct CremaPalette: Sendable, Equatable {
    let bg, surface, surfaceWarm, hairline, cream, secondary: UInt32
    let crema, cremaBright, matcha, matchaBright, connected, danger: UInt32
    let pressureTileStart, pressureTileEnd, flowTileStart, flowTileEnd: UInt32
    let primarySurface, heroSurface, heroInk: UInt32

    static func forPhase(_ phase: CremaDayPhase) -> Self {
        switch phase {
        case .morning:
            return Self(
                bg: 0xF1F0EB, surface: 0xFAFAF7, surfaceWarm: 0xE7E7DF,
                hairline: 0xDBDDD5, cream: 0x282C26, secondary: 0x73766D,
                crema: 0xB9693C, cremaBright: 0xD58E59,
                matcha: 0x56775D, matchaBright: 0x86A47A,
                connected: 0x346D4D, danger: 0xA74133,
                pressureTileStart: 0xE7A875, pressureTileEnd: 0xEBD2AB,
                flowTileStart: 0xB5CAA9, flowTileEnd: 0xDFE6D2,
                primarySurface: 0x282C26, heroSurface: 0x282C26, heroInk: 0xF8F7EF
            )
        case .day:
            return Self(
                bg: 0xF0F1EC, surface: 0xFCFCF9, surfaceWarm: 0xE5E8DF,
                hairline: 0xDADDD4, cream: 0x252B25, secondary: 0x70776D,
                crema: 0xAC603B, cremaBright: 0xCE8958,
                matcha: 0x4F7358, matchaBright: 0x7C9D74,
                connected: 0x346D4D, danger: 0xA74133,
                pressureTileStart: 0xE7AC7D, pressureTileEnd: 0xEED8B8,
                flowTileStart: 0xADC9A6, flowTileEnd: 0xDCE6D2,
                primarySurface: 0x252B25, heroSurface: 0x253129, heroInk: 0xEEF4EA
            )
        case .evening:
            return Self(
                bg: 0x20221D, surface: 0x292C25, surfaceWarm: 0x34372E,
                hairline: 0x44473D, cream: 0xE3E4D6, secondary: 0xA7AA9B,
                crema: 0xDAAA78, cremaBright: 0xE4B98B,
                matcha: 0xA6BC93, matchaBright: 0xBED0AC,
                connected: 0x8FB89A, danger: 0xE8957C,
                pressureTileStart: 0x574330, pressureTileEnd: 0x3A3428,
                flowTileStart: 0x3E5039, flowTileEnd: 0x303A2C,
                primarySurface: 0xD8DBC9, heroSurface: 0x23271F, heroInk: 0xE3E4D6
            )
        case .night:
            return Self(
                bg: 0x151713, surface: 0x20231C, surfaceWarm: 0x2A2E24,
                hairline: 0x373C30, cream: 0xC7CCB9, secondary: 0x929B85,
                crema: 0xBD986E, cremaBright: 0xCAA77E,
                matcha: 0x91A77D, matchaBright: 0xA7BA94,
                connected: 0x81A087, danger: 0xC98573,
                pressureTileStart: 0x3D3325, pressureTileEnd: 0x2C281F,
                flowTileStart: 0x303D29, flowTileEnd: 0x242D20,
                primarySurface: 0xAFB79F, heroSurface: 0x181C15, heroInk: 0xC7CCB9
            )
        }
    }
}

enum CremaColor {
    private static var appearance: CremaAppearanceSnapshot { CremaAppearance.shared.snapshot }
    private static var palette: CremaPalette { appearance.palette }

    /// The small continuous warmth shift follows MonitorShade's 05–09 / 14–20 curve.
    private static func warmed(_ hex: UInt32, snapshot: CremaAppearanceSnapshot,
                               strength: Double) -> Color {
        let blend = snapshot.warmth * strength
        let warm: UInt32 = 0xC9955F
        func channel(_ shift: UInt32) -> Double {
            let a = Double((hex >> shift) & 0xFF)
            let b = Double((warm >> shift) & 0xFF)
            return (a + (b - a) * blend) / 255
        }
        return Color(.sRGB, red: channel(16), green: channel(8), blue: channel(0), opacity: 1)
    }

    static var bg: Color {
        let state = appearance
        return warmed(state.palette.bg, snapshot: state, strength: state.phase.isDark ? 0.035 : 0.018)
    }
    static var surface: Color {
        let state = appearance
        return warmed(state.palette.surface, snapshot: state, strength: state.phase.isDark ? 0.02 : 0.008)
    }
    static var surfaceWarm: Color { Color(hex: palette.surfaceWarm) }
    static var hairline: Color { Color(hex: palette.hairline) }
    static var cream: Color { Color(hex: palette.cream) }
    static var secondary: Color { Color(hex: palette.secondary) }
    static var crema: Color { Color(hex: palette.crema) }
    static var cremaBright: Color { Color(hex: palette.cremaBright) }
    static var matcha: Color { Color(hex: palette.matcha) }
    static var matchaBright: Color { Color(hex: palette.matchaBright) }
    static var connected: Color { Color(hex: palette.connected) }
    static var danger: Color { Color(hex: palette.danger) }
    static var waitGray: Color { Color(hex: palette.secondary) }
    static var divergence: Color { crema }
    static var pressureTileStart: Color { Color(hex: palette.pressureTileStart) }
    static var pressureTileEnd: Color { Color(hex: palette.pressureTileEnd) }
    static var flowTileStart: Color { Color(hex: palette.flowTileStart) }
    static var flowTileEnd: Color { Color(hex: palette.flowTileEnd) }
    static var primarySurface: Color { Color(hex: palette.primarySurface) }
    static var onPrimary: Color { appearance.phase.isDark ? espresso : onEspresso }
    static var onAccent: Color { appearance.phase.isDark ? espresso : .white }
    static var onDanger: Color { onAccent }
    static var heroSurface: Color { Color(hex: palette.heroSurface) }
    static var heroInk: Color { Color(hex: palette.heroInk) }

    /// Fixed material colors for artwork, shadows, and dark ink on amber badges.
    static let espresso = Color(hex: 0x29251F)
    static let onEspresso = Color(hex: 0xF8F3EA)
    static let amber = Color(hex: 0xE9AD68)
}

enum CremaGradient {
    static var appBackground: RadialGradient {
        RadialGradient(
            colors: [CremaColor.surfaceWarm.opacity(0.6), CremaColor.bg],
            center: .topTrailing, startRadius: 40, endRadius: 1100
        )
    }
    static var vignette: RadialGradient {
        RadialGradient(
            colors: [.clear, CremaColor.surfaceWarm.opacity(0.18)],
            center: .center, startRadius: 200, endRadius: 900
        )
    }
    static var pressureStroke: LinearGradient {
        LinearGradient(colors: [CremaColor.cremaBright, CremaColor.crema],
                       startPoint: .top, endPoint: .bottom)
    }
    static var flowStroke: LinearGradient {
        LinearGradient(colors: [CremaColor.matchaBright, CremaColor.matcha],
                       startPoint: .top, endPoint: .bottom)
    }
}

enum CremaFont {
    static func hudNumber(_ size: CGFloat = 44) -> Font {
        CremaTypography.font(size: size, weight: .regular).monospacedDigit()
    }
    static var hudCaption: Font { CremaTypography.font(size: 12, weight: .medium) }
    static var headerTitle: Font { CremaTypography.font(size: 17, weight: .semibold) }
    static var headerMeta: Font { CremaTypography.font(size: 12).monospacedDigit() }
    static var stageLabel: Font { CremaTypography.font(size: 11, weight: .medium) }
    static var valuePill: Font { CremaTypography.font(size: 10, weight: .semibold).monospacedDigit() }
    static var axisLabel: Font { CremaTypography.font(size: 10).monospacedDigit() }
}

struct CremaGlassBackground<S: InsettableShape>: ViewModifier {
    let shape: S
    let tint: Color?

    func body(content: Content) -> some View {
        content.background {
            shape.fill(CremaColor.surface)
                .overlay {
                    shape.fill(
                        LinearGradient(
                            colors: [tint?.opacity(0.05) ?? .clear, .clear],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        )
                    )
                }
                .overlay(shape.strokeBorder(CremaColor.cream.opacity(0.045), lineWidth: 0.6))
        }
    }
}

extension View {
    func cremaGlass<S: InsettableShape>(in shape: S, tint: Color? = nil) -> some View {
        modifier(CremaGlassBackground(shape: shape, tint: tint))
    }
}

/// Numeric geometry follows the user's Lofree Hub instruments: 2.8-unit pitch,
/// 0.82-unit dot radius, 17-unit digit advance and a proportional 6-unit decimal.
/// Only lit dots are drawn. Uniform scaling preserves hollow glyphs at every size.
struct CremaDotReadout: View {
    let text: String
    var color: Color = CremaColor.cream
    var size: CGFloat = 48

    private var advance: CGFloat {
        text.reduce(0) { $0 + (($1 == "." || $1 == ":") ? 6 : 17) }
    }
    private var displayWidth: CGFloat { max(2, advance - 3) }

    var body: some View {
        Canvas { context, canvasSize in
            let scale = min(canvasSize.width / displayWidth, canvasSize.height / 20)
            let left = (canvasSize.width - displayWidth * scale) / 2
            let top = (canvasSize.height - 20 * scale) / 2
            var offset: CGFloat = 0
            var dots = Path()
            for character in text {
                let rows = Self.glyphs[character] ?? Self.glyphs["—"]!
                for (y, row) in rows.enumerated() {
                    for (x, pixel) in row.enumerated() where pixel == "1" {
                        let radius = 0.82 * scale
                        let center = CGPoint(
                            x: left + (offset + CGFloat(x) * 2.8 + 1) * scale,
                            y: top + (CGFloat(y) * 2.8 + 1) * scale
                        )
                        dots.addEllipse(in: CGRect(x: center.x - radius, y: center.y - radius,
                                                   width: radius * 2, height: radius * 2))
                    }
                }
                offset += character == "." || character == ":" ? 6 : 17
            }
            // Center the actual lit dots, not the nominal character cells.
            // A final "1" and proportional punctuation leave asymmetric blank
            // space; the reference grid also has 0.6 units of vertical bias.
            if !dots.isEmpty {
                let ink = dots.boundingRect
                dots = dots.applying(CGAffineTransform(
                    translationX: canvasSize.width / 2 - ink.midX,
                    y: canvasSize.height / 2 - ink.midY
                ))
            }
            context.drawLayer { halo in
                halo.addFilter(.blur(radius: scale * 1.6))
                halo.opacity = 0.09
                halo.fill(dots, with: .color(color))
            }
            context.drawLayer { halo in
                halo.addFilter(.blur(radius: scale * 0.5))
                halo.opacity = 0.18
                halo.fill(dots, with: .color(color))
            }
            context.fill(dots, with: .color(color))
        }
        .frame(idealWidth: displayWidth * size / 20, maxWidth: displayWidth * size / 20)
        .frame(height: size)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text(text))
    }

    private static let glyphs: [Character: [String]] = [
        "0": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
        "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
        "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
        "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
        "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
        "5": ["11111", "10000", "10000", "11110", "00001", "00001", "11110"],
        "6": ["01110", "10000", "10000", "11110", "10001", "10001", "01110"],
        "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
        "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
        "9": ["01110", "10001", "10001", "01111", "00001", "00001", "01110"],
        ".": ["0", "0", "0", "0", "0", "0", "1"],
        ":": ["0", "0", "1", "0", "1", "0", "0"],
        "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
        "—": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
        "%": ["11001", "11010", "00100", "01000", "10110", "00110", "00000"],
        " ": ["000", "000", "000", "000", "000", "000", "000"]
    ]
}
