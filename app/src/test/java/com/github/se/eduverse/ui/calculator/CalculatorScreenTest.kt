package com.github.se.eduverse.ui.calculator

import org.junit.Assert.*
import org.junit.Test

class CalculatorTest {

  @Test
  fun testEvaluateExpression() {
    assertEquals("4", evaluateExpression("2+2"))
    assertEquals("30", evaluateExpression("5*6"))
    assertEquals("1", evaluateExpression("4-3"))
    assertEquals("20", evaluateExpression("(2+3)*4"))
    assertEquals("Error, press C to clear", evaluateExpression("2+"))
    assertEquals("Error, press C to clear", evaluateExpression("5*/3"))
    assertEquals("1,428571", evaluateExpression("10/7"))
    assertEquals("333333333,333333", evaluateExpression("1000000000/3"))
  }

  @Test
  fun testApplyOp() {
    assertEquals(4.0, applyOp('+', 2.0, 2.0), 0.0)
    assertEquals(-2.0, applyOp('-', 4.0, 2.0), 0.0)
    assertEquals(8.0, applyOp('*', 4.0, 2.0), 0.0)
    assertEquals(0.5, applyOp('/', 4.0, 2.0), 0.0)

    assertThrows(UnsupportedOperationException::class.java) { applyOp('/', 0.0, 4.0) }
  }
}
