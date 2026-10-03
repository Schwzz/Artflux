package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FilterState
import com.example.model.MediaRating
import com.example.model.MediaType
import com.example.model.Orientation
import com.example.model.SortOption
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonIndigoLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

val PRESET_TAGS = listOf(
    "Cyberpunk",
    "Anime",
    "Fantasy",
    "Wallpaper",
    "Space",
    "Architecture",
    "Abstract",
    "Nature",
    "Retro",
    "Minimal"
)

@Composable
fun SearchBarAndFilters(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    filterState: FilterState,
    onFilterChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_text_input"),
            placeholder = {
                Text(
                    text = "Search tags, artists, concepts...",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search icon",
                    tint = NeonIndigoLight,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                onSearch()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Filter Button with Badge
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("open_filters_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (filterState.activeFilterCount > 0) {
                                    Badge(
                                        containerColor = NeonIndigo,
                                        contentColor = TextPrimary
                                    ) {
                                        Text("${filterState.activeFilterCount}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Open filters",
                                tint = if (filterState.activeFilterCount > 0) NeonIndigoLight else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = NeonIndigo,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                onSearch()
            })
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Preset Tags Horizontal Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PRESET_TAGS.forEach { tag ->
                val isSelected = searchQuery.equals(tag, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NeonIndigoLight else CardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            if (isSelected) {
                                onQueryChange("")
                            } else {
                                onQueryChange(tag)
                            }
                            onSearch()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("tag_chip_$tag")
                ) {
                    Text(
                        text = "#$tag",
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterModalDialog(
            currentFilter = filterState,
            onDismiss = { showFilterDialog = false },
            onApply = { newFilters ->
                onFilterChange(newFilters)
                showFilterDialog = false
            }
        )
    }
}

@Composable
fun FilterModalDialog(
    currentFilter: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit
) {
    var sort by remember { mutableStateOf(currentFilter.sort) }
    var rating by remember { mutableStateOf(currentFilter.rating) }
    var orientation by remember { mutableStateOf(currentFilter.orientation) }
    var mediaType by remember { mutableStateOf(currentFilter.mediaType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter & Sort Media",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = {
                        sort = SortOption.LATEST
                        rating = MediaRating.ALL
                        orientation = Orientation.ALL
                        mediaType = MediaType.ALL
                    }
                ) {
                    Text("Reset", color = NeonIndigoLight, fontSize = 13.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sort Section
                FilterSectionHeader("Sort Order")
                FilterChipGrid(
                    options = SortOption.values().toList(),
                    selected = sort,
                    labelExtractor = { it.label },
                    onSelect = { sort = it }
                )

                // Rating Section
                FilterSectionHeader("Content Rating")
                FilterChipGrid(
                    options = MediaRating.values().filter { it != MediaRating.UNKNOWN },
                    selected = rating,
                    labelExtractor = { it.label },
                    onSelect = { rating = it }
                )

                // Orientation Section
                FilterSectionHeader("Orientation")
                FilterChipGrid(
                    options = Orientation.values().toList(),
                    selected = orientation,
                    labelExtractor = { it.label },
                    onSelect = { orientation = it }
                )

                // Media Type Section
                FilterSectionHeader("Media Type")
                FilterChipGrid(
                    options = MediaType.values().toList(),
                    selected = mediaType,
                    labelExtractor = { it.label },
                    onSelect = { mediaType = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(FilterState(sort, rating, orientation, mediaType))
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("apply_filters_button")
            ) {
                Text("Apply Filters", color = TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = NeonIndigoLight,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
}

@Composable
fun <T> FilterChipGrid(
    options: List<T>,
    selected: T,
    labelExtractor: (T) -> String,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) NeonIndigo else DarkSurfaceVariant)
                    .border(
                        1.dp,
                        if (isSelected) NeonIndigoLight else CardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = labelExtractor(option),
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
