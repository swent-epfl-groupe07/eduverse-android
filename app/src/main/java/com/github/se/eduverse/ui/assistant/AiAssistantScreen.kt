package com.github.se.eduverse.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar
import com.github.se.eduverse.ui.speechRecognition.SpeechRecognizerInterface
import com.github.se.eduverse.viewmodel.AiAssistantViewModel
import kotlinx.coroutines.launch

/**
 * Composable function that represents the AI Assistant screen. This screen allows users to interact
 * with an AI assistant by sending questions and receiving answers in a chat-like interface.
 *
 * @param navigationActions Object used for handling navigation actions.
 * @param viewModel ViewModel instance that manages the state and logic for the AI assistant.
 */
@Composable
fun AiAssistantScreen(navigationActions: NavigationActions, viewModel: AiAssistantViewModel) {
  // Collecting state from the ViewModel
  val conversation by viewModel.conversation.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val errorMessage by viewModel.errorMessage.collectAsState()

  // State for managing the user's question input
  var userQuestion by remember { mutableStateOf("") }
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()
  var showDialog by remember { mutableStateOf(false) }
  var showRecordDialog by remember { mutableStateOf(false) }

  // Show dialog if an error occurs
  LaunchedEffect(errorMessage) { if (errorMessage != null) showDialog = true }

  // Scaffold for handling the screen layout
  Scaffold(
      topBar = { TopNavigationBar(navigationActions, screenTitle = null) },
      modifier = Modifier.testTag("aiAssistantChatScreenScaffold")) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .testTag("aiAssistantChatScreen")) {
              // LazyColumn to display the conversation (user questions and AI answers)
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
                            // User's question bubble
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End) {
                                  Surface(
                                      shape = MaterialTheme.shapes.medium,
                                      color = MaterialTheme.colorScheme.inversePrimary,
                                      tonalElevation = 1.dp,
                                      modifier =
                                          Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(
                                            text = question,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onSecondary,
                                            fontWeight = FontWeight.SemiBold)
                                      }
                                }

                            Spacer(modifier = Modifier.height(4.dp))

                            // AI's answer bubble
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start) {
                                  Surface(
                                      shape = MaterialTheme.shapes.medium,
                                      color = MaterialTheme.colorScheme.primary,
                                      tonalElevation = 1.dp,
                                      modifier =
                                          Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(
                                            text = answer,
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onPrimary)
                                      }
                                }
                          }
                    }
                  }

              // Display a loading indicator if loading is active
              if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("assistantLoadingRow"),
                    horizontalArrangement = Arrangement.Center) {
                      CircularProgressIndicator(
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.testTag("assistantLoadingIndicator"))
                    }
              }

              // Input row for the user to type questions and send them
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
                        modifier =
                            Modifier.weight(1f)
                                .background(MaterialTheme.colorScheme.background)
                                .testTag("assistantQuestionInput"),
                        placeholder = {
                          Text("Ask a question", color = MaterialTheme.colorScheme.onBackground)
                        },
                        shape = CircleShape,
                        colors =
                            TextFieldDefaults.outlinedTextFieldColors(
                                textColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.onBackground,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline))

                    Spacer(modifier = Modifier.width(8.dp))

                    // Button to send the question
                    SendQuestionButton(
                        onSend = {
                          focusManager.clearFocus()
                          if (userQuestion.isNotBlank()) {
                            scope.launch {
                              viewModel.sendQuestion(userQuestion)
                              userQuestion = ""
                            }
                          }
                        },
                        Modifier.testTag("askAssistantButton"))

                    Spacer(modifier = Modifier.width(4.dp))

                    // Button to record a question
                    IconButton(
                        onClick = { showRecordDialog = true },
                        modifier = Modifier.testTag("voiceInputButton"),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary)) {
                          Icon(
                              imageVector = Icons.Default.Mic,
                              contentDescription = "Voice input",
                              tint = MaterialTheme.colorScheme.onBackground)
                        }
                  }

              // Dialog for recording a question using speech recognition service
              if (showRecordDialog) {
                VoiceInputDialog(onDismiss = { showRecordDialog = false }) {
                  if (it.isNotBlank()) {
                    scope.launch {
                      viewModel.sendQuestion(it)
                      userQuestion = ""
                    }
                  }
                }
              }

              // AlertDialog for error messages
              if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Error") },
                    text = { Text(errorMessage ?: "An unknown error occurred.") },
                    confirmButton = { Button(onClick = { showDialog = false }) { Text("OK") } })
              }
            }
      }
}

/**
 * Composable function that represents a button to send a question to the AI assistant.
 *
 * @param onSend Callback to send the question.
 * @param modifier Modifier for styling the button.
 */
@Composable
fun SendQuestionButton(onSend: () -> Unit, modifier: Modifier = Modifier) {
  Button(onClick = { onSend() }, modifier = modifier) {
    Text("Send", color = MaterialTheme.colorScheme.onPrimary)
  }
}

/**
 * Composable function that represents a dialog for recording a question using voice input.
 *
 * @param onDismiss Callback to dismiss the dialog.
 * @param onSend Callback to send the recorded question.
 */
@Composable
fun VoiceInputDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
  var question by remember { mutableStateOf("") }
  SpeechRecognizerInterface(
      context = LocalContext.current,
      title = "Record your question",
      description =
          "Press the bottom button to record your question. The recorded question will be displayed. If you're happy with it, press the send button to submit it.\n\n" +
              if (question.isNotEmpty()) {
                "Recorded question: $question?"
              } else "",
      onResult = { question = it },
      onDismiss = onDismiss) {
        SendQuestionButton(
            onSend = {
              onSend(question)
              onDismiss()
            },
            modifier = Modifier.width(100.dp).testTag("voiceInputDialogSendButton"))
      }
}
