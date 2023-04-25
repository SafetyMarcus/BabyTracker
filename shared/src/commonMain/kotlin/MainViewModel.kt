import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.coroutineScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.launch

class MainViewModel: KMMViewModel() {
    val name = mutableStateOf("Sebastian")

    val events = mutableStateListOf<String>()

    init {
        viewModelScope.coroutineScope.launch {
            Firebase.firestore.collection("events").snapshots.collect {
                events.clear()
                events.addAll(it.documents.map { document ->
                    document.get("event")
                })
            }
        }
    }

    fun setCurrentTab(index: Int) = when (index) {
        0 -> name.value = "Sebastian"
        else -> name.value = "Baby 2"
    }
}