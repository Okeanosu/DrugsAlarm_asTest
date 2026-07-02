package com.example.drugsalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.drugsalarm.data.AppDatabase
import com.example.drugsalarm.data.MedicineScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            val scheduler = MedicineScheduler(context)
            
            CoroutineScope(Dispatchers.IO).launch {
                val medicines = db.medicineDao().getAllMedicines()
                medicines.forEach { medicine ->
                    if (medicine.isEnabled && medicine.timeInMillis > System.currentTimeMillis()) {
                        scheduler.schedule(medicine)
                    }
                }
            }
        }
    }
}
