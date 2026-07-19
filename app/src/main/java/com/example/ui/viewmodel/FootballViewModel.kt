package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FootballRepository
import com.example.simulator.MatchSimulatorManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class FootballViewModel(application: Application) : AndroidViewModel(application) {
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            application.applicationContext,
            AppDatabase::class.java,
            "footdirect_db"
        ).build()
    }

    private val repository: FootballRepository by lazy {
        FootballRepository(application.applicationContext, database)
    }

    // Live streams from repository
    val allMatches: StateFlow<List<Match>> = repository.matches
    val standings: StateFlow<List<LeagueStanding>> = repository.standings
    val favorites: StateFlow<List<Favorite>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notificationLogs: StateFlow<List<NotificationLog>> = repository.notificationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Filter States ---
    private val _selectedDate = MutableStateFlow("Aujourd'hui") // "Hier", "Aujourd'hui", "Demain"
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedLeagueFilter = MutableStateFlow<String?>(null) // null means all leagues
    val selectedLeagueFilter: StateFlow<String?> = _selectedLeagueFilter.asStateFlow()

    // --- Active Selected Match State ---
    private val _selectedMatch = MutableStateFlow<Match?>(null)
    val selectedMatch: StateFlow<Match?> = _selectedMatch.asStateFlow()

    // --- AI Analyst State ---
    private val _aiAnalysisState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val aiAnalysisState: StateFlow<UiState<String>> = _aiAnalysisState.asStateFlow()

    private val _customQuestionState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val customQuestionState: StateFlow<UiState<String>> = _customQuestionState.asStateFlow()

    // --- Settings State ---
    private val _simulationSpeedSec = MutableStateFlow(5) // Default 5 seconds per simulated minute
    val simulationSpeedSec: StateFlow<Int> = _simulationSpeedSec.asStateFlow()

    private val _isSimulationRunning = MutableStateFlow(true)
    val isSimulationRunning: StateFlow<Boolean> = _isSimulationRunning.asStateFlow()

    init {
        // Initialize Match Simulator
        MatchSimulatorManager.initialize(application.applicationContext)
    }

    // Refresh selected match to pick up live ticker events
    fun selectMatch(match: Match?) {
        _selectedMatch.value = match
        if (match != null) {
            // Observe the live match updates dynamically to update details panel in real time
            viewModelScope.launch {
                allMatches.collect { list ->
                    val updated = list.find { it.id == match.id }
                    if (updated != null) {
                        _selectedMatch.value = updated
                    }
                }
            }
        }
        // Reset AI analysis states when changing match
        _aiAnalysisState.value = UiState.Idle
        _customQuestionState.value = UiState.Idle
    }

    fun setDateFilter(date: String) {
        _selectedDate.value = date
    }

    fun setLeagueFilter(league: String?) {
        _selectedLeagueFilter.value = league
    }

    fun toggleFavorite(id: String, type: String, name: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id, type, name)
        }
    }

    fun isFavoriteFlow(id: String): Flow<Boolean> {
        return repository.isFavoriteFlow(id)
    }

    // --- AI Analysis Operations ---
    fun runAiAnalysis(match: Match) {
        _aiAnalysisState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.getAiAnalysis(match)
                _aiAnalysisState.value = UiState.Success(result)
            } catch (e: Exception) {
                _aiAnalysisState.value = UiState.Error(e.localizedMessage ?: "Erreur d'analyse.")
            }
        }
    }

    fun askCustomQuestion(match: Match, question: String) {
        if (question.isBlank()) return
        _customQuestionState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.askCustomQuestion(match, question)
                _customQuestionState.value = UiState.Success(result)
            } catch (e: Exception) {
                _customQuestionState.value = UiState.Error(e.localizedMessage ?: "Erreur lors de l'envoi.")
            }
        }
    }

    // --- Notification History Operations ---
    fun markNotificationAsRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearNotificationHistory() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    // --- Simulator Settings Controls ---
    fun setSimulationSpeed(seconds: Int) {
        _simulationSpeedSec.value = seconds
        MatchSimulatorManager.simulationSpeedMs = seconds * 1000L
    }

    fun toggleSimulationState() {
        val next = !_isSimulationRunning.value
        _isSimulationRunning.value = next
        MatchSimulatorManager.isSimulationRunning = next
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FootballViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FootballViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
