package com.github.se.eduverse.ui.quizz

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.se.eduverse.model.Question
import com.github.se.eduverse.repository.QuizzRepository
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.theme.COLOR_CORRECT
import com.github.se.eduverse.ui.theme.COLOR_INCORRECT
import com.github.se.eduverse.ui.theme.COLOR_TEXT_PLACEHOLDER
import kotlinx.coroutines.launch

// Main composable for the quiz screen
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun QuizScreen(navigationActions: NavigationActions, quizzRepository: QuizzRepository) {
  // Mutable states for managing quiz inputs and data
  var topic by remember { mutableStateOf("") }
  var difficulty by remember { mutableStateOf("medium") }
  var numberOfQuestions by remember { mutableStateOf(10) }
  var questions by remember { mutableStateOf(listOf<Question>()) }
  val selectedAnswers = remember { mutableStateMapOf<Int, String>() }
  var isQuizSubmitted by remember { mutableStateOf(false) }
  var score by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  Scaffold(
      topBar = {
        // Top navigation bar with back button
        TopAppBar(
            title = { Text("Quiz Generator", color = Color.Black) },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("goBackButton")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                  }
            },
            backgroundColor = MaterialTheme.colorScheme.surface)
      }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).testTag("quizScreen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top) {

              // Input field for quiz topic
              BasicTextField(
                  value = topic,
                  onValueChange = { topic = it },
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(8.dp)
                          .background(
                              MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                              MaterialTheme.shapes.medium)
                          .padding(16.dp)
                          .testTag("topicInput"),
                  decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                      if (topic.isEmpty()) {
                        // Placeholder text for topic input
                        Text("Enter a topic for the quiz", color = COLOR_TEXT_PLACEHOLDER)
                      }
                      innerTextField()
                    }
                  })

              Spacer(modifier = Modifier.height(16.dp))

              // Dropdowns for quiz difficulty and number of questions
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    DropdownMenuDemo(
                        label = "Difficulty",
                        value = difficulty,
                        modifier = Modifier.testTag("difficultyDropdown")) { selected ->
                          difficulty = selected
                        }
                    DropdownMenuDemo(
                        label = "Questions",
                        value = numberOfQuestions.toString(),
                        options = (1..30).map { it.toString() },
                        modifier = Modifier.testTag("numberOfQuestionsDropdown")) { selected ->
                          numberOfQuestions = selected.toInt()
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Button to generate quiz questions
              Button(
                  onClick = {
                    focusManager.clearFocus()
                    scope.launch {
                      try {
                        isQuizSubmitted = false
                        questions =
                            quizzRepository.getQuestionsFromGPT(
                                topic, difficulty, numberOfQuestions)
                        selectedAnswers.clear()
                      } catch (e: Exception) {
                        e.printStackTrace()
                      }
                    }
                  },
                  modifier = Modifier.padding(8.dp).testTag("generateQuizButton"),
                  colors =
                      ButtonDefaults.buttonColors(
                          backgroundColor = MaterialTheme.colorScheme.primary)) {
                    Text("Generate Quiz", color = MaterialTheme.colorScheme.onPrimary)
                  }

              Spacer(modifier = Modifier.height(16.dp))

              // Display list of questions
              if (questions.isNotEmpty()) {
                LazyColumn(modifier = Modifier.weight(1f).testTag("questionsList")) {
                  itemsIndexed(questions) { index, question ->
                    QuestionItem(
                        question = question,
                        selectedAnswer = selectedAnswers[index],
                        onAnswerSelected = { selectedAnswers[index] = it },
                        onAnswerDeselected = { selectedAnswers.remove(index) },
                        isQuizSubmitted = isQuizSubmitted,
                        questionIndex = index)
                  }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Button to submit the quiz
                if (!isQuizSubmitted) {
                  Button(
                      onClick = {
                        isQuizSubmitted = true
                        score =
                            questions.countIndexed { i, question ->
                              selectedAnswers[i] == question.correctAnswer
                            }
                      },
                      modifier = Modifier.padding(8.dp).testTag("submitQuizButton"),
                      colors =
                          ButtonDefaults.buttonColors(
                              backgroundColor = MaterialTheme.colorScheme.primary)) {
                        Text("Submit Quiz", color = MaterialTheme.colorScheme.onPrimary)
                      }
                } else {
                  // Display score after submission
                  Text(
                      text = "Your Score: $score / ${questions.size}",
                      style =
                          MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                      modifier = Modifier.padding(8.dp).testTag("scoreText"),
                      color = Color.Black)
                  // Button to reset quiz
                  Button(
                      onClick = {
                        topic = ""
                        difficulty = "easy"
                        numberOfQuestions = 5
                        questions = emptyList()
                        selectedAnswers.clear()
                        isQuizSubmitted = false
                      },
                      modifier = Modifier.padding(8.dp).testTag("generateNewQuizButton"),
                      colors =
                          ButtonDefaults.buttonColors(
                              backgroundColor = MaterialTheme.colorScheme.primary)) {
                        Text("Generate New Quiz", color = MaterialTheme.colorScheme.onPrimary)
                      }
                }
              }
            }
      }
}

