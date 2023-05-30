import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.model.ImageRequest
import com.seiko.imageloader.rememberAsyncImagePainter
import dev.gitlive.firebase.firestore.Timestamp
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun App(
    viewModel: MainViewModel,
    showTimePicker: (Timestamp?, Child, EventType) -> Unit = { _, _, _ -> },
    editEvent: (String, Timestamp) -> Unit = { _, _ -> },
    deleteEvent: (String) -> Unit = { },
) = AppTheme {
    var selectedTabPosition by remember { mutableStateOf(0) }
    val children = remember { viewModel.children }
    val currentChild by remember(children) { derivedStateOf { children.getOrNull(selectedTabPosition) } }
    val eventTypes = remember { viewModel.eventTypes }
    val allEvents = remember { viewModel.events }
    val allSummaries = remember { viewModel.summaries }
    var selectedCard by remember { mutableStateOf<Rows.Event?>(null) }
    val currentDay by remember { derivedStateOf { selectedCard?.day } }
    val currentSummary by remember {
        derivedStateOf {
            val day = currentDay
                ?: allEvents
                    .filterIsInstance<Rows.Day>()
                    .firstOrNull() { it.child == currentChild?.id }
                    ?.label

            day?.let { safeDay ->
                allSummaries.firstOrNull {
                    it.day == safeDay && it.child == currentChild?.id
                }
            }
        }
    }
    val currentTabEvents by remember(currentChild, allEvents) {
        derivedStateOf { allEvents.filter { it.child() == currentChild?.id } }
    }
    var showingOptions by remember { mutableStateOf<Pair<Timestamp?, Boolean>>(null to false) }

    val listState = rememberLazyListState()
    val firstEvent by remember { derivedStateOf { currentTabEvents.firstOrNull() } }
    LaunchedEffect(firstEvent?.id) { listState.animateScrollToItem(0) }

    Scaffold(
        topBar = {
            Toolbar(
                children = children,
                selectedTabPosition = selectedTabPosition,
                summary = currentSummary,
                onTabSelected = {
                    selectedTabPosition = it
                    selectedCard = null
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showingOptions = null to !showingOptions.second },
                content = { Icon(Icons.Default.Add, null) }
            )
        }
    ) {
        EventsList(
            it = it,
            state = listState,
            selectedDay = currentDay ?: "",
            selectedEvent = selectedCard?.id,
            currentTabEvents = currentTabEvents,
            onItemSelected = {
                (it as? Rows.Event)?.let { selectedCard = it }
            },
            onItemEdited = { editEvent(it.id, it.timeStamp) },
            onItemDeleted = { deleteEvent(it.id) },
            addItem = { showingOptions = it.day to true },
        )
    }
    AnimatedVisibility(
        visible = showingOptions.second,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
    ) {
        EventPicker(
            eventTypes = eventTypes,
            close = { showingOptions = null to false },
            onTypeSelected = { eventType ->
                showTimePicker(showingOptions.first, currentChild!!, eventType)
                showingOptions = null to false
            }
        )
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
        value = summary?.sleepTotalSeconds?.div(3600)?.format() ?: "0",
        label = "Hours\nAsleep",
        color = MaterialTheme.colorScheme.primary,
    )
    Tracker(
        value = summary?.feedsTotal?.roundToInt()?.toString() ?: "0",
        label = "Total\nFeeds",
        color = MaterialTheme.colorScheme.secondary,
    )
}

@Composable
private fun EventsList(
    it: PaddingValues,
    state: LazyListState,
    selectedDay: String,
    selectedEvent: String?,
    currentTabEvents: List<Rows>,
    addItem: (Rows.AddNew) -> Unit,
    onItemSelected: (Rows) -> Unit,
    onItemEdited: (Rows.Event) -> Unit,
    onItemDeleted: (Rows.Event) -> Unit,
) = LazyColumn(
    modifier = Modifier.padding(it).fillMaxWidth(),
    state = state,
    reverseLayout = true,
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

            is Rows.AddNew -> AddNewEvent(
                onClick = { addItem(row) }
            )

            is Rows.Event -> Event(
                event = row,
                selected = selectedEvent == row.id,
                onClick = { onItemSelected(row) },
                onEdit = onItemEdited,
                onDelete = onItemDeleted,
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
    onDelete: (Rows.Event) -> Unit,
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
                Row {
                    IconButton(
                        modifier = Modifier.size(40.dp).padding(end = 16.dp),
                        onClick = { onEdit(event) },
                        content = { Icon(Icons.Filled.Edit, null) }
                    )
                    Spacer(Modifier.size(12.dp))
                    IconButton(
                        modifier = Modifier.size(40.dp).padding(end = 16.dp),
                        onClick = { onDelete(event) },
                        content = { Icon(Icons.Filled.Delete, null) }
                    )
                }
            }
        }
        Spacer(Modifier.size(16.dp))
    }
}

@Composable
private fun AddNewEvent(
    onClick: () -> Unit,
) = TextButton(
    modifier = Modifier.padding(horizontal = 12.dp),
    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    onClick = onClick,
) {
    Icon(Icons.Filled.Add, null)
    Spacer(Modifier.size(8.dp))
    Text("Add New Event")
}

@Composable
private fun EventPicker(
    eventTypes: List<EventTypes>,
    onTypeSelected: (EventType) -> Unit,
    close: () -> Unit,
) = ElevatedCard(
    modifier = Modifier.fillMaxSize(),
    shape = RectangleShape,
    elevation = CardDefaults.elevatedCardElevation(6.dp),
) {
    Column(
        Modifier.padding(24.dp)
    ) {
        IconButton(onClick = close) {
            Icon(
                Icons.Filled.Close,
                null
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        EventGrid(eventTypes, onTypeSelected)
    }
}

@Composable
private fun EventGrid(
    eventTypes: List<EventTypes>,
    onTypeSelected: (EventType) -> Unit,
) = LazyVerticalGrid(
    modifier = Modifier.fillMaxSize(),
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(
        count = eventTypes.size,
    ) {
        val event = eventTypes[it]
        OutlinedButton(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            onClick = {
                onTypeSelected(EventType.valueOf(event.id))
            }
        ) { EventButton(event) }
    }
}

@Composable
private fun EventButton(event: EventTypes) = Column(Modifier.fillMaxSize()) {
    CompositionLocalProvider(
        LocalImageLoader provides generateImageLoader()
    ) {
        Icon(
            painter = rememberAsyncImagePainter(
                ImageRequest { data(event.image) }
            ),
            tint = Color.Unspecified,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.fillMaxWidth().padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 24.dp,
            ),
            text = event.label,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

expect fun getPlatformName(): String

expect fun Timestamp.format(format: String): String

expect fun randomUUID(): String

expect fun generateImageLoader(): ImageLoader

expect fun Float.format(): String