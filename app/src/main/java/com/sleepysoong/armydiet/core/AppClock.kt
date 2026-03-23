package com.sleepysoong.armydiet.core

import java.time.LocalDate
import java.time.LocalTime

interface AppClock {
    fun nowMillis(): Long
    fun currentDate(): LocalDate
    fun currentTime(): LocalTime
}

object SystemAppClock : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun currentDate(): LocalDate = LocalDate.now()

    override fun currentTime(): LocalTime = LocalTime.now()
}
