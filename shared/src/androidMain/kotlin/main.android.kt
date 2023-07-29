import android.content.Context
import android.icu.number.NumberFormatter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.mapper.Base64Mapper
import com.seiko.imageloader.component.setupBase64Components
import com.seiko.imageloader.component.setupCommonComponents
import com.seiko.imageloader.component.setupDefaultComponents
import dev.gitlive.firebase.firestore.Timestamp
import okio.Path.Companion.toOkioPath
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

actual fun getPlatformName(): String = "Android"

actual fun Timestamp.format(format: String) = SimpleDateFormat(
    /* pattern = */ format,
    /* locale = */ Locale.getDefault()
).format(Date(this.seconds * 1000))

actual fun randomUUID(): String = UUID.randomUUID().toString()

private var imageLoader: ImageLoader? = null

actual fun generateImageLoader(): ImageLoader = imageLoader!!

actual fun Float.format() = DecimalFormat("#.#").apply {
    roundingMode = RoundingMode.DOWN
}.format(this)

@Composable
actual fun DeleteAlert(
    deleteClicked: () -> Unit,
    cancelClicked: () -> Unit,
) = AlertDialog(
    title = { Text(text = "Delete") },
    text = { Text(text = "Are you sure you want to delete this event?") },
    confirmButton = {
        TextButton(
            onClick = deleteClicked
        ) { Text(text = "Delete") }
    },
    dismissButton = {
        TextButton(onClick = cancelClicked) {
            Text(text = "Cancel")
        }
    },
    onDismissRequest = cancelClicked,
)

@Composable
fun MainView(
    viewModel: MainViewModel,
    context: Context,
    showTimePicker: (Timestamp?, Child, EventType) -> Unit = { _, _, _ -> },
    editEvent: (String, Timestamp) -> Unit = { _, _ -> },
) = App(viewModel, showTimePicker, editEvent).also {
    imageLoader = ImageLoader {
        components {
            setupDefaultComponents(context)
        }
        interceptor {
            memoryCacheConfig {
                // Set the max size to 25% of the app's available memory.
                maxSizePercent(context, 0.25)
            }
            diskCacheConfig {
                directory(context.cacheDir.resolve("image_cache").toOkioPath())
                maxSizeBytes(512L * 1024 * 1024) // 512MB
            }
        }
    }
}
