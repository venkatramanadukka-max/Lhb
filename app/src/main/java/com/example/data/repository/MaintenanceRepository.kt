package com.example.data.repository

import com.example.data.db.MaintenanceDao
import com.example.data.model.MaintenanceLog
import kotlinx.coroutines.flow.Flow

class MaintenanceRepository(private val maintenanceDao: MaintenanceDao) {
    val allLogs: Flow<List<MaintenanceLog>> = maintenanceDao.getAllLogs()

    suspend fun insertLog(log: MaintenanceLog) {
        maintenanceDao.insertLog(log)
    }

    suspend fun deleteLogById(id: Int) {
        maintenanceDao.deleteLogById(id)
    }

    suspend fun deleteAllLogs() {
        maintenanceDao.deleteAllLogs()
    }
}
