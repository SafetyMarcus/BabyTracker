import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.gitlive.firebase.firestore.Timestamp

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun App(
    viewModel: MainViewModel
) = AppTheme {
    var selectedTabPosition by remember { mutableStateOf(0) }
    val children = remember { viewModel.children }
    val currentChild by remember(children) { derivedStateOf { children.getOrNull(selectedTabPosition) } }
    val allEvents = remember { viewModel.events }
    val currentTabEvents by remember(currentChild, allEvents) {
        derivedStateOf {
            allEvents.filter { it.child() == currentChild?.id }
        }
    }

    Scaffold(
        topBar = {
            if (children.isNotEmpty()) TabRow(
                selectedTabIndex = selectedTabPosition,
                modifier = Modifier.fillMaxWidth(),
                divider = { Divider() },
            ) {
                children.forEachIndexed { i, child ->
                    Tab(
                        selected = selectedTabPosition == i,
                        onClick = { selectedTabPosition = i },
                        text = { Text(child.firstName) }
                    )
                }
            }
        }
    ) {
        if (currentTabEvents.isNotEmpty()) LazyColumn(
            modifier = Modifier.padding(it).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(
                count = currentTabEvents.size
            ) {
                when (val row = currentTabEvents[it]) {
                    is Rows.Day -> Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    is Rows.Event -> Event(row)
                }
            }
        }
    }
}

@Composable
private fun Event(event: Rows.Event) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = CenterVertically,
) {
    Text(
        text = event.time,
        style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.size(16.dp))
    Text(
        text = event.label,
        style = MaterialTheme.typography.headlineLarge,
    )
}

expect fun getPlatformName(): String

expect fun Timestamp.format(format: String): String