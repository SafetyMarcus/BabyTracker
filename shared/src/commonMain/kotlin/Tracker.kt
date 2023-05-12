import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Tracker(
    value: String,
    label: String,
    color: Color,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Box {
        CircularProgressIndicator(
            modifier = Modifier
                .size(64.dp)
                .padding(8.dp)
                .align(Alignment.Center),
            color = color,
            progress = 100f,
        )
        AnimatedContent(
            modifier = Modifier.align(Alignment.Center),
            targetState = value,
            transitionSpec = slidingTextCounter(),
        ) { targetValue ->
            Text(
                text = targetValue,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun slidingTextCounter(): AnimatedContentScope<String>.() -> ContentTransform = {
    if ((targetState.toFloatOrNull() ?: 0f) > (initialState.toFloatOrNull() ?: 0f)) {
        //Queue a slide in (bottom up) for after any existing text animates out
        slideInVertically(
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = 100
            )
        ) { height -> height } + fadeIn(
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = 100
            )
            //Queue an instant slide out (upwards) and fade animation for existing text
        ) with slideOutVertically(
            animationSpec = tween(durationMillis = 100)
        ) { height -> -height } + fadeOut(
            animationSpec = tween(durationMillis = 100)
        )
    } else { //the inverse of the above animation. Needs to be built as a separate animation
        slideInVertically(
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = 100
            )
        ) { height -> -height } + fadeIn(
            animationSpec = tween(
                durationMillis = 100,
                delayMillis = 100
            )
        ) with slideOutVertically(
            animationSpec = tween(durationMillis = 100)
        ) { height -> height } + fadeOut(
            animationSpec = tween(durationMillis = 100)
        )
    }
}