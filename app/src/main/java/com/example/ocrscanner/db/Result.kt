package com.example.ocrscanner.db

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import java.util.*

@Entity
@Parcelize
data class Result(
    @PrimaryKey
    var id: String = UUID.randomUUID().toString(),
    var time: String = "",
    var content: String = "",
    var pathUrl: String = ""
): Parcelable