import androidx.compose.ui.window.ComposeUIViewController
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.toMilliseconds
import platform.Foundation.*

actual fun getPlatformName(): String = "iOS"

actual fun Timestamp.format(format: String): String {
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.dateFormat = format
    return formatter.stringFromDate(
        NSDate(timeIntervalSinceReferenceDate = this.seconds.toDouble())
    )
}

fun MainViewController(viewModel: MainViewModel) = ComposeUIViewController { App(viewModel) }