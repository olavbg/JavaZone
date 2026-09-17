package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.RoomTag
import com.olavbg.javazone.ui.components.sharedElementModifier
import com.olavbg.javazone.ui.theme.FavoriteRed
import com.olavbg.javazone.util.calculateSessionDurationMinutes
import com.olavbg.javazone.util.extractRoomNumber
import com.olavbg.javazone.util.formatTime
import com.olavbg.javazone.util.isSessionActive
import com.olavbg.javazone.util.localizedDayName
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onSessionClick: (String, Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    sharedScope: SharedTransitionScope? = null,
) {
    val sessions by viewModel.sessions.collectAsState()
    val groupedSessions by viewModel.groupedSessions.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
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
    val selectedYear by viewModel.selectedYear.collectAsState()
    val isCurrentYear by viewModel.isCurrentYear.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val availableDays by viewModel.availableDays.collectAsState()
    val showLiveIndicators by viewModel.showLiveIndicators.collectAsState()
    val currentConferenceDay by viewModel.currentConferenceDay.collectAsState()

    var isSearchVisible by remember { mutableStateOf(value = false) }
    var isYearPickerVisible by remember { mutableStateOf(value = false) }

    val roomsList = remember(groupedSessions, availableRooms) {
        availableRooms.ifEmpty { 
            groupedSessions.flatMap { it.sessions }.asSequence().map { it.room }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy({ extractRoomNumber(it) }, { it })).toList()
        }
    }

    val listState = rememberLazyListState()
    // Bottom inset of the navigation bar; the Scaffold uses zero insets so list content
    // can scroll behind it, and this is added to the content padding instead.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val yearState = rememberUpdatedState(selectedYear)
    val scope = rememberCoroutineScope()
    // Index of the active/upcoming time slot on the current day, or null if none
    val nowIndex = remember(groupedSessions, currentTime) {
        findFirstActiveOrUpcomingIndex(groupedSessions, currentTime)
    }

    var previousYear by remember { mutableIntStateOf(selectedYear) }
    var previousDay by remember { mutableStateOf(selectedDay) }
    var hasAutoScrolledColdStart by rememberSaveable { mutableStateOf(false) }

    // Scroll back to the very top when the selected year changes
    LaunchedEffect(selectedYear) {
        if (selectedYear != previousYear) {
            listState.scrollToItem(0)
            previousYear = selectedYear
        }
    }

    // Auto-scroll to now on cold start once the list is loaded for the current conference day
    LaunchedEffect(groupedSessions, selectedDay, currentConferenceDay, nowIndex) {
        if (!hasAutoScrolledColdStart &&
            isCurrentYear &&
            selectedDay != null &&
            selectedDay.equals(currentConferenceDay, ignoreCase = true) &&
            isGroupedSessionsForDay(groupedSessions, selectedDay)
        ) {
            if (listState.awaitContent()) {
                delay(120) // Give the window and initial Compose pass a brief moment to settle
                val target = nowIndex
                if (target != null) {
                    if (target > 0) {
                        listState.smoothScrollToItemEased(target)
                    }
                    hasAutoScrolledColdStart = true
                }
            }
        }
    }

    // Auto-scroll when switching days:
    // - If switching to current conference day: smoothly scroll to "live nå"
    // - If switching to any other day: scroll to top (item 0)
    // The first transition (null -> auto-selected today) is owned by the cold-start effect
    // above, but still records previousDay so later manual day switches scroll as expected.
    LaunchedEffect(selectedDay, groupedSessions, currentConferenceDay) {
        if (selectedDay != previousDay) {
            if (!isGroupedSessionsForDay(groupedSessions, selectedDay)) {
                // Wait until groupedSessions emits sessions matching the newly selected day
                return@LaunchedEffect
            }
            val fromInitialSelection = previousDay == null
            previousDay = selectedDay

            // The cold-start effect owns the scroll for the day auto-selected at launch.
            if (fromInitialSelection) return@LaunchedEffect

            if (listState.awaitContent()) {
                if (selectedDay != null && selectedDay.equals(currentConferenceDay, ignoreCase = true)) {
                    val target = nowIndex
                    if (target != null) {
                        listState.smoothScrollToItemEased(target)
                    } else {
                        listState.scrollToItem(0)
                    }
                } else {
                    listState.scrollToItem(0)
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
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
                    selectedYear = selectedYear,
                    onYearClick = { isYearPickerVisible = true },
                    availableDays = availableDays,
                )
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            // Keep the list composed so the warm-up pass isn't thrown away on first scroll.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
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
                        if (isLoading) {
                            // Mirrors the empty-state layout, clearly visible at startup (also for archive years).
                            TimelineLoadingState()
                        } else if (allSessions.isEmpty() && !isCurrentYear) {
                            // The selected archive year has no sessions registered at all
                            EmptyStateView(
                                isSearchActive = false,
                                title = "Fant ingen foredrag for JavaZone $selectedYear",
                                subtitle = "Det var ingen registrerte foredrag for dette året."
                            )
                        } else {
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
                        }
                    } else {
                        AgendaListView(
                            groupedSessions = groupedSessions,
                            currentTime = currentTime,
                            listState = listState,
                            onSessionClick = { session ->
                                onSessionClick(session.id, yearState.value)
                            },
                            onFavoriteClick = viewModel::toggleFavorite,
                            showFavorite = isCurrentYear,
                            liveIndicators = showLiveIndicators,
                            sharedScope = sharedScope,
                            contentPadding = PaddingValues(
                                bottom = 32.dp + navBarBottom + contentPadding.calculateBottomPadding()
                            )
                        )
                    }
                }
            }
        }

        // "Scroll to now" pill, shown only when the viewport is several rows away from
        // the active/upcoming slot. The arrow points towards "now": up when already
        // scrolled past it, down when it is still further down.
        val nowFabState by remember(groupedSessions, nowIndex, isCurrentYear, selectedDay, currentConferenceDay) {
            derivedStateOf {
                val isToday = isCurrentYear && selectedDay != null && selectedDay.equals(currentConferenceDay, ignoreCase = true)
                if (!isToday || groupedSessions.isEmpty() || nowIndex == null) {
                    NowFabState.Hidden
                } else {
                    val delta = nowIndex - listState.firstVisibleItemIndex
                    when {
                        delta >= NOW_FAB_SLOP_ITEMS -> NowFabState.ShowDown
                        delta <= -NOW_FAB_SLOP_ITEMS -> NowFabState.ShowUp
                        else -> NowFabState.Hidden
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = nowFabState != NowFabState.Hidden,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp + navBarBottom + contentPadding.calculateBottomPadding())
        ) {
            Surface(
                onClick = {
                    val target = nowIndex
                    if (target != null) {
                        scope.launch {
                            listState.smoothScrollToItemEased(target)
                        }
                    }
                },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shadowElevation = 6.dp,
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = if (nowFabState == NowFabState.ShowDown)
                            Icons.Filled.KeyboardArrowDown
                        else
                            Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nå",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isYearPickerVisible) {
            YearPickerSheet(
                years = availableYears,
                sessionCountsByYear = viewModel.sessionCountsByYear,
                selectedYear = selectedYear,
                onYearSelected = { year ->
                    viewModel.setYear(year)
                    isYearPickerVisible = false
                },
                onDismiss = { isYearPickerVisible = false }
            )
        }
    }
}

@Composable
private fun TimelineLoadingState() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Laster inn foredrag…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearPickerSheet(
    years: List<Int>,
    sessionCountsByYear: Map<Int, Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Text(
            text = "Velg år",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(years, key = { it }) { year ->
                val isSelected = year == selectedYear
                val count = sessionCountsByYear[year]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onYearSelected(year) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = year.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (count != null) {
                            Text(
                                text = "$count foredrag",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Valgt år",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
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
    onSettingsClick: () -> Unit,
    selectedYear: Int,
    onYearClick: () -> Unit,
    availableDays: List<String>
) {
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Give the expand animation a moment to lay the field out before requesting
    // focus, so the keyboard slides in together with the field.
    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible) {
            delay(300)
            runCatching { searchFocusRequester.requestFocus() }
            keyboardController?.show()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
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
                            onClick = onYearClick,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "$selectedYear",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Velg år",
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
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
                            tint = if (onlyFavorites) FavoriteRed else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Innstillinger")
                    }
                },
                windowInsets = WindowInsets.statusBars,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

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
                        .focusRequester(searchFocusRequester)
                )
            }

            if (availableDays.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        val count = availableDays.size + 1
                        SegmentedButton(
                            selected = selectedDay == null,
                            onClick = { onDaySelected(null) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = count)
                        ) {
                            Text("Alle dager", fontSize = 11.sp)
                        }
                        availableDays.forEachIndexed { index, day ->
                            SegmentedButton(
                                selected = selectedDay == day,
                                onClick = { onDaySelected(day) },
                                shape = SegmentedButtonDefaults.itemShape(index = index + 1, count = count)
                            ) {
                                Text(localizedDayName(day), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

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
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = FavoriteRed,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

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

                items(availableLanguages) { lang ->
                    val isSelected = selectedLanguage == lang
                    FilterChip(
                        selected = isSelected,
                        onClick = { onLanguageSelected(if (isSelected) null else lang) },
                        label = { Text(if (lang.contains("no", ignoreCase = true)) "🇳🇴 NO" else "🇬🇧 EN") }
                    )
                }

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
    groupedSessions: List<AgendaGroup>,
    currentTime: Instant,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSessionClick: (Session) -> Unit,
    onFavoriteClick: (Session) -> Unit,
    showFavorite: Boolean = true,
    liveIndicators: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(bottom = 32.dp),
    sharedScope: SharedTransitionScope? = null,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Vertical guide line.
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
            var nextStickyIndex = 0
            groupedSessions.forEach { group ->
                val stickyIndex = nextStickyIndex
                nextStickyIndex += 1 + group.sessions.size
                val isLiveSlot = group.sessions.any { isSessionActive(it, currentTime) }

                // Sticky time header. The pin state reads the scroll position inside the header
                // itself (deferred through derivedStateOf), so the outer content builder is not
                // re-executed on every scroll frame just to compute it.
                stickyHeader(key = "agenda-sticky-${group.key}") {
                    TimelineStickyTimeHeader(
                        timeSlot = group.headerLabel,
                        isLiveSlot = isLiveSlot,
                        sessionCount = group.sessions.size,
                        pinnedRange = (stickyIndex + 1) until nextStickyIndex,
                        listState = listState
                    )
                }

                itemsIndexed(
                    items = group.sessions,
                    key = { _, session -> "agenda-item-${session.id}" },
                    contentType = { _, _ -> "session-row" }
                ) { _, session ->
                    TimelineSessionRow(
                        session = session,
                        isPast = liveIndicators && isSessionPast(session, currentTime),
                        isActive = liveIndicators && isSessionActive(session, currentTime),
                        currentTime = currentTime,
                        onFavoriteClick = remember(session) { { onFavoriteClick(session) } },
                        onClick = remember(session) { { onSessionClick(session) } },
                        showFavorite = showFavorite,
                        sharedScope = sharedScope
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
    sessionCount: Int,
    pinnedRange: IntRange,
    listState: LazyListState
) {
    // Pinned while the first visible row is one of this group's sessions (the header then sticks
    // above the scrolling rows). Reading the scroll position via derivedStateOf keeps the churn
    // contained to this single header instead of recomposing the entire list content per frame.
    val isPinned by remember(pinnedRange) {
        derivedStateOf { listState.firstVisibleItemIndex in pinnedRange }
    }

    // The header background is nearly invisible in its natural list position, but
    // fades to a readable translucent tint while pinned over scrolling rows. It uses
    // the screen background colour (not the raised "surface" tone) so it never reads
    // as an offset panel below the toolbar.
    val bgAlpha by animateFloatAsState(
        targetValue = if (isPinned) 0.72f else 0.10f,
        animationSpec = tween(durationMillis = 250),
        label = "stickyHeaderBg"
    )
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = bgAlpha),
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
            
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun TimelineSessionRow(
    session: Session,
    isPast: Boolean,
    isActive: Boolean,
    currentTime: Instant,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    showFavorite: Boolean = true,
    sharedScope: SharedTransitionScope? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(52.dp)
        ) {
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

            Box(
                modifier = Modifier
                    .size(if (isActive) 12.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isActive -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape
                    )
            )
        }

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
                showFavorite = showFavorite,
                sharedScope = sharedScope,
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
    modifier: Modifier = Modifier,
    showFavorite: Boolean = true,
    sharedScope: SharedTransitionScope? = null
) {
    val id = session.id
    val cardAlpha = if (isPast) 0.55f else 1f

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 3.dp else 1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .graphicsLayer { this.alpha = cardAlpha }
            .border(
                width = if (isActive) 1.5.dp else 0.5.dp,
                color = if (isActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val durationMins = remember(session) { calculateSessionDurationMinutes(session) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!session.startTimeZulu.isBlank()) {
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
                }
                RoomTag(room = session.room)
                Spacer(modifier = Modifier.width(8.dp))
                FormatBadge(
                    format = session.format,
                    modifier = Modifier.sharedElementModifier(sharedScope, "session-format-$id")
                )
                if (session.language != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.language.contains("no", ignoreCase = true)) "🇳🇴" else "🇬🇧",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                if (showFavorite) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onFavoriteClick, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (session.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (session.isFavorite) FavoriteRed else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.sharedElementModifier(sharedScope, "session-title-$id")
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
                        text = "${formatTime(session.start)} – ${formatTime(session.end)}",
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
                        text = "${formatTime(session.start)} – ${formatTime(session.end)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    if (minsUntil <= 60 && !isSessionPast(session, currentTime)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (minsUntil > 0) "Om $minsUntil min" else "Om < 1 min",
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
    onClearFilters: () -> Unit = {},
    title: String? = null,
    subtitle: String? = null
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
                text = title ?: if (isSearchActive) "Ingen foredrag passer søket/filteret" else "Ingen foredrag funnet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle ?: if (isSearchActive) "Prøv å endre på søkeordene eller tilbakestill filtrene." else "Sjekk internettforbindelsen eller prøv igjen senere.",
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

private fun isSessionPast(session: Session, currentTime: Instant): Boolean {
    val end = session.end ?: return false
    return currentTime.isAfter(end)
}

private fun calculateSessionProgress(session: Session, currentTime: Instant): Float {
    val start = session.start ?: return 0f
    val end = session.end ?: return 0f
    val total = Duration.between(start, end).toMillis()
    val elapsed = Duration.between(start, currentTime).toMillis()
    return if (total <= 0) 0f else (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
}

private fun calculateRemainingMinutes(session: Session, currentTime: Instant): Long {
    val end = session.end ?: return 0
    val remaining = Duration.between(currentTime, end).toMinutes()
    return if (remaining < 0) 0 else remaining
}

private fun calculateMinutesUntilStart(session: Session, currentTime: Instant): Long {
    val start = session.start ?: return 0
    val minutes = Duration.between(currentTime, start).toMinutes()
    return if (minutes < 0) 0 else minutes
}

private fun findFirstActiveOrUpcomingIndex(
    groupedSessions: List<AgendaGroup>,
    currentTime: Instant
): Int? {
    var index = 0
    var bestIndex: Int? = null

    for (group in groupedSessions) {
        val anyActive = group.sessions.any { isSessionActive(it, currentTime) }
        val isFuture = group.sessions.firstOrNull()?.start?.let { it.isAfter(currentTime) || it == currentTime } ?: false

        if (anyActive) {
            return index
        }
        if (isFuture && bestIndex == null) {
            bestIndex = index
        }

        index += 1 + group.sessions.size // header + sessions count
    }

    return bestIndex
}

private fun isGroupedSessionsForDay(groupedSessions: List<AgendaGroup>, selectedDay: String?): Boolean {
    if (groupedSessions.isEmpty()) return false
    if (selectedDay == null) return true
    val firstSession = groupedSessions.firstOrNull()?.sessions?.firstOrNull() ?: return false
    return getDayFromZulu(firstSession.start).equals(selectedDay, ignoreCase = true)
}

// Wait until the list has laid-out content, giving up after a timeout so callers never stall.
private suspend fun LazyListState.awaitContent(): Boolean =
    withTimeoutOrNull(SCROLL_READY_TIMEOUT_MS) {
        snapshotFlow { layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
    } == true

// Drive the scroll with a single, continuous eased glide. Starts with an ease-in acceleration
// from a standstill, cruises smoothly at high speed, and decelerates into the target with exact
// pixel landing - no intermediate stops, no hitches, no overshoot.
@OptIn(ExperimentalFoundationApi::class)
private suspend fun LazyListState.smoothScrollToItemEased(targetIndex: Int): Boolean {
    if (!awaitContent()) return false

    val totalItems = layoutInfo.totalItemsCount
    if (totalItems <= 0) return false
    val target = targetIndex.coerceIn(0, totalItems - 1)

    var finishedSuccessfully = false
    scroll {
        val itemScope = LazyLayoutScrollScope(this@smoothScrollToItemEased, this)
        if (itemScope.itemCount <= 0) return@scroll

        // A sticky header is the landing target, but while it is "stuck" pinned at the top the
        // list measures its distance as ~0 even though the first card is still sliding under it.
        // Anchor the landing to the first card below the header, sitting exactly one header height
        // below the top, so the trip always ends with the header at the very top and the first
        // card tucked right underneath - no overlap, no extra offset.
        fun remainingToLanding(): Float {
            val headerInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == target }
            return if (headerInfo != null && target + 1 < itemScope.itemCount) {
                (itemScope.calculateDistanceTo(target + 1) - headerInfo.size).toFloat()
            } else {
                itemScope.calculateDistanceTo(target).toFloat()
            }
        }

        val initialDistance = remainingToLanding()
        if (initialDistance == 0f) {
            finishedSuccessfully = true
            return@scroll
        }

        val rowsToMove = abs(target - firstVisibleItemIndex)
        // Per-frame pixel step cap: keeps the number of rows composed per frame low enough to
        // avoid frame drops. Higher cap = faster long trips (more rows stream past per frame).
        val maxStepPx = (layoutInfo.viewportSize.height * 0.45f).coerceIn(720f, 1500f)

        // For a quick trip the eased duration (~160-260ms) is used as-is. For long trips the
        // duration is stretched to the minimum that fits the eased curve inside the step cap,
        // so the scroll cruises at full allowed speed instead of being clamped (which would
        // add a drawn-out catch-up tail).
        val easedMs = (150 + rowsToMove * 3).coerceIn(160, 260)
        val capAwareMs = (1.35f * abs(initialDistance) / maxStepPx * 16.7f).toInt()
        val durationMs = maxOf(easedMs, capAwareMs).coerceIn(160, 420)

        var previousProgress = 0f
        var lastFrameNanos = 0L
        var accumulatedMillis = 0f

        while (true) {
            val frameNanos = withFrameNanos { it }
            if (lastFrameNanos == 0L) {
                lastFrameNanos = frameNanos
                continue
            }

            // Clamp frame delta time to at most 20ms to prevent frame drop cascades
            val deltaNanos = frameNanos - lastFrameNanos
            lastFrameNanos = frameNanos
            val deltaMillis = (deltaNanos / 1_000_000f).coerceIn(1f, 20f)
            accumulatedMillis += deltaMillis

            val remainingDistance = remainingToLanding()
            if (abs(remainingDistance) < 1f) {
                if (remainingDistance != 0f) {
                    itemScope.scrollBy(remainingDistance)
                }
                break
            }

            val rawFraction = (accumulatedMillis / durationMs).coerceIn(0f, 1f)
            val currentProgress = ScrollToNowEasing.transform(rawFraction)

            // While the eased ramp is running the step is a fixed share of the travel left, so
            // the easing curve is honoured all the way (including its deceleration into the
            // target - no drawn-out crawl at the end). Only when the ramp has already finished
            // but the position is still catching up (frame-drop lag) do we settle the leftover
            // quickly instead of stalling.
            val stepFraction = if (rawFraction >= 1f) {
                LANDING_CATCHUP_PER_FRAME
            } else {
                val progressRemaining = (1f - previousProgress).coerceAtLeast(0.0001f)
                ((currentProgress - previousProgress) / progressRemaining).coerceIn(0f, 1f)
            }

            var stepPx = remainingDistance * stepFraction

            // Clamp stepPx to maxStepPx so Compose never has to compose multiple cards in one frame
            stepPx = stepPx.coerceIn(-maxStepPx, maxStepPx)

            if (abs(stepPx) > 0.001f) {
                val consumed = itemScope.scrollBy(stepPx)
                if (abs(consumed) < 0.001f && abs(stepPx) > 1f) {
                    break
                }
            }

            previousProgress = currentProgress
        }

        finishedSuccessfully = true
    }

    return finishedSuccessfully
}

// Ease-in curve with a visible acceleration ramp and a mild deceleration into the landing zone.
private val ScrollToNowEasing = CubicBezierEasing(0.25f, 0.0f, 0.25f, 1.0f)

// If the eased ramp finishes while the position is still catching up (e.g. a frame-dropping
// spell during cold start), the leftover is settled fast - a few frames, never a drawn-out tail.
private const val LANDING_CATCHUP_PER_FRAME = 0.35f

// How long to wait for the list to have laid-out content before giving up on the auto-scroll.
private const val SCROLL_READY_TIMEOUT_MS = 5_000L

// Rows the viewport must move away from now before the "Scroll to now" pill appears.
private const val NOW_FAB_SLOP_ITEMS = 6

private enum class NowFabState { Hidden, ShowUp, ShowDown }
