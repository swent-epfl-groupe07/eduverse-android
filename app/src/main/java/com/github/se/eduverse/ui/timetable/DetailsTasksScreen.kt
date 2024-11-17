package com.github.se.eduverse.ui.timetable

//noinspection UsingMaterialAndMaterial3Libraries
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.Scheduled
import com.github.se.eduverse.model.ScheduledType
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.model.millisecInHour
import com.github.se.eduverse.model.millisecInMin
import com.github.se.eduverse.ui.SaveIcon
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.Screen
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.theme.transparentButtonColor
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import com.github.se.eduverse.viewmodel.TodoListViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsTasksScreen(
    timeTableViewModel: TimeTableViewModel,
    todoListViewModel: TodoListViewModel,
    navigationActions: NavigationActions
) {
  val context = LocalContext.current
  val task: Scheduled =
      timeTableViewModel.opened
          ?: Scheduled("", ScheduledType.TASK, Calendar.getInstance(), 0, "", "", "")
  var todo by remember { mutableStateOf(todoListViewModel.getTodoById(task.content)) }
  if (todo == null) navigationActions.goBack()

  var newName by remember { mutableStateOf(task.name) }
  var date by remember { mutableStateOf((task.start.clone() as Calendar)) }
  var lengthH by remember { mutableIntStateOf((task.length / millisecInHour).toInt()) }
  var lengthM by remember {
    mutableIntStateOf((task.length % millisecInHour / millisecInMin).toInt())
  }

  Scaffold(
      topBar = {
        TopNavigationBar(task.name, navigationActions) {
          IconButton(
              onClick = {
                timeTableViewModel.deleteScheduled(task)
                navigationActions.goBack()
              },
              modifier = Modifier.testTag("deleteButton")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
              }
        }
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  // Name of the task
                  Title("Name")
                  OutlinedTextField(
                      value = newName,
                      modifier = Modifier.fillMaxWidth(0.9f).testTag("nameTextField"),
                      onValueChange = { newName = it },
                      placeholder = { Text("Name the task") },
                      suffix = {
                        SaveIcon(
                            onClick = {
                              timeTableViewModel.updateScheduled(task.apply { name = newName })
                              newName += " " // Change the value to trigger calculation of isEnabled
                              newName.dropLast(1)
                            },
                            isEnabled = { task.name != newName })
                      })

                  // Date, time and length of the task
                  DateAndTimePickers(
                      context = context,
                      selectedDate = date,
                      lengthHour = lengthH,
                      lengthMin = lengthM,
                      selectDate = { date = it },
                      selectTime = { hour, min ->
                        lengthH = hour
                        lengthM = min
                      }) { type ->
                        SaveIcon(
                            onClick = {
                              when (type) {
                                "date" -> {
                                  // Get the date of the new event but not the time
                                  task.start.set(Calendar.YEAR, date.get(Calendar.YEAR))
                                  task.start.set(Calendar.MONTH, date.get(Calendar.MONTH))
                                  task.start.set(
                                      Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
                                  timeTableViewModel.updateScheduled(task)
                                }
                                "time" -> {
                                  // Get the time of the new event but not the date
                                  task.start.set(
                                      Calendar.HOUR_OF_DAY, date.get(Calendar.HOUR_OF_DAY))
                                  task.start.set(Calendar.MINUTE, date.get(Calendar.MINUTE))
                                  task.start.set(Calendar.SECOND, date.get(Calendar.SECOND))
                                  task.start.set(
                                      Calendar.MILLISECOND, date.get(Calendar.MILLISECOND))
                                  timeTableViewModel.updateScheduled(task)
                                }
                                "length" -> {
                                  timeTableViewModel.updateScheduled(
                                      task.apply {
                                        length =
                                            ((lengthH + lengthM.toDouble() / 60) * millisecInHour)
                                                .toLong()
                                      })
                                  lengthH +=
                                      1 // Change the value to trigger calculation of isEnabled
                                  lengthH -= 1
                                }
                                else -> Log.e("DetailsTasksScreen", "Unknown save icon : $type")
                              }
                            },
                            isEnabled = {
                              when (type) {
                                "date" -> date.timeInMillis != task.start.timeInMillis
                                "time" -> date.timeInMillis != task.start.timeInMillis
                                "length" ->
                                    task.length !=
                                        ((lengthH + lengthM.toDouble() / 60) * millisecInHour)
                                            .toLong()
                                else -> {
                                  Log.e("DetailsTasksScreen", "Unknown save icon : $type")
                                  false
                                }
                              }
                            })
                      }

                  // Status of the task
                  Title("Status")
                  OutlinedCard(
                      modifier = Modifier.fillMaxWidth(0.9f).testTag("status"),
                      shape = OutlinedTextFieldDefaults.shape) {
                        Text(
                            text = if (todo?.status == TodoStatus.DONE) "Completed" else "Current",
                            modifier = Modifier.padding(horizontal = 25.dp, vertical = 10.dp))
                      }

                  // Buttons related to todo
                  Row(
                      modifier = Modifier.fillMaxWidth(0.9f),
                      horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { navigationActions.navigateTo(Screen.TODO_LIST) },
                            modifier = Modifier.fillMaxWidth(2f / 5).testTag("seeTodo")) {
                              Text("See todo")
                            }
                        if (todo?.status == TodoStatus.ACTUAL) {
                          OutlinedButton(
                              onClick = {
                                todoListViewModel.setTodoDone(todo!!)
                                todo = todoListViewModel.getTodoById(todo!!.uid)
                              },
                              modifier = Modifier.fillMaxWidth(2f / 3).testTag("markAsDone"),
                              colors = transparentButtonColor(MaterialTheme.colorScheme.tertiary)) {
                                Text("Mark as done")
                              }
                        } else {
                          OutlinedButton(
                              onClick = {
                                todoListViewModel.setTodoActual(todo!!)
                                todo = todoListViewModel.getTodoById(todo!!.uid)
                              },
                              modifier = Modifier.fillMaxWidth(2f / 3).testTag("markAsCurrent"),
                              colors = transparentButtonColor(MaterialTheme.colorScheme.tertiary)) {
                                Text("Mark as current")
                              }
                        }
                      }
                }
              }
            }
      }
}
