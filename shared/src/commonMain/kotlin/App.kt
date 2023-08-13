import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.model.ImageRequest
import com.seiko.imageloader.rememberImagePainter
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun App(
    viewModel: MainViewModel,
) = AppTheme {
    var selectedTabPosition by remember { mutableStateOf(0) }
    val children = remember { viewModel.children }
    val currentChild by remember(children) { derivedStateOf { children.getOrNull(selectedTabPosition) } }
    val eventTypes = remember { viewModel.eventTypes }
    val allEvents = remember { viewModel.events }
    val allSummaries = remember { viewModel.summaries }
    var selectedCard by remember { mutableStateOf<Rows.Event?>(null) }
    val currentDay by remember { derivedStateOf { selectedCard?.day } }

    //Event editing
    var showingDelete by remember { mutableStateOf<String?>(null) }
    var showingTimePicker by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(Timestamp.now()) }
    var currentEvent by remember { mutableStateOf<String?>(null) }
    var current by remember { mutableStateOf<Pair<Child, EventType>?>(null) }

    val currentSummary by remember {
        derivedStateOf {
            val day = currentDay
                ?: allEvents
                    .filterIsInstance<Rows.Day>()
                    .firstOrNull { it.child == currentChild?.id }
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

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        confirmValueChange = {
            if (it == SheetValue.Hidden) showingOptions = null to false
            true
        }
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = sheetState,
    )
    val scope = rememberCoroutineScope()
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 0.dp,
        sheetContent = {
            EventGrid(
                eventTypes = eventTypes,
                onTypeSelected = { eventType ->
                    scope.launch {
                        currentTime = showingOptions.first ?: Timestamp.now()
                        current = currentChild!! to eventType
                        showingTimePicker = true
                        showingOptions = null to false
                        sheetState.hide()
                    }
                }
            )
        },
        topBar = {
            Surface(
                tonalElevation = 4.dp
            ) {
                Toolbar(
                    children = children,
                    selectedTabPosition = selectedTabPosition,
                    summary = currentSummary,
                    onTabSelected = {
                        selectedTabPosition = it
                        selectedCard = null
                    }
                )
            }
        },
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            showingOptions = null to true
                            sheetState.expand()
                        }
                    },
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
                onItemEdited = {
                    currentEvent = it.id
                    currentTime = it.timeStamp
                    current = null
                    showingTimePicker = true
                },
                onItemDeleted = { showingDelete = it.id },
                addItem = {
                    scope.launch {
                        showingOptions = it.day to true
                        sheetState.expand()
                    }
                },
            )
            if (showingDelete != null) DeleteAlert(
                deleteClicked = {
                    viewModel.deleteEvent(showingDelete!!)
                    showingDelete = null
                },
                cancelClicked = { showingDelete = null },
            )
            if (showingTimePicker) TimePickerAlert(
                current = currentTime,
                onSet = {
                    current?.let { (child, type) ->
                        viewModel.addEvent(child, type, it)
                    } ?: viewModel.editEvent(currentEvent ?: "", it)
                    showingTimePicker = false
                },
                onDismiss = { showingTimePicker = false }
            )
        }
    }
}

@Composable
private fun Toolbar(
    children: SnapshotStateList<Child>,
    selectedTabPosition: Int,
    summary: Summary?,
    onTabSelected: (Int) -> Unit,
) = Column {
    Spacer(modifier = Modifier.height(48.dp))
    SummaryDisplay(summary)
    if (children.isNotEmpty()) TabRow(
        selectedTabIndex = selectedTabPosition,
        modifier = Modifier.fillMaxWidth(),
        divider = { Divider() },
    ) {
        children.forEachIndexed { i, child ->
            Tab(
                modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(24.dp)),
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
        value = summary?.sleepTotalSeconds?.div(3600)?.format() ?: "0",
        label = "Hours\nAsleep",
        color = MaterialTheme.colorScheme.primary,
    )
    Tracker(
        value = summary?.feedsTotal?.roundToInt()?.toString() ?: "0",
        label = "Total\nFeeds",
        color = MaterialTheme.colorScheme.secondary,
    )
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
    modifier = Modifier.fillMaxWidth(),
    state = state,
    reverseLayout = true,
    contentPadding = PaddingValues(
        top = 16.dp + it.calculateTopPadding(),
        start = 16.dp + it.calculateStartPadding(LayoutDirection.Ltr),
        end = 16.dp + it.calculateEndPadding(LayoutDirection.Ltr),
        bottom = 56.dp + it.calculateBottomPadding(),
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
        Row(verticalAlignment = CenterVertically) {
            Spacer(Modifier.size(8.dp))
            Column {
                Row {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = rememberImagePainter(event.image ?: ""),
                        tint = Color.Unspecified,
                        contentDescription = null,
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        text = event.label,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 28.dp),
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
private fun EventGrid(
    eventTypes: List<EventTypes>,
    onTypeSelected: (EventType) -> Unit,
) = LazyColumn(
    contentPadding = PaddingValues(
        bottom = 64.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(8.dp),
) {
    items(
        count = eventTypes.size,
    ) {
        val event = eventTypes[it]
        TextButton(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            onClick = {
                onTypeSelected(EventType.valueOf(event.id))
            }
        ) { EventButton(event) }
        if (it != eventTypes.size - 1) {
            Spacer(modifier = Modifier.size(8.dp))
            Divider(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EventButton(event: EventTypes) = Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = CenterVertically,
) {
    Icon(
        modifier = Modifier.size(48.dp),
        painter = rememberImagePainter(event.image),
        tint = Color.Unspecified,
        contentDescription = null,
    )
    Text(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        text = event.label,
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.titleMedium,
    )
}

expect fun getPlatformName(): String

expect fun Timestamp.format(format: String): String

expect fun randomUUID(): String

expect fun generateImageLoader(): ImageLoader

expect fun Float.format(): String

@Composable
expect fun DeleteAlert(
    deleteClicked: () -> Unit,
    cancelClicked: () -> Unit,
)

@Composable
expect fun TimePickerAlert(
    current: Timestamp,
    onSet: (Timestamp) -> Unit,
    onDismiss: () -> Unit,
)