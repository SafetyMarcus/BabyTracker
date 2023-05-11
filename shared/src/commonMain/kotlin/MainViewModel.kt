import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
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

    fun addEvent(child: Child, eventType: EventType) {
        viewModelScope.coroutineScope.launch {
            MainRepository.createEvent(child.id, eventType)
        }
    }
}