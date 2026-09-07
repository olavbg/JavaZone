package com.olavbg.javazone.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import java.time.LocalDate

class ArchiveViewModel(private val repository: SessionRepository) : ViewModel() {

    private val _sessionsMap = MutableStateFlow<Map<Int, List<Session>>>(emptyMap())
    
    private val _loadingMap = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    
    private val _showWorkshops = MutableStateFlow(true)
    val showWorkshops: StateFlow<Boolean> = _showWorkshops

    val currentYear = LocalDate.now().year
    val availableYears = (2014 until currentYear).reversed().toList()

    fun getSessionsForYear(year: Int): Flow<List<Session>> {
        return _sessionsMap.map { map ->
            val sessions = map[year] ?: emptyList()
            sessions.sortedBy { it.startTimeZulu }
        }.combine(_showWorkshops) { sessions, showWorkshops ->
            if (showWorkshops) {
                sessions
            } else {
                sessions.filter { !it.format.contains("workshop", ignoreCase = true) }
            }
        }
    }

    fun isLoading(year: Int): Flow<Boolean> {
        return _loadingMap.map { it[year] ?: false }
    }

    fun loadSessions(year: Int) {
        if (_sessionsMap.value.containsKey(year) && _loadingMap.value[year] != true) return
        
        viewModelScope.launch {
            _loadingMap.value = _loadingMap.value + (year to true)
            val sessions = repository.getArchiveSessions(year)
            _sessionsMap.value = _sessionsMap.value + (year to sessions)
            _loadingMap.value = _loadingMap.value + (year to false)
        }
    }

    fun toggleWorkshops() {
        _showWorkshops.value = !_showWorkshops.value
    }
}

class ArchiveViewModelFactory(private val repository: SessionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArchiveViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
