import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.fromMilliseconds
import okio.Path.Companion.toOkioPath
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun TimePickerAlert(
    current: Timestamp,
    onSet: (Timestamp) -> Unit,
    onDismiss: () -> Unit,
) = AlertDialog(
    onDismissRequest = onDismiss,
) {
    DialogContent(current = current, onSet = onSet, onDismiss = onDismiss)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogContent(
    current: Timestamp = Timestamp.now(),
    onSet: (Timestamp) -> Unit,
    onDismiss: () -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 8.dp
        ),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val calendar by remember {
        derivedStateOf {
            Calendar.getInstance().apply { timeInMillis = current.seconds * 1000 }
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )
    TimePicker(state = timePickerState)
    DialogButtons(
        onCancelClicked = onDismiss,
        onDoneClicked = {
            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            calendar.set(Calendar.MINUTE, timePickerState.minute)
            onSet(Timestamp.fromMilliseconds(calendar.timeInMillis.toDouble()))
        },
    )
}

@Composable
private fun DialogButtons(
    onDoneClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
) {
    TextButton(
        onClick = onCancelClicked,
        content = { Text("Cancel") },
    )
    TextButton(
        onClick = onDoneClicked,
        content = { Text("Ok") },
    )
}

@Composable
fun MainView(
    viewModel: MainViewModel,
    context: Context,
) = App(viewModel).also {
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
