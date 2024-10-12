package com.github.se.project.model.folder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimeTableTest {

  @Test
  fun accessOutOfRange() {
    val timeTable = TimeTable()
    assertThrows(IllegalArgumentException::class.java) { timeTable.isSelected(1, 6) }
    assertThrows(IllegalArgumentException::class.java) { timeTable.isSelected(1, 21) }
    assertThrows(IllegalArgumentException::class.java) { timeTable.isSelected(0, 9) }
    assertThrows(IllegalArgumentException::class.java) { timeTable.isSelected(8, 9) }
  }

  @Test
  fun onCreationAllFalse() {
    val timeTable = TimeTable()
    for (i in 1..7) {
      for (j in 7..20) {
        assertEquals(timeTable.isSelected(i, j), false)
      }
    }
  }

  @Test
  fun selectSlotTest() {
    val timeTable = TimeTable()
    for (i in 1..7) {
      for (j in 7..20) {
        timeTable.selectSlot(i, j)
        assertEquals(timeTable.isSelected(i, j), true)
      }
    }
    timeTable.selectSlot(2, 12)
    assertEquals(timeTable.isSelected(2, 12), true)
  }

  @Test
  fun unselectSlotTest() {
    val timeTable = TimeTable()

    timeTable.unselectSlot(2, 12)
    assertEquals(timeTable.isSelected(2, 12), false)
    for (i in 1..7) {
      for (j in 7..20) {
        timeTable.selectSlot(i, j)
      }
    }
    assertEquals(timeTable.isSelected(2, 12), true)
    timeTable.unselectSlot(2, 12)
    assertEquals(timeTable.isSelected(2, 12), false)

    // still hold if already false
    timeTable.unselectSlot(2, 12)
    assertEquals(timeTable.isSelected(2, 12), false)
  }
}
