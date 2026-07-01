package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_logs")
data class MaintenanceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coachNumber: String,
    val scheduleType: String, // D1, D2, D3, SS1, etc.
    val technicianName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String,
    // Checklist items stored as serialized JSON strings or booleans
    val isBrakePadsOk: Boolean,
    val isBrakeDiscsOk: Boolean,
    val isCylinderDampersOk: Boolean,
    val isCtrbTemperatureOk: Boolean,
    val isAirSpringPressureOk: Boolean,
    val isPhonicWheelSensorOk: Boolean,
    val isTractionRodOk: Boolean
)
