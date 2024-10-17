package com.github.se.eduverse.ui.calculator

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun CalculatorScreen(navigationActions: NavigationActions) {
    var display by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    val context = LocalContext.current

    val buttonsLayout = listOf(
        listOf("(", ")", "C", "DEL"),
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ",", "+", "=")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Calculator") },
                navigationIcon = {
                    IconButton(
                        onClick = { navigationActions.goBack() },
                        modifier = Modifier.testTag("goBackButton")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (result.isEmpty()) Color.Black else Color(0xFF006400))
                    .testTag("display"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = if (result.isEmpty()) display else result,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    color = Color.White,
                    modifier = Modifier
                        .padding(16.dp)
                        .testTag("displayText")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                items(buttonsLayout.size) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        buttonsLayout[index].forEach { label ->
                            CalculatorButton(label = label, onClick = {
                                if (result == "Error, press C to clear") {
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
                                }
                                else {
                                    when (label) {
                                        "C" -> {
                                            display = ""
                                            result = ""
                                        }

                                        "DEL" -> if (display.isNotEmpty()) display =
                                            display.dropLast(1)

                                        "=" -> {
                                            result = try {
                                                evaluateExpression(display)
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Invalid Expression",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                "error"
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

                                        else -> {
                                            if (result.isNotEmpty()) {
                                                display = ""
                                                result = ""
                                            }
                                            display += label
                                        }
                                    }
                                }
                            }, testTag = "button_$label")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(label: String, onClick: () -> Unit, testTag: String) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.Blue)
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


fun evaluateExpression(expression: String): String {
    return try {
        val sanitizedExpression = expression.replace(",", ".")
        val result = evaluate(sanitizedExpression)

        String.format("%.6f", result).trimEnd('0').trimEnd('.', ',')
    } catch (e: Exception) {
        "Error, press C to clear"
    }
}

fun evaluate(expression: String): Double {
    val tokens = expression.toCharArray()

    val values: Stack<Double> = Stack()

    val ops: Stack<Char> = Stack()

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
        }
        else if (tokens[i] == '(') {
            ops.push(tokens[i])
        }
        else if (tokens[i] == ')') {
            while (ops.peek() != '(') {
                values.push(applyOp(ops.pop(), values.pop(), values.pop()))
            }
            ops.pop()
        }
        else if (tokens[i] == '+' || tokens[i] == '-' ||
            tokens[i] == '*' || tokens[i] == '/'
        ) {
            while (!ops.empty() && hasPrecedence(tokens[i], ops.peek())) {
                values.push(applyOp(ops.pop(), values.pop(), values.pop()))
            }
            ops.push(tokens[i])
        }
        i++
    }

    while (!ops.empty()) {
        values.push(applyOp(ops.pop(), values.pop(), values.pop()))
    }
    return values.pop()
}


fun hasPrecedence(op1: Char, op2: Char): Boolean {
    if (op2 == '(' || op2 == ')') return false
    return !(op1 == '*' || op1 == '/') || (op2 != '+' && op2 != '-')
}


fun applyOp(op: Char, b: Double, a: Double): Double {
    return when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0.0) throw UnsupportedOperationException("Cannot divide by zero") else a / b
        else -> 0.0
    }
}

