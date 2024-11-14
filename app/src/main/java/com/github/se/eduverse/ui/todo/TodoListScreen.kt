package com.github.se.eduverse.ui.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.github.se.eduverse.model.Todo
import com.github.se.eduverse.model.TodoStatus
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.TodoListViewModel

/** Composable that represents the user's todo list screen */
@Composable
fun TodoListScreen(navigationActions: NavigationActions, todoListViewModel: TodoListViewModel) {
  val actualTodos = todoListViewModel.actualTodos.collectAsState()
  val doneTodos = todoListViewModel.doneTodos.collectAsState()
  var showCompleted by remember { mutableStateOf(false) }

  Scaffold(
      topBar = { TopNavigationBar("Todo List", navigationActions = navigationActions) },
      content = { pd ->
        Column(modifier = Modifier.fillMaxSize().padding(pd).testTag("todoListScreen")) {
          AddTodoEntry { name -> todoListViewModel.addNewTodo(name) }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
              modifier = Modifier.padding(8.dp),
          ) {
            TextButton(
                onClick = { showCompleted = false },
                enabled = showCompleted,
                colors =
                    ButtonColors(Color.White, Color(0xFF217384), Color(0xFF217384), Color.White),
                modifier = Modifier.testTag("currentTasksButton")) {
                  Text("Current Tasks", modifier = Modifier.padding(8.dp), fontSize = 20.sp)
                }
            TextButton(
                onClick = { showCompleted = true },
                enabled = !showCompleted,
                colors =
                    ButtonColors(Color.White, Color(0xFF217384), Color(0xFF217384), Color.White),
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
                  { todoListViewModel.setTodoDone(todo) }) {
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
                  }
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
    rightMostButton: @Composable () -> Unit
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
                      Color(0xFF217384), Color.White, Color.Transparent, Color.LightGray)) {
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
                  Text(text = "Add a new Task", color = Color.Gray)
                }
                innerTextField()
              })
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
      onDismissRequest = { onDismiss() },
      title = { Text("Rename Todo") },
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
            onClick = {
              onRename(newName.text)
              onDismiss()
            },
            enabled = newName.text.isNotEmpty(),
            colors =
                ButtonColors(
                    Color.Transparent, Color(0xFF217384), Color.Transparent, Color.LightGray)) {
              Text("Rename")
            }
      },
      dismissButton = {
        TextButton(
            onClick = { onDismiss() },
            colors =
                ButtonColors(
                    Color.Transparent, Color(0xFF217384), Color.Transparent, Color.LightGray)) {
              Text("Cancel")
            }
      })
}
