import UIKit
import SwiftUI
import KMMViewModelSwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    var viewModel: MainViewModel
    
    init() {
        self.viewModel = MainViewModel()
    }
    
    func makeUIViewController(context: Context) -> UIViewController {
        Main_iosKt.MainViewController(viewModel: viewModel)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    
    var body: some View {
        ZStack {
            let composeView = ComposeView()
            composeView.ignoresSafeArea(.keyboard) // Compose has own keyboard handler
        }
    }
}
