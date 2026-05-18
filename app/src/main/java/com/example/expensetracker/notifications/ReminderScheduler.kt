package com.example.expensetracker.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Calendar

private const val TAG = "ReminderScheduler"
const val REMINDER_PREFS = "reminder_prefs"
const val PREF_ENABLED = "daily_reminder_enabled"
const val PREF_HOUR = "reminder_hour"
const val PREF_MINUTE = "reminder_minute"
const val ALARM_REQUEST_CODE = 1001

object ReminderScheduler {

    fun saveLocally(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE).edit().apply {
            putBoolean(PREF_ENABLED, enabled)
            putInt(PREF_HOUR, hour)
            putInt(PREF_MINUTE, minute)
            apply()
        }
        Log.d(TAG, "Saved locally: enabled=$enabled hour=$hour minute=$minute")
    }

    fun loadLocal(context: Context): Triple<Boolean, Int, Int> {
        val prefs = context.getSharedPreferences(REMINDER_PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(PREF_ENABLED, false)
        val hour = prefs.getInt(PREF_HOUR, 21)
        val minute = prefs.getInt(PREF_MINUTE, 0)
        return Triple(enabled, hour, minute)
    }

    fun scheduleReminder(context: Context, hour: Int, minute: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager unavailable")
                return
            }

            // Check notification permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted, skipping schedule")
                    return
                }
            }

            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // If the time is already past today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Reminder scheduled at $hour:$minute, first fire: ${cal.time}")
        } catch (e: Exception) {
            Log.e(TAG, "scheduleReminder failed", e)
        }
    }

    fun cancelReminder(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
            Log.d(TAG, "Reminder cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "cancelReminder failed", e)
        }
    }
}
