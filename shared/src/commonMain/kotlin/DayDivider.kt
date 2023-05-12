import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DayDivider(
    label: String,
    selected: Boolean,
) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondary
        else LocalContentColor.current.copy(alpha = 0.65f),
    )
    CompositionLocalProvider(
        LocalContentColor provides color,
    ) {
        Text(
            modifier = Modifier.padding(
                top = 16.dp,
                bottom = 8.dp,
                start = 8.dp,
                end = 8.dp,
            ),
            text = label,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}