package com.github.se.eduverse.model

import android.os.Parcel
import android.os.Parcelable
import java.util.Calendar

data class MyFile(
    val id: String, // The id of the MyFile in the folder
    val fileId: String, // The id of the file it reflects
    var name: String,
    val creationTime: Calendar,
    var lastAccess: Calendar,
    var numberAccess: Int
) : Parcelable {

  // Parcelable implementation
  constructor(
      parcel: Parcel
  ) : this(
      id = parcel.readString() ?: "",
      fileId = parcel.readString() ?: "",
      name = parcel.readString() ?: "",
      creationTime = Calendar.getInstance().apply { timeInMillis = parcel.readLong() },
      lastAccess = Calendar.getInstance().apply { timeInMillis = parcel.readLong() },
      numberAccess = parcel.readInt())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(id)
    parcel.writeString(fileId)
    parcel.writeString(name)
    parcel.writeLong(creationTime.timeInMillis)
    parcel.writeLong(lastAccess.timeInMillis)
    parcel.writeInt(numberAccess)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<MyFile> {
    override fun createFromParcel(parcel: Parcel): MyFile {
      return MyFile(parcel)
    }

    override fun newArray(size: Int): Array<MyFile?> {
      return arrayOfNulls(size)
    }
  }
}
