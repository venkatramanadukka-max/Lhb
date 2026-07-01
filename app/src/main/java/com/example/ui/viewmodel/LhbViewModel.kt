package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.MaintenanceLog
import com.example.data.remote.GeminiService
import com.example.data.repository.MaintenanceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiChatState {
    object Idle : AiChatState
    object Loading : AiChatState
    data class Success(val response: String) : AiChatState
    data class Error(val message: String) : AiChatState
}

data class ChatMessage(
    val sender: String, // "technician" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

class LhbViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = MaintenanceRepository(database.maintenanceDao())

    // Expose local database logs
    val logsState: StateFlow<List<MaintenanceLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // We will make sure repository has allLogs flow
    }

    // AI Chat States
    var chatMessages by mutableStateOf<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "ai",
                text = "Hello! I am your LHB Bogie Technical Assistant. Ask me any questions about FIAT bogie tolerances, maintenance schedules, or troubleshooting procedures."
            )
        )
    )
        private set

    var aiChatState by mutableStateOf<AiChatState>(AiChatState.Idle)
        private set

    var currentChatInput by mutableStateOf("")

    // Active Checklist State for New Log
    var newCoachNumber by mutableStateOf("")
    var newScheduleType by mutableStateOf("D1")
    var newTechnicianName by mutableStateOf("")
    var newNotes by mutableStateOf("")
    var checkBrakePads by mutableStateOf(true)
    var checkBrakeDiscs by mutableStateOf(true)
    var checkCylinderDampers by mutableStateOf(true)
    var checkCtrbTemp by mutableStateOf(true)
    var checkAirSpringPressure by mutableStateOf(true)
    var checkPhonicWheelSensor by mutableStateOf(true)
    var checkTractionRod by mutableStateOf(true)

    fun resetNewLogFields() {
        newCoachNumber = ""
        newScheduleType = "D1"
        newTechnicianName = ""
        newNotes = ""
        checkBrakePads = true
        checkBrakeDiscs = true
        checkCylinderDampers = true
        checkCtrbTemp = true
        checkAirSpringPressure = true
        checkPhonicWheelSensor = true
        checkTractionRod = true
    }

    fun submitMaintenanceLog(onComplete: () -> Unit) {
        if (newCoachNumber.isBlank() || newTechnicianName.isBlank()) return

        viewModelScope.launch {
            val log = MaintenanceLog(
                coachNumber = newCoachNumber.trim().uppercase(),
                scheduleType = newScheduleType,
                technicianName = newTechnicianName.trim(),
                notes = newNotes.trim(),
                isBrakePadsOk = checkBrakePads,
                isBrakeDiscsOk = checkBrakeDiscs,
                isCylinderDampersOk = checkCylinderDampers,
                isCtrbTemperatureOk = checkCtrbTemp,
                isAirSpringPressureOk = checkAirSpringPressure,
                isPhonicWheelSensorOk = checkPhonicWheelSensor,
                isTractionRodOk = checkTractionRod
            )
            repository.insertLog(log)
            resetNewLogFields()
            onComplete()
        }
    }

    fun deleteLog(logId: Int) {
        viewModelScope.launch {
            repository.deleteLogById(logId)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.deleteAllLogs()
        }
    }

    fun sendAiQuestion() {
        val prompt = currentChatInput.trim()
        if (prompt.isEmpty()) return

        // Append user message
        chatMessages = chatMessages + ChatMessage(sender = "technician", text = prompt)
        currentChatInput = ""
        aiChatState = AiChatState.Loading

        viewModelScope.launch {
            val response = GeminiService.askAssistant(prompt)
            if (response.startsWith("Error:") || response.startsWith("API Error:")) {
                aiChatState = AiChatState.Error(response)
                chatMessages = chatMessages + ChatMessage(sender = "ai", text = "Sorry, I encountered an issue. $response")
            } else {
                aiChatState = AiChatState.Success(response)
                chatMessages = chatMessages + ChatMessage(sender = "ai", text = response)
            }
        }
    }
}
