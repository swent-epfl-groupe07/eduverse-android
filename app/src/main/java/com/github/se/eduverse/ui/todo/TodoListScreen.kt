package com.github.se.eduverse.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.ui.navigation.BottomNavigationMenu
import com.github.se.eduverse.ui.navigation.LIST_TOP_LEVEL_DESTINATION
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.speechRecognition.SpeechRecognizerInterface
import com.github.se.eduverse.viewmodel.TodoListViewModel

/** Composable that represents the user's todo list screen */
@Composable
fun TodoListScreen(navigationActions: NavigationActions, todoListViewModel: TodoListViewModel) {
  val actualTodos = todoListViewModel.actualTodos.collectAsState()
  val doneTodos = todoListViewModel.doneTodos.collectAsState()
  var showCompleted by remember { mutableStateOf(false) }

  Scaffold(
      topBar = { TopNavigationBar(navigationActions, screenTitle = null) },
      bottomBar = {
        BottomNavigationMenu({ navigationActions.navigateTo(it) }, LIST_TOP_LEVEL_DESTINATION, "")
      },
      content = { pd ->
        Column(modifier = Modifier.fillMaxSize().padding(pd).testTag("todoListScreen")) {
          AddTodoEntry { name ->
            todoListViewModel.addNewTodo(name)
            // For better UX display the current tasks whenever a new task is added so that the user
            // can see the added task even if the done todos are currently being displayed
            showCompleted = false
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
              modifier = Modifier.padding(8.dp),
          ) {
            TextButton(
                onClick = { showCompleted = false },
                enabled = showCompleted,
                colors =
                    ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondary,
                        disabledContentColor = MaterialTheme.colorScheme.onSecondary),
                modifier = Modifier.testTag("currentTasksButton")) {
                  Text("Current Tasks", modifier = Modifier.padding(8.dp), fontSize = 20.sp)
                }
            TextButton(
                onClick = { showCompleted = true },
                enabled = !showCompleted,
                colors =
                    ButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.secondary,
                        disabledContentColor = MaterialTheme.colorScheme.onSecondary),
                modifier = Modifier.testTag("completedTasksButton")) {
                  Text("Completed Tasks", modifier = Modifier.padding(8.dp), fontSize = 20.sp)
                }
          }
          LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            val todosToShow = if (showCompleted) doneTodos.value else actualTodos.value
            items(todosToShow.size) { i ->
              val todo = todosToShow[i]
              var showRenameDialog by remember { mutableStateOf(false) }
              TodoItem(
                  todo,
                  { todoListViewModel.setTodoActual(todo) },
                  { todoListViewModel.setTodoDone(todo) },
                  { DefaultTodoTimeIcon(todo) },
                  {
                    Box {
                      var expanded by remember { mutableStateOf(false) }
                      IconButton(
                          onClick = { expanded = !expanded },
                          modifier =
                              Modifier.testTag("todoOptionsButton_${todo.uid}").padding(8.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Todo Options")
                          }
                      TodoOptionsMenu(
                          todo.uid,
                          expanded,
                          { expanded = false },
                          { todoListViewModel.deleteTodo(todo.uid) },
                          { showRenameDialog = true })
                    }
                  },
                  "${todo.timeSpent/3600}h${todo.timeSpent/60}")
              if (showRenameDialog) {
                RenameTodoDialog(
                    todo.name,
                    { newName -> todoListViewModel.renameTodo(todo, newName) },
                    { showRenameDialog = false })
              }
            }
          }
        }
      })
}

/**
 * Composable that represents a single todo item
 *
 * @param todo the todo to display
 * @param onUndo code executed when the done button is clicked while the todo is completed
 * @param onDone code executed when the done button is clicked while the todo is not completed
 * @param rightMostButton the button to display on the right side of the todo item, it depends on
 *   the screen the todo is displayed on
 */
