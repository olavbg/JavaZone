package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.olavbg.javazone.R
import com.olavbg.javazone.data.repository.SessionRepository
import com.olavbg.javazone.util.extractRoomNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val selectedYear by viewModel.selectedYear.collectAsState()
    val isCurrentYear by viewModel.isCurrentYear.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()
    val availableDays by viewModel.availableDays.collectAsState()
    val showLiveIndicators by viewModel.showLiveIndicators.collectAsState()
    val currentConferenceDay by viewModel.currentConferenceDay.collectAsState()
    val filtersExpanded by viewModel.filtersExpanded.collectAsState()

    // Start with search open if a query is already active, so a recreated timeline scene
    // never silently filters the list behind a hidden field (e.g. after returning from a talk).
    var isSearchVisible by remember { mutableStateOf(value = searchQuery.isNotBlank()) }
    var isYearPickerVisible by remember { mutableStateOf(value = false) }

    val roomsList = remember(groupedSessions, availableRooms) {
        availableRooms.ifEmpty { 
            groupedSessions.flatMap { it.sessions }.asSequence().map { it.room }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy({ extractRoomNumber(it) }, { it })).toList()
        }
    }

    // Recreate the list state when the selected day changes so the new day's list starts at
    // the very top instead of resuming the previous day's scroll offset (which would flash a
    // mid-list viewport before the auto-scroll settles it).
    val listState = key(selectedDay) { rememberLazyListState() }
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

    // Scroll back to the very top when the selected year changes. Returning to the current
    // year behaves like a fresh cold start: today's day is re-auto-selected and, once today's
    // content is loaded, the list glides down to the live slot instead of staying at the top.
    LaunchedEffect(selectedYear) {
        if (selectedYear != previousYear) {
            listState.scrollToItem(0)
            if (selectedYear == SessionRepository.CURRENT_YEAR) {
                hasAutoScrolledColdStart = false
            }
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
                    },
                    onSettingsClick = onSettingsClick,
                    selectedYear = selectedYear,
                    onYearClick = { isYearPickerVisible = true },
                    availableDays = availableDays,
                    isFiltersExpanded = filtersExpanded,
                    onFiltersExpandedChange = viewModel::setFiltersExpanded,
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
                    if (sessions.isEmpty()) {
                        if (isLoading) {
                            // Mirrors the empty-state layout, clearly visible at startup (also for archive years).
                            TimelineLoadingState()
                        } else if (allSessions.isEmpty() && !isCurrentYear) {
                            // The selected archive year has no sessions registered at all
                            EmptyStateView(
                                isSearchActive = false,
                                title = stringResource(R.string.empty_archive_title, selectedYear),
                                subtitle = stringResource(R.string.empty_archive_subtitle)
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
                        // Rebuild the list with a fresh identity every time the selected day
                        // changes. The day swap coincides with an immediate scroll jump, which
                        // can otherwise cut the item exit animations short and leave the previous
                        // day's cards stuck rendered underneath the new ones.
                        key(selectedDay) {
                            AgendaListView(
                                groupedSessions = groupedSessions,
                                currentTime = currentTime,
                                listState = listState,
                                onSessionClick = { session ->
                                    onSessionClick(session.id, yearState.value)
                                },
                                onFavoriteClick = viewModel::toggleFavorite,
                                favoriteDisplay = if (isCurrentYear) FavoriteDisplay.Toggle else FavoriteDisplay.FavoriteOnly,
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
        // The pill's visibility is driven through a MutableTransitionState so we can detect when it
        // is mid-transition. The arrow direction is frozen on exit: it only updates when the pill
        // (re)appears, so the arrow never visibly flips at the same moment the pill animates away.
        val fabTransition = remember { MutableTransitionState(false) }
        fabTransition.targetState = nowFabState != NowFabState.Hidden
        var frozenArrowDown by remember { mutableStateOf(true) }
        LaunchedEffect(fabTransition.targetState) {
            if (fabTransition.targetState) {
                frozenArrowDown = nowFabState == NowFabState.ShowDown
            }
        }
        LaunchedEffect(nowFabState) {
            if (nowFabState != NowFabState.Hidden && fabTransition.isIdle && fabTransition.targetState) {
                frozenArrowDown = nowFabState == NowFabState.ShowDown
            }
        }
        AnimatedVisibility(
            visibleState = fabTransition,
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
                        imageVector = if (frozenArrowDown)
                            Icons.Filled.KeyboardArrowDown
                        else
                            Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.now),
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
                text = stringResource(R.string.timeline_loading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
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
                text = title ?: if (isSearchActive) stringResource(R.string.empty_filtered_title) else stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle ?: if (isSearchActive) stringResource(R.string.empty_filtered_subtitle) else stringResource(R.string.empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isSearchActive) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClearFilters) {
                    Text(stringResource(R.string.reset_filters))
                }
            }
        }
    }
}