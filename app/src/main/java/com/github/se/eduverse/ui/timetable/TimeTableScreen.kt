package com.github.se.eduverse.ui.timetable

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.millisecInHour
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val blue = ButtonColors(Color(0xFF00A5FF), Color.Black, Color.LightGray, Color.Black)
private val orange = ButtonColors(Color(0xFFFFA500), Color.Black, Color.LightGray, Color.Black)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTableScreen(
    timeTableViewModel: TimeTableViewModel,
    todoViewModel: TodoListViewModel,
    navigationActions: NavigationActions
) {
  val context = LocalContext.current

  val weeklyTable by timeTableViewModel.table.collectAsState()
  val actualTodos by todoViewModel.actualTodos.collectAsState()
  var showDialog by remember { mutableStateOf(false) }

  var idTaskOrEvent = ""
  var newElementType by remember { mutableStateOf(ScheduledType.EVENT) }
  var name by remember { mutableStateOf("") }
  var selectedDate by remember { mutableStateOf<Calendar>(Calendar.getInstance()) }
  var lengthHour by remember { mutableIntStateOf(1) }
  var lengthMin by remember { mutableIntStateOf(0) }
  var addedTodo by remember { mutableStateOf<Todo?>(null) }

  if (showDialog) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = { showDialog = false },
        title = {
          if (newElementType == ScheduledType.TASK) {
            Text("Plan to do a task")
          } else {
            Text("Schedule an event")
          }
        },
        text = {
          if (newElementType == ScheduledType.TASK) {
            todoViewModel.getActualTodos()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text("Todo", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                  LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.25f)) {
                    items(actualTodos.size) { index ->
                      Card(
                          modifier =
                              Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                                addedTodo = actualTodos[index]
                                name = actualTodos[index].name
                                idTaskOrEvent = actualTodos[index].uid
                              },
                          shape = RoundedCornerShape(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                  if (addedTodo == actualTodos[index]) {
                                    Icon(Icons.Rounded.CheckCircle, "Checked")
                                  } else {
                                    Icon(Icons.Rounded.RadioButtonUnchecked, "Unchecked")
                                  }
                                  Text(actualTodos[index].name)
                                }
                          }
                    }
                  }
                  DateAndTimePickers(
                      context,
                      selectedDate,
                      lengthHour,
                      lengthMin,
                      selectDate = { selectedDate = it },
                      selectTime = { hour, min ->
                        lengthHour = hour
                        lengthMin = min
                      })
                }
          } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Text("Name", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                  OutlinedTextField(
                      value = name,
                      modifier = Modifier.fillMaxWidth(0.9f),
                      onValueChange = { name = it },
                      placeholder = { Text("Name the task") })
                  DateAndTimePickers(
                      context,
                      selectedDate,
                      lengthHour,
                      lengthMin,
                      selectDate = { selectedDate = it },
                      selectTime = { hour, min ->
                        lengthHour = hour
                        lengthMin = min
                      })
                }
          }
        },
        confirmButton = {
          TextButton(
              onClick = {
                showDialog = false
                timeTableViewModel.addScheduled(
                    Scheduled(
                        timeTableViewModel.getNewUid(),
                        newElementType,
                        selectedDate,
                        ((lengthHour + lengthMin.toDouble() / 60) * millisecInHour).toLong(),
                        idTaskOrEvent,
                        timeTableViewModel.auth.currentUser!!.uid,
                        name))
                idTaskOrEvent = ""
                name = ""
                selectedDate = Calendar.getInstance()
                lengthHour = 1
                lengthMin = 0
                addedTodo = null
              },
              enabled = newElementType == ScheduledType.EVENT || addedTodo != null) {
                Text("Confirm")
              }
        },
        dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } })
  }

  Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  "Time Table",
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.testTag("topBarTitle"))
            },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("backButton")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                  }
            },
            colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent))
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

          // Buttons to add new event/task
          Row(
              modifier = Modifier.fillMaxWidth().padding(15.dp),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                      newElementType = ScheduledType.TASK
                      showDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(0.45f),
                    colors = orange) {
                      Text("Add task")
                    }
                Button(
                    onClick = {
                      newElementType = ScheduledType.EVENT
                      showDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(0.82f),
                    colors = blue) {
                      Text("Add event")
                    }
              }

          // Buttons to change of week
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { timeTableViewModel.getPreviousWeek() }) {
                  Icon(Icons.Default.KeyboardDoubleArrowLeft, "left arrow")
                  Text("last week")
                }

                TextButton(onClick = { timeTableViewModel.getNextWeek() }) {
                  Text("next week")
                  Icon(Icons.Default.KeyboardDoubleArrowRight, "right arrow")
                }
              }

          // Table
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(1) {
              Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                Spacer(modifier = Modifier.fillMaxWidth(0.12f).padding(top = 25.dp))
                for (c in 0..6) {
                  Text(
                      text = timeTableViewModel.getDateAtDay(c),
                      modifier = Modifier.fillMaxWidth((1.0 / (7 - c)).toFloat()),
                      textAlign = TextAlign.Center)
                }
              }
              Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(0.12f).padding(top = 25.dp),
                    horizontalAlignment = Alignment.End) {
                      for (i in 1..23) {
                        Text(
                            "${i}h",
                            modifier =
                                Modifier.padding(top = 15.dp, bottom = 15.dp, end = 5.dp)
                                    .height(20.dp))
                      }
                    }
                for (c in 0..6) {
                  TableColumn((1.0 / (7 - c)).toFloat(), weeklyTable[c])
                }
              }
            }
          }
        }
      }
}

