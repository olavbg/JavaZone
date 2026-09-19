package com.olavbg.javazone.ui.timeline

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.data.repository.SettingsRepository
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.util.extractRoomNumber
import com.olavbg.javazone.util.shortDayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Shared, thread-safe formatters/zones so per-item formatting never allocates a new one.
internal val OSLO_ZONE: ZoneId = ZoneId.of("Europe/Oslo")
internal val ENGLISH_DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE", java.util.Locale.ENGLISH)
private val SHORT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

internal fun getDayFromZulu(instant: Instant?): String {
    return try {
        instant?.atZone(OSLO_ZONE)?.format(ENGLISH_DAY_FORMATTER) ?: ""
    } catch (_: Exception) {
        ""
    }
}

internal fun conferenceDayForDate(sessions: List<Session>, date: LocalDate): String? {
    return sessions.firstOrNull { session ->
        session.start?.atZone(OSLO_ZONE)?.toLocalDate() == date
    }?.let { getDayFromZulu(it.start) }
}

@Immutable
data class AgendaGroup(
    val key: String,
    val headerLabel: String,
    val sessions: List<Session>
)

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(
    private val repository: SessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val availableYears: StateFlow<List<Int>> = repository.availableYears
    val sessionCountsByYear: Map<Int, Int> = SessionRepository.sessionCountsByYear

    val filtersExpanded: StateFlow<Boolean> = settingsRepository.filtersExpanded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _selectedYear = MutableStateFlow(SessionRepository.CURRENT_YEAR)
    val selectedYear = _selectedYear.asStateFlow()

    val isCurrentYear: StateFlow<Boolean> = selectedYear
        .map { it == SessionRepository.CURRENT_YEAR }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _currentTime = MutableStateFlow(Instant.now())
    val currentTime: StateFlow<Instant> = _currentTime
        .combine(settingsRepository.simulatedTimeOffset) { time, offset ->
            time.plusMillis(offset)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Instant.now())

    val allSessions: StateFlow<List<Session>> = _selectedYear
        .flatMapLatest { year -> repository.getSessionsFlow(year) }
        // Eagerly: start streaming from Room the moment the ViewModel is created, so
        // cached data is already available on the very first frame (no empty-list flash).
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val availableDays: StateFlow<List<String>> = allSessions.map { list ->
        list.mapNotNull { it.start?.atZone(OSLO_ZONE)?.toLocalDate() }
            .distinct()
            .sorted()
            .map { date -> date.format(ENGLISH_DAY_FORMATTER) }
            .distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showLiveIndicators: StateFlow<Boolean> = combine(allSessions, currentTime, _selectedYear) { sessions, time, year ->
        if (year != SessionRepository.CURRENT_YEAR) false
        else sessions.any { it.end?.isAfter(time) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentConferenceDay: StateFlow<String?> = combine(allSessions, currentTime, _selectedYear) { list, time, year ->
        if (year != SessionRepository.CURRENT_YEAR || list.isEmpty()) null
        else conferenceDayForDate(list, time.atZone(OSLO_ZONE).toLocalDate())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Sessions filtered by selected day only (used for calculating dynamic filter options)
    val daySessions: StateFlow<List<Session>> = combine(allSessions, _selectedDay) { list, day ->
        if (day == null) list else list.filter { getDayFromZulu(it.start) == day }
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

    @Suppress("UNCHECKED_CAST")
    val sessions: StateFlow<List<Session>> = combine(
        allSessions,
        _selectedDay,
        _onlyFavorites,
        _selectedFormat,
        _selectedLanguage,
        _selectedRoom,
        _searchQuery
    ) { args: Array<Any?> ->
        val list = args[0] as List<Session>
        val day = args[1] as String?
        val favoritesOnly = args[2] as Boolean
        val format = args[3] as String?
        val language = args[4] as String?
        val room = args[5] as String?
        val query = args[6] as String

        var filtered = list
        if (day != null) {
            filtered = filtered.filter { getDayFromZulu(it.start) == day }
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
        // Run the filter chain off the main thread; it re-executes on every source
        // change and would otherwise add main-thread spikes during the first seconds.
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

val groupedSessions: StateFlow<List<AgendaGroup>> = sessions.map { sessionList ->
        val grouped = sessionList
            .sortedWith(
                compareBy<Session> { it.startTimeZulu }
                    .thenBy { extractRoomNumber(it.room) }
                    .thenBy { it.room },
            )
            .groupBy { session ->
                val date = session.start?.atZone(OSLO_ZONE)?.toLocalDate()
                "${date ?: java.time.LocalDate.MIN}|${formatTime(session.start)}"
            }
        val multiDay = grouped.keys.map { it.substringBefore('|') }.distinct().size > 1
        grouped.map { (key, groupSessions) ->
            val dayKey = runCatching {
                java.time.LocalDate.parse(key.substringBefore('|'))
                    .format(DateTimeFormatter.ofPattern("EEEE", java.util.Locale.ENGLISH))
            }.getOrDefault("")
            val time = key.substringAfter('|')
            AgendaGroup(
                key = key,
                headerLabel = if (multiDay) "${shortDayName(dayKey)} $time".trim() else time,
                sessions = groupSessions
            )
        }
        // Sorting + grouping + formatting runs off the main thread so the eagerly-shared
        // pipeline never blocks the first frames/scroll.
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _isLoading = MutableStateFlow(value = true)
    val isLoading: StateFlow<Boolean> = combine(
        _isLoading,
        _selectedYear,
        repository.archiveLoadingFlow()
    ) { initialLoading, year, loadingMap ->
        if (year == SessionRepository.CURRENT_YEAR) initialLoading
        else loadingMap[year] ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var hasAutoSelectedDay = false

    init {
        viewModelScope.launch {
            repository.loadAvailableYears()
        }
        viewModelScope.launch {
            try {
                repository.refreshSessions()
            } finally {
                _isLoading.value = false
            }
        }
        // Auto-select current day matching effective currentTime (including simulated time settings)
        viewModelScope.launch {
            currentConferenceDay.collect { matchingDay ->
                if (!hasAutoSelectedDay && _selectedDay.value == null && matchingDay != null) {
                    _selectedDay.value = matchingDay
                    hasAutoSelectedDay = true
                }
            }
        }
        // Update current time at each whole clock minute (second = 0)
        viewModelScope.launch {
            while (true) {
                _currentTime.value = Instant.now()
                val millisUntilNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
                delay(millisUntilNextMinute)
            }
        }
    }

    fun setYear(year: Int) {
        if (year == _selectedYear.value) return
        _selectedYear.value = year
        // Reset filters that don't make sense across years
        _selectedDay.value = null
        _selectedFormat.value = null
        _selectedLanguage.value = null
        _selectedRoom.value = null
        _searchQuery.value = ""
        _onlyFavorites.value = false
        if (year == SessionRepository.CURRENT_YEAR) {
            hasAutoSelectedDay = false
        } else {
            viewModelScope.launch {
                repository.loadArchiveSessions(year)
            }
        }
    }

    fun setDay(day: String?) {
        _selectedDay.value = day
        // Clear format/room selection if no longer available in the new day
        if (_selectedFormat.value != null && !(availableFormats.value.contains(_selectedFormat.value))) {
            _selectedFormat.value = null
        }
        if (_selectedRoom.value != null && !availableRooms.value.contains(_selectedRoom.value)) {
            _selectedRoom.value = null
        }
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFiltersExpanded(expanded: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFiltersExpanded(expanded)
        }
    }

    fun toggleFavorite(session: Session) {
        viewModelScope.launch {
            repository.toggleFavorite(session.id, !session.isFavorite)
        }
    }

    private fun formatTime(instant: Instant?): String {
        return try {
            instant?.atZone(OSLO_ZONE)?.format(SHORT_TIME_FORMATTER) ?: ""
        } catch (_: Exception) {
            ""
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
}
