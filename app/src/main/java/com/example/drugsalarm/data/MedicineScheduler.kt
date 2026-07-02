package com.example.drugsalarm.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.drugsalarm.receiver.AlarmReceiver
import java.util.Calendar

class MedicineScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(medicine: Medicine) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MEDICINE_ID", medicine.id)
            putExtra("MEDICINE_NAME", medicine.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ensure we are scheduling in the future
        var triggerTime = medicine.timeInMillis
        if (triggerTime <= System.currentTimeMillis()) {
            triggerTime = calculateNextOccurrence(triggerTime, medicine.frequency)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Fallback to inexact alarm if permission is not granted
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Use setExactAndAllowWhileIdle for critical medicine reminders
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Final fallback to avoid crash
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun calculateNextOccurrence(currentTime: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
        val now = Calendar.getInstance()

        // Loop to find the next valid time in the future
        while (calendar.before(now) || calendar.timeInMillis <= now.timeInMillis) {
            when (frequency) {
                "Every 1 Hour" -> calendar.add(Calendar.HOUR_OF_DAY, 1)
                "Every 2 Hours" -> calendar.add(Calendar.HOUR_OF_DAY, 2)
                "Every 4 Hours" -> calendar.add(Calendar.HOUR_OF_DAY, 4)
                "Every 8 Hours" -> calendar.add(Calendar.HOUR_OF_DAY, 8)
                "Every 12 Hours" -> calendar.add(Calendar.HOUR_OF_DAY, 12)
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Every 2 Weeks" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                else -> return currentTime // "Once" or unknown doesn't repeat
            }
        }
        return calendar.timeInMillis
    }

    fun cancel(medicineId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
