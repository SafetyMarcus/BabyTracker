import androidx.compose.runtime.Composable
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.toMilliseconds
import java.text.SimpleDateFormat
import java.util.*

actual fun getPlatformName(): String = "Android"

actual fun Timestamp.format(format: String) = SimpleDateFormat(
    /* pattern = */ format,
    /* locale = */ Locale.getDefault()
).format(Date(this.toMilliseconds().toLong()))

@Composable
fun MainView(viewModel: MainViewModel) = App(viewModel)
