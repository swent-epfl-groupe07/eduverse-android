package com.github.se.eduverse.ui.calculator

import net.objecthunter.exp4j.ExpressionBuilder

class Evaluator {
  fun evaluate(expression: String): String {
    return try {

      val sanitizedExpression = expression.replace("×", "*").replace(",", ".")

      val exp = ExpressionBuilder(sanitizedExpression).build()

      val result = exp.evaluate()

      String.format("%.6f", result).trimEnd('0').trimEnd('.', ',')
    } catch (e: Exception) {
      "Undefined"
    }
  }
}
