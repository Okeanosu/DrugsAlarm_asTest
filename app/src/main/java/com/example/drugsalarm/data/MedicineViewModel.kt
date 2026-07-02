package com.example.drugsalarm.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MedicineViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).medicineDao()
    private val scheduler = MedicineScheduler(application)
    
    val allMedicines: Flow<List<Medicine>> = dao.getAllMedicinesFlow()
    val allLogs: Flow<List<IntakeLog>> = dao.getAllLogs()

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
                
                // Check if there's a recent "Missed" entry to convert
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
