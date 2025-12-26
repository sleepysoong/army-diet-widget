package com.sleepysoong.armydiet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey 
    val date: String,
    val breakfast: String,
    val lunch: String,
    val dinner: String,
    val adspcfd: String,
    val sumCal: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object
}
