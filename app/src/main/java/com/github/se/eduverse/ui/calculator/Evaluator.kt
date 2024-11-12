package com.github.se.eduverse.ui.calculator

import kotlin.math.*
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function

class Evaluator {
  fun evaluate(expression: String): String {
    return try {

      val sanitizedExpression = expression.replace("√", "sqrt").replace("×", "*").replace(",", ".")

      val exp =
          ExpressionBuilder(sanitizedExpression)
              .variables("pi", "e")
              .functions(*customFunctions.toTypedArray())
              .build()
              .setVariable("pi", Math.PI)
              .setVariable("e", Math.E)

      val result = exp.evaluate()

      String.format("%.6f", result).trimEnd('0').trimEnd('.', ',')
    } catch (e: Exception) {
      "Undefined"
    }
  }

  private val customFunctions =
      listOf(
          object : Function("cot", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] % Math.PI == 0.0)
                  throw IllegalArgumentException("Invalid input for cotangent")
              return 1 / tan(args[0])
            }
          },
          object : Function("arcsin", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] < -1 || args[0] > 1)
                  throw IllegalArgumentException("Input for arcsin must be between -1 and 1")
              return asin(args[0])
            }
          },
          object : Function("arccos", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] < -1 || args[0] > 1)
                  throw IllegalArgumentException("Input for arccos must be between -1 and 1")
              return acos(args[0])
            }
          },
          object : Function("arctan", 1) {
            override fun apply(args: DoubleArray): Double = atan(args[0])
          },
          object : Function("arccot", 1) {
            override fun apply(args: DoubleArray): Double {
              return atan(1 / args[0])
            }
          },
          object : Function("sinh", 1) {
            override fun apply(args: DoubleArray): Double = sinh(args[0])
          },
          object : Function("cosh", 1) {
            override fun apply(args: DoubleArray): Double = cosh(args[0])
          },
          object : Function("tanh", 1) {
            override fun apply(args: DoubleArray): Double = tanh(args[0])
          },
          object : Function("coth", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] == 0.0) throw IllegalArgumentException("Invalid input for coth")
              return 1 / tanh(args[0])
            }
          },
          object : Function("arsinh", 1) {
            override fun apply(args: DoubleArray): Double =
                ln(args[0] + sqrt(args[0] * args[0] + 1))
          },
          object : Function("arcosh", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] < 1) throw IllegalArgumentException("Input for arcosh must be >= 1")
              return ln(args[0] + sqrt(args[0] * args[0] - 1))
            }
          },
          object : Function("artanh", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] <= -1 || args[0] >= 1)
                  throw IllegalArgumentException("Input for artanh must be between -1 and 1")
              return 0.5 * ln((1 + args[0]) / (1 - args[0]))
            }
          },
          object : Function("arcoth", 1) {
            override fun apply(args: DoubleArray): Double {
              if (args[0] > -1 && args[0] < 1)
                  throw IllegalArgumentException("Input for arcoth must be <= -1 or >= 1")
              return 0.5 * ln((args[0] + 1) / (args[0] - 1))
            }
          })
}
