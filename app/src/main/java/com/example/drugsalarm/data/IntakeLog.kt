package com.example.drugsalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intake_logs")
data class IntakeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicineId: Int,
    val medicineName: String,
    val intakeTime: Long,
    val status: String // "Taken", "Snoozed", "Missed"
)
