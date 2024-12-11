package com.github.se.eduverse.ui.calculator

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.navigation.NavigationActions
import com.github.se.eduverse.ui.navigation.TopNavigationBar

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CalculatorScreen(navigationActions: NavigationActions) {
  var display by remember { mutableStateOf("") }
  var result by remember { mutableStateOf("") }
  val context = LocalContext.current

  var selectedMenu by remember { mutableStateOf("Basic") }

  val basicButtons =
      listOf(
          listOf("(", ")", "7", "8", "9", "/"),
          listOf("^", "√", "4", "5", "6", "×"),
          listOf("e", "x", "1", "2", "3", "-"),
          listOf("π", "%", "0", ".", "=", "+"))

  val functionButtons =
      listOf(
          listOf("exp", "sin", "cos", "tan", "cot"),
          listOf("ln", "arcsin", "arccos", "arctan", "arccot"),
          listOf("log", "sinh", "cosh", "tanh", "coth"),
          listOf("rad", "arsinh", "arcosh", "artanh", "arcoth"))

  val buttonsLayout =
      when (selectedMenu) {
        "Basic" -> basicButtons
        else -> functionButtons
      }

  Scaffold(topBar = { TopNavigationBar("Calculator", navigationActions) }) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        MenuButton("Basic", selectedMenu == "Basic") { selectedMenu = "Basic" }
        MenuButton("Functions", selectedMenu == "Functions") { selectedMenu = "Functions" }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Box(
          modifier =
              Modifier.fillMaxWidth()
                  .weight(1f)
                  .background(Color.White)
                  .padding(16.dp)
                  .testTag("display"),
          contentAlignment = Alignment.TopStart) {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = display,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Start,
                        color = Color.Black,
                        modifier = Modifier.weight(1f).testTag("displayText"))

                    IconButton(
                        onClick = {
                          display = ""
                          result = ""
                        },
                        modifier = Modifier.testTag("clearButton")) {
                          Icon(
                              Icons.Default.Cancel,
                              contentDescription = "C",
                              tint = Color.LightGray)
                        }
                  }

              if (result.isNotEmpty()) {

                Spacer(modifier = Modifier.height(8.dp))

                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                  val dashWidth = 3.dp.toPx()
                  val gapWidth = 3.dp.toPx()

                  drawLine(
                      color = Color.LightGray,
                      start = Offset(0f, center.y),
                      end = Offset(size.width, center.y),
                      strokeWidth = 1.dp.toPx(),
                      pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                      val barColor =
                          if (result == "Undefined") Color(0xFFD51F1F) else Color(0xFF07A92D)
                      Box(
                          modifier =
                              Modifier.width(5.dp)
                                  .height(50.dp)
                                  .background(barColor)
                                  .testTag("resultBar"))

                      Spacer(modifier = Modifier.width(16.dp))
                      Text(
                          text = result,
                          fontSize = 20.sp,
                          color = Color.Gray,
                          textAlign = TextAlign.Start,
                          fontWeight = FontWeight.Light,
                          modifier = Modifier.padding(top = 8.dp).testTag("resultText"))
                    }
              }
            }
          }

      Column(
          modifier =
              Modifier.fillMaxWidth().fillMaxSize().align(Alignment.CenterHorizontally).weight(3f),
          verticalArrangement = Arrangement.Bottom) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                elevation = 10.dp,
                color = Color.White) {
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(
                            onClick = {
                              Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT)
                                  .show()
                            },
                            modifier =
                                Modifier.background(
                                    Color.White, shape = RoundedCornerShape(topStart = 16.dp))) {
                              Text("abc", fontSize = 16.sp)
                            }

                        IconButton(
                            onClick = {
                              Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT)
                                  .show()
                            }) {
                              Icon(Icons.Default.History, contentDescription = "History")
                            }

                        IconButton(
                            onClick = {
                              Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT)
                                  .show()
                            }) {
                              Icon(Icons.Default.ArrowBack, contentDescription = "Left Arrow")
                            }

                        IconButton(
                            onClick = {
                              Toast.makeText(context, "Not yet implemented", Toast.LENGTH_SHORT)
                                  .show()
                            }) {
                              Icon(Icons.Default.ArrowForward, contentDescription = "Right Arrow")
                            }

                        IconButton(
                            onClick = {
                              if (result == "Undefined") {} else {
                                result =
                                    when {
                                      display.isEmpty() -> "Undefined"
                                      else -> {
                                        try {
                                          evaluateExpression(display)
                                        } catch (e: Exception) {
                                          "Undefined"
                                        }
                                      }
                                    }
                              }
                            }) {
                              Icon(Icons.Default.SubdirectoryArrowLeft, contentDescription = "=")
                            }

                        IconButton(
                            onClick = {
                              if (display.isNotEmpty()) {
                                display = display.dropLast(1)
                              }
                            },
                            modifier =
                                Modifier.background(
                                        Color.White, shape = RoundedCornerShape(topEnd = 16.dp))
                                    .testTag("backspaceButton")) {
                              Icon(Icons.Default.Backspace, contentDescription = "DEL")
                            }
                      }
                }

            buttonsLayout.forEach { row ->
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    row.forEach { label ->
                      CalculatorButton(
                          label = label,
                          onClick = {
                            if (result == "Undefined") {
                              when (label) {
                                "=" -> {}
                                else -> {
                                  display = label
                                  result = ""
                                }
                              }
                            } else {
                              when (label) {
                                "=" -> {
                                  result =
                                      try {
                                        evaluateExpression(display)
                                      } catch (e: Exception) {
                                        "Undefined"
                                      }
                                }
                                "+",
                                "-",
                                "*",
                                "/",
                                "×",
                                "^" -> {
                                  if (result.isNotEmpty()) {
                                    display = result + label
                                    result = ""
                                  } else {
                                    display += label
                                  }
                                }
                                "√",
                                "exp",
                                "sin",
                                "cos",
                                "tan",
                                "cot",
                                "ln",
                                "arcsin",
                                "arccos",
                                "arctan",
                                "arccot",
                                "log",
                                "sinh",
                                "cosh",
                                "tanh",
                                "coth",
                                "rad",
                                "arsinh",
                                "arcosh",
                                "artanh",
                                "arcoth" -> {
                                  if (result.isNotEmpty()) {
                                    display = "$label($result)"
                                    result = ""
                                  } else {
                                    display += "$label("
                                  }
                                }
                                else -> {
                                  if (result.isNotEmpty()) {
                                    display = ""
                                    result = ""
                                  }
                                  display += label
                                }
                              }
                            }
                          },
                          testTag = "button_$label",
                          selectedMenu = selectedMenu)
                    }
                  }
            }
          }
    }
  }
}

