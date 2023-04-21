import androidx.compose.ui.window.ComposeUIViewController

actual fun getPlatformName(): String = "iOS"

fun MainViewController(viewModel: MainViewModel) = ComposeUIViewController { App(viewModel) }