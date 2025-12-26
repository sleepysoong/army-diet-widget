package com.sleepysoong.armydiet.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey val date: String, // YYYYMMDD
    val breakfast: String, // brst
    val lunch: String,     // lunc
    val dinner: String,    // dinr
    val adspcfd: String,   // adspcfd
    val sumCal: String,    // sum_cal
    val updatedAt: Long = System.currentTimeMillis()
)