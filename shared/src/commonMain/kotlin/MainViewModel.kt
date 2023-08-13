import androidx.compose.runtime.mutableStateListOf
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.launch

class MainViewModel : KMMViewModel() {
    val children = mutableStateListOf<Child>()
    val eventTypes = mutableStateListOf<EventTypes>()
    val events = MainRepository.events
    val summaries = MainRepository.summaries

    init {
        viewModelScope.coroutineScope.launch {
            MainRepository.logIn()
            children.clear()
            children.addAll(MainRepository.getChildren())
            eventTypes.clear()
            eventTypes.addAll(MainRepository.getEventTypes())
            viewModelScope.coroutineScope.launch {
                MainRepository.getEvents()
            }
        }
    }

    fun addEvent(
        child: Child,
        eventType: EventType,
        time: Timestamp,
    ) = viewModelScope.coroutineScope.launch {
        MainRepository.createEvent(child = child.id, eventType = eventType, timestamp = time)
    }

    fun editEvent(
        id: String,
        time: Timestamp,
    ) = viewModelScope.coroutineScope.launch {
        MainRepository.updateEvent(id = id, time = time)
    }

    fun deleteEvent(
        id: String
    ) = viewModelScope.coroutineScope.launch {
        MainRepository.deleteEvent(id = id)
    }
}