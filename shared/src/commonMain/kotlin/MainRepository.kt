import androidx.compose.runtime.mutableStateListOf
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

object MainRepository {

    val children = ArrayList<Child>()
    val eventTypes = ArrayList<EventTypes>()
    val events = mutableStateListOf<Rows>()
    val summaries = mutableStateListOf<Summary>()

    suspend fun logIn() = Firebase.auth.signInWithEmailAndPassword(
        email = "",
        password = ""
    )

    suspend fun getChildren() = Firebase.firestore
        .collection("children")
        .get()
        .let {
            children.clear()
            children.addAll(
                it.documents
                    .map<DocumentSnapshot, Child> { it.data() }
                    .sortedBy { it.position }
            )
            children
        }

    suspend fun getEvents() = Firebase.firestore
        .collection("events")
        .snapshots
        .collect {
            val updatedEvents = ArrayList<Rows>()
            val updatedSummaries = ArrayList<Summary>()
            val days = children.associate { it.id to arrayListOf<Rows.Day>() }
            val labels = HashMap<String, String>()
            val images = HashMap<String, String>()
            eventTypes.forEach {
                labels[it.id] = it.label
                images[it.id] = it.image
            }
            it.documents
                .map { it.data<Event>().apply { id = it.id } }
                .sortedBy { it.time.seconds }
                .forEach { event ->
                    processEvent(
                        eventLabels = labels,
                        eventImages = images,
                        event = event,
                        days = days,
                        updatedEvents = updatedEvents,
                        updatedSummaries = updatedSummaries
                    )
                }
                .also {
                    updatedSummaries.forEach { summary ->
                        if (summary.day == Timestamp.now().format("d MMMM")) return@forEach
                        summary.sleepStartTemp?.let {
                            summary.sleepTotalSeconds += it.endOfDay().seconds - it.seconds
                        }
                        summary.sleepStartTemp = null
                    }
                }
            events.clear()
            events.addAll(updatedEvents.reversed())
            summaries.clear()
            summaries.addAll(updatedSummaries)
        }

    //TODO replace this with event tracking per child as this is unnecessarily poorly performant
    private fun ArrayList<Rows>.lastForChild(child: String) = this
        .filterIsInstance<Rows.Event>()
        .lastOrNull { it.child == child }

    private fun processEvent(
        eventLabels: Map<String, String>,
        eventImages: Map<String, String>,
        event: Event,
        days: Map<String, MutableList<Rows.Day>>,
        updatedEvents: ArrayList<Rows>,
        updatedSummaries: ArrayList<Summary>
    ) {
        val dayRow = Rows.Day(
            label = event.time.format("d MMMM"),
            child = event.child
        )
        if (days[event.child]?.contains(dayRow) != true) {
            //TODO ensure not empty check is per child
            val previous = updatedEvents.lastForChild(event.child)
            if (previous != null) {
                println("Previous day was ${previous.timeStamp.format("d MMMM")}")
                updatedEvents.add(
                    Rows.AddNew(
                        child = event.child,
                        day = previous.timeStamp.startOfDay()
                    )
                )
            }
            days[event.child]?.add(dayRow)
            updatedEvents.add(dayRow)
            updatedSummaries.add(
                Summary(
                    child = event.child,
                    day = dayRow.label,
                )
            )
        }
        val type = EventType.valueOf(event.event)
        updatedEvents.add(
            Rows.Event(
                _id = event.id,
                label = eventLabels[event.event] ?: "Unknown",
                image = eventImages[event.event],
                child = event.child,
                day = dayRow.label,
                timeStamp = event.time,
            )
        )
        updatedSummaries.lastOrNull {
            it.child == event.child && it.day == dayRow.label
        }?.apply {
            when (type) {
                EventType.MIXED_NAPPY -> mixedNappyTotal++
                EventType.WET_NAPPY -> wetNappyTotal++
                EventType.SLEEP_START -> sleepStartTemp = event.time
                EventType.SLEEP_END -> {
                    val start = sleepStartTemp ?: event.time.startOfDay()
                    sleepTotalSeconds += event.time.seconds - start.seconds
                    sleepStartTemp = null
                }

                EventType.LEFT_FEED, EventType.RIGHT_FEED -> feedsTotal++
            }
        }
    }

