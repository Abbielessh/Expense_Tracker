package com.example.expensetracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "BootReceiver"

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device rebooted — checking if reminder should be rescheduled")
            try {
                val (enabled, hour, minute) = ReminderScheduler.loadLocal(context)
                if (enabled) {
                    ReminderScheduler.scheduleReminder(context, hour, minute)
                    Log.d(TAG, "Rescheduled reminder at $hour:$minute after reboot")
                } else {
                    Log.d(TAG, "Reminder disabled, skipping reschedule")
                }
            } catch (e: Exception) {
                Log.e(TAG, "BootReceiver failed", e)
            }
        }
    }
}
