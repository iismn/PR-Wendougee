import UIKit
import SwiftUI
import Shared

/// SwiftUI's hosting controller owns the window's status bar, above the native/Compose child.
final class NativeSystemUiState: ObservableObject {
    @Published var fullScreen = false
}

struct ComposeView: UIViewControllerRepresentable {
    @ObservedObject var systemUi: NativeSystemUiState

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = NativeGlassViewController()
        controller.onFullScreenChanged = { [weak systemUi] enabled in
            guard let systemUi, systemUi.fullScreen != enabled else { return }
            withAnimation(.easeInOut(duration: 0.2)) { systemUi.fullScreen = enabled }
        }
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @StateObject private var systemUi = NativeSystemUiState()

    var body: some View {
        ComposeView(systemUi: systemUi)
            .ignoresSafeArea()
            .statusBarHidden(systemUi.fullScreen)
    }
}
