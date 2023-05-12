import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FabOption(
    visible: Boolean,
    text: String,
    index: Int,
    onClick: () -> Unit
) = AnimatedVisibility(
    visible = visible,
    enter = fadeIn(
        tween(delayMillis = 100 * index)
    ) + slideInVertically(
        tween(delayMillis = 100 * index)
    ) { it / 2 },
    exit = slideOutVertically { it / 2 } + fadeOut()
) {
    ExtendedFloatingActionButton(
        modifier = Modifier.padding(bottom = 16.dp),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        content = { Text(text) }
    )
}