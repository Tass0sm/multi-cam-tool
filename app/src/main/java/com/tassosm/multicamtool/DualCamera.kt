package com.tassosm.multicamtool

import android.os.Parcel
import android.os.Parcelable

/**
 * Helper class used to encapsulate a logical camera and two underlying
 * physical cameras
 */
data class DualCamera(val logicalId: String, val physicalId1: String, val physicalId2: String) :
    Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(logicalId)
        parcel.writeString(physicalId1)
        parcel.writeString(physicalId2)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DualCamera> {
        override fun createFromParcel(parcel: Parcel): DualCamera {
            return DualCamera(parcel)
        }

        override fun newArray(size: Int): Array<DualCamera?> {
            return arrayOfNulls(size)
        }
    }
}
