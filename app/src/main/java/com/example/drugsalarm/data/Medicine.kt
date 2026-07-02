package com.example.drugsalarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val timeInMillis: Long,
    val totalQuantity: Int,
    val remainingQuantity: Int,
    val isEnabled: Boolean = true
)
