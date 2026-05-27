package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar
import kotlin.random.Random

object NotificationHelper {
    const val REMINDER_CHANNEL_ID = "expense_reminders"
    const val BUDGET_CHANNEL_ID = "budget_alerts"
    
    private val REMINDER_MESSAGES = listOf(
        "Don't forget to record your coffee or commute today! ☕🚌",
        "Quick check! Have you tracked your recent expenses? 📝",
        "A wise budget starts with tracking. Tap to log your transactions. 💰",
        "Keep your finances safe: review and log your spendings now! 🔒",
        "Spot something you bought today? Let's add it in WiseWallet! 💳",
        "Spent some money recently? Record it quickly to stay on budget! 📈"
    )

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Reminder channel
            val reminderChannel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Intermittent reminders to track your daily spendings"
            }
            
            // Budget channel
            val budgetChannel = NotificationChannel(
                BUDGET_CHANNEL_ID,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts when monthly spending budget is reached or exceeded"
            }
            
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(budgetChannel)
        }
    }

    fun scheduleRandomReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Define 3 windows (Hour ranges) for daily reminders (2 to 3 times random everyday)
        val windows = listOf(
            9 to 12,   // Morning reminder slot
            14 to 17,  // Afternoon reminder slot
            19 to 21   // Evening reminder slot
        )
        
        windows.forEachIndexed { index, (startHour, endHour) ->
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            
            // Generate random hour and minute in this window
            val randomHour = Random.nextInt(startHour, endHour + 1)
            val randomMinute = Random.nextInt(0, 60)
            
            calendar.set(Calendar.HOUR_OF_DAY, randomHour)
            calendar.set(Calendar.MINUTE, randomMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            // If the calculated time is already passed today, schedule it for tomorrow
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val intent = Intent(context, NotificationReminderReceiver::class.java).apply {
                action = "com.example.ACTION_NOTIFICATION_REMINDER"
                putExtra("reminder_slot_index", index)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000 + index, // Unique Request Code for each slot
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (e: Throwable) {
                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (fallbackEx: Throwable) {
                    fallbackEx.printStackTrace()
                }
            }
        }
    }

    fun showReminderNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val randomMessage = REMINDER_MESSAGES.random()
            
            val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("WiseWallet Reminder")
                .setContentText(randomMessage)
                .setStyle(NotificationCompat.BigTextStyle().bigText(randomMessage))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                
            notificationManager.notify(2001, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun showBudgetOverrunNotification(context: Context, budget: Double, currentSpent: Double) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val formattedLimit = String.format("%.2f", budget)
            val formattedSpent = String.format("%.2f", currentSpent)
            val title = "⚠️ Monthly Budget Exceeded!"
            val text = "You have spent ₹$formattedSpent, exceeding your monthly budget limit of ₹$formattedLimit!"
            
            val builder = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                
            notificationManager.notify(2501, builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun showSmsParsedNotification(context: Context, description: String, amount: Double) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val formattedAmount = String.format("%.2f", amount)
            val title = "💳 SMS Expense Auto-Added"
            val text = "Successfully parsed and tracked expense of ₹$formattedAmount for \"$description\" from SMS."
            
            val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_save)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                
            notificationManager.notify(3001 + Random.nextInt(1000), builder.build())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
