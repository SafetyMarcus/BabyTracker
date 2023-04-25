import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            allEvents.filter { it.child == currentChild?.id }
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
            contentPadding = PaddingValues(16.dp)
        ) {
            items(
                count = currentTabEvents.size
            ) {
                val event = currentTabEvents[it]
                Text(
                    text = event.event,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

expect fun getPlatformName(): String