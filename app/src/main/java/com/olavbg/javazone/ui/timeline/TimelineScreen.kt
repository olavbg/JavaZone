package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.RoomTag
import java.time.Duration
import java.time.Instant

private val DayOptions = listOf(null, "Wednesday", "Thursday")

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onSessionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onYearClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val groupedSessions by viewModel.groupedSessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val onlyFavorites by viewModel.onlyFavorites.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedRoom by viewModel.selectedRoom.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val availableFormats by viewModel.availableFormats.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val availableRooms by viewModel.availableRooms.collectAsState()
    val filterSpeaker by viewModel.filterSpeaker.collectAsState()
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var filtersVisible by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(groupedSessions, selectedDay, selectedYear) {
        if (groupedSessions.isNotEmpty() && viewModel.shouldScrollToNow()) {
            listState.scrollToItem(findFirstActiveOrUpcomingIndex(groupedSessions, currentTime))
            viewModel.markScrolledToNow()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                Surface(tonalElevation = 1.dp) {
                    TopAppBar(
                        title = {
                            TextButton(onClick = onYearClick, contentPadding = PaddingValues(horizontal = 4.dp)) {
                                Text("JavaZone", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(6.dp))
                                Text("$selectedYear", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("  ▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        actions = {
                            IconButton(onClick = { searchVisible = !searchVisible; if (!searchVisible) viewModel.setSearchQuery("") }) {
                                Icon(if (searchVisible) Icons.Default.Clear else Icons.Default.Search, "Søk")
                            }
                            IconButton(onClick = { viewModel.setOnlyFavorites(!onlyFavorites) }) {
                                Icon(if (onlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favoritter", tint = if (onlyFavorites) MaterialTheme.colorScheme.tertiary else LocalContentColor.current)
                            }
                            IconButton(onClick = onSettingsClick) { Icon(Icons.Rounded.Settings, "Innstillinger") }
                        },
                        windowInsets = WindowInsets.statusBars
                    )
                }

                AnimatedVisibility(searchVisible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Søk foredrag, foredragsholder eller rom…") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = if (searchQuery.isNotEmpty()) ({ IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Default.Clear, "Nullstill") } }) else null,
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
                        DayOptions.forEachIndexed { index, day ->
                            SegmentedButton(
                                selected = selectedDay == day,
                                onClick = { viewModel.setDay(day) },
                                shape = SegmentedButtonDefaults.itemShape(index, DayOptions.size)
                            ) { Text(dayLabel(day), fontSize = 12.sp, maxLines = 1) }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { filtersVisible = !filtersVisible }) {
                        BadgedBox(badge = { if (viewModel.activeFilterCount() > 0) Badge { Text(viewModel.activeFilterCount().toString()) } }) {
                            Icon(Icons.Default.FilterList, "Filtre")
                        }
                    }
                }

                AnimatedVisibility(filtersVisible || viewModel.activeFilterCount() > 0) {
                    LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onlyFavorites) item { FilterChip(true, { viewModel.setOnlyFavorites(false) }, label = { Text("Favoritter") }) }
                        items(availableFormats) { format -> FilterChip(selectedFormat.equals(format, true), { viewModel.setFormat(if (selectedFormat.equals(format, true)) null else format) }, label = { Text(formatLabel(format)) }) }
                        items(availableLanguages) { lang -> FilterChip(selectedLanguage == lang, { viewModel.setLanguage(if (selectedLanguage == lang) null else lang) }, label = { Text(languageLabel(lang)) }) }
                        items(availableRooms) { room -> FilterChip(selectedRoom == room, { viewModel.setRoom(if (selectedRoom == room) null else room) }, label = { Text(room) }) }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (sessions.isEmpty()) {
                EmptyStateView(searchQuery.isNotBlank() || viewModel.activeFilterCount() > 0, viewModel::clearFilters)
            } else {
                TimelineList(groupedSessions, currentTime, listState, onSessionClick, viewModel::toggleFavorite, contentPadding)
            }

            if (filterSpeaker != null) {
                AssistChip(
                    onClick = { viewModel.setFilterSpeaker(null) },
                    label = { Text("Foredrag av $filterSpeaker") },
                    leadingIcon = { Icon(Icons.Default.Person, null, Modifier.size(16.dp)) },
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineList(
    grouped: Map<String, List<Session>>,
    currentTime: Instant,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSessionClick: (String) -> Unit,
    onFavoriteClick: (Session) -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp + contentPadding.calculateBottomPadding()), modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (time, sessions) ->
            val live = sessions.any { isActive(it, currentTime) }
            stickyHeader(key = "time-$time") {
                Surface(color = MaterialTheme.colorScheme.background.copy(alpha = .96f)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp), color = if (live) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                            Text(time, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontWeight = FontWeight.Black, color = if (live) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (sessions.size == 1) "1 foredrag" else "${sessions.size} parallelle", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        if (live) AssistChip(onClick = {}, label = { Text("NÅ") }, enabled = false)
                    }
                }
            }
            items(sessions, key = { it.id }) { session ->
                val active = isActive(session, currentTime)
                val past = isPast(session, currentTime)
                SessionCard(session, active, past, currentTime, onFavoriteClick, { onSessionClick(session.id) })
            }
        }
    }
}

@Composable
private fun SessionCard(session: Session, active: Boolean, past: Boolean, currentTime: Instant, onFavoriteClick: (Session) -> Unit, onClick: () -> Unit) {
    val alpha by androidx.compose.animation.core.animateFloatAsState(if (past) .48f else 1f, label = "pastAlpha")
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).alpha(alpha), shape = RoundedCornerShape(if (active) 24.dp else 18.dp), colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow), border = if (active) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = .7f)) else null) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${formatTime(session.startTimeZulu)}–${formatTime(session.endTimeZulu)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                RoomTag(session.room)
                Spacer(Modifier.width(6.dp))
                FormatBadge(session.format)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onFavoriteClick(session) }, modifier = Modifier.size(32.dp)) { Icon(if (session.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favoritt", tint = if (session.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (session.speakers.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(session.speakers.joinToString { it.name }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (active) {
                Spacer(Modifier.height(10.dp))
                val progress = progress(session, currentTime)
                LinearProgressIndicator({ progress }, Modifier.fillMaxWidth().height(5.dp), strokeCap = StrokeCap.Round)
                Spacer(Modifier.height(5.dp))
                Text("NÅ · ${remaining(session, currentTime)} min igjen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun EmptyStateView(active: Boolean, clear: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.EventBusy, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .5f))
            Spacer(Modifier.height(16.dp))
            Text(if (active) "Ingen foredrag passer filteret" else "Ingen foredrag funnet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            if (active) { Spacer(Modifier.height(12.dp)); FilledTonalButton(onClick = clear) { Text("Nullstill filtre") } }
        }
    }
}

private fun dayLabel(day: String?) = when (day) { null -> "Alle"; "Wednesday" -> "Onsdag"; "Thursday" -> "Torsdag"; else -> day }
private fun formatLabel(format: String) = when { format.contains("presentation", true) -> "Foredrag"; format.contains("lightning", true) -> "Lynforedrag"; format.contains("workshop", true) -> "Workshop"; else -> format }
private fun languageLabel(lang: String) = if (lang.contains("no", true)) "🇳🇴 NO" else "🇬🇧 EN"
private fun formatTime(zulu: String): String = try { java.time.Instant.parse(zulu).atZone(java.time.ZoneId.of("Europe/Oslo")).toLocalTime().toString().take(5) } catch (_: Exception) { "--:--" }
private fun isActive(s: Session, now: Instant) = try { now >= Instant.parse(s.startTimeZulu) && now < Instant.parse(s.endTimeZulu) } catch (_: Exception) { false }
private fun isPast(s: Session, now: Instant) = try { now > Instant.parse(s.endTimeZulu) } catch (_: Exception) { false }
private fun progress(s: Session, now: Instant): Float = try { val a=Instant.parse(s.startTimeZulu); val b=Instant.parse(s.endTimeZulu); (Duration.between(a, now).toMillis().toFloat()/Duration.between(a,b).toMillis()).coerceIn(0f,1f) } catch (_: Exception) { 0f }
private fun remaining(s: Session, now: Instant): Long = try { Duration.between(now, Instant.parse(s.endTimeZulu)).toMinutes().coerceAtLeast(0) } catch (_: Exception) { 0 }
private fun findFirstActiveOrUpcomingIndex(grouped: Map<String,List<Session>>, now: Instant): Int { var index=0; var future=-1; for ((_, list) in grouped) { if (list.any { isActive(it,now) }) return index; if (future<0 && list.firstOrNull()?.let { try { Instant.parse(it.startTimeZulu)>now } catch (_:Exception){false} } == true) future=index; index += 1+list.size }; return future.coerceAtLeast(0) }
