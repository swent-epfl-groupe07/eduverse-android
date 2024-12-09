package com.github.se.eduverse.ui.notifications

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.github.se.eduverse.model.NotifAuthorizations
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val taskText =
    "Do you want to receive notifications when you should start working on a task ?"
private val eventText = "Do you want to receive notifications when an event is about to start ?"

@Composable
fun NotificationsScreen(
    notifAuthorizations: NotifAuthorizations,
    navigationActions: NavigationActions,
    context: Context = LocalContext.current, // Dependency injection for tests
    notificationsEnabled: Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
) {
  val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

  var taskNotifEnabled by remember { mutableStateOf(notifAuthorizations.taskEnabled) }
  var eventNotifEnabled by remember { mutableStateOf(notifAuthorizations.eventEnabled) }

  Scaffold(
      topBar = { TopNavigationBar(navigationActions) },
      bottomBar = {
        BottomNavigationMenu(
            { navigationActions.navigateTo(it) },
            LIST_TOP_LEVEL_DESTINATION,
            "") // No item is selected, as it is not one of the screens on the bottom bar
      }) { padding ->
        if (notificationsEnabled) { // Notifications enabled for the app (in phone settings)
          LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
              TextWithSwitch(taskText, taskNotifEnabled, "taskSwitch") {
                taskNotifEnabled = !taskNotifEnabled
                notifAuthorizations.taskEnabled = taskNotifEnabled
                storeAuthorizations(notifAuthorizations, sharedPreferences)
              }
              TextWithSwitch(eventText, eventNotifEnabled, "eventSwitch") {
                eventNotifEnabled = !eventNotifEnabled
                notifAuthorizations.eventEnabled = eventNotifEnabled
                storeAuthorizations(notifAuthorizations, sharedPreferences)
              }
            }
          }
        } else {
          Column(
              modifier = Modifier.padding(padding).fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center) {
                Text("Notifications are disabled.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                      context.startActivity(
                          Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                          })
                    },
                    modifier = Modifier.testTag("enableButton")) {
                      Text("Enable")
                    }
              }
        }
      }
}

@Composable
fun TextWithSwitch(text: String, checked: Boolean, testTag: String, onCheckedChange: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(0.8f),
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.testTag(testTag),
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary))
      }
}

fun storeAuthorizations(
    notifAuthorizations: NotifAuthorizations,
    sharedPreferences: SharedPreferences
) {
  val json = Json.encodeToString(notifAuthorizations)
  sharedPreferences.edit().putString("notifAuthKey", json).apply()
}
