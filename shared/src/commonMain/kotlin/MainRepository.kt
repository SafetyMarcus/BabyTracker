import androidx.compose.runtime.mutableStateListOf
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object MainRepository {

    val children = mutableStateListOf<Child>()
    val events = mutableStateListOf<Rows>()
    val summaries = mutableStateListOf<Rows>()

    suspend fun getChildren() {
        Firebase.firestore
            .collection("children")
            .snapshots
            .collect {
                children.clear()
                children.addAll(
                    it.documents
                        .map<DocumentSnapshot, Child> { it.data() }
                        .sortedBy { it.position }
                )
            }
    }

    suspend fun getEvents() {
        Firebase.firestore
            .collection("events")
            .snapshots
            .collect {
                events.clear()
                summaries.clear()

                var day = ""
                it.documents
                    .map { it.data<Event>() }
                    .sortedBy { it.time.seconds }
                    .forEach { event ->
                        val eventDay = event.time.format("d MMMM")
                        if (day != eventDay) {
                            val dayRow = Rows.Day(
                                label = eventDay,
                                child = event.child
                            )
                            events.add(dayRow)
                            summaries.add(dayRow)
                            summaries.add(Rows.Summary(event.child))
                            day = eventDay
                        }
                        events.add(
                            Rows.Event(
                                label = event.eventDisplay,
                                child = event.child,
                                time = event.time.format("hh:mm a")
                            )
                        )
                        (summaries.last() as? Rows.Summary)?.apply {
                            when (event.type) {
                                EventType.MIXED_NAPPY -> mixedNappyTotal++
                                EventType.WET_NAPPY -> wetNappyTotal++
                                EventType.SLEEP_START -> sleepStartTemp = event.time
                                EventType.SLEEP_END -> {
                                    sleepStartTemp?.let {
                                        sleepTotal += event.time.seconds - it.seconds
                                    }
                                    sleepStartTemp = null
                                }
                            }
                        }
                    }
            }
    }

    suspend fun createEvent(
        child: String,
        eventType: EventType
    ) {
        Firebase.firestore
            .collection("events")
            .add(
                Event(
                    child = child,
                    event = eventType.name.uppercase(),
                    time = Timestamp.now()
                )
            )
    }
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
    val type = EventType.valueOf(event.uppercase())
    val eventDisplay = type.display
}

sealed class Rows {
    fun child() = when (this) {
        is Day -> child
        is Event -> child
        is Summary -> child
    }

    data class Day(
        val label: String,
        val child: String,
    ) : Rows()

    data class Event(
        val label: String,
        val child: String,
        val time: String,
    ) : Rows()

    data class Summary(
        val child: String,
    ) : Rows() {
        var wetNappyTotal: Float = 0f
        var mixedNappyTotal: Float = 0f
        var sleepTotal: Float = 0f
        var sleepStartTemp: Timestamp? = null

        val text get() = """
            $wetNappyTotal wet ${if (wetNappyTotal > 1) "nappies" else "nappy" }
            $wetNappyTotal mixed ${if (wetNappyTotal > 1) "nappies" else "nappy" }
            Slept for ${sleepTotal / 60} hours
        """.trimIndent()
    }
}

enum class EventType(
    val display: String,
) {
    WET_NAPPY("Wet nappy"),
    MIXED_NAPPY("Mixed nappy"),
    SLEEP_START("Sleep start"),
    SLEEP_END("Sleep end"),
}