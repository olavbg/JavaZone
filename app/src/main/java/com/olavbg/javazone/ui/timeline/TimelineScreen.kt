package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.RoomTag
import com.olavbg.javazone.ui.theme.*
import com.olavbg.javazone.util.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val groupedSessions by viewModel.groupedSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availableRooms by viewModel.availableRooms.collectAsState()
    val availableFormats by viewModel.availableFormats.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val onlyFavorites by viewModel.onlyFavorites.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterSpeaker by viewModel.filterSpeaker.collectAsState()

    var isSearchVisible by remember { mutableStateOf(value = false) }

    val roomsList = remember(groupedSessions, availableRooms) {
        availableRooms.ifEmpty { 
            groupedSessions.values.flatten().asSequence().map { it.room }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy({ extractRoomNumber(it) }, { it })).toList()
        }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to active or upcoming time slot when day changes or data first arrives
    LaunchedEffect(groupedSessions, selectedDay) {
        if (groupedSessions.isNotEmpty() && viewModel.shouldScrollToNow()) {
            val activeIndex = findFirstActiveOrUpcomingIndex(groupedSessions, currentTime)
            if (activeIndex > 0) {
                listState.scrollToItem(activeIndex)
            }
            viewModel.markScrolledToNow()
        }
    }

    Scaffold(
        topBar = {
            TimelineHeader(
                selectedDay = selectedDay,
                onDaySelected = viewModel::setDay,
                onlyFavorites = onlyFavorites,
                onFavoritesToggled = viewModel::setOnlyFavorites,
                selectedFormat = selectedFormat,
                onFormatSelected = viewModel::setFormat,
                availableFormats = availableFormats,
                selectedLanguage = selectedLanguage,
                onLanguageSelected = viewModel::setLanguage,
                availableLanguages = availableLanguages,
                selectedRoom = selectedRoom,
                onRoomSelected = viewModel::setRoom,
                availableRooms = roomsList,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                isSearchVisible = isSearchVisible,
                onToggleSearch = {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) viewModel.setSearchQuery("")
                },
                onSettingsClick = onSettingsClick,
            )
        },
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (filterSpeaker != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Foredrag av $filterSpeaker",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.setFilterSpeaker(null) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fjern filter")
                                }
                            }
                        }
                    }

                    if (sessions.isEmpty()) {
                        EmptyStateView(
                            isSearchActive = (searchQuery.isNotBlank() || onlyFavorites || selectedFormat != null || selectedRoom != null),
                            onClearFilters = {
                                viewModel.setSearchQuery("")
                                viewModel.setOnlyFavorites(false)
                                viewModel.setFormat(null)
                                viewModel.setLanguage(null)
                                viewModel.setRoom(null)
                            }
                        )
                    } else {
                        AgendaListView(
                            groupedSessions = groupedSessions,
                            currentTime = currentTime,
                            listState = listState,
                            onSessionClick = onSessionClick,
                            onFavoriteClick = viewModel::toggleFavorite,
                            contentPadding = PaddingValues(
                                bottom = 32.dp + contentPadding.calculateBottomPadding()
                            )
                        )
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineHeader(
    selectedDay: String?,
    onDaySelected: (String?) -> Unit,
    onlyFavorites: Boolean,
    onFavoritesToggled: (Boolean) -> Unit,
    selectedFormat: String?,
    onFormatSelected: (String?) -> Unit,
    availableFormats: List<String>,
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    availableLanguages: List<String>,
    selectedRoom: String?,
    onRoomSelected: (String?) -> Unit,
    availableRooms: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchVisible: Boolean,
    onToggleSearch: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 1.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "JavaZone",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "2026",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Outlined.Search,
                            contentDescription = "Søk"
                        )
                    }
                    IconButton(onClick = { onFavoritesToggled(!onlyFavorites) }) {
                        Icon(
                            imageVector = if (onlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritter",
                            tint = if (onlyFavorites) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Innstillinger")
                    }
                },
                windowInsets = WindowInsets.statusBars
            )

            // Search Bar
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Søk tittel, foredragsholder, rom...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Nullstill")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Day & View Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    SegmentedButton(
                        selected = selectedDay == null,
                        onClick = { onDaySelected(null) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Alle dager", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = selectedDay == "Wednesday",
                        onClick = { onDaySelected("Wednesday") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Onsdag", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = selectedDay == "Thursday",
                        onClick = { onDaySelected("Thursday") },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Torsdag", fontSize = 11.sp)
                    }
                }
            }

            // Dynamic Quick Filter Chips (Format, Language, Room)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onlyFavorites) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { onFavoritesToggled(false) },
                            label = { Text("Favoritter") },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }

                // Show format chips dynamically available for the selected day
                items(availableFormats) { format ->
                    val isSelected = selectedFormat?.equals(format, ignoreCase = true) == true
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFormatSelected(if (isSelected) null else format) },
                        label = {
                            Text(
                                when {
                                    format.contains("presentation", ignoreCase = true) -> "Foredrag"
                                    format.contains("lightning", ignoreCase = true) -> "Lynforedrag"
                                    format.contains("workshop", ignoreCase = true) -> "Workshop"
                                    else -> format
                                }
                            )
                        }
                    )
                }

                // Show language chips dynamically available for the selected day
                items(availableLanguages) { lang ->
                    val isSelected = selectedLanguage == lang
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLanguageSelected(if (isSelected) null else lang) },
                        label = { Text(if (lang.contains("no", ignoreCase = true)) "🇳🇴 NO" else "🇬🇧 EN") }
                    )
                }

                // Show room chips dynamically available for the selected day
                if (availableRooms.isNotEmpty()) {
                    items(availableRooms) { room ->
                        val isSelected = selectedRoom == room
                        FilterChip(
                            selected = isSelected,
                            onClick = { onRoomSelected(if (isSelected) null else room) },
                            label = { Text(room) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AgendaListView(
    groupedSessions: Map<String, List<Session>>,
    currentTime: Instant,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSessionClick: (String) -> Unit,
    onFavoriteClick: (Session) -> Unit,
    contentPadding: PaddingValues = PaddingValues(bottom = 32.dp)
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Continuous Vertical Guide Line (positioned at x = 36.dp)
        Box(
            modifier = Modifier
                .padding(start = 36.dp)
                .width(2.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            groupedSessions.forEach { (timeSlot, sessionsAtTime) ->
                val isLiveSlot = sessionsAtTime.any { isSessionActive(it, currentTime) }

                // Sticky Header: Time Node Badge pinned to top-left (shown once for group)
                stickyHeader(key = "agenda-sticky-$timeSlot") {
                    TimelineStickyTimeHeader(
                        timeSlot = timeSlot,
                        isLiveSlot = isLiveSlot,
                        sessionCount = sessionsAtTime.size
                    )
                }

                // Session Rows in this Time Block (sorted by room number)
                itemsIndexed(
                    items = sessionsAtTime,
                    key = { _, session -> "agenda-item-${session.id}" },
                    contentType = { _, _ -> "session-row" }
                ) { _, session ->
                    TimelineSessionRow(
                        session = session,
                        isPast = isSessionPast(session.endTimeZulu, currentTime),
                        isActive = isSessionActive(session, currentTime),
                        currentTime = currentTime,
                        onFavoriteClick = { onFavoriteClick(session) },
                        onClick = { onSessionClick(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineStickyTimeHeader(
    timeSlot: String,
    isLiveSlot: Boolean,
    sessionCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isLiveSlot) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(
                        1.dp,
                        if (isLiveSlot) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    ),
                    shadowElevation = if (isLiveSlot) 2.dp else 0.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timeSlot,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isLiveSlot) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (isLiveSlot) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                HorizontalDivider(
                    color = if (isLiveSlot) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "$sessionCount ${if (sessionCount == 1) "foredrag" else "parallell-løp"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Just a bit of vertical line spacing
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Timeline Session Row
 * Features:
 * - Vertically centered timeline connector node aligned with card height
 */
@Composable
fun TimelineSessionRow(
    session: Session,
    isPast: Boolean,
    isActive: Boolean,
    currentTime: Instant,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Left Column Node Connector (Centered at x = 36.dp)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(52.dp)
        ) {
            // Horizontal Connector Arm from vertical line to card
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(36.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f)
                        .background(
                            if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                )
            }

            // Connector Dot on the vertical line (vertically centered with card)
            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else -> FreshGreen
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape
                    )
            )
        }

        // Right Column: Session Card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            DetailedSessionCard(
                session = session,
                isPast = isPast,
                isActive = isActive,
                currentTime = currentTime,
                onFavoriteClick = onFavoriteClick,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DetailedSessionCard(
    session: Session,
    isPast: Boolean,
    isActive: Boolean,
    currentTime: Instant,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(if (isPast) 0.55f else 1f, label = "alpha")

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .alpha(alpha)
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val durationMins = remember(session) { calculateSessionDurationMinutes(session) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Duration Tag (e.g. 60 min, 10 min)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "$durationMins min",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                RoomTag(room = session.room)
                Spacer(modifier = Modifier.width(8.dp))
                FormatBadge(format = session.format)
                if (session.language != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.language.contains("no", ignoreCase = true)) "🇳🇴" else "🇬🇧",
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (session.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (session.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (session.speakers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.speakers.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = calculateSessionProgress(session, currentTime)
                val remainingMins = calculateRemainingMinutes(session, currentTime)

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {}
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatTime(session.startTimeZulu)} – ${formatTime(session.endTimeZulu)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (remainingMins > 0) "$remainingMins min igjen" else "Avsluttes nå",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (!isPast) {
                val minsUntil = calculateMinutesUntilStart(session, currentTime)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatTime(session.startTimeZulu)} – ${formatTime(session.endTimeZulu)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (minsUntil in 1..60) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Om $minsUntil min",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EmptyStateView(
    isSearchActive: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.EventBusy,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearchActive) "Ingen foredrag passer søket/filteret" else "Ingen foredrag funnet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isSearchActive) "Prøv å endre på søkeordene eller tilbakestill filtrene." else "Sjekk internettforbindelsen eller prøv igjen senere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSearchActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClearFilters) {
                    Text("Nullstill alle filter")
                }
            }
        }
    }
}


private fun isSessionActive(session: Session, currentTime: Instant): Boolean {
    return try {
        val start = Instant.parse(session.startTimeZulu)
        val end = Instant.parse(session.endTimeZulu)
        (currentTime.isAfter(start) || currentTime == start) && currentTime.isBefore(end)
    } catch (_: Exception) {
        false
    }
}

private fun isSessionPast(endTimeZulu: String, currentTime: Instant): Boolean {
    return try {
        val end = Instant.parse(endTimeZulu)
        currentTime.isAfter(end)
    } catch (_: Exception) {
        false
    }
}

private fun calculateSessionProgress(session: Session, currentTime: Instant): Float {
    return try {
        val start = Instant.parse(session.startTimeZulu)
        val end = Instant.parse(session.endTimeZulu)
        val total = Duration.between(start, end).toMillis()
        val elapsed = Duration.between(start, currentTime).toMillis()
        if (total <= 0) 0f else (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    } catch (_: Exception) {
        0f
    }
}

private fun calculateRemainingMinutes(session: Session, currentTime: Instant): Long {
    return try {
        val end = Instant.parse(session.endTimeZulu)
        val remaining = Duration.between(currentTime, end).toMinutes()
        if (remaining < 0) 0 else remaining
    } catch (_: Exception) {
        0
    }
}


private fun calculateMinutesUntilStart(session: Session, currentTime: Instant): Long {
    return try {
        val start = Instant.parse(session.startTimeZulu)
        val minutes = Duration.between(currentTime, start).toMinutes()
        if (minutes < 0) 0 else minutes
    } catch (_: Exception) {
        0
    }
}

private fun extractRoomNumber(room: String): Int {
    val digits = room.filter { it.isDigit() }
    return digits.toIntOrNull() ?: Int.MAX_VALUE
}

private fun findFirstActiveOrUpcomingIndex(
    groupedSessions: Map<String, List<Session>>,
    currentTime: Instant
): Int {
    var index = 0
    var bestIndex = -1

    for ((_, sessions) in groupedSessions) {
        val anyActive = sessions.any { isSessionActive(it, currentTime) }
        val isFuture = sessions.firstOrNull()?.let {
            try { Instant.parse(it.startTimeZulu).isAfter(currentTime) } catch (_: Exception) { false }
        } ?: false

        if (anyActive) {
            return index
        }
        if (isFuture && bestIndex == -1) {
            bestIndex = index
        }

        index += 1 + sessions.size // header + sessions count
    }

    return if (bestIndex != -1) bestIndex else 0
}
