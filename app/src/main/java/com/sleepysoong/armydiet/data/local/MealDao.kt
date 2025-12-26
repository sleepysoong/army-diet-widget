package com.sleepysoong.armydiet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date")
    suspend fun getMeal(date: String): MealEntity?

    @Query("SELECT * FROM meals WHERE date IN (:dates)")
    suspend fun getMealsByDates(dates: List<String>): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)
}
