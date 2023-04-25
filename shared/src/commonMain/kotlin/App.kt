import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.initialize

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun App(
    viewModel: MainViewModel
) = AppTheme {
    val currentName by remember { viewModel.name }
    val event by remember { derivedStateOf { viewModel.events.getOrElse(0) { "" } } }
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
                    onClick = {
                        selectedTabPosition = 0
                        viewModel.setCurrentTab(0)
                    },
                    text = { Text("Sebastian") }
                )
                Tab(
                    selected = selectedTabPosition == 1,
                    onClick = {
                        selectedTabPosition = 1
                        viewModel.setCurrentTab(1)
                    },
                    text = { Text("Baby 2") }
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(it)) {
            Text(
                text = event,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

expect fun getPlatformName(): String