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
import com.github.se.eduverse.repository.AiAssistantRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    navigationActions: NavigationActions,
    assistantRepository: AiAssistantRepository
) {
  // A list of (question, answer) pairs representing the conversation
  val conversation = remember { mutableStateListOf<Pair<String, String>>() }

  var userQuestion by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()

  Scaffold(
      topBar = { TopNavigationBar("AI Assistant", navigationActions) },
      modifier = Modifier.testTag("aiAssistantChatScreenScaffold")) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background) // Use a nice background color
                    .padding(paddingValues)
                    .testTag("aiAssistantChatScreen")) {
              // Display the conversation in a LazyColumn
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
                            // User question bubble (aligned to the end - right)
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

                            // Assistant answer bubble (aligned to the start - left)
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

              // Display error message if any and not loading
              if (errorMessage != null && !isLoading) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp).testTag("assistantErrorMessageText"),
                    fontWeight = FontWeight.Bold)
              }

              // Display loading indicator if needed
              if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("assistantLoadingRow"),
                    horizontalArrangement = Arrangement.Center) {
                      CircularProgressIndicator(
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.testTag("assistantLoadingIndicator"))
                    }
              }

              // Input field and send button at the bottom
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
                              isLoading = true
                              errorMessage = null
                              val currentQuestion = userQuestion
                              userQuestion = ""
                              try {
                                val response = assistantRepository.askAssistant(currentQuestion)
                                // Add the new Q&A pair to the conversation
                                conversation.add(currentQuestion to response)
                              } catch (e: Exception) {
                                errorMessage = "An error occurred. Please try again."
                              } finally {
                                isLoading = false
                              }
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
