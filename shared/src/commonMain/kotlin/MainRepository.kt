import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object MainRepository {

    val children = mutableStateListOf<Child>()
    val events = mutableStateListOf<Event>()

    suspend fun getChildren() = withContext(Dispatchers.Default) {
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

    suspend fun getEvents() = withContext(Dispatchers.Default) {
        Firebase.firestore
            .collection("events")
            .snapshots
            .collect {
                events.clear()
                events.addAll(it.documents.map { it.data() })
            }
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
    val time: Timestamp,
)