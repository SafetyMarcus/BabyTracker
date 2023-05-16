import androidx.compose.ui.window.ComposeUIViewController
import dev.gitlive.firebase.firestore.Timestamp
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSUUID

actual fun getPlatformName(): String = "iOS"

actual fun Timestamp.format(format: String): String {
    val formatter = NSDateFormatter()
    formatter.dateStyle = NSDateFormatterMediumStyle
    formatter.dateFormat = format
    return formatter.stringFromDate(
        NSDate(timeIntervalSinceReferenceDate = this.seconds.toDouble())
    )
}

actual fun randomUUID(): String = NSUUID().UUIDString

fun MainViewController(
    viewModel: MainViewModel,
    showTimePicker: (Child, EventType) -> Unit = { _, _ -> },
    editEvent: (String, Timestamp) -> Unit = { _, _ -> },
) = ComposeUIViewController {
    App(
        viewModel = viewModel,
        showTimePicker = showTimePicker,
        editEvent = editEvent,
    )
}