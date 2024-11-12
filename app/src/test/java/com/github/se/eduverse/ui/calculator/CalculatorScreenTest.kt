package com.github.se.eduverse.ui.calculator

import org.junit.Assert.*
import org.junit.Test

class EvaluatorTest {

  private val evaluator = Evaluator()

  @Test
  fun testEvaluateBasicExpression() {
    assertEquals("5", evaluator.evaluate("2 + 3"))
    assertEquals("25", evaluator.evaluate("5 * 5"))
    assertEquals("3", evaluator.evaluate("9 / 3"))
    assertEquals("5", evaluator.evaluate("105-100"))
    assertEquals("8", evaluator.evaluate("2 ^ 3"))
  }
}
