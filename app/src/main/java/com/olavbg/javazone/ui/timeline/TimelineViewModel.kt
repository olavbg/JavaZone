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
import java.util.Locale

class TimelineViewModel(private val repository: SessionRepository, private val settingsRepository: SettingsRepository) : ViewModel() {
    companion object { const val CURRENT_YEAR = 2026 }
    private val _selectedYear = MutableStateFlow(CURRENT_YEAR)
    val selectedYear = _selectedYear.asStateFlow()
    private val _selectedDay = MutableStateFlow<String?>(null)
    val selectedDay = _selectedDay.asStateFlow()
    private val _onlyFavorites = MutableStateFlow(false)
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
    private val _yearSessions = MutableStateFlow<List<Session>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private var hasAutoSelectedDay = false
    private var hasScrolledForContext = false

    private val _currentTime = MutableStateFlow(Instant.now())
    val currentTime: StateFlow<Instant> = _currentTime.combine(settingsRepository.simulatedTimeOffset) { time, offset -> time.plusMillis(offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Instant.now())
    private val currentYearSessions = repository.getSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val sourceSessions = combine(_selectedYear, currentYearSessions, _yearSessions) { year, current, historical -> if (year == CURRENT_YEAR) current else historical }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Days are derived from the selected programme instead of being hard-coded to the current conference. */
    val availableDays = sourceSessions.map { list ->
        list.map { getDayFromZulu(it.startTimeZulu) }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { daySortIndex(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val daySessions = combine(sourceSessions, _selectedDay) { list, day -> if (day == null) list else list.filter { getDayFromZulu(it.startTimeZulu) == day } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val availableFormats = daySessions.map { list -> list.map { it.format }.filter { it.isNotBlank() }.distinct().filter { it.contains("presentation", true) || it.contains("lightning", true) || it.contains("workshop", true) }.sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val availableLanguages = daySessions.map { list -> list.mapNotNull { it.language }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val availableRooms = daySessions.map { list -> list.map { it.room }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy({ extractRoomNumber(it) }, { it })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions = combine(sourceSessions, _selectedDay, _onlyFavorites, _selectedFormat, _selectedLanguage, _filterSpeaker, _selectedRoom, _searchQuery) { args ->
        val list = args[0] as List<Session>; val day = args[1] as String?; val favorites = args[2] as Boolean
        val format = args[3] as String?; val language = args[4] as String?; val speaker = args[5] as String?; val room = args[6] as String?; val query = args[7] as String
        var filtered = list
        if (day != null && speaker == null) filtered = filtered.filter { getDayFromZulu(it.startTimeZulu) == day }
        if (favorites) filtered = filtered.filter { it.isFavorite }
        if (format != null) filtered = filtered.filter { isFormatMatch(it.format, format) }
        if (language != null) filtered = filtered.filter { it.language?.equals(language, true) == true }
        if (speaker != null) filtered = filtered.filter { it.speakers.any { s -> s.name.equals(speaker, true) } }
        if (room != null) filtered = filtered.filter { it.room.equals(room, true) }
        if (query.isNotBlank()) { val q = query.trim().lowercase(); filtered = filtered.filter { it.title.lowercase().contains(q) || it.room.lowercase().contains(q) || it.abstract.lowercase().contains(q) || it.speakers.any { s -> s.name.lowercase().contains(q) } } }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val groupedSessions = sessions.map { list -> list.sortedWith(compareBy<Session> { it.startTimeZulu }.thenBy { extractRoomNumber(it.room) }.thenBy { it.room }).groupBy { formatTime(it.startTimeZulu) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch { try { repository.refreshSessions() } finally { _isLoading.value = false } }
        viewModelScope.launch {
            combine(currentYearSessions, currentTime) { list, time -> list to time }.collect { (list, time) ->
                if (!hasAutoSelectedDay && list.isNotEmpty()) {
                    val today = time.atZone(ZoneId.of("Europe/Oslo")).format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
                    list.map { getDayFromZulu(it.startTimeZulu) }.firstOrNull { it.equals(today, true) }?.let { _selectedDay.value = it }
                    hasAutoSelectedDay = true
                }
            }
        }
        viewModelScope.launch { while (true) { _currentTime.value = Instant.now(); delay(Duration.ofMinutes(1).toMillis()) } }
    }

    fun setYear(year: Int) {
        if (_selectedYear.value == year) return
        _selectedYear.value = year; _selectedDay.value = null; _onlyFavorites.value = false
        _selectedFormat.value = null; _selectedLanguage.value = null; _selectedRoom.value = null; _filterSpeaker.value = null; _searchQuery.value = ""; hasScrolledForContext = false
        if (year != CURRENT_YEAR) loadHistoricalYear(year)
    }
    private fun loadHistoricalYear(year: Int) { viewModelScope.launch { _isLoading.value = true; _yearSessions.value = repository.getArchiveSessions(year); _isLoading.value = false } }
    fun setDay(day: String?) { _selectedDay.value = day; hasScrolledForContext = false; if (_selectedFormat.value != null && !availableFormats.value.contains(_selectedFormat.value)) _selectedFormat.value = null; if (_selectedRoom.value != null && !availableRooms.value.contains(_selectedRoom.value)) _selectedRoom.value = null }
    fun setOnlyFavorites(value: Boolean) { _onlyFavorites.value = value }
    fun setFormat(value: String?) { _selectedFormat.value = value }
    fun setLanguage(value: String?) { _selectedLanguage.value = value }
    fun setRoom(value: String?) { _selectedRoom.value = value }
    fun setFilterSpeaker(value: String?) { _filterSpeaker.value = value; if (value != null) _selectedDay.value = null }
    fun setSearchQuery(value: String) { _searchQuery.value = value }
    fun clearFilters() { _onlyFavorites.value = false; _selectedFormat.value = null; _selectedLanguage.value = null; _selectedRoom.value = null; _searchQuery.value = "" }
    fun activeFilterCount() = listOf(_onlyFavorites.value, _selectedFormat.value != null, _selectedLanguage.value != null, _selectedRoom.value != null, _searchQuery.value.isNotBlank()).count { it }
    fun shouldScrollToNow() = !hasScrolledForContext
    fun markScrolledToNow() { hasScrolledForContext = true }
    fun toggleFavorite(session: Session) { if (_selectedYear.value == CURRENT_YEAR) viewModelScope.launch { repository.toggleFavorite(session.id, !session.isFavorite) } }

    private fun formatTime(zulu: String) = try { Instant.parse(zulu).atZone(ZoneId.of("Europe/Oslo")).format(DateTimeFormatter.ofPattern("HH:mm")) } catch (_: Exception) { "" }
    private fun getDayFromZulu(zulu: String) = try { Instant.parse(zulu).atZone(ZoneId.of("Europe/Oslo")).format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)) } catch (_: Exception) { "" }
    private fun daySortIndex(day: String) = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").indexOf(day).let { if (it < 0) Int.MAX_VALUE else it }
    private fun isFormatMatch(value: String, target: String) = when { target.contains("lightning", true) -> value.contains("lightning", true); target.contains("workshop", true) -> value.contains("workshop", true); target.contains("presentation", true) -> value.contains("presentation", true) || value.contains("foredrag", true); else -> value.equals(target, true) }
    private fun extractRoomNumber(room: String) = room.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
}
