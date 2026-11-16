import UIKit
import Shared

/// Native background and glass stay below a transparent Compose foreground.
/// UIVisualEffectView can therefore sample real UIKit content, not an opaque Skia canvas.
final class NativeGlassViewController: UIViewController, NativeGlassHost {
    private let ambient = CoffeeAmbientView()
    private let materials = UIView()
    private var glassViews: [String: UIVisualEffectView] = [:]
    private var phase: Int32 = -1
    private var compose: UIViewController!
    private var reviewScene = ""

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        ambient.isUserInteractionEnabled = false
        materials.isUserInteractionEnabled = false
        view.addSubview(ambient)
        view.addSubview(materials)
        var night = false
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "--design-review"), arguments.indices.contains(index + 1) {
            reviewScene = arguments[index + 1]
            night = arguments.contains("--night")
        }
        #endif
        compose = MainViewControllerKt.MainViewController(nativeGlassHost: NativeGlassRelay(owner: self), reviewScene: reviewScene, reviewNight: night)
        addChild(compose)
        compose.view.backgroundColor = .clear
        compose.view.isOpaque = false
        view.addSubview(compose.view)
        compose.didMove(toParent: self)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        ambient.frame = view.bounds
        materials.frame = view.bounds
        compose.view.frame = view.bounds
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        #if DEBUG
        if !reviewScene.isEmpty, #available(iOS 16.0, *) {
            view.window?.windowScene?.requestGeometryUpdate(.iOS(interfaceOrientations: .landscapeLeft))
        }
        #endif
    }

    func setPhase(phase: Int32) {
        guard self.phase != phase else { return }
        let initial = self.phase == -1
        self.phase = phase
        // Style the materials without overriding Compose's system-theme observer.
        materials.overrideUserInterfaceStyle = phase >= 2 ? .dark : .light
        #if DEBUG
        if !reviewScene.isEmpty { overrideUserInterfaceStyle = materials.overrideUserInterfaceStyle }
        #endif
        let changes = { self.ambient.phase = phase; self.ambient.setNeedsDisplay() }
        if initial || UIAccessibility.isReduceMotionEnabled { changes() }
        else { UIView.transition(with: ambient, duration: 1.2, options: .transitionCrossDissolve, animations: changes) }
        recordReviewEvidence()
    }

    func updateMaterial(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
        let effectView: UIVisualEffectView
        if let existing = glassViews[key] { effectView = existing }
        else {
            if #available(iOS 26.0, *) {
                let effect = UIGlassEffect(style: .regular)
                effect.isInteractive = true
                effectView = UIVisualEffectView(effect: effect)
            } else {
                effectView = UIVisualEffectView(effect: UIBlurEffect(style: .systemMaterial))
            }
            effectView.isUserInteractionEnabled = false
            effectView.accessibilityElementsHidden = true
            glassViews[key] = effectView
            materials.addSubview(effectView)
        }
        let frame = CGRect(x: x, y: y, width: max(0, width), height: max(0, height))
        effectView.frame = materials.convert(frame, from: view.window)
        effectView.isHidden = width <= 0 || height <= 0
        if #available(iOS 26.0, *) { effectView.cornerConfiguration = .corners(radius: .fixed(CGFloat(radius))) }
        else {
            effectView.layer.cornerRadius = CGFloat(radius)
            effectView.layer.cornerCurve = .continuous
            effectView.clipsToBounds = true
        }
        recordReviewEvidence()
    }

    func setMaterialPressed(key: String, pressed: Bool) {
        guard let effectView = glassViews[key], #available(iOS 26.0, *) else { return }
        let effect = UIGlassEffect(style: .regular)
        effect.isInteractive = true
        effect.tintColor = pressed ? UIColor.label.withAlphaComponent(0.045) : nil
        UIView.animate(withDuration: UIAccessibility.isReduceMotionEnabled ? 0 : 0.18) { effectView.effect = effect }
    }

    func removeMaterial(key: String) {
        glassViews.removeValue(forKey: key)?.removeFromSuperview()
        recordReviewEvidence()
    }

    // Debug captures include an audit of the actual UIKit objects in the running app.
    private func recordReviewEvidence() {
        #if DEBUG
        guard !reviewScene.isEmpty else { return }
        let values: [[String: Any]] = glassViews.map { key, value in
            ["id": key, "effect": String(describing: type(of: value.effect!)),
             "x": value.frame.minX, "y": value.frame.minY,
             "width": value.frame.width, "height": value.frame.height]
        }
        let document: [String: Any] = ["scene": reviewScene, "phase": phase,
            "os": UIDevice.current.systemVersion, "materials": values]
        if let data = try? JSONSerialization.data(withJSONObject: document, options: [.prettyPrinted, .sortedKeys]),
           let directory = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first {
            try? data.write(to: directory.appendingPathComponent("native-materials.json"), options: .atomic)
            let theme = phase == 3 ? "night" : "day"
            try? data.write(to: directory.appendingPathComponent("ipad-\(reviewScene)-\(theme).json"), options: .atomic)
        }
        #endif
    }
}

/// Kotlin retains its host callback; the relay avoids retaining the owning controller.
private final class NativeGlassRelay: NSObject, NativeGlassHost {
    private weak var owner: NativeGlassViewController?
    init(owner: NativeGlassViewController) { self.owner = owner }
    func setPhase(phase: Int32) { owner?.setPhase(phase: phase) }
    func updateMaterial(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
        owner?.updateMaterial(key: key, x: x, y: y, width: width, height: height, radius: radius)
    }
    func setMaterialPressed(key: String, pressed: Bool) { owner?.setMaterialPressed(key: key, pressed: pressed) }
    func removeMaterial(key: String) { owner?.removeMaterial(key: key) }
}

private final class CoffeeAmbientView: UIView {
    var phase: Int32 = 1
    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }
        let dark = phase >= 2
        (dark ? (phase == 3 ? UIColor.black : UIColor(rgb: 0x060B18)) : UIColor(rgb: 0xFAFCFF)).setFill()
        context.fill(bounds)
        let glow: UIColor
        switch phase {
        case 0: glow = UIColor(rgb: 0xFF865D, alpha: 0.60)
        case 1: glow = UIColor(rgb: 0xFF5A36, alpha: 0.73)
        case 2: glow = UIColor(rgb: 0x1A3771, alpha: 0.90)
        default: glow = UIColor(rgb: 0x0B2B68, alpha: 0.90)
        }
        radial(context, color: glow, center: CGPoint(x: bounds.width * 0.95, y: bounds.height * 0.05), radius: bounds.width * 0.82)
        radial(context, color: dark ? UIColor(rgb: 0x0B142D, alpha: 0.72) : UIColor(rgb: 0xD8E9FF, alpha: 0.85),
               center: CGPoint(x: bounds.width * 0.08, y: bounds.height * 0.95), radius: bounds.width * 0.68)
    }

    private func radial(_ context: CGContext, color: UIColor, center: CGPoint, radius: CGFloat) {
        guard let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(),
            colors: [color.cgColor, color.withAlphaComponent(0).cgColor] as CFArray, locations: [0, 1]) else { return }
        context.drawRadialGradient(gradient, startCenter: center, startRadius: 0, endCenter: center, endRadius: radius, options: [])
    }
}

private extension UIColor {
    convenience init(rgb: UInt32, alpha: CGFloat = 1) {
        self.init(red: CGFloat((rgb >> 16) & 255) / 255, green: CGFloat((rgb >> 8) & 255) / 255,
                  blue: CGFloat(rgb & 255) / 255, alpha: alpha)
    }
}
