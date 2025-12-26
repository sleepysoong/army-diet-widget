package com.sleepysoong.armydiet.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DebugLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun log(tag: String, message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val newLog = "[$timestamp] $tag: $message"
        // 최신 로그가 위로 오게 추가
        _logs.value = listOf(newLog) + _logs.value.take(100) // 최대 100줄 유지
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
