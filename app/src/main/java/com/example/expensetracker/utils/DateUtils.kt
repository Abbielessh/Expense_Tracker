package com.example.expensetracker.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatDate(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(millis)
}

/** Display-only format: dd/MM/yyyy (e.g. 18/05/2026). Internal storage still uses yyyy-MM-dd. */
fun formatDisplayDate(millis: Long): String {
    return SimpleDateFormat("dd/MM/yyyy", Locale.US).format(millis)
}

/** Convert a stored yyyy-MM-dd string to display format dd/MM/yyyy. */
fun formatDisplayDateStr(dateStr: String): String {
    return try {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: return dateStr
        SimpleDateFormat("dd/MM/yyyy", Locale.US).format(d)
    } catch (_: Exception) { dateStr }
}

fun parseDateStart(dateText: String): Long? {
    return try {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.isLenient = false
        val date = formatter.parse(dateText.trim()) ?: return null

        Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (_: Exception) {
        null
    }
}

fun parseDateEnd(dateText: String): Long? {
    val start = parseDateStart(dateText) ?: return null

    return Calendar.getInstance().apply {
        timeInMillis = start
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

fun todayStartMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun endOfDayMillis(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}

fun weekStartMillis(): Long {
    return Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun monthStartMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