@Composable
fun TableColumn(width: Float, content: List<Scheduled>) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth(width).fillMaxHeight()) {
    val boxWidth = maxWidth

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
      for (i in 0..23) {
        Row(
            modifier =
                Modifier.border(width = 0.5.dp, color = Color.LightGray)
                    .height(50.dp)
                    .fillMaxWidth()) {}
      }
    }
    val openedButton = emptyList<Scheduled>().toMutableList()
    val unseenButtons = content.toMutableList() // copy
    var lastButtonEnd = 0f
    content.forEach { new ->
      openedButton.removeAll { old -> old.start.timeInMillis + old.length < new.start.timeInMillis }
      if (openedButton.size == 0) lastButtonEnd = 0f

      val parallelUnseenButtons =
          unseenButtons.fold(0) { num, el ->
            if (el.start.timeInMillis < new.start.timeInMillis + new.length) {
              num + 1
            } else {
              num
            }
          }
      val numberParallel = openedButton.size + parallelUnseenButtons
      val widthPercent = (1.0 / numberParallel).toFloat()
      var xOffset = lastButtonEnd
      if (xOffset + widthPercent > 1f) xOffset = 0f
      Button(
          onClick = {},
          modifier =
              Modifier.fillMaxWidth(widthPercent)
                  .height((new.length.toDouble() / millisecInHour * 50).dp)
                  .offset(
                      x = boxWidth * xOffset,
                      y =
                          (new.start.get(Calendar.HOUR_OF_DAY) * 50).dp +
                              (new.start.get(Calendar.MINUTE).toDouble() / 60 * 50).dp)
                  .padding(1.dp),
          shape = RoundedCornerShape(5.dp),
          colors = if (new.type == ScheduledType.TASK) orange else blue,
          contentPadding = PaddingValues(1.dp)) {
            Text(new.name, modifier = Modifier.fillMaxSize())
          }

      lastButtonEnd = xOffset + widthPercent
      openedButton.add(new)
      unseenButtons.remove(new)
    }
  }
}

@Composable
fun DateAndTimePickers(
    context: Context,
    selectedDate: Calendar,
    lengthHour: Int,
    lengthMin: Int,
    selectDate: (Calendar) -> Unit,
    selectTime: (Int, Int) -> Unit
) {
  // Pick the date
  Text("Date", fontWeight = FontWeight.Bold, fontSize = 24.sp)
  OutlinedButton(
      onClick = {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Create a DatePickerDialog
        DatePickerDialog(
                context,
                { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                  val pickedCalendar = Calendar.getInstance()
                  pickedCalendar.set(selectedYear, selectedMonth, selectedDay)
                  selectDate(pickedCalendar)
                },
                year,
                month,
                day)
            .show()
      },
      modifier = Modifier.fillMaxWidth(0.9f),
      shape = OutlinedTextFieldDefaults.shape) {
        Text(
            text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(selectedDate.time),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start)
      }

  // Pick the time
  Text("Time", fontWeight = FontWeight.Bold, fontSize = 24.sp)
  OutlinedButton(
      onClick = {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        // TimePickerDialog to pick the time
        TimePickerDialog(
                context,
                { _: TimePicker, hourOfDay: Int, pickedMinute: Int ->
                  val newCalendar = Calendar.getInstance()
                  newCalendar.apply {
                    timeInMillis = selectedDate.timeInMillis
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, pickedMinute)
                  }
                  selectDate(newCalendar)
                },
                hour,
                minute,
                true // Use 24-hour format
                )
            .show()
      },
      modifier = Modifier.fillMaxWidth(0.9f),
      shape = OutlinedTextFieldDefaults.shape) {
        Text(
            text =
                hourToString(
                    selectedDate.get(Calendar.HOUR_OF_DAY), selectedDate.get(Calendar.MINUTE)),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start)
      }

  // Pick the length
  Text("Length", fontWeight = FontWeight.Bold, fontSize = 24.sp)
  OutlinedButton(
      onClick = {
        TimePickerDialog(
                context,
                { _: TimePicker, hourOfDay: Int, minute: Int -> selectTime(hourOfDay, minute) },
                lengthHour,
                lengthMin,
                true)
            .show()
      },
      modifier = Modifier.fillMaxWidth(0.9f),
      shape = OutlinedTextFieldDefaults.shape) {
        Text(
            text = hourToString(lengthHour, lengthMin),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start)
      }
}

fun hourToString(hour: Int, minute: Int): String {
  val lM = if (minute < 10) "0$minute" else "$minute"
  return "${hour}h" + lM
}
