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
                bg: 0xF3F0E9, surface: 0xFFFFFF, surfaceWarm: 0xE9E3D8,
                hairline: 0xDDD6CA, cream: 0x29251F, secondary: 0x756E63,
                crema: 0xA76231, cremaBright: 0xCE8650,
                matcha: 0x4E7A68, matchaBright: 0x77A58A,
                connected: 0x346D4D, danger: 0xA74133,
                pressureTileStart: 0xF9E2C7, pressureTileEnd: 0xEFCFAA,
                flowTileStart: 0xE9F1E9, flowTileEnd: 0xD6E6DB,
                primarySurface: 0x29251F, heroSurface: 0x29251F, heroInk: 0xF8F3EA
            )
        case .day:
            return Self(
                bg: 0xEEF1ED, surface: 0xFCFDFC, surfaceWarm: 0xE3E9E1,
                hairline: 0xD5DDD2, cream: 0x252C26, secondary: 0x647063,
                crema: 0x8D623F, cremaBright: 0xBA8559,
                matcha: 0x4D7766, matchaBright: 0x7AA38B,
                connected: 0x346D4D, danger: 0xA74133,
                pressureTileStart: 0xEFE2CF, pressureTileEnd: 0xE6D4BC,
                flowTileStart: 0xE4EEE3, flowTileEnd: 0xD2E1D3,
                primarySurface: 0x29251F, heroSurface: 0x253129, heroInk: 0xEEF4EA
            )
        case .evening:
            return Self(
                bg: 0x241F1A, surface: 0x302A23, surfaceWarm: 0x3B332A,
                hairline: 0x51473B, cream: 0xF1E6D5, secondary: 0xBAAB95,
                crema: 0xE1A264, cremaBright: 0xE6B47E,
                matcha: 0x9DB898, matchaBright: 0xB2C7A4,
                connected: 0x8FB89A, danger: 0xE8957C,
                pressureTileStart: 0x4B3827, pressureTileEnd: 0x3D2E22,
                flowTileStart: 0x303E33, flowTileEnd: 0x29332B,
                primarySurface: 0x4C3A28, heroSurface: 0x272019, heroInk: 0xE6D5BB
            )
        case .night:
            return Self(
                bg: 0x141310, surface: 0x201E19, surfaceWarm: 0x2B2720,
                hairline: 0x3D372C, cream: 0xD5C7B0, secondary: 0x9D907B,
                crema: 0xC39360, cremaBright: 0xD4A877,
                matcha: 0x87A08D, matchaBright: 0xA3B49B,
                connected: 0x81A087, danger: 0xC98573,
                pressureTileStart: 0x322A20, pressureTileEnd: 0x29231B,
                flowTileStart: 0x252D26, flowTileEnd: 0x1F251F,
                primarySurface: 0x352D21, heroSurface: 0x181510, heroInk: 0xC9B89B
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
    static var onPrimary: Color { appearance.phase.isDark ? cream : onEspresso }
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
                .overlay(shape.strokeBorder(CremaColor.hairline.opacity(0.75), lineWidth: 0.8))
                .shadow(color: CremaColor.espresso.opacity(0.045), radius: 18, x: 0, y: 7)
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
