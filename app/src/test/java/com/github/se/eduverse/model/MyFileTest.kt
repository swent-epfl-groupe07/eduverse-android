package com.github.se.eduverse.model

import android.os.Parcel
import java.util.Calendar
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class MyFileTest {
  private lateinit var file: MyFile
  private lateinit var parcel: Parcel

  @Before
  fun setUp() {
    file = MyFile("id", "fileId", "name", Calendar.getInstance(), Calendar.getInstance(), 0)
    parcel = mock(Parcel::class.java)
  }

  @Test
  fun constructorTest() {
    var cmpt = 0
    `when`(parcel.readString()).then {
      cmpt += 1
      when (cmpt) {
        1 -> "id"
        2 -> "fileId"
        else -> "name"
      }
    }
    `when`(parcel.readLong()).thenReturn(0)
    `when`(parcel.readInt()).thenReturn(0)

    val defaultFile = MyFile.CREATOR.createFromParcel(parcel)

    assertEquals(defaultFile.id, "id")
    assertEquals(defaultFile.fileId, "fileId")
    assertEquals(defaultFile.name, "name")
    assertEquals(defaultFile.creationTime, Calendar.getInstance().apply { timeInMillis = 0 })
    assertEquals(defaultFile.lastAccess, Calendar.getInstance().apply { timeInMillis = 0 })
    assertEquals(defaultFile.numberAccess, 0)

    assertArrayEquals(MyFile.CREATOR.newArray(3), arrayOf(null, null, null))
  }

  @Test
  fun writeToParcelTest() {
    var test = 0
    `when`(parcel.writeString(any())).then {
      test += 1
      null
    }
    `when`(parcel.writeLong(any())).then {
      test += 1
      null
    }
    `when`(parcel.writeInt(any())).then {
      test += 1
      null
    }

    file.writeToParcel(parcel, 0)
    assertEquals(test, 6)
  }

  @Test
  fun describeContentsTest() {
    assertEquals(file.describeContents(), 0)
  }
}
