import UIKit
import Shared

/// Native background and glass stay below a transparent Compose foreground.
/// UIVisualEffectView can therefore sample real UIKit content, not an opaque Skia canvas.
final class NativeGlassViewController: UIViewController, NativeGlassHost {
    private let ambient = CoffeeAmbientView()
    private let materials = UIView()
    private let modalBackdropBlur = UIVisualEffectView()
    private var modalBackdropAnimator: UIViewPropertyAnimator?
    private var modalBackdropProgress: CGFloat = 0
    private var glassViews: [String: UIVisualEffectView] = [:]
    // Returning to a page reuses its material objects instead of constructing
    // several UIGlassEffect views on the first frame of the pop animation.
    private var reusableGlassViews: [UIVisualEffectView] = []
    private var materialOpacities: [String: CGFloat] = [:]
    private var materialMasks: [String: String] = [:]
    private var pressedMaterials = Set<String>()
    private var phase: Int32 = -1
    private var compose: UIViewController!
    private var reviewScene = ""
    private var fullScreen = false
    var onFullScreenChanged: ((Bool) -> Void)?

    override var prefersStatusBarHidden: Bool { fullScreen }
    override var preferredStatusBarUpdateAnimation: UIStatusBarAnimation { .fade }
    override var prefersHomeIndicatorAutoHidden: Bool { fullScreen }

