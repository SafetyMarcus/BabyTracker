import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
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
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun App(
    viewModel: MainViewModel,
    showTimePicker: (Child, EventType) -> Unit = { _, _ -> },
    editEvent: (String, Timestamp) -> Unit = { _, _ -> },
) = AppTheme {
    var selectedTabPosition by remember { mutableStateOf(0) }
    val children = remember { viewModel.children }
    val currentChild by remember(children) { derivedStateOf { children.getOrNull(selectedTabPosition) } }
    val allEvents = remember { viewModel.events }
    val allSummaries = remember { viewModel.summaries }
    var currentDay by remember { mutableStateOf("") }
    var selectedCard by remember { mutableStateOf<String?>(null) }
    val currentSummary by remember {
        derivedStateOf {
            val day = currentDay.takeUnless { it.isEmpty() }
                ?: allEvents.filterIsInstance<Rows.Day>().lastOrNull()?.label
            day?.let { safeDay -> allSummaries.firstOrNull { it.day == safeDay } }
        }
    }
    val currentTabEvents by remember(currentChild, allEvents) {
        derivedStateOf { allEvents.filter { it.child() == currentChild?.id } }
    }
    var showingOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Toolbar(
                children = children,
                selectedTabPosition = selectedTabPosition,
                summary = currentSummary,
                onTabSelected = { selectedTabPosition = it }
            )
        },
    ) {
        EventsList(
            it = it,
            selectedDay = currentDay,
            selectedEvent = selectedCard,
            currentTabEvents = currentTabEvents,
            onItemSelected = {
                (it as? Rows.Event)?.let {
                    currentDay = it.day
                    selectedCard = it.id
                }
            },
            onItemEdited = { editEvent(it.id, it.timeStamp) }
        )
    }
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
                    currentChild?.let { showTimePicker(it, eventType) }
                }
            }

            val rotation by animateFloatAsState(if (showingOptions) 45f else 0f)
            val container by animateColorAsState(
                if (showingOptions) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer
            )
            val content by animateColorAsState(
                if (showingOptions) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )
            FloatingActionButton(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .rotate(rotation),
                containerColor = container,
                contentColor = content,
                onClick = { showingOptions = !showingOptions },
                content = { Icon(Icons.Default.Add, null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    children: SnapshotStateList<Child>,
    selectedTabPosition: Int,
    summary: Summary?,
    onTabSelected: (Int) -> Unit,
) = Column {
    CenterAlignedTopAppBar(title = { Text("Events") })
    SummaryDisplay(summary)
    if (children.isNotEmpty()) TabRow(
        selectedTabIndex = selectedTabPosition,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
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
private fun SummaryDisplay(summary: Summary?) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly,
) {
    Tracker(
        value = summary?.wetNappyTotal?.roundToInt()?.toString() ?: "0",
        label = "Wet\nNappies",
        color = MaterialTheme.colorScheme.primary,
    )
    Tracker(
        value = summary?.mixedNappyTotal?.roundToInt()?.toString() ?: "0",
        label = "Mixed\nNappies",
        color = MaterialTheme.colorScheme.secondary,
    )
    Tracker(
        value = summary?.sleepTotal?.div(60f)?.roundToInt()?.toString() ?: "0",
        label = "Hours\nAsleep",
        color = MaterialTheme.colorScheme.tertiary,
    )
}

@Composable
private fun EventsList(
    it: PaddingValues,
    selectedDay: String,
    selectedEvent: String?,
    currentTabEvents: List<Rows>,
    onItemSelected: (Rows) -> Unit,
    onItemEdited: (Rows.Event) -> Unit,
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
            is Rows.Day -> DayDivider(
                label = row.label,
                selected = selectedDay == row.label,
            )

            is Rows.Event -> Event(
                event = row,
                selected = selectedEvent == row.id,
                onClick = { onItemSelected(row) },
                onEdit = onItemEdited,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Event(
    event: Rows.Event,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: (Rows.Event) -> Unit,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    )
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = container,
            contentColor = content,
        ),
        onClick = onClick,
    ) {
        Spacer(Modifier.size(16.dp))
        Row {
            Column {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = event.label,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = event.time,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(
                modifier = Modifier.fillMaxHeight().align(CenterVertically),
                visible = selected,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = slideOutHorizontally { it / 2 } + fadeOut(),
            ) {
                IconButton(
                    modifier = Modifier.size(40.dp).padding(end = 16.dp),
                    onClick = { onEdit(event) },
                    content = { Icon(Icons.Filled.Edit, null) }
                )
            }
        }
        Spacer(Modifier.size(16.dp))
    }
}

expect fun getPlatformName(): String

expect fun Timestamp.format(format: String): String

expect fun randomUUID(): String