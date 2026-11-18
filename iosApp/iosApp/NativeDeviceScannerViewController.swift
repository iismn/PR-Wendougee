import AVFoundation
import UIKit

/// The preview owns an AVCaptureVideoPreviewLayer, so its size follows UIKit layout.
private final class DeviceCameraPreview: UIView {
    override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
    var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
}

final class NativeDeviceScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    enum Outcome { case scanned(String), closed, failed(String) }

    private let closeTitle: String
    private let onFinish: (Outcome) -> Void
    private let session = AVCaptureSession()
    private let captureQueue = DispatchQueue(label: "app.wendougee.device-qr-camera")
    private let preview = DeviceCameraPreview()
    private var started = false
    private var finished = false
    private var errorObserver: NSObjectProtocol?
    private var rotationCoordinator: AVCaptureDevice.RotationCoordinator?
    private var rotationObservation: NSKeyValueObservation?

    init(title: String, closeTitle: String, onFinish: @escaping (Outcome) -> Void) {
        self.closeTitle = closeTitle
        self.onFinish = onFinish
        super.init(nibName: nil, bundle: nil)
        self.title = title
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) has not been implemented") }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .black
        navigationItem.leftBarButtonItem = UIBarButtonItem(title: closeTitle, style: .plain, target: self, action: #selector(closeScanner))
        preview.previewLayer.session = session
        preview.previewLayer.videoGravity = .resizeAspectFill
        preview.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(preview)
        NSLayoutConstraint.activate([
            preview.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            preview.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            preview.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            preview.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        errorObserver = NotificationCenter.default.addObserver(forName: AVCaptureSession.runtimeErrorNotification, object: session, queue: .main) { [weak self] _ in
            self?.finish(.failed("camera-unavailable"))
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        updatePreviewRotation()
    }

    private func configurePreviewRotation(for camera: AVCaptureDevice) {
        // Camera sensors have different native orientations across iPad models.
        // Let AVFoundation calculate rotation relative to this preview layer.
        let coordinator = AVCaptureDevice.RotationCoordinator(device: camera, previewLayer: preview.previewLayer)
        rotationCoordinator = coordinator
        rotationObservation = coordinator.observe(\.videoRotationAngleForHorizonLevelPreview, options: [.initial, .new]) { [weak self] coordinator, _ in
            self?.applyPreviewRotation(coordinator.videoRotationAngleForHorizonLevelPreview)
        }
    }

    private func updatePreviewRotation() {
        guard let rotationCoordinator else { return }
        applyPreviewRotation(rotationCoordinator.videoRotationAngleForHorizonLevelPreview)
    }

    private func applyPreviewRotation(_ angle: CGFloat) {
        guard let connection = preview.previewLayer.connection,
              connection.isVideoRotationAngleSupported(angle) else { return }
        connection.videoRotationAngle = angle
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        guard !started else { return }
        started = true
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: startCamera()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    guard let self, !self.finished else { return }
                    if granted { self.startCamera() }
                    else { self.finish(.failed("camera-permission")) }
                }
            }
        default: finish(.failed("camera-permission"))
        }
    }

    private func startCamera() {
        guard !finished else { return }
        captureQueue.async { [self] in
            session.beginConfiguration()
            session.sessionPreset = .high
            do {
                guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
                    throw CameraError.unavailable
                }
                let input = try AVCaptureDeviceInput(device: camera)
                let output = AVCaptureMetadataOutput()
                guard session.canAddInput(input) else { throw CameraError.unavailable }
                session.addInput(input)
                guard session.canAddOutput(output) else { throw CameraError.unavailable }
                session.addOutput(output)
                guard output.availableMetadataObjectTypes.contains(.qr) else { throw CameraError.unavailable }
                output.setMetadataObjectsDelegate(self, queue: .main)
                output.metadataObjectTypes = [.qr]
                session.commitConfiguration()
                session.startRunning()
                DispatchQueue.main.async { [weak self] in self?.configurePreviewRotation(for: camera) }
            } catch {
                session.commitConfiguration()
                DispatchQueue.main.async { [weak self] in self?.finish(.failed("camera-unavailable")) }
            }
        }
    }

    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        stopCamera()
    }

    private func stopCamera() {
        captureQueue.async { [session] in
            if session.isRunning { session.stopRunning() }
        }
    }

    func metadataOutput(_ output: AVCaptureMetadataOutput, didOutput metadataObjects: [AVMetadataObject], from connection: AVCaptureConnection) {
        for case let barcode as AVMetadataMachineReadableCodeObject in metadataObjects {
            if let payload = barcode.stringValue, !payload.isEmpty { finish(.scanned(payload)); return }
        }
    }

    @objc private func closeScanner() { finish(.closed) }

    private func finish(_ outcome: Outcome) {
        guard !finished else { return }
        finished = true
        stopCamera()
        onFinish(outcome)
    }

    deinit {
        if let errorObserver { NotificationCenter.default.removeObserver(errorObserver) }
    }

    private enum CameraError: Error { case unavailable }
}