    private fun Timestamp.startOfDay() = Instant.fromEpochSeconds(seconds).toLocalDateTime(
        TimeZone.currentSystemDefault()
    ).let {
        val dateTime = LocalDateTime(
            year = it.year,
            monthNumber = it.monthNumber,
            dayOfMonth = it.dayOfMonth,
            hour = 0,
            minute = 0,
            second = 0,
            nanosecond = 0
        )
        Timestamp(
            seconds = dateTime.toInstant(TimeZone.currentSystemDefault()).epochSeconds,
            nanoseconds = 0,
        )
    }

    private fun Timestamp.endOfDay() = Instant.fromEpochSeconds(seconds).toLocalDateTime(
        TimeZone.currentSystemDefault()
    ).let {
        val dateTime = LocalDateTime(
            year = it.year,
            monthNumber = it.monthNumber,
            dayOfMonth = it.dayOfMonth,
            hour = 23,
            minute = 59,
            second = 59,
            nanosecond = 0
        )
        Timestamp(
            seconds = dateTime.toInstant(TimeZone.currentSystemDefault())
                .plus(1.seconds).epochSeconds,
            nanoseconds = 0,
        )
    }

    suspend fun getEventTypes() = Firebase.firestore
        .collection("event_types")
        .get()
        .let {
            eventTypes.clear()
            eventTypes.addAll(
                it.documents
                    .map { it.data<EventTypes>().apply { id = it.id } }
                    .sortedBy { it.position }
            )
            eventTypes
        }

    suspend fun createEvent(
        child: String,
        eventType: EventType,
        timestamp: Timestamp,
    ) = Firebase.firestore
        .collection("events")
        .add(
            Event(
                child = child,
                event = eventType.name.uppercase(),
                time = timestamp,
            )
        )

    suspend fun updateEvent(
        id: String,
        time: Timestamp,
    ) = Firebase.firestore
        .collection("events")
        .document(id)
        .update(mapOf("time" to time))

    suspend fun deleteEvent(
        id: String
    ) = Firebase.firestore
        .collection("events")
        .document(id)
        .delete()
}

@Serializable
data class Child(
    val id: String,
    val position: Int,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String
)

@Serializable
data class Event(
    val child: String,
    val event: String,
    var time: Timestamp,
) {
    var id: String = ""
}

@Serializable
data class EventTypes(
    val image: String,
    val label: String,
    val position: Int,
) {
    var id: String = ""
}

data class Summary(
    val child: String,
    val day: String,
) {
    var feedsTotal: Float = 0f
    var wetNappyTotal: Float = 0f
    var mixedNappyTotal: Float = 0f
    var sleepTotalSeconds: Float = 0f
    var sleepStartTemp: Timestamp? = null
        set(value) {
            field = value?.let {
                val instant = Instant.fromEpochSeconds(it.seconds)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val time = LocalDateTime(
                    year = instant.year,
                    monthNumber = instant.monthNumber,
                    dayOfMonth = instant.dayOfMonth,
                    hour = instant.hour,
                    minute = instant.minute,
                    second = 0,
                    nanosecond = 0,
                )
                Timestamp(
                    seconds = time.toInstant(TimeZone.currentSystemDefault()).epochSeconds,
                    nanoseconds = 0
                )
            } ?: value
        }
}

sealed class Rows(
    val id: String,
) {
    fun child() = when (this) {
        is Day -> child
        is Event -> child
        is AddNew -> child
    }

    data class Day(
        val label: String,
        val child: String,
    ) : Rows(id = randomUUID())

    data class Event(
        private val _id: String,
        val label: String,
        val child: String,
        val day: String,
        val timeStamp: Timestamp,
        val image: String?,
    ) : Rows(id = _id) {
        val time = timeStamp.format("hh:mm a")
    }

    data class AddNew(
        val child: String,
        val day: Timestamp,
    ) : Rows(id = randomUUID())
}

enum class EventType {
    WET_NAPPY,
    MIXED_NAPPY,
    SLEEP_START,
    SLEEP_END,
    LEFT_FEED,
    RIGHT_FEED,
}