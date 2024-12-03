package com.github.se.eduverse.ui.quizz

import org.junit.Assert.assertEquals
import org.junit.Test

class CountIndexedTest {

  @Test
  fun testCountIndexed() {
    val list = listOf("a", "b", "c", "d", "e")
    val count =
        list.countIndexed { index, element -> index % 2 == 0 && element in listOf("a", "e") }
    assertEquals(2, count)
  }
}
