import UIKit
import SwiftUI
import KMMViewModelSwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    @StateViewModel var viewModel = MainViewModel()
    
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController(viewModel: viewModel)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
