package com.github.se.eduverse.ui.timetable

//noinspection UsingMaterialAndMaterial3Libraries
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import com.github.se.eduverse.model.millisecInHour
import com.github.se.eduverse.model.millisecInMin
import com.github.se.eduverse.ui.SaveIcon
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.TimeTableViewModel
import java.util.Calendar

@Composable
fun DetailsEventScreen(
    timeTableViewModel: TimeTableViewModel,
    navigationActions: NavigationActions
) {
  val context = LocalContext.current
  val event: Scheduled =
      timeTableViewModel.opened
          ?: run {
            navigationActions.goBack()

            /* The value there doesn't mather as we will leave the screen anyway, but it is important
            to make event non-null*/
            Scheduled("", ScheduledType.EVENT, Calendar.getInstance(), 0, "", "", "")
          }

  var newName by remember { mutableStateOf(event.name) }
  var description by remember { mutableStateOf(event.content) }
  var date by remember { mutableStateOf((event.start.clone() as Calendar)) }
  var lengthH by remember { mutableIntStateOf((event.length / millisecInHour).toInt()) }
  var lengthM by remember {
    mutableIntStateOf((event.length % millisecInHour / millisecInMin).toInt())
  }

  Scaffold(
      topBar = {
        TopNavigationBar(
            navigationActions = navigationActions,
            screenTitle = event.name,
            actions = {
              IconButton(
                  onClick = {
                    timeTableViewModel.deleteScheduled(event)
                    navigationActions.goBack()
                  },
                  modifier = Modifier.testTag("deleteButton")) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "delete")
                  }
            })
      },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {
              item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  // Name of the event
                  Title("Name")
                  OutlinedTextField(
                      value = newName,
                      modifier = Modifier.fillMaxWidth(0.9f).testTag("nameTextField"),
                      onValueChange = { newName = it },
                      placeholder = { Text("Name the event") },
                      suffix = {
                        SaveIcon(
                            onClick = {
                              timeTableViewModel.updateScheduled(event.apply { name = newName })
                              newName += " " // Change the value to trigger calculation of isEnabled
                              newName = newName.dropLast(1)
                            },
                            isEnabled = { event.name != newName })
                      })

                  // Description of the event
                  Title("Description")
                  OutlinedTextField(
                      value = description,
                      modifier = Modifier.fillMaxWidth(0.9f).testTag("descTextField"),
                      onValueChange = { description = it },
                      placeholder = { Text("No description provided") },
                      suffix = {
                        SaveIcon(
                            onClick = {
                              timeTableViewModel.updateScheduled(
                                  event.apply { content = description })
                              description +=
                                  " " // Change the value to trigger calculation of isEnabled
                              description = description.dropLast(1)
                            },
                            isEnabled = { event.content != description })
                      },
                      minLines = 7)

                  // Date, time and length of the event
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
                                  event.start.set(Calendar.YEAR, date.get(Calendar.YEAR))
                                  event.start.set(Calendar.MONTH, date.get(Calendar.MONTH))
                                  event.start.set(
                                      Calendar.DAY_OF_MONTH, date.get(Calendar.DAY_OF_MONTH))
                                  timeTableViewModel.updateScheduled(event)

                                  // Trigger recomposition
                                  date = date.clone() as Calendar
                                }
                                "time" -> {
                                  // Get the time of the new event but not the date
                                  event.start.set(
                                      Calendar.HOUR_OF_DAY, date.get(Calendar.HOUR_OF_DAY))
                                  event.start.set(Calendar.MINUTE, date.get(Calendar.MINUTE))
                                  timeTableViewModel.updateScheduled(event)

                                  // Trigger recomposition
                                  date = date.clone() as Calendar
                                }
                                "length" -> {
                                  timeTableViewModel.updateScheduled(
                                      event.apply {
                                        length =
                                            ((lengthH + lengthM.toDouble() / 60) * millisecInHour)
                                                .toLong()
                                      })
                                  lengthH +=
                                      1 // Change the value to trigger calculation of isEnabled
                                  lengthH -= 1
                                }
                                else -> Log.e("DetailsEventScreen", "Unknown save icon : $type")
                              }
                            },
                            isEnabled = {
                              when (type) {
                                "date" ->
                                    date.get(Calendar.YEAR) != event.start.get(Calendar.YEAR) ||
                                        date.get(Calendar.DAY_OF_YEAR) !=
                                            event.start.get(Calendar.DAY_OF_YEAR)
                                "time" ->
                                    date.get(Calendar.HOUR_OF_DAY) !=
                                        event.start.get(Calendar.HOUR_OF_DAY) ||
                                        date.get(Calendar.MINUTE) !=
                                            event.start.get(Calendar.MINUTE)
                                "length" ->
                                    event.length !=
                                        ((lengthH + lengthM.toDouble() / 60) * millisecInHour)
                                            .toLong()
                                else -> {
                                  Log.e("DetailsEventScreen", "Unknown save icon : $type")
                                  false
                                }
                              }
                            })
                      }
                }
              }
            }
      }
}
