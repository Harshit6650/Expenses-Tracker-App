package com.example.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.example.ACTION_NOTIFICATION_REMINDER") {
            // Trigger randomized spendings notification
            NotificationHelper.showReminderNotification(context)
            
            // Re-schedule future reminders to keep the continuous everyday loop active
            NotificationHelper.scheduleRandomReminders(context)
        } else if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule reminds upon system startup
            NotificationHelper.scheduleRandomReminders(context)
        }
    }
}
