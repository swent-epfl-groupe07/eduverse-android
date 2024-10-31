package com.github.se.eduverse.ui.calculator

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.se.eduverse.ui.navigation.NavigationActions
import java.util.Stack
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.Canvas
import kotlin.math.*

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CalculatorScreen(navigationActions: NavigationActions) {
  var display by remember { mutableStateOf("") }
  var result by remember { mutableStateOf("") }
  val context = LocalContext.current

  var selectedMenu by remember { mutableStateOf("Basic") }

  val basicButtons =
      listOf(
          listOf("(", ")", "7", "8", "9", "÷"),
          listOf("^", "√", "4", "5", "6", "×"),
          listOf("e", "x", "1", "2", "3", "-"),
          listOf("π", "%", "0", ".", "=", "+"))

    val functionButtons = listOf(
        listOf("exp", "sin", "cos", "tan", "cot"),
        listOf("ln", "arcsin", "arccos", "arctan", "arccot"),
        listOf("log□", "sinh", "cosh", "tanh", "coth"),
        listOf("rad", "arsinh", "arcosh", "artanh", "arcoth")
    )

    val equationButtons = listOf(
        listOf("<", ">", "≤", "≥", "≠"),
        listOf("x", "f(x)", "i", "C(□ □)", "Matrix")
    )

  val graphButtons = listOf(listOf("y=", "y≠", "y<", "y>"), listOf("y≤", "y≥", "f(x)", "g(x)"))

  val buttonsLayout =
      when (selectedMenu) {
        "Basic" -> basicButtons
        "Functions" -> functionButtons
        "Equations" -> equationButtons
        "Graphs" -> graphButtons
        else -> basicButtons
      }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(text = "Calculator", color = Color.Black) },
            navigationIcon = {
              IconButton(
                  onClick = { navigationActions.goBack() },
                  modifier = Modifier.testTag("goBackButton")) {
                    Icon(
                        Icons.Default.ArrowBack, contentDescription = "Go Back", tint = Color.Black)
                  }
            },
            backgroundColor = Color.White,
            elevation = 4.dp)
      }) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MenuButton("Basic", selectedMenu == "Basic") { selectedMenu = "Basic" }
            MenuButton("Functions", selectedMenu == "Functions") { selectedMenu = "Functions" }
            MenuButton("Equations", selectedMenu == "Equations") { selectedMenu = "Equations" }
            MenuButton("Graphs", selectedMenu == "Graphs") { selectedMenu = "Graphs" }
          }

          Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White)
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = display,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Start,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            display = ""
                            result = ""
                        }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = Color.LightGray)
                        }
                    }

                    if (result.isNotEmpty()) {

                        Spacer(modifier = Modifier.height(8.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                        ) {
                            val dashWidth = 3.dp.toPx()
                            val gapWidth = 3.dp.toPx()

                            drawLine(
                                color = Color.LightGray,
                                start = Offset(0f, center.y),
                                end = Offset(size.width, center.y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barColor = if (result == "Undefined") Color(0xFFD51F1F) else Color(0xFF07A92D)
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(50.dp)
                                    .background(barColor)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = result,
                                fontSize = 20.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize()
                    .align(Alignment.CenterHorizontally)
                    .weight(3f),
                verticalArrangement = Arrangement.Bottom
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                    elevation = 10.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier.background(Color.White, shape = RoundedCornerShape(topStart = 16.dp))
                        ) {
                            Text("abc", fontSize = 16.sp)
                        }

                        IconButton(onClick = {
                            Toast.makeText(context, "History feature not implemented", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }

                        IconButton(onClick = {
                            if (display.isNotEmpty()) {
                                display = display.dropLast(1) + display.last()
                            }
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Left Arrow")
                        }

                        IconButton(onClick = {
                            // Action for "Right Arrow" - Implement navigation right logic
                            Toast.makeText(context, "Right Arrow feature not implemented", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Right Arrow")
                        }

                        IconButton(onClick = {}) {
                            Icon(Icons.Default.SubdirectoryArrowLeft, contentDescription = "Expand")
                        }

                        IconButton(
                            onClick = {
                                if (display.isNotEmpty()) {
                                    display = display.dropLast(1)
                                }
                            },
                            modifier = Modifier.background(Color.White, shape = RoundedCornerShape(topEnd = 16.dp))
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Delete")
                        }
                    }
                }

                buttonsLayout.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        row.forEach { label ->
                            CalculatorButton(
                                label = label,
                                onClick = {
                                    if (result == "Undefined") {
                                        when (label) {
                                            "C", "DEL", "=" -> {
                                                display = ""
                                                result = ""
                                            }
                                            else -> {
                                                display = label
                                                result = ""
                                            }
                                        }
                                    } else {
                                        when (label) {
                                            "C" -> {
                                                display = ""
                                                result = ""
                                            }
                                            "DEL" -> if (display.isNotEmpty()) display = display.dropLast(1)
                                            "=" -> {
                                                result = try {
                                                    evaluateExpression(display)
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Invalid Expression",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    "Error"
                                                }
                                            }
                                            "+", "-", "*", "/" -> {
                                                if (result.isNotEmpty()) {
                                                    display = result + label
                                                    result = ""
                                                } else {
                                                    display += label
                                                }
                                            }
                                            "^", "√", "exp", "sin", "cos", "tan" -> {
                                                display += label
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
                                selectedMenu = selectedMenu
                            )
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
fun CalculatorButton(label: String, onClick: () -> Unit, testTag: String, selectedMenu : String) {
  val isNumber = label.all { it.isDigit() }
  val backgroundColor = if (isNumber) Color(0xFFEBE9E9) else Color.White
  val fontSize = if (selectedMenu == "Basic") 24 else 18
  Box(
      modifier =
          Modifier.size(64.dp)
              .background(backgroundColor)
              .border(BorderStroke(0.3.dp, Color.LightGray))
              .clickable { onClick() }
              .testTag(testTag),
      contentAlignment = Alignment.Center) {
        Text(text = label, fontSize = fontSize.sp, fontWeight = FontWeight.Light, color = Color.Black)
      }
}

fun evaluateExpression(expression: String): String {
    return try {
        val sanitizedExpression = expression.replace(",", ".")
        val result = evaluate(sanitizedExpression)

        String.format("%.6f", result).trimEnd('0').trimEnd('.', ',')
    } catch (e: Exception) {
        "Undefined"
    }
}

fun evaluate(expression: String): Double {
    val tokens = expression.toCharArray()

    val values: Stack<Double> = Stack()
    val ops: Stack<String> = Stack()

    var i = 0
    while (i < tokens.size) {
        if (tokens[i] == ' ') {
            i++
            continue
        }

        if (tokens[i] in '0'..'9' || tokens[i] == '.') {
            val sbuf = StringBuilder()
            while (i < tokens.size && (tokens[i] in '0'..'9' || tokens[i] == '.')) {
                sbuf.append(tokens[i++])
            }
            values.push(sbuf.toString().toDouble())
            i--
        } else if (tokens[i] == '(') {
            ops.push(tokens[i].toString())
        } else if (tokens[i] == ')') {
            while (ops.peek() != "(") {
                values.push(applyOp(ops.pop(), values.pop(), values.pop()))
            }
            ops.pop()
        } else {
            val operator = parseOperator(i, tokens)
            while (!ops.empty() && hasPrecedence(operator, ops.peek())) {
                values.push(applyOp(ops.pop(), values.pop(), values.pop()))
            }
            ops.push(operator)
        }
        i++
    }

    while (!ops.empty()) {
        values.push(applyOp(ops.pop(), values.pop(), values.pop()))
    }
    return values.pop()
}

fun parseOperator(index: Int, tokens: CharArray): String {
    return when {
        tokens[index] == '√' -> "√"
        tokens[index] == '^' -> "^"
        else -> tokens[index].toString()
    }
}

fun applyOp(op: String, b: Double, a: Double): Double {
    return when (op) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b == 0.0) throw UnsupportedOperationException("Cannot divide by zero") else a / b
        "^" -> a.pow(b)
        "√" -> sqrt(a)
        "sin" -> sin(b)
        "cos" -> cos(b)
        "tan" -> tan(b)
        "ln" -> ln(b)
        "log" -> log10(b)
        else -> 0.0
    }
}

fun hasPrecedence(op1: String, op2: String): Boolean {
    return !(op1 == "*" || op1 == "/" || op1 == "^" || op1 == "√") || (op2 != "+" && op2 != "-")
}