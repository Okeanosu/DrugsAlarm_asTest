package com.example.drugsalarm.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

data class DailyStats(
    val date: Long,
    val takenCount: Int,
    val missedCount: Int
)

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).medicineDao()
    private val scheduler = MedicineScheduler(application)
    
    val allMedicines: Flow<List<Medicine>> = dao.getAllMedicinesFlow()
    val allLogs: Flow<List<IntakeLog>> = dao.getAllLogs()

    val weeklyStats: Flow<List<DailyStats>> = allLogs.map { logs ->
        val statsMap = mutableMapOf<Long, Pair<Int, Int>>()
        val cal = Calendar.getInstance()
        
        // Initialize last 7 days
        for (i in 0..6) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            dateCal.set(Calendar.HOUR_OF_DAY, 0)
            dateCal.set(Calendar.MINUTE, 0)
            dateCal.set(Calendar.SECOND, 0)
            dateCal.set(Calendar.MILLISECOND, 0)
            statsMap[dateCal.timeInMillis] = Pair(0, 0)
        }

        logs.forEach { log ->
            cal.timeInMillis = log.intakeTime
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            
            if (statsMap.containsKey(dayStart)) {
                val current = statsMap[dayStart]!!
                if (log.status == "Taken") {
                    statsMap[dayStart] = Pair(current.first + 1, current.second)
                } else if (log.status == "Missed") {
                    statsMap[dayStart] = Pair(current.first, current.second + 1)
                }
            }
        }

        statsMap.entries.sortedBy { it.key }.map { 
            DailyStats(it.key, it.value.first, it.value.second)
        }
    }

    fun addMedicine(name: String, dosage: String, frequency: String, timeInMillis: Long, quantity: Int) {
        viewModelScope.launch {
            val medicine = Medicine(
                name = name,
                dosage = dosage,
                frequency = frequency,
                timeInMillis = timeInMillis,
                totalQuantity = quantity,
                remainingQuantity = quantity
            )
            val id = dao.insertMedicine(medicine).toInt()
            scheduler.schedule(medicine.copy(id = id))
        }
    }

    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            dao.updateMedicine(medicine)
            scheduler.cancel(medicine.id)
            scheduler.schedule(medicine)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            dao.deleteMedicine(medicine)
            scheduler.cancel(medicine.id)
        }
    }

    fun takeMedicine(medicine: Medicine) {
        viewModelScope.launch {
            if (medicine.remainingQuantity > 0) {
                dao.decrementQuantity(medicine.id)
                
                val latestMissed = dao.getLatestMissedLog(medicine.id)
                if (latestMissed != null) {
                    dao.updateIntakeLog(latestMissed.copy(
                        status = "Taken",
                        intakeTime = System.currentTimeMillis()
                    ))
                } else {
                    dao.insertIntakeLog(IntakeLog(
                        medicineId = medicine.id,
                        medicineName = medicine.name,
                        intakeTime = System.currentTimeMillis(),
                        status = "Taken"
                    ))
                }
            }
        }
    }

    fun deleteLog(log: IntakeLog) {
        viewModelScope.launch {
            dao.deleteIntakeLog(log)
        }
    }

    fun updateLog(log: IntakeLog) {
        viewModelScope.launch {
            dao.updateIntakeLog(log)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.deleteAllLogs()
            dao.resetIntakeLogId()
        }
    }

    fun clearAllMedicines() {
        viewModelScope.launch {
            val medicines = dao.getAllMedicines()
            medicines.forEach { scheduler.cancel(it.id) }
            dao.deleteAllMedicines()
            dao.resetMedicineId()
        }
    }
}