// Dropdown menu for quiz settings
@Composable
fun DropdownMenuDemo(
    label: String,
    value: String,
    options: List<String> = listOf("easy", "medium", "hard"),
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Box(
      modifier =
          modifier
              .background(
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.shapes.medium)
              .height(48.dp)
              .padding(horizontal = 8.dp)) {
        // Button to open dropdown
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag("${label.lowercase()}Button")) {
              Text("$label: $value", color = COLOR_TEXT_PLACEHOLDER, fontWeight = FontWeight.Bold)
            }
        // Dropdown menu with selectable options
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("${label.lowercase()}DropdownMenu")) {
              options.forEach { option ->
                DropdownMenuItem(
                    onClick = {
                      onValueChange(option)
                      expanded = false
                    },
                    modifier = Modifier.testTag("${label.lowercase()}Option_$option")) {
                      Text(option)
                    }
              }
            }
      }
}

// Displays a single quiz question and its answers
@Composable
fun QuestionItem(
    question: Question,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onAnswerDeselected: () -> Unit,
    isQuizSubmitted: Boolean,
    questionIndex: Int
) {
  Column(modifier = Modifier.padding(8.dp).testTag("questionItem_$questionIndex")) {
    // Question text
    Text(
        text = question.text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp).testTag("questionText_$questionIndex"))

    // Answer options
    question.answers.forEachIndexed { answerIndex, answer ->
      val isSelected = selectedAnswer == answer

      // Highlight correct/incorrect answers if submitted
      val backgroundColor =
          when {
            isQuizSubmitted && isSelected && selectedAnswer == question.correctAnswer ->
                COLOR_CORRECT
            isQuizSubmitted && isSelected && selectedAnswer != question.correctAnswer ->
                COLOR_INCORRECT
            else -> Color.Transparent
          }

      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .background(backgroundColor, MaterialTheme.shapes.small)
                  .padding(8.dp)
                  .testTag("answerBox_${questionIndex}_$answerIndex")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              // Select answer with radio button
              RadioButton(
                  selected = isSelected,
                  onClick = {
                    if (!isQuizSubmitted) {
                      if (isSelected) {
                        onAnswerDeselected()
                      } else {
                        onAnswerSelected(answer)
                      }
                    }
                  },
                  colors =
                      RadioButtonDefaults.colors(
                          selectedColor = MaterialTheme.colorScheme.primary,
                          unselectedColor = Color.Gray),
                  modifier = Modifier.testTag("radioButton_${questionIndex}_$answerIndex"))
              // Display answer text
              Text(
                  text = answer,
                  modifier =
                      Modifier.padding(start = 8.dp)
                          .testTag("answerText_${questionIndex}_$answerIndex"),
                  color = MaterialTheme.colorScheme.onSurface)
            }
          }
    }

    // Display correct answer if selected answer is incorrect
    if (isQuizSubmitted && selectedAnswer != question.correctAnswer) {
      Text(
          text = "Correct Answer: ${question.correctAnswer}",
          color = COLOR_CORRECT,
          fontWeight = FontWeight.Bold,
          fontSize = MaterialTheme.typography.bodyMedium.fontSize,
          modifier =
              Modifier.padding(start = 16.dp, top = 8.dp)
                  .testTag("correctAnswerText_$questionIndex"))
    }
  }
}

// Counts elements in a list that match a condition
fun <T> List<T>.countIndexed(predicate: (Int, T) -> Boolean): Int {
  var count = 0
  forEachIndexed { index, element -> if (predicate(index, element)) count++ }
  return count
}