@Composable
fun MenuButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
  Box(
      modifier =
          Modifier.padding(4.dp)
              .background(
                  color = if (isSelected) Color.Black else Color.Gray,
                  shape = RoundedCornerShape(16.dp))
              .clickable { onClick() }
              .padding(vertical = 8.dp, horizontal = 16.dp),
      contentAlignment = Alignment.Center) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.SemiBold)
      }
}

@Composable
fun CalculatorButton(label: String, onClick: () -> Unit, testTag: String, selectedMenu: String) {
  val isNumber = label.all { it.isDigit() }
  val backgroundColor = if (isNumber) Color(0xFFEBE9E9) else Color.White
  val fontSize = if (selectedMenu == "Basic") 24 else 18

  val buttonWidth =
      when (selectedMenu) {
        "Basic" -> 64.dp
        else -> 76.8.dp
      }

  Box(
      modifier =
          Modifier.width(buttonWidth)
              .height(64.dp)
              .background(backgroundColor)
              .border(BorderStroke(0.3.dp, Color.LightGray))
              .clickable { onClick() }
              .testTag(testTag),
      contentAlignment = Alignment.Center) {
        Text(
            text = label,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Light,
            color = Color.Black)
      }
}

fun evaluateExpression(expression: String): String {
  val evaluator = Evaluator()
  return evaluator.evaluate(expression)
}