@Composable
fun TodoItem(
    todo: Todo,
    onUndo: () -> Unit,
    onDone: () -> Unit,
    timeSpentIcon: @Composable () -> Unit,
    rightMostButton: @Composable () -> Unit,
    timeSpent: String = "0h0"
) {
  val completed = todo.status == TodoStatus.DONE

  Card(
      modifier =
          Modifier.height(48.dp)
              .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
              .fillMaxWidth()
              .testTag("todoItem_${todo.uid}"),
      shape = RoundedCornerShape(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
              onClick = {
                if (completed) {
                  onUndo()
                } else {
                  onDone()
                }
              },
              modifier = Modifier.testTag("todoDoneButton_${todo.uid}").padding(8.dp).size(24.dp),
              colors =
                  IconButtonColors(
                      if (completed) Color.Green else Color.LightGray,
                      Color.White,
                      Color.LightGray,
                      Color.White)) {
                Icon(
                    Icons.Rounded.Done,
                    contentDescription = "Todo Done",
                    modifier = Modifier.size(16.dp))
              }
          Text(
              todo.name,
              modifier =
                  Modifier.weight(1f)
                      .padding(start = 8.dp, end = 8.dp)
                      .testTag("todoName_${todo.uid}"),
              textDecoration = if (completed) TextDecoration.LineThrough else TextDecoration.None,
              color = if (completed) Color.Gray else Color.Unspecified)

          Row(modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            timeSpentIcon()
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = timeSpent,
                modifier = Modifier.testTag("todoTimeSpent_${todo.uid}"),
                color =
                    if (todo.status == TodoStatus.ACTUAL) MaterialTheme.colorScheme.secondary
                    else Color.LightGray,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium)
          }

          rightMostButton()
        }
      }
}

/**
 * Composable that represents the entry to add a new todo
 *
 * @param onAdd code executed when a new todo is added
 */
@Composable
fun AddTodoEntry(onAdd: (String) -> Unit) {
  var name by remember { mutableStateOf("") }
  var showRecordDialog by remember { mutableStateOf(false) }
  Card(
      modifier = Modifier.padding(8.dp).fillMaxWidth().testTag("addTodoEntry"),
      shape = RoundedCornerShape(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
              onClick = {
                onAdd(name)
                name = ""
              },
              modifier = Modifier.padding(8.dp).testTag("addTodoButton"),
              enabled = name.isNotEmpty(),
              colors =
                  IconButtonColors(
                      containerColor = MaterialTheme.colorScheme.secondary,
                      contentColor = MaterialTheme.colorScheme.onSecondary,
                      disabledContainerColor = Color.Transparent,
                      disabledContentColor = Color.LightGray)) {
                Icon(Icons.Default.Add, contentDescription = "Add Todo")
              }
          BasicTextField(
              value = name,
              onValueChange = { name = it },
              modifier =
                  Modifier.weight(1f).padding(start = 8.dp, end = 8.dp).testTag("addTodoTextField"),
              textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, color = Color.Black),
              singleLine = true,
              decorationBox = { innerTextField ->
                if (name.isEmpty()) {
                  Text(text = "Add a new task", color = Color.Gray)
                }
                innerTextField()
              })

          // Button to record todo name
          IconButton(
              onClick = { showRecordDialog = true },
              modifier = Modifier.testTag("voiceInputButton"),
              colors =
                  IconButtonColors(
                      containerColor = MaterialTheme.colorScheme.secondary,
                      contentColor = MaterialTheme.colorScheme.onSecondary,
                      disabledContainerColor = Color.Transparent,
                      disabledContentColor = Color.LightGray)) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice input")
              }

          // Dialog to record todo name
          if (showRecordDialog) {
            RecordNameDialog(onAdd = { onAdd(it) }, onDismiss = { showRecordDialog = false })
          }
        }
      }
}

/**
 * Composable that displays a dialog to record the new todo name
 *
 * @param onAdd code executed when the record button is clicked
 * @param onDismiss code executed when the dialog is dismissed
 */
