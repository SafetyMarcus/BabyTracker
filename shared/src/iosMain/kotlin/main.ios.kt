import androidx.compose.ui.window.ComposeUIViewController
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.toMilliseconds
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeInterval

actual fun getPlatformName(): String = "iOS"

actual fun Timestamp.format(format: String): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = format
    return formatter.stringFromDate(NSDate(this.toMilliseconds()))
}

fun MainViewController(viewModel: MainViewModel) = ComposeUIViewController { App(viewModel) }