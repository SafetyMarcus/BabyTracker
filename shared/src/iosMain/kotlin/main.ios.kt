import androidx.compose.ui.window.ComposeUIViewController
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import dev.gitlive.firebase.firestore.Timestamp
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterRoundDown
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

actual fun generateImageLoader(): ImageLoader = ImageLoader {
    components {
        setupDefaultComponents(imageScope)
    }
    interceptor {
        memoryCacheConfig {
            maxSizePercent(0.25)
        }
        diskCacheConfig {
            maxSizeBytes(512L * 1024 * 1024) // 512MB
        }
    }
}

actual fun Float.format() = NSNumberFormatter().apply {
    roundingMode = NSNumberFormatterRoundDown
}.stringFromNumber(NSNumber(this)) ?: ""

fun MainViewController(
    viewModel: MainViewModel,
    showTimePicker: (Timestamp?, Child, EventType) -> Unit = { _, _, _ -> },
    editEvent: (String, Timestamp) -> Unit = { _, _ -> },
    deleteEvent: (String) -> Unit = { },
) = ComposeUIViewController {
    App(
        viewModel = viewModel,
        showTimePicker = showTimePicker,
        editEvent = editEvent,
        deleteEvent = deleteEvent,
    )
}