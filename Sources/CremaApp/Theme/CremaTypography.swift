import SwiftUI
import CoreText

/// Pretendard is the app's text face. System symbols keep their own explicit fonts.
enum CremaTypography {
    private static let faces = [
        "Pretendard-Regular", "Pretendard-Medium", "Pretendard-SemiBold", "Pretendard-Bold"
    ]

    /// Process-scoped registration works on both iOS and macOS, including previews.
    /// UIAppFonts/ATSApplicationFontsPath also declare the bundled Fonts directory.
    private static let registration: Void = {
        for face in faces {
            guard let url = Bundle.main.url(forResource: face, withExtension: "otf", subdirectory: "Fonts")
                ?? Bundle.main.url(forResource: face, withExtension: "otf") else {
                assertionFailure("Missing bundled font: \(face).otf")
                continue
            }
            // A font declared in the platform plist may already be registered.
            CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
        }
    }()

    static func registerFonts() {
        _ = registration
    }

    /// Ignores .rounded/.serif/.monospaced designs so all text uses Pretendard.
    static func font(
        size: CGFloat,
        weight: Font.Weight = .regular,
        design: Font.Design = .default
    ) -> Font {
        registerFonts()
        return .custom(postScriptName(for: weight), size: size)
    }

    /// Semantic text styles preserve Dynamic Type via Font.custom(relativeTo:).
    static func font(
        _ style: Font.TextStyle,
        design: Font.Design = .default,
        weight: Font.Weight? = nil
    ) -> Font {
        registerFonts()
        let resolvedWeight = weight ?? (style == .headline ? .semibold : .regular)
        return .custom(postScriptName(for: resolvedWeight), size: baseSize(for: style), relativeTo: style)
    }

    private static func postScriptName(for weight: Font.Weight) -> String {
        switch weight {
        case .black, .heavy, .bold: return "Pretendard-Bold"
        case .semibold: return "Pretendard-SemiBold"
        case .medium: return "Pretendard-Medium"
        default: return "Pretendard-Regular"
        }
    }

    private static func baseSize(for style: Font.TextStyle) -> CGFloat {
        switch style {
        case .largeTitle: return 34
        case .title: return 28
        case .title2: return 22
        case .title3: return 20
        case .headline, .body: return 17
        case .callout: return 16
        case .subheadline: return 15
        case .footnote: return 13
        case .caption: return 12
        case .caption2: return 11
        @unknown default: return 17
        }
    }
}
