package com.example.drugsalarm.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines")
    fun getAllMedicinesFlow(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines")
    suspend fun getAllMedicines(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: Int): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: Medicine): Long

    @Update
    suspend fun updateMedicine(medicine: Medicine)

    @Delete
    suspend fun deleteMedicine(medicine: Medicine)

    @Query("UPDATE medicines SET remainingQuantity = remainingQuantity - 1 WHERE id = :id AND remainingQuantity > 0")
    suspend fun decrementQuantity(id: Int)

    @Insert
    suspend fun insertIntakeLog(log: IntakeLog)

    @Update
    suspend fun updateIntakeLog(log: IntakeLog)

    @Delete
    suspend fun deleteIntakeLog(log: IntakeLog)

    @Query("SELECT * FROM intake_logs ORDER BY intakeTime DESC")
    fun getAllLogs(): Flow<List<IntakeLog>>

    @Query("DELETE FROM intake_logs")
    suspend fun deleteAllLogs()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'intake_logs'")
    suspend fun resetIntakeLogId()

    @Query("DELETE FROM medicines")
    suspend fun deleteAllMedicines()

    @Query("DELETE FROM sqlite_sequence WHERE name = 'medicines'")
    suspend fun resetMedicineId()

    @Query("SELECT * FROM intake_logs WHERE medicineId = :medicineId AND status = 'Missed' ORDER BY intakeTime DESC LIMIT 1")
    suspend fun getLatestMissedLog(medicineId: Int): IntakeLog?
}
