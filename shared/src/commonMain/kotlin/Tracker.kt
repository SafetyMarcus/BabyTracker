import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun Tracker(
    value: String,
    label: String,
    color: Color,
) = Column(
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = slidingTextCounter(),
    ) { targetValue ->
        Text(
            text = targetValue,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun slidingTextCounter(): AnimatedContentTransitionScope<String>.() -> ContentTransform = {
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
        ) togetherWith slideOutVertically(
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
        ) togetherWith slideOutVertically(
            animationSpec = tween(durationMillis = 100)
        ) { height -> height } + fadeOut(
            animationSpec = tween(durationMillis = 100)
        )
    }
}