import androidx.compose.runtime.mutableStateOf
import com.rickclephas.kmm.viewmodel.KMMViewModel
import com.rickclephas.kmm.viewmodel.stateIn

class MainViewModel: KMMViewModel() {
    val name = mutableStateOf("Sebastian")

    fun setCurrentTab(index: Int) = when (index) {
        0 -> name.value = "Sebastian"
        else -> name.value = "Baby 2"
    }
}