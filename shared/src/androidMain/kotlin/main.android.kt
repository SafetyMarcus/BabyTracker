import androidx.compose.runtime.Composable

actual fun getPlatformName(): String = "Android"

@Composable fun MainView(viewModel: MainViewModel) = App(viewModel)
