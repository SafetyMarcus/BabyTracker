import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier


@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun App() = AppTheme {
    var selectedTabPosition by remember { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = selectedTabPosition,
                modifier = Modifier.fillMaxWidth(),
                divider = { Divider() },
            ) {
                Tab(
                    selected = selectedTabPosition == 0,
                    onClick = { selectedTabPosition = 0 },
                    text = { Text("Sebastian") }
                )
                Tab(
                    selected = selectedTabPosition == 1,
                    onClick = { selectedTabPosition = 1 },
                    text = { Text("Baby 2") }
                )
            }
        }
    ) {
    }
}

expect fun getPlatformName(): String