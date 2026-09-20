package com.olavbg.javazone.ui.timeline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.olavbg.javazone.R
import com.olavbg.javazone.ui.components.BackgroundAnimationWatch
import com.olavbg.javazone.ui.components.roomAccentColor
import com.olavbg.javazone.ui.theme.FavoriteRed
import com.olavbg.javazone.ui.theme.LocalJavaZoneThemeTokens
import com.olavbg.javazone.util.shortDayName
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = LocalJavaZoneThemeTokens.current.bottomSheetSurfaceAlpha
        )
    ) {
        PauseBackgroundWhileOpen()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerHeaderIcon(imageVector = Icons.Rounded.CalendarMonth)
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.select_year),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(years, key = { it }) { year ->
                val isSelected = year == selectedYear
                val count = sessionCountsByYear[year]
                SheetPickerRow(
                    isSelected = isSelected,
                    onClick = { onYearSelected(year) },
                    title = year.toString(),
                    titleStyle = MaterialTheme.typography.titleLarge,
                    support = count?.let { pluralStringResource(R.plurals.year_talk_count, it, it) }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomPickerSheet(
    rooms: List<String>,
    selectedRoom: String?,
    onRoomSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(
            alpha = LocalJavaZoneThemeTokens.current.bottomSheetSurfaceAlpha
        )
    ) {
        PauseBackgroundWhileOpen()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PickerHeaderIcon(imageVector = Icons.Default.LocationOn)
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.filter_rooms),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (selectedRoom != null) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    TextButton(
                        onClick = {
                            onRoomSelected(null)
                            onDismiss()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(stringResource(R.string.reset_room))
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rooms, key = { it }) { room ->
                val isSelected = room == selectedRoom
                val accent = roomAccentColor(room, MaterialTheme.colorScheme.primary)
                SheetPickerRow(
                    isSelected = isSelected,
                    onClick = {
                        onRoomSelected(if (isSelected) null else room)
                        onDismiss()
                    },
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        accent.copy(alpha = 1f)
                                    } else {
                                        accent.copy(alpha = 0.6f)
                                    }
                                )
                        )
                    },
                    title = room
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PauseBackgroundWhileOpen() {
    DisposableEffect(Unit) {
        BackgroundAnimationWatch.pause()
        onDispose { BackgroundAnimationWatch.resume() }
    }
}

@Composable
private fun PickerHeaderIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SheetPickerRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    title: String,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    support: String? = null
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = borderColor
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!support.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = support,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactFilterToggle(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    shape: Shape = FilterChipDefaults.shape,
    content: @Composable (selected: Boolean) -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                content(selected)
            }
        },
        shape = shape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.secondary,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.dp
        ),
        modifier = modifier.height(32.dp)
    )
}

