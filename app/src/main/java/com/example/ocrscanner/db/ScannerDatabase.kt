package com.example.ocrscanner.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Result::class], version = 3)
abstract class ScannerDatabase : RoomDatabase() {

    abstract fun resultDao(): ResultDao

    companion object {
        private var INSTANCE: ScannerDatabase ?= null

        @JvmStatic
        fun getInstance(context: Context): ScannerDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(context, ScannerDatabase::class.java, "scanner.db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return INSTANCE!!
        }
    }
}