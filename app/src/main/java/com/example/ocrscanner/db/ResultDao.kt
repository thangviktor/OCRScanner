package com.example.ocrscanner.db

import androidx.room.*
import io.reactivex.Observable

@Dao
interface ResultDao {

    @Query("SELECT * FROM Result ORDER BY time DESC")
    fun getAllResults(): Observable<List<Result>>

    @Query("SELECT * FROM Result WHERE id = :id")
    fun getById(id: String): Result

    @Update
    fun update(result: Result): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(result: Result): Long

    @Delete
    fun delete(result: Result)

    @Query("DELETE FROM Result WHERE id = :id")
    fun deleteById(id: String): Int

    @Query("DELETE FROM Result")
    fun deleteAllResults(): Int
}