import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
    var showingOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Toolbar(children, selectedTabPosition) {
                selectedTabPosition = it
            }
        },
    ) { EventsList(currentTabEvents, it) }
    val bg by animateColorAsState(
        if (showingOptions) Color.Black.copy(alpha = 0.3f)
        else Color.Transparent
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
        contentAlignment = BottomEnd,
    ) {
        if (showingOptions) Box(
            modifier = Modifier.fillMaxSize().clickable { showingOptions = false }
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            val values = EventType.values().reversed()
            values.forEachIndexed { index, eventType ->
                FabOption(
                    visible = showingOptions,
                    index = values.size - index,
                    text = eventType
                        .name
                        .replace("_", " ")
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                ) {
                    showingOptions = false
                    currentChild?.let { viewModel.addEvent(it, eventType) }
                }
            }

            val rotation by animateFloatAsState(if (showingOptions) 45f else 0f)
            FloatingActionButton(
                modifier = Modifier.rotate(rotation),
                onClick = { showingOptions = !showingOptions },
                content = { Icon(Icons.Default.Add, null) }
            )
        }
    }
}

@Composable
private fun FabOption(
    visible: Boolean,
    text: String,
    index: Int,
    onClick: () -> Unit
) = AnimatedVisibility(
    visible,
    enter = fadeIn(
        tween(delayMillis = 100 * index)
    ) + slideInVertically(
        tween(delayMillis = 100 * index)
    ) { it / 2 },
    exit = slideOutVertically { it / 2 } + fadeOut()
) {
    ExtendedFloatingActionButton(
        modifier = Modifier.padding(bottom = 16.dp),
        onClick = onClick,
        content = { Text(text) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    children: SnapshotStateList<Child>,
    selectedTabPosition: Int,
    onTabSelected: (Int) -> Unit,
) = Column {
    CenterAlignedTopAppBar(title = { Text("Events") })
    if (children.isNotEmpty()) TabRow(
        selectedTabIndex = selectedTabPosition,
        modifier = Modifier.fillMaxWidth(),
        divider = { Divider() },
    ) {
        children.forEachIndexed { i, child ->
            Tab(
                selected = selectedTabPosition == i,
                onClick = { onTabSelected(i) },
                text = { Text(child.firstName) }
            )
        }
    }
}

@Composable
private fun EventsList(
    currentTabEvents: List<Rows>,
    it: PaddingValues
) = LazyColumn(
    modifier = Modifier.padding(it).fillMaxSize(),
    contentPadding = PaddingValues(
        top = 16.dp,
        start = 16.dp,
        end = 16.dp,
        bottom = 56.dp,
    ),
) {
    items(
        count = currentTabEvents.size
    ) {
        when (val row = currentTabEvents[it]) {
            is Rows.Day -> Text(
                modifier = Modifier.padding(top = 16.dp),
                text = row.label,
                style = MaterialTheme.typography.bodyLarge,
            )
            is Rows.Event -> Event(row)
        }
    }
}

@Composable
private fun Event(event: Rows.Event) = Row(
    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    verticalAlignment = CenterVertically,
) {
    Text(
        text = event.time,
        style = MaterialTheme.typography.titleLarge,
    )
    Spacer(Modifier.size(16.dp))
    Text(
        text = event.label,
        style = MaterialTheme.typography.headlineLarge,
    )
}

expect fun getPlatformName(): String

expect fun Timestamp.format(format: String): String