package com.example.drugsalarm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
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
                // 1. Ghi nhận là "Missed" ban đầu
                val missedLog = IntakeLog(
                    medicineId = it.id,
                    medicineName = it.name,
                    intakeTime = System.currentTimeMillis(),
                    status = "Missed"
                )
                db.medicineDao().insertIntakeLog(missedLog)

                // 2. Hiển thị thông báo kèm chuông báo thức
                showNotification(context, it.name, it.id)

                // 3. Đặt lịch cho lần kế tiếp
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
        
        // Lấy âm thanh báo thức mặc định của hệ thống
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders to take your medicine"
                enableVibration(true)
                // Thiết lập âm thanh cho Channel (Android 8.0+)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(alarmSound, audioAttributes)
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
            .setContentTitle("Đến giờ uống thuốc!")
            .setContentText("Bạn cần uống: $medicineName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound) // Thiết lập âm thanh cho các bản Android cũ hơn
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setFullScreenIntent(mainPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_edit, "Đã uống", takePendingIntent)

        notificationManager.notify(medicineId, builder.build())
    }
}
