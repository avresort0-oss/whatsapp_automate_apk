package com.example

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutomationLogManager {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    data class LogEntry(
        val timestamp: String,
        val message: String,
        val type: LogType = LogType.INFO
    )

    enum class LogType {
        INFO, SUCCESS, WARNING, ERROR
    }

    fun log(message: String, type: LogType = LogType.INFO) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newEntry = LogEntry(timestamp, message, type)
        _logs.value = (listOf(newEntry) + _logs.value).take(100) // Keep last 100 logs
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
