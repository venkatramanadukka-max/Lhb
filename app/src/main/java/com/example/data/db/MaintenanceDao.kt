package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.MaintenanceLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceDao {
    @Query("SELECT * FROM maintenance_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MaintenanceLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MaintenanceLog)

    @Query("DELETE FROM maintenance_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
    
    @Query("DELETE FROM maintenance_logs")
    suspend fun deleteAllLogs()
}
