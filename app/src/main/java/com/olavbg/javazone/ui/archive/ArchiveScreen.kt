package com.olavbg.javazone.ui.archive

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.olavbg.javazone.model.Session
import com.olavbg.javazone.ui.components.FormatBadge
import com.olavbg.javazone.ui.components.resolveVideoUrl
import com.olavbg.javazone.util.calculateSessionDurationMinutes
import com.olavbg.javazone.util.formatDay
import com.olavbg.javazone.util.formatTime
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val showWorkshops by viewModel.showWorkshops.collectAsState()
    val years = viewModel.availableYears
    val pagerState = rememberPagerState(pageCount = { years.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(
                        onClick = viewModel::toggleWorkshops,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (showWorkshops) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showWorkshops) Icons.Default.FilterList else Icons.Default.FilterListOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showWorkshops) "Workshops på" else "Workshops av",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
            YearSelector(
                years = years,
                selectedYearIndex = pagerState.currentPage,
                onYearSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { page ->
                val year = years[page]
                val sessions by viewModel.getSessionsForYear(year).collectAsState(emptyList())
                val isLoading by viewModel.isLoading(year).collectAsState(false)
                
                val listState = rememberLazyListState()
                
                val firstNonWorkshopIndex = remember(sessions) {
                    sessions.indexOfFirst { !it.format.contains("workshop", ignoreCase = true) }
                }

                val showSkipButton by remember(sessions, showWorkshops) {
                    derivedStateOf {
                        showWorkshops && 
                        firstNonWorkshopIndex > 0 &&
                        listState.firstVisibleItemIndex < firstNonWorkshopIndex
                    }
                }

                LaunchedEffect(year) {
                    viewModel.loadSessions(year)
                }

                if (isLoading && sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp + contentPadding.calculateBottomPadding()
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sessions) { session ->
                                ArchiveSessionCard(session = session) { url ->
                                    try {
                                        val videoUrl = resolveVideoUrl(url)
                                        val intent = Intent(Intent.ACTION_VIEW, videoUrl.toUri()).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }

                        if (showSkipButton) {
                            Surface(
                                onClick = {
                                    if (firstNonWorkshopIndex != -1) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(firstNonWorkshopIndex)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 24.dp),
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shadowElevation = 8.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Hopp over workshops", 
                                        style = MaterialTheme.typography.labelLarge, 
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearSelector(
    years: List<Int>,
    selectedYearIndex: Int,
    onYearSelected: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedYearIndex,
        edgePadding = 16.dp,
        divider = {}
    ) {
        years.forEachIndexed { index, year ->
            Tab(
                selected = selectedYearIndex == index,
                onClick = { onYearSelected(index) }
            ) {
                Text(
                    text = year.toString(),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ArchiveSessionCard(
    session: Session,
    onVideoClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val durationMins = remember(session) { calculateSessionDurationMinutes(session) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "$durationMins min",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                FormatBadge(format = session.format)
                if (session.language != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.language.contains("no", ignoreCase = true)) "🇳🇴" else "🇬🇧",
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = session.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )

            if (session.speakers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = session.speakers.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${formatDay(session.startTimeZulu)}, ${formatTime(session.startTimeZulu)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!session.videoUrl.isNullOrEmpty()){
                    Spacer(modifier = Modifier.width(12.dp))
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Surface(
                            onClick = { onVideoClick(session.videoUrl) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Se video",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
