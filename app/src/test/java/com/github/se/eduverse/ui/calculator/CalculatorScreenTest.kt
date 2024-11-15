package com.github.se.eduverse.ui.calculator

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.tanh
import org.junit.Assert.*
import org.junit.Test

class EvaluatorTest {

  private val evaluator = Evaluator()

  @Test
  fun testEvaluateBasicExpression() {
    assertEquals("5", evaluator.evaluate("2 + 3"))
    assertEquals("25", evaluator.evaluate("5 * 5"))
    assertEquals("3", evaluator.evaluate("9 / 3"))
  }

  @Test
  fun testEvaluateAdvancedExpression() {
    assertEquals("1", evaluator.evaluate("sin(pi / 2)"))
    assertEquals("1", evaluator.evaluate("cos(0)"))
    assertEquals("0", evaluator.evaluate("tan(0)"))
    assertEquals("4", evaluator.evaluate("sqrt(16)"))
    assertEquals("8", evaluator.evaluate("2 ^ 3"))
  }

  @Test
  fun testCustomFunctionsValidInputs() {
    assertEquals("1", evaluator.evaluate("cot(pi / 4)"))
    assertEquals("0", evaluator.evaluate("arcsin(0)"))
    assertEquals("0", evaluator.evaluate("arccos(1)"))
    assertEquals("0", evaluator.evaluate("arctan(0)"))
    assertEquals("0", evaluator.evaluate("arsinh(0)"))
  }

  @Test
  fun testCustomFunctionsOutOfDomain() {
    assertEquals("Undefined", evaluator.evaluate("arcsin(2)"))
    assertEquals("Undefined", evaluator.evaluate("arccos(-2)"))
    assertEquals("Undefined", evaluator.evaluate("cot(0)"))
    assertEquals("Undefined", evaluator.evaluate("artanh(2)"))
    assertEquals("Undefined", evaluator.evaluate("arcoth(0.5)"))
    assertEquals("Undefined", evaluator.evaluate("arcosh(0.5)"))
  }

  @Test
  fun testInvalidExpressions() {
    assertEquals("Undefined", evaluator.evaluate("5 / (3 - 3)"))
    assertEquals("Undefined", evaluator.evaluate("2 + "))
    assertEquals("NaN", evaluator.evaluate("sqrt(-1)"))
    assertEquals("Undefined", evaluator.evaluate("arcsin(abc)"))
  }

  @Test
  fun testInvalidCot() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "cot" }!!.apply(PI)
    }
  }

  @Test
  fun testInvalidArcsin() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "arcsin" }!!.apply(2.0)
    }
  }

  @Test
  fun testInvalidArccos() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "arccos" }!!.apply(2.0)
    }
  }

  @Test
  fun testInvalidCoth() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "coth" }!!.apply(0.0)
    }
  }

  @Test
  fun testInvalidArcosh() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "arcosh" }!!.apply(0.5)
    }
  }

  @Test
  fun testInvalidArtanh() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "artanh" }!!.apply(1.5)
    }
  }

  @Test
  fun testInvalidArcoth() {
    assertThrows(IllegalArgumentException::class.java) {
      evaluator.customFunctions.find { it.name == "arcoth" }!!.apply(0.5)
    }
  }

  @Test
  fun testArctan() {
    val arctan = evaluator.customFunctions.find { it.name == "arctan" }!!
    assert(arctan.apply(1.0) == atan(1.0))
  }

  @Test
  fun testSinh() {
    val sinh = evaluator.customFunctions.find { it.name == "sinh" }!!
    assert(sinh.apply(0.0).toInt() == 0)
  }

  @Test
  fun testCosh() {
    val cosh = evaluator.customFunctions.find { it.name == "cosh" }!!
    assert(cosh.apply(0.0).toInt() == 1)
  }

  @Test
  fun testTanh() {
    val tanh = evaluator.customFunctions.find { it.name == "tanh" }!!
    assert(tanh.apply(1.0) == tanh(1.0))
  }

  @Test
  fun testCoth() {
    val coth = evaluator.customFunctions.find { it.name == "coth" }!!

    assert(coth.apply(1.0) == 1 / tanh(1.0))

    assertThrows(IllegalArgumentException::class.java) { coth.apply(0.0) }
  }

  @Test
  fun testArsinh() {
    val arsinh = evaluator.customFunctions.find { it.name == "arsinh" }!!
    assert(arsinh.apply(0.0).toInt() == 0)
  }

  @Test
  fun testArcosh() {
    val arcosh = evaluator.customFunctions.find { it.name == "arcosh" }!!

    assert(arcosh.apply(1.0).toInt() == 0)

    assertThrows(IllegalArgumentException::class.java) { arcosh.apply(0.5) }
  }

  @Test
  fun testArtanh() {
    val artanh = evaluator.customFunctions.find { it.name == "artanh" }!!

    assert(artanh.apply(0.5) == 0.5 * ln((1 + 0.5) / (1 - 0.5)))

    assertThrows(IllegalArgumentException::class.java) { artanh.apply(1.0) }
    assertThrows(IllegalArgumentException::class.java) { artanh.apply(-1.0) }
  }

  @Test
  fun testArcoth() {
    val arcoth = evaluator.customFunctions.find { it.name == "arcoth" }!!

    assert(arcoth.apply(2.0) == 0.5 * ln((2.0 + 1) / (2.0 - 1)))

    assertThrows(IllegalArgumentException::class.java) { arcoth.apply(0.5) }
  }
}