@Composable
private fun ExpandableChipLabel(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandHorizontally(animationSpec = tween(160)) + fadeIn(animationSpec = tween(100)),
        exit = shrinkHorizontally(animationSpec = tween(160)) + fadeOut(animationSpec = tween(100))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(6.dp))
            content()
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
    isFiltersExpanded: Boolean,
    onFiltersExpandedChange: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    selectedYear: Int,
    onYearClick: () -> Unit,
    availableDays: List<String>
) {
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isRoomPickerVisible by remember { mutableStateOf(false) }

    // Only grab focus when the user explicitly opens search, not when the field is
    // restored because a query is still active (e.g. after the scene is recreated).
    var wasSearchVisible by remember { mutableStateOf(isSearchVisible) }
    LaunchedEffect(isSearchVisible) {
        if (isSearchVisible && !wasSearchVisible) {
            delay(300.milliseconds)
            runCatching { searchFocusRequester.requestFocus() }
            keyboardController?.show()
        }
        wasSearchVisible = isSearchVisible
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = LocalJavaZoneThemeTokens.current.topBarSurfaceAlpha
        ),
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
                                    contentDescription = stringResource(R.string.select_year),
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSearch) {
                        Box(modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                            if (!isSearchVisible && searchQuery.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .background(FavoriteRed, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
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
                    placeholder = { Text(stringResource(R.string.search_placeholder)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp)
                        .focusRequester(searchFocusRequester)
                )
            }

            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    if (availableDays.isNotEmpty()) {
                        CompactFilterToggle(
                            selected = selectedDay == null,
                            onClick = { onDaySelected(null) }
                        ) {
                            Text(
                                text = stringResource(R.string.all_days),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        availableDays.forEach { day ->
                            val isSelected = selectedDay == day
                            CompactFilterToggle(
                                selected = isSelected,
                                onClick = { onDaySelected(if (isSelected) null else day) }
                            ) {
                                Text(
                                    text = shortDayName(day),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    CompactFilterToggle(
                        selected = onlyFavorites,
                        onClick = { onFavoritesToggled(!onlyFavorites) }
                    ) { selected ->
                        Icon(
                            imageVector = if (selected) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorites),
                            tint = if (selected) FavoriteRed else LocalContentColor.current,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.favorites),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }

                    val hasExpandableFilters =
                        availableFormats.isNotEmpty() ||
                            availableLanguages.isNotEmpty() ||
                            availableRooms.isNotEmpty()
                    if (hasExpandableFilters) {
                        val hasActiveFilters =
                            selectedFormat != null || selectedLanguage != null || selectedRoom != null
                        val isSelected = isFiltersExpanded || hasActiveFilters
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable { onFiltersExpandedChange(!isFiltersExpanded) },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isFiltersExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = stringResource(
                                        if (isFiltersExpanded) R.string.hide_filters else R.string.show_filters
                                    ),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (!isFiltersExpanded && hasActiveFilters) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(7.dp)
                                            .background(FavoriteRed, CircleShape)
                                            .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isFiltersExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {

                        availableFormats.forEach { format ->
                            val isSelected = selectedFormat?.equals(format, ignoreCase = true) == true
                            val label = when {
                                format.contains("presentation", ignoreCase = true) -> stringResource(R.string.format_talk)
                                format.contains("lightning", ignoreCase = true) -> stringResource(R.string.format_lightning_talk)
                                format.contains("workshop", ignoreCase = true) -> stringResource(R.string.format_workshop)
                                else -> format
                            }
                            val icon = when {
                                format.contains("presentation", ignoreCase = true) -> Icons.Outlined.RecordVoiceOver
                                format.contains("lightning", ignoreCase = true) -> Icons.Outlined.Bolt
                                else -> Icons.Outlined.Construction
                            }
                            CompactFilterToggle(
                                selected = isSelected,
                                onClick = { onFormatSelected(if (isSelected) null else format) }
                            ) { selected ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(18.dp)
                                )
                                ExpandableChipLabel(visible = selected) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        availableLanguages.forEach { lang ->
                            val isSelected = selectedLanguage == lang
                            val isNorwegian = lang.contains("no", ignoreCase = true)
                            CompactFilterToggle(
                                selected = isSelected,
                                onClick = { onLanguageSelected(if (isSelected) null else lang) }
                            ) { selected ->
                                Text(
                                    text = if (isNorwegian) "🇳🇴" else "🇬🇧",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                ExpandableChipLabel(visible = selected) {
                                    Text(
                                        text = if (isNorwegian) "NO" else "EN",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        if (availableRooms.isNotEmpty()) {
                            CompactFilterToggle(
                                selected = selectedRoom != null,
                                onClick = { isRoomPickerVisible = true }
                            ) { selected ->
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = stringResource(R.string.filter_rooms),
                                    tint = LocalContentColor.current,
                                    modifier = Modifier.size(18.dp)
                                )
                                ExpandableChipLabel(visible = selected) {
                                    Text(
                                        text = selectedRoom.orEmpty(),
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isRoomPickerVisible) {
                RoomPickerSheet(
                    rooms = availableRooms,
                    selectedRoom = selectedRoom,
                    onRoomSelected = onRoomSelected,
                    onDismiss = { isRoomPickerVisible = false }
                )
            }
        }
    }
}