@Composable
fun RecordNameDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
  var newName by remember { mutableStateOf("") }
  SpeechRecognizerInterface(
      context = LocalContext.current,
      title = "Add a new task",
      description =
          "Press the bottom button to record the name of the new task to add. The recorded name will be displayed below. If you're happy with it, press the add button to add the new task to your todo list.\n\n" +
              if (newName.isNotEmpty()) {
                "New task name: $newName"
              } else "",
      onDismiss = { onDismiss() },
      onResult = { newName = it }) {
        Button(
            onClick = {
              onAdd(newName)
              onDismiss()
            },
            modifier = Modifier.width(100.dp).testTag("recordNameDialogAddButton"),
            enabled = newName.isNotEmpty()) {
              Text("Add")
            }
      }
}

/**
 * Composable that represents the options menu to display when the TodoItem options button is
 * clicked
 *
 * @param expanded whether the menu is expanded
 * @param onDismiss code executed when the menu is dismissed
 * @param onDelete code executed when the delete option is clicked
 * @param onRename code executed when the rename option is clicked
 */
@Composable
fun TodoOptionsMenu(
    todoId: String,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
) {
  DropdownMenu(
      expanded = expanded,
      onDismissRequest = { onDismiss() },
      properties = PopupProperties(focusable = false)) {
        DropdownMenuItem(
            text = { Text("Delete", color = Color.Red) },
            onClick = {
              onDelete()
              onDismiss()
            },
            modifier = Modifier.testTag("deleteTodoButton_${todoId}"))

        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = {
              onRename()
              onDismiss()
            },
            modifier = Modifier.testTag("renameTodoButton_${todoId}"))
      }
}

/**
 * Composable that represents a dialog to rename a todo
 *
 * @param todoName the current name of the todo
 * @param onRename code executed when the rename button is clicked
 * @param onDismiss code executed when the dialog is dismissed
 */
@Composable
fun RenameTodoDialog(todoName: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
  var newName by remember { mutableStateOf(TextFieldValue(todoName, TextRange(todoName.length))) }
  AlertDialog(
      modifier = Modifier.testTag("renameTodoDialog"),
      onDismissRequest = { onDismiss() },
      title = { Text("Rename Todo", modifier = Modifier.testTag("renameTodoDialogTitle")) },
      text = {
        OutlinedTextField(
            value = newName,
            onValueChange = { newName = it },
            singleLine = true,
            modifier = Modifier.padding(8.dp).fillMaxWidth().testTag("renameTodoTextField"),
            shape = RoundedCornerShape(32.dp),
        )
      },
      confirmButton = {
        TextButton(
            modifier = Modifier.testTag("renameTodoConfirmButton"),
            onClick = {
              onRename(newName.text)
              onDismiss()
            },
            enabled = newName.text.isNotEmpty(),
            colors =
                ButtonColors(
                    Color.Transparent,
                    MaterialTheme.colorScheme.tertiary,
                    Color.Transparent,
                    Color.LightGray)) {
              Text("Rename")
            }
      },
      dismissButton = {
        TextButton(
            modifier = Modifier.testTag("renameTodoDismissButton"),
            onClick = { onDismiss() },
            colors =
                ButtonColors(
                    Color.Transparent,
                    MaterialTheme.colorScheme.tertiary,
                    Color.Transparent,
                    Color.LightGray)) {
              Text("Cancel")
            }
      })
}

@Composable
fun DefaultTodoTimeIcon(todo: Todo) {
  Icon(
      Icons.Default.Timer,
      contentDescription = "Time Spent",
      modifier = Modifier.size(22.dp).testTag("todoTimeIcon_${todo.uid}"),
      tint =
          if (todo.status == TodoStatus.ACTUAL) MaterialTheme.colorScheme.secondary
          else Color.LightGray)
}
