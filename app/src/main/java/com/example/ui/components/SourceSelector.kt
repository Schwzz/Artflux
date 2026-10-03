package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaSourceConfig
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonIndigoLight
import com.example.ui.theme.RoseBadge
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun SourceSelectorBar(
    activeSource: MediaSourceConfig,
    onOpenPicker: () -> Unit,
    onAddSourceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active Source Selector Chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenPicker)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("source_selector_button"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getSourceIcon(activeSource),
                contentDescription = "Source icon",
                tint = NeonIndigoLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "SOURCE",
                    color = TextTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = activeSource.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Open source list",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        // Add Source Button
        Button(
            onClick = onAddSourceClick,
            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(40.dp)
                .testTag("add_source_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Source icon",
                tint = TextPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Add Source",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcePickerBottomSheet(
    sources: List<MediaSourceConfig>,
    activeSource: MediaSourceConfig,
    onSelectSource: (MediaSourceConfig) -> Unit,
    onDeleteSource: (String) -> Unit,
    onAddNewSource: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Media Source",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        onDismiss()
                        onAddNewSource()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("sheet_add_source_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Source",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Source", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Built-in Sources
                item {
                    Text(
                        text = "BUILT-IN SOURCES",
                        color = NeonIndigoLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(sources.filter { it.isBuiltIn }) { source ->
                    SourceItemCard(
                        source = source,
                        isActive = source.id == activeSource.id,
                        onClick = {
                            onSelectSource(source)
                            onDismiss()
                        },
                        onDelete = null
                    )
                }

                // Custom Sources
                val customSources = sources.filter { !it.isBuiltIn }
                if (customSources.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = CardBorder
                        )
                        Text(
                            text = "CUSTOM SOURCES",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(customSources) { source ->
                        SourceItemCard(
                            source = source,
                            isActive = source.id == activeSource.id,
                            onClick = {
                                onSelectSource(source)
                                onDismiss()
                            },
                            onDelete = { onDeleteSource(source.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SourceItemCard(
    source: MediaSourceConfig,
    isActive: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isActive) NeonIndigo else CardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .testTag("source_item_${source.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) DarkSurfaceVariant else DarkBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isActive) NeonIndigo.copy(alpha = 0.2f) else DarkSurface)
                        .border(1.dp, if (isActive) NeonIndigoLight else CardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getSourceIcon(source),
                        contentDescription = null,
                        tint = if (isActive) NeonIndigoLight else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = source.name,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!source.isBuiltIn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CyanAccent.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "CUSTOM",
                                    color = CyanAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (source.description.isNotBlank()) {
                        Text(
                            text = source.description,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active source",
                        tint = NeonIndigoLight,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 4.dp)
                    )
                }

                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete custom source",
                            tint = RoseBadge,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

fun getSourceIcon(source: MediaSourceConfig): ImageVector {
    return when {
        source.id.contains("safebooru") || source.name.contains("safebooru", true) -> Icons.Default.AutoAwesome
        source.name.contains("danbooru", true) || source.name.contains("booru", true) -> Icons.Default.PhotoLibrary
        else -> Icons.Default.Dns
    }
}
