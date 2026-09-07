package com.olavbg.javazone.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.Session
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimelineViewModel(
    private val repository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _selectedDay = MutableStateFlow<String?>(null)
    val selectedDay = _selectedDay.asStateFlow()

    private val _onlyFavorites = MutableStateFlow(value = false)
    val onlyFavorites = _onlyFavorites.asStateFlow()

    private val _selectedFormat = MutableStateFlow<String?>(null)
    val selectedFormat = _selectedFormat.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage = _selectedLanguage.asStateFlow()

    private val _selectedRoom = MutableStateFlow<String?>(null)
    val selectedRoom = _selectedRoom.asStateFlow()

    private val _filterSpeaker = MutableStateFlow<String?>(null)
    val filterSpeaker = _filterSpeaker.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentTime = MutableStateFlow(Instant.now())
    val currentTime: StateFlow<Instant> = _currentTime
        .combine(settingsRepository.simulatedTimeOffset) { time, offset ->
            time.plusMillis(offset)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Instant.now())

    val allSessions: StateFlow<List<Session>> = repository.getSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sessions filtered by selected day only (used for calculating dynamic filter options)
    val daySessions: StateFlow<List<Session>> = combine(allSessions, _selectedDay) { list, day ->
        if (day == null) list else list.filter { getDayFromZulu(it.startTimeZulu) == day }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableFormats: StateFlow<List<String>> = daySessions.map { list ->
        val result = mutableListOf<String>()
        if (list.any { isFormatMatch(it.format, "Presentation") }) result.add("Presentation")
        if (list.any { isFormatMatch(it.format, "Lightning Talk") }) result.add("Lightning Talk")
        if (list.any { isFormatMatch(it.format, "Workshop") }) result.add("Workshop")
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableLanguages: StateFlow<List<String>> = daySessions.map { list ->
        list.asSequence().mapNotNull { it.language }.distinct().sorted().toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableRooms: StateFlow<List<String>> = daySessions.map { list ->
        list.asSequence()
            .map { it.room }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareBy({ extractRoomNumber(it) }, { it }))
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<Session>> = combine(
        allSessions,
        _selectedDay,
        _onlyFavorites,
        _selectedFormat,
        _selectedLanguage,
        _filterSpeaker,
        _selectedRoom,
        _searchQuery
    ) { args: Array<Any?> ->
        val list = args[0] as List<Session>
        val day = args[1] as String?
        val favoritesOnly = args[2] as Boolean
        val format = args[3] as String?
        val language = args[4] as String?
        val speaker = args[5] as String?
        val room = args[6] as String?
        val query = args[7] as String

        var filtered = list
        if (day != null && speaker == null) { // Don't filter by day if looking for a specific speaker's all talks
            filtered = filtered.filter { getDayFromZulu(it.startTimeZulu) == day }
        }
        if (favoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }
        if (format != null) {
            filtered = filtered.filter { isFormatMatch(it.format, format) }
        }
        if (language != null) {
            filtered = filtered.filter { it.language?.equals(language, ignoreCase = true) == true }
        }
        if (speaker != null) {
            filtered = filtered.filter { it.speakers.any { s -> s.name.equals(speaker, ignoreCase = true) } }
        }
        if (room != null) {
            filtered = filtered.filter { it.room.equals(room, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filtered = filtered.filter { session ->
                session.title.lowercase().contains(q) ||
                session.room.lowercase().contains(q) ||
                session.speakers.any { it.name.lowercase().contains(q) } ||
                session.abstract.lowercase().contains(q)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedSessions: StateFlow<Map<String, List<Session>>> = sessions.map { list ->
        list.sortedWith(
            compareBy<Session> { it.startTimeZulu }
                .thenBy { extractRoomNumber(it.room) }
                .thenBy { it.room },
        ).groupBy { formatTime(it.startTimeZulu) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _isLoading = MutableStateFlow(value = true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var hasAutoSelectedDay = false
    private var hasScrolledForDay = false

    init {
        viewModelScope.launch {
            try {
                repository.refreshSessions()
            } finally {
                _isLoading.value = false
            }
        }
        // Auto-select current day matching effective currentTime (including simulated time settings)
        viewModelScope.launch {
            combine(allSessions, currentTime) { list, time -> list to time }
                .collect { (list, time) ->
                    if (!hasAutoSelectedDay && list.isNotEmpty()) {
                        val currentDayName = try {
                            time.atZone(ZoneId.of("Europe/Oslo")).format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.ENGLISH))
                        } catch (_: Exception) { "" }

                        val matchingDay = list.map { getDayFromZulu(it.startTimeZulu) }
                            .firstOrNull { it.equals(currentDayName, ignoreCase = true) }

                        if (matchingDay != null) {
                            _selectedDay.value = matchingDay
                            hasAutoSelectedDay = true
                        }
                    }
                }
        }
        // Update current time every minute
        viewModelScope.launch {
            while (true) {
                _currentTime.value = Instant.now()
                delay(Duration.ofMinutes(1).toMillis())
            }
        }
    }

    fun setDay(day: String?) {
        _selectedDay.value = day
        hasScrolledForDay = false
        // Clear format/room selection if no longer available in the new day
        if (_selectedFormat.value != null && !(availableFormats.value.contains(_selectedFormat.value))) {
            _selectedFormat.value = null
        }
        if (_selectedRoom.value != null && !availableRooms.value.contains(_selectedRoom.value)) {
            _selectedRoom.value = null
        }
    }

    fun shouldScrollToNow(): Boolean {
        return !hasScrolledForDay
    }

    fun markScrolledToNow() {
        hasScrolledForDay = true
    }

    fun setOnlyFavorites(only: Boolean) {
        _onlyFavorites.value = only
    }

    fun setFormat(format: String?) {
        _selectedFormat.value = format
    }

    fun setLanguage(language: String?) {
        _selectedLanguage.value = language
    }

    fun setRoom(room: String?) {
        _selectedRoom.value = room
    }

    fun setFilterSpeaker(speaker: String?) {
        _filterSpeaker.value = speaker
        if (speaker != null) {
            _selectedDay.value = null // Show all days for the speaker
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(session: Session) {
        viewModelScope.launch {
            repository.toggleFavorite(session.id, !session.isFavorite)
        }
    }

    private fun formatTime(zulu: String): String {
        return try {
            val instant = Instant.parse(zulu)
            val dateTime = instant.atZone(ZoneId.of("Europe/Oslo"))
            dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e: Exception) {
            ""
        }
    }

    private fun getDayFromZulu(zulu: String): String {
        return try {
            val instant = Instant.parse(zulu)
            val dateTime = instant.atZone(ZoneId.of("Europe/Oslo"))
            dateTime.format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.ENGLISH))
        } catch (e: Exception) {
            ""
        }
    }

    private fun isSessionActive(session: Session, currentTime: Instant): Boolean {
        return try {
            val start = Instant.parse(session.startTimeZulu)
            val end = Instant.parse(session.endTimeZulu)
            (currentTime.isAfter(start) || currentTime == start) && currentTime.isBefore(end)
        } catch (e: Exception) {
            false
        }
    }

    private fun isFormatMatch(sessionFormat: String, targetFormat: String): Boolean {
        val s = sessionFormat.lowercase().trim()
        val target = targetFormat.lowercase().trim()
        return when {
            target.contains("lightning") -> s.contains("lightning")
            target.contains("workshop") -> s.contains("workshop")
            target.contains("presentation") -> s.contains("presentation") || s.contains("foredrag")
            else -> s == target
        }
    }

    private fun extractRoomNumber(room: String): Int {
        val digits = room.filter { it.isDigit() }
        return digits.toIntOrNull() ?: Int.MAX_VALUE
    }
}


