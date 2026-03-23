package com.sleepysoong.armydiet.testutil

import com.sleepysoong.armydiet.core.AppClock
import java.time.LocalDate
import java.time.LocalTime

class FakeAppClock(
    private var nowMillis: Long,
    private var currentDate: LocalDate,
    private var currentTime: LocalTime
) : AppClock {
    override fun nowMillis(): Long = nowMillis

    override fun currentDate(): LocalDate = currentDate

    override fun currentTime(): LocalTime = currentTime

    fun setNow(nowMillis: Long, currentDate: LocalDate, currentTime: LocalTime) {
        this.nowMillis = nowMillis
        this.currentDate = currentDate
        this.currentTime = currentTime
    }
}