    func setFullScreen(enabled: Bool) {
        fullScreen = enabled
        onFullScreenChanged?(enabled)
        let update = {
            self.setNeedsStatusBarAppearanceUpdate()
            self.parent?.setNeedsStatusBarAppearanceUpdate()
            self.setNeedsUpdateOfHomeIndicatorAutoHidden()
        }
        if UIAccessibility.isReduceMotionEnabled { update() }
        else { UIView.animate(withDuration: 0.2, animations: update) }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .clear
        ambient.isUserInteractionEnabled = false
        materials.isUserInteractionEnabled = false
        materials.clipsToBounds = true
        view.addSubview(ambient)
        view.addSubview(materials)
        // The modal's Compose foreground is above this; every native glass object is below it.
        modalBackdropBlur.isUserInteractionEnabled = false
        modalBackdropBlur.accessibilityElementsHidden = true
        modalBackdropBlur.isHidden = true
        modalBackdropBlur.clipsToBounds = true
        view.addSubview(modalBackdropBlur)
        var night = false
        var clockMinute: Int32 = -1
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        if let index = arguments.firstIndex(of: "--design-review"), arguments.indices.contains(index + 1) {
            reviewScene = arguments[index + 1]
            night = arguments.contains("--night")
            if let clockIndex = arguments.firstIndex(of: "--review-clock-minute"),
               arguments.indices.contains(clockIndex + 1) {
                clockMinute = Int32(arguments[clockIndex + 1]) ?? -1
            }
        }
        #endif
        compose = MainViewControllerKt.MainViewController(nativeGlassHost: NativeGlassRelay(owner: self),
            reviewScene: reviewScene, reviewNight: night, reviewClockMinute: clockMinute)
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
        modalBackdropBlur.frame = view.bounds
        compose.view.frame = view.bounds
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        setFullScreen(enabled: fullScreen)
        #if DEBUG
        if !reviewScene.isEmpty, #available(iOS 16.0, *) {
            view.window?.windowScene?.requestGeometryUpdate(.iOS(interfaceOrientations: .landscapeLeft))
        }
        #endif
    }

    func setPhase(phase: Int32) {
        guard self.phase != phase else { return }
        self.phase = phase
        // Style the materials without overriding Compose's system-theme observer.
        materials.overrideUserInterfaceStyle = phase >= 2 ? .dark : .light
        modalBackdropBlur.overrideUserInterfaceStyle = materials.overrideUserInterfaceStyle
        #if DEBUG
        if !reviewScene.isEmpty { overrideUserInterfaceStyle = materials.overrideUserInterfaceStyle }
        #endif
        recordReviewEvidence()
    }

    func setAmbientColors(base: Int32, glow: Int32, cool: Int32) {
        let colors = CoffeeAmbientView.Colors(base: base, glow: glow, cool: cool)
        guard ambient.colors != colors else { return }
        let initial = ambient.colors == nil
        let changes = {
            self.ambient.colors = colors
            self.ambient.setNeedsDisplay()
        }
        if initial || UIAccessibility.isReduceMotionEnabled {
            changes()
        } else {
            UIView.transition(with: ambient, duration: 1.2,
                options: [.transitionCrossDissolve, .beginFromCurrentState, .allowUserInteraction],
                animations: changes)
        }
    }

    func setBackdropBlur(progress: Double) {
        let value = CGFloat(progress.isFinite ? min(1, max(0, progress)) : 0)
        guard modalBackdropProgress != value else { return }
        modalBackdropProgress = value
        if value <= 0 {
            modalBackdropAnimator?.stopAnimation(true)
            modalBackdropAnimator = nil
            UIView.performWithoutAnimation {
                modalBackdropBlur.effect = nil
                modalBackdropBlur.isHidden = true
            }
            return
        }
        if modalBackdropAnimator == nil {
            modalBackdropBlur.effect = nil
            let animator = UIViewPropertyAnimator(duration: 1, curve: .linear) { [weak self] in
                self?.modalBackdropBlur.effect = UIBlurEffect(style: .systemUltraThinMaterial)
            }
            animator.pausesOnCompletion = true
            animator.startAnimation()
            animator.pauseAnimation()
            modalBackdropAnimator = animator
        }
        modalBackdropBlur.isHidden = false
        modalBackdropAnimator?.fractionComplete = value
    }

    func updateMaterial(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
        let effectView: UIVisualEffectView
        if let existing = glassViews[key] { effectView = existing }
        else {
            if let reusable = reusableGlassViews.popLast() { effectView = reusable }
            else if #available(iOS 26.0, *) {
                let effect = UIGlassEffect(style: .regular)
                effect.isInteractive = true
                effectView = UIVisualEffectView(effect: effect)
            } else {
                effectView = UIVisualEffectView(effect: UIBlurEffect(style: .systemMaterial))
            }
            effectView.isUserInteractionEnabled = false
            effectView.accessibilityElementsHidden = true
            effectView.clipsToBounds = true
            glassViews[key] = effectView
            materials.addSubview(effectView)
        }
        let frame = CGRect(x: x, y: y, width: max(0, width), height: max(0, height))
        UIView.performWithoutAnimation {
            effectView.frame = materials.convert(frame, from: view.window)
            effectView.isHidden = width <= 0 || height <= 0
            effectView.alpha = (materialOpacities[key] ?? 1) * (pressedMaterials.contains(key) ? 0.90 : 1)
            if #available(iOS 26.0, *) { effectView.cornerConfiguration = .corners(radius: .fixed(CGFloat(radius))) }
            else {
                effectView.layer.cornerRadius = CGFloat(radius)
                effectView.layer.cornerCurve = .continuous
                effectView.clipsToBounds = true
            }
            applyMask(key: key, to: effectView)
        }
        recordReviewEvidence()
    }

    func setMaterialPressed(key: String, pressed: Bool) {
        guard let effectView = glassViews[key] else { return }
        if pressed { pressedMaterials.insert(key) } else { pressedMaterials.remove(key) }
        let opacity = materialOpacities[key] ?? 1
        UIView.animate(withDuration: UIAccessibility.isReduceMotionEnabled ? 0 : 0.16,
                       delay: 0, options: [.beginFromCurrentState, .allowUserInteraction]) {
            effectView.alpha = opacity * (pressed ? 0.90 : 1)
        }
    }

    func setMaterialOpacity(key: String, opacity: Double) {
        let value = CGFloat(min(1, max(0, opacity)))
        guard materialOpacities[key] != value else { return }
        materialOpacities[key] = value
        guard let effectView = glassViews[key] else { return }
        effectView.layer.removeAnimation(forKey: "opacity")
        UIView.performWithoutAnimation {
            effectView.alpha = value * (pressedMaterials.contains(key) ? 0.90 : 1)
        }
    }

    func removeMaterial(key: String) {
        if let effectView = glassViews.removeValue(forKey: key) {
            effectView.layer.removeAllAnimations()
            effectView.removeFromSuperview()
            effectView.alpha = 0
            if reusableGlassViews.count < 12 { reusableGlassViews.append(effectView) }
        }
        materialOpacities.removeValue(forKey: key)
        materialMasks.removeValue(forKey: key)
        pressedMaterials.remove(key)
        recordReviewEvidence()
    }

    func setMaterialMask(key: String, path: String) {
        guard materialMasks[key] != path else { return }
        materialMasks[key] = path
        if let view = glassViews[key] { UIView.performWithoutAnimation { applyMask(key: key, to: view) } }
    }

    private func applyMask(key: String, to effectView: UIVisualEffectView) {
        guard let encoded = materialMasks[key], !encoded.isEmpty else {
            effectView.mask = nil
            if #available(iOS 26.0, *), !(effectView.effect is UIGlassEffect) {
                let effect = UIGlassEffect(style: .regular)
                effect.isInteractive = true
                effectView.effect = effect
            }
            return
        }
        // UIGlassEffect's optical backdrop ignores arbitrary layer masks on iPadOS.
        // A regular UIKit blur honors UIVisualEffectView.mask for the curved lens.
        if !(effectView.effect is UIBlurEffect) { effectView.effect = UIBlurEffect(style: .systemMaterial) }
        let path = UIBezierPath()
        for command in encoded.split(separator: ";") {
            let parts = command.split(separator: ",")
            let values: [CGFloat] = parts.dropFirst().compactMap { component in
                guard let value = Double(component), value.isFinite else { return nil }
                return CGFloat(value)
            }
            switch parts.first {
            case "M" where values.count == 2: path.move(to: CGPoint(x: values[0], y: values[1]))
            case "L" where values.count == 2: path.addLine(to: CGPoint(x: values[0], y: values[1]))
            case "Q" where values.count == 4: path.addQuadCurve(to: CGPoint(x: values[2], y: values[3]), controlPoint: CGPoint(x: values[0], y: values[1]))
            case "Z": path.close()
            default: break
            }
        }
        let maskView = (effectView.mask as? CoffeeMaterialMaskView) ?? CoffeeMaterialMaskView()
        maskView.frame = effectView.bounds
        let layer = maskView.layer as! CAShapeLayer
        layer.fillColor = UIColor.black.cgColor
        layer.path = path.cgPath
        effectView.mask = maskView
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        reusableGlassViews.removeAll()
    }

    func scanDeviceCode(title: String, closeTitle: String, onScanned: @escaping (String) -> Void, onClosed: @escaping () -> Void, onError: @escaping (String) -> Void) {
        guard presentedViewController == nil else {
            onError("camera-unavailable")
            return
        }
        view.endEditing(true)
        let camera = NativeDeviceScannerViewController(title: title, closeTitle: closeTitle) { [weak self] outcome in
            self?.dismiss(animated: true) {
                switch outcome {
                case .scanned(let code): onScanned(code)
                case .closed: onClosed()
                case .failed(let reason): onError(reason)
                }
            }
        }
        let navigation = UINavigationController(rootViewController: camera)
        navigation.modalPresentationStyle = .fullScreen
        present(navigation, animated: true)
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

private final class CoffeeMaterialMaskView: UIView {
    override class var layerClass: AnyClass { CAShapeLayer.self }
}

/// Kotlin retains its host callback; the relay avoids retaining the owning controller.
private final class NativeGlassRelay: NSObject, NativeGlassHost {
    private weak var owner: NativeGlassViewController?
    init(owner: NativeGlassViewController) { self.owner = owner }
    func setFullScreen(enabled: Bool) { owner?.setFullScreen(enabled: enabled) }
    func setBackdropBlur(progress: Double) { owner?.setBackdropBlur(progress: progress) }
    func setPhase(phase: Int32) { owner?.setPhase(phase: phase) }
    func setAmbientColors(base: Int32, glow: Int32, cool: Int32) {
        owner?.setAmbientColors(base: base, glow: glow, cool: cool)
    }
    func updateMaterial(key: String, x: Double, y: Double, width: Double, height: Double, radius: Double) {
        owner?.updateMaterial(key: key, x: x, y: y, width: width, height: height, radius: radius)
    }
    func setMaterialPressed(key: String, pressed: Bool) { owner?.setMaterialPressed(key: key, pressed: pressed) }
    func setMaterialOpacity(key: String, opacity: Double) { owner?.setMaterialOpacity(key: key, opacity: opacity) }
    func setMaterialMask(key: String, path: String) { owner?.setMaterialMask(key: key, path: path) }
    func removeMaterial(key: String) { owner?.removeMaterial(key: key) }
    func scanDeviceCode(title: String, closeTitle: String, onScanned: @escaping (String) -> Void, onClosed: @escaping () -> Void, onError: @escaping (String) -> Void) {
        owner?.scanDeviceCode(title: title, closeTitle: closeTitle, onScanned: onScanned, onClosed: onClosed, onError: onError)
    }
}

private final class CoffeeAmbientView: UIView {
    struct Colors: Equatable {
        let base: Int32
        let glow: Int32
        let cool: Int32
    }

    var colors: Colors?

    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }
        (colors.map { UIColor(argb: $0.base) } ?? .black).setFill()
        context.fill(bounds)
        guard let colors else { return }
        radial(context, color: UIColor(argb: colors.glow),
               center: CGPoint(x: bounds.width * 0.95, y: bounds.height * 0.05), radius: bounds.width * 0.82)
        radial(context, color: UIColor(argb: colors.cool),
               center: CGPoint(x: bounds.width * 0.08, y: bounds.height * 0.95), radius: bounds.width * 0.68)
    }

    private func radial(_ context: CGContext, color: UIColor, center: CGPoint, radius: CGFloat) {
        guard color.cgColor.alpha > 0, let gradient = CGGradient(colorsSpace: CGColorSpaceCreateDeviceRGB(),
            colors: [color.cgColor, color.withAlphaComponent(0).cgColor] as CFArray, locations: [0, 1]) else { return }
        context.drawRadialGradient(gradient, startCenter: center, startRadius: 0, endCenter: center, endRadius: radius, options: [])
    }
}

private extension UIColor {
    convenience init(argb: Int32) {
        let bits = UInt32(bitPattern: argb)
        self.init(red: CGFloat((bits >> 16) & 255) / 255, green: CGFloat((bits >> 8) & 255) / 255,
                  blue: CGFloat(bits & 255) / 255, alpha: CGFloat((bits >> 24) & 255) / 255)
    }
}
