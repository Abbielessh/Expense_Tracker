package com.example.expensetracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.expensetracker.MainActivity
import com.example.expensetracker.R

private const val TAG = "ReminderReceiver"
const val CHANNEL_ID = "expense_daily_reminder"
const val CHANNEL_NAME = "Daily Expense Reminder"
const val NOTIFICATION_ID = 2001

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: showing daily reminder notification")
        try {
            showReminderNotification(context)
            // Reschedule for next day so the alarm keeps firing daily
            val (enabled, hour, minute) = ReminderScheduler.loadLocal(context)
            if (enabled) {
                ReminderScheduler.scheduleReminder(context, hour, minute)
                Log.d(TAG, "Rescheduled next reminder at $hour:$minute")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show/reschedule notification", e)
        }
    }

    companion object {
        fun showReminderNotification(context: Context) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as? NotificationManager ?: return

                // Create channel (safe to call multiple times on Android 8+)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily reminder to record your expenses"
                }
                notificationManager.createNotificationChannel(channel)

                // Tap action: open MainActivity
                val tapIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    tapIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_nav_home)
                    .setContentTitle("Add today's expenses")
                    .setContentText("Don't forget to record your spending for today.")
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
                Log.d(TAG, "Notification shown")
            } catch (e: Exception) {
                Log.e(TAG, "showReminderNotification failed", e)
            }
        }
    }
}
