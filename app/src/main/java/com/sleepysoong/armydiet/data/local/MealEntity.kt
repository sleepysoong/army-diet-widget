package com.sleepysoong.armydiet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val date: String, // YYYYMMDD
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val updatedAt: String = LocalDateTime.now().toString()
)
