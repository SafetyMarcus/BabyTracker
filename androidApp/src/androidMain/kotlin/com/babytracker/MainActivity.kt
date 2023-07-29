package com.babytracker

import AppTheme
import Child
import EventType
import MainView
import MainViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.fromMilliseconds
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            AppTheme {
                val showing = remember { mutableStateOf(false) }
                var currentTime by remember { mutableStateOf(Timestamp.now()) }
                var currentEvent by remember { mutableStateOf<String?>(null) }
                var current by remember { mutableStateOf<Pair<Child, EventType>?>(null) }

                MainView(
                    viewModel = viewModel,
                    context = this,
                    showTimePicker = { timestamp, child, type ->
                        currentTime = timestamp ?: Timestamp.now()
                        current = child to type
                        showing.value = true
                    },
                    editEvent = { id, time ->
                        currentEvent = id
                        currentTime = time
                        current = null
                        showing.value = true
                    },
                )
                TimePickerAlert(
                    showing = showing,
                    current = currentTime,
                ) {
                    current?.let { (child, type) ->
                        viewModel.addEvent(child, type, it)
                    } ?: viewModel.editEvent(currentEvent ?: "", it)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerAlert(
    showing: MutableState<Boolean>,
    current: Timestamp,
    onSet: (Timestamp) -> Unit
) = showing.takeIf { it.value }?.let {
    AlertDialog(
        properties = DialogProperties(decorFitsSystemWindows = false),
        onDismissRequest = { showing.value = false },
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) { DialogContent(current = current, showing = showing, onSet = onSet) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogContent(
    current: Timestamp = Timestamp.now(),
    showing: MutableState<Boolean>,
    onSet: (Timestamp) -> Unit,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 8.dp
        ),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val calendar by remember {
        derivedStateOf {
            Calendar.getInstance().apply { timeInMillis = current.seconds * 1000 }
        }
    }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE)
    )
    TimePicker(state = timePickerState)
    DialogButtons(
        onCancelClicked = { showing.value = false },
        onDoneClicked = {
            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            calendar.set(Calendar.MINUTE, timePickerState.minute)
            onSet(Timestamp.fromMilliseconds(calendar.timeInMillis.toDouble()))
            showing.value = false
        },
    )
}

@Composable
private fun DialogButtons(
    onDoneClicked: () -> Unit,
    onCancelClicked: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End,
) {
    TextButton(
        onClick = onCancelClicked,
        content = { Text("Cancel") },
    )
    TextButton(
        onClick = onDoneClicked,
        content = { Text("Ok") },
    )
}