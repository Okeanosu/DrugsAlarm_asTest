package com.example.drugsalarm.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.drugsalarm.data.AppDatabase
import com.example.drugsalarm.data.IntakeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getIntExtra("MEDICINE_ID", -1)
        val medicineName = intent.getStringExtra("MEDICINE_NAME") ?: "Medicine"
        
        if (medicineId == -1) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(medicineId)

        if (intent.action == "ACTION_TAKE_MEDICINE") {
            val pendingResult = goAsync()
            val db = AppDatabase.getDatabase(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Decrement quantity
                    db.medicineDao().decrementQuantity(medicineId)
                    
                    // 2. Check for latest "Missed" log for this medicine and update it to "Taken"
                    val latestMissed = db.medicineDao().getLatestMissedLog(medicineId)
                    if (latestMissed != null) {
                        db.medicineDao().updateIntakeLog(latestMissed.copy(
                            status = "Taken",
                            intakeTime = System.currentTimeMillis()
                        ))
                    } else {
                        // Fallback: just insert a new Taken log
                        db.medicineDao().insertIntakeLog(IntakeLog(
                            medicineId = medicineId,
                            medicineName = medicineName,
                            intakeTime = System.currentTimeMillis(),
                            status = "Taken"
                        ))
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
