import android.app.TimePickerDialog
import androidx.compose.runtime.Composable
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.toMilliseconds
import java.text.SimpleDateFormat
import java.util.*

actual fun getPlatformName(): String = "Android"

actual fun Timestamp.format(format: String) = SimpleDateFormat(
    /* pattern = */ format,
    /* locale = */ Locale.getDefault()
).format(Date(this.seconds * 1000))

actual fun randomUUID(): String = UUID.randomUUID().toString()

@Composable
fun MainView(
    viewModel: MainViewModel,
    showTimePicker: (Child, EventType) -> Unit = { _, _ -> },
    editEvent: (String, Timestamp) -> Unit = { _, _ -> },
) = App(viewModel, showTimePicker, editEvent)
