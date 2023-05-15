import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.launch

class MainViewModel : KMMViewModel() {
    val children = MainRepository.children
    val events = MainRepository.events
    val summaries = MainRepository.summaries

    init {
        viewModelScope.coroutineScope.launch {
            MainRepository.getChildren()
        }
        viewModelScope.coroutineScope.launch {
            MainRepository.getEvents()
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
}