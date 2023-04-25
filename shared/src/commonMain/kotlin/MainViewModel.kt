import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class MainViewModel: KMMViewModel() {
    val name = mutableStateOf("Sebastian")

    val events = mutableStateListOf<Event>()

    init {
        viewModelScope.coroutineScope.launch {
            Firebase.firestore.collection("events").snapshots.collect {
                events.clear()
                events.addAll(it.documents.map { document ->
                    document.data()
                })
            }
        }
    }

    fun setCurrentTab(index: Int) = when (index) {
        0 -> name.value = "Sebastian"
        else -> name.value = "Baby 2"
    }
}

@Serializable
data class Event(
    val child: String,
    val event: String,
    val time: Timestamp,
)