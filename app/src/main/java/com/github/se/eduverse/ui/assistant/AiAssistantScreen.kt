package com.github.se.eduverse.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.viewmodel.AiAssistantViewModel
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(navigationActions: NavigationActions, viewModel: AiAssistantViewModel) {
  val conversation by viewModel.conversation.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  var userQuestion by remember { mutableStateOf("") }
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()

  Scaffold(
      topBar = { TopNavigationBar("AI Assistant", navigationActions) },
      modifier = Modifier.testTag("aiAssistantChatScreenScaffold")) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .testTag("aiAssistantChatScreen")) {
              LazyColumn(
                  modifier =
                      Modifier.fillMaxWidth()
                          .weight(1f)
                          .padding(horizontal = 8.dp)
                          .testTag("aiAssistantMessageList"),
                  contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(conversation) { (question, answer) ->
                      Column(
                          modifier =
                              Modifier.fillMaxWidth()
                                  .padding(vertical = 8.dp)
                                  .testTag("messageItem")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End) {
                                  Surface(
                                      shape = MaterialTheme.shapes.medium,
                                      color = MaterialTheme.colorScheme.secondaryContainer,
                                      tonalElevation = 1.dp,
                                      modifier =
                                          Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(
                                            text = question,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.SemiBold)
                                      }
                                }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start) {
                                  Surface(
                                      shape = MaterialTheme.shapes.medium,
                                      color = MaterialTheme.colorScheme.surfaceVariant,
                                      tonalElevation = 1.dp,
                                      modifier =
                                          Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(
                                            text = answer,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                      }
                                }
                          }
                    }
                  }

              if (errorMessage != null && !isLoading) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp).testTag("assistantErrorMessageText"),
                    fontWeight = FontWeight.Bold)
              }

              if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("assistantLoadingRow"),
                    horizontalArrangement = Arrangement.Center) {
                      CircularProgressIndicator(
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.testTag("assistantLoadingIndicator"))
                    }
              }

              Row(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(MaterialTheme.colorScheme.surface)
                          .padding(8.dp)
                          .testTag("assistantInputRow"),
                  verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = userQuestion,
                        onValueChange = { userQuestion = it },
                        modifier = Modifier.weight(1f).testTag("assistantQuestionInput"),
                        placeholder = { Text("Ask a question") },
                        colors =
                            TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline))

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                          focusManager.clearFocus()
                          if (userQuestion.isNotBlank()) {
                            scope.launch {
                              viewModel.sendQuestion(userQuestion)
                              userQuestion = ""
                            }
                          }
                        },
                        modifier = Modifier.testTag("askAssistantButton"),
                        colors =
                            ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colorScheme.primary)) {
                          Text("Send", color = MaterialTheme.colorScheme.onPrimary)
                        }
                  }
            }
      }
}
