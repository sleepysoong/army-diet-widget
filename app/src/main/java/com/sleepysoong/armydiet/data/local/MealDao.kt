package com.sleepysoong.armydiet.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date")
    suspend fun getMeal(date: String): MealEntity?

    @Query("SELECT * FROM meals WHERE date IN (:dates)")
    suspend fun getMealsByDates(dates: List<String>): List<MealEntity>
    
    @Query("SELECT * FROM meals WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMealsInRange(startDate: String, endDate: String): List<MealEntity>
    
    @Query("SELECT * FROM meals")
    fun getAllMealsFlow(): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)
}
