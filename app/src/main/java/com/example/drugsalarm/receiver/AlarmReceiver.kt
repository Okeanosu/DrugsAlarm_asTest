package com.example.drugsalarm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.drugsalarm.MainActivity
import com.example.drugsalarm.data.AppDatabase
import com.example.drugsalarm.data.IntakeLog
import com.example.drugsalarm.data.MedicineScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra("MEDICINE_ID", -1)
        if (medicineId == -1) return

        val db = AppDatabase.getDatabase(context)
        val scheduler = MedicineScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val medicine = db.medicineDao().getMedicineById(medicineId)
            medicine?.let {
                // 1. Record as "Missed" initially
                val missedLog = IntakeLog(
                    medicineId = it.id,
                    medicineName = it.name,
                    intakeTime = System.currentTimeMillis(),
                    status = "Missed"
                )
                db.medicineDao().insertIntakeLog(missedLog)

                // 2. Show notification
                showNotification(context, it.name, it.id)

                // 3. Reschedule for the next occurrence if it's a repeating medication
                if (it.frequency != "Once") {
                    val nextTime = scheduler.calculateNextOccurrence(it.timeInMillis, it.frequency)
                    val updatedMedicine = it.copy(timeInMillis = nextTime)
                    db.medicineDao().updateMedicine(updatedMedicine)
                    scheduler.schedule(updatedMedicine)
                }
            }
        }
    }

    private fun showNotification(context: Context, medicineName: String, medicineId: Int) {
        val channelId = "medicine_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take your medicine"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_TAKE_MEDICINE"
            putExtra("MEDICINE_ID", medicineId)
            putExtra("MEDICINE_NAME", medicineName)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            medicineId * 2,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            medicineId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Medicine Reminder")
            .setContentText("Time to take your medication: $medicineName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setFullScreenIntent(mainPendingIntent, true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .addAction(android.R.drawable.ic_menu_edit, "Taken", takePendingIntent)

        notificationManager.notify(medicineId, builder.build())
    }
}
