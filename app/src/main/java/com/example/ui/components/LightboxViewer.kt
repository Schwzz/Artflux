package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaType
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonIndigoLight
import com.example.ui.theme.RoseBadge
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.ArtfluxImageLoader

@kotlin.OptIn(ExperimentalLayoutApi::class)
@OptIn(UnstableApi::class)
@Composable
fun LightboxViewer(
    item: MediaItem,
    currentIndex: Int,
    totalCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val context = LocalContext.current
    var showInfoSheet by remember { mutableStateOf(false) }

    // Pinch-to-zoom & pan states for images/GIFs
    var scale by remember(item.id) { mutableFloatStateOf(1f) }
    var offsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var offsetY by remember(item.id) { mutableFloatStateOf(0f) }

    BackHandler {
        if (showInfoSheet) {
            showInfoSheet = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground.copy(alpha = 0.98f))
            .testTag("lightbox_viewer")
    ) {
        // Main Media Display: Video (ExoPlayer) or Image/GIF (Coil)
        if (item.mediaType == MediaType.VIDEO) {
            // Media3 / ExoPlayer Video Player
            val exoPlayer = remember(item.id) {
                ExoPlayer.Builder(context).build().apply {
                    val media3Item = Media3Item.fromUri(Uri.parse(item.imageUrl))
                    setMediaItem(media3Item)
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = true
                    prepare()
                }
            }

            DisposableEffect(item.id) {
                onDispose {
                    exoPlayer.stop()
                    exoPlayer.release()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 70.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Image / Animated GIF Display with Pinch-to-Zoom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(item.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    imageLoader = ArtfluxImageLoader.get(context),
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 60.dp, horizontal = 12.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        ),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeonIndigoLight,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                )
            }
        }

        // Top Navigation & Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(DarkBackground.copy(alpha = 0.85f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .testTag("lightbox_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close lightbox",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${currentIndex + 1} / $totalCount",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Media Type Badge in Top Bar
                Spacer(modifier = Modifier.width(8.dp))
                when (item.mediaType) {
                    MediaType.GIF -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MagentaAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("GIF", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    MediaType.VIDEO -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanAccent)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("VIDEO", color = DarkBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {}
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Info Sheet Toggle
                IconButton(
                    onClick = { showInfoSheet = !showInfoSheet },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showInfoSheet) NeonIndigo else DarkSurface)
                        .testTag("lightbox_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Toggle info",
                        tint = if (showInfoSheet) TextPrimary else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Share Button
                IconButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, item.title)
                            putExtra(Intent.EXTRA_TEXT, "${item.title} - ${item.imageUrl}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Media Link"))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .testTag("lightbox_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share media",
                        tint = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Download Button
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(NeonIndigo)
                        .testTag("lightbox_download_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download media file",
                        tint = TextPrimary
                    )
                }
            }
        }

        // Floating Prev Button
        if (currentIndex > 0) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .border(1.dp, CardBorder, CircleShape)
                    .testTag("lightbox_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous media",
                    tint = TextPrimary
                )
            }
        }

        // Floating Next Button
        if (currentIndex < totalCount - 1) {
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.85f))
                    .border(1.dp, CardBorder, CircleShape)
                    .testTag("lightbox_next_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next media",
                    tint = TextPrimary
                )
            }
        }

        // Bottom Info Sheet Overlay
        if (showInfoSheet) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                color = DarkSurface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            if (!item.author.isNullOrBlank()) {
                                Text(
                                    text = "By ${item.author}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NeonIndigoLight,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Open post in browser
                        if (!item.postUrl.isNullOrBlank()) {
                            IconButton(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(item.postUrl))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface.copy(alpha = 0.8f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = "Open post in browser",
                                    tint = NeonIndigoLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Metadata details row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source Name Chip
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.sourceName,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Rating Chip
                        val (ratingColor, ratingText) = when (item.rating) {
                            MediaRating.SAFE -> Pair(EmeraldSafe, "SAFE")
                            MediaRating.SUGGESTIVE -> Pair(Color(0xFFF59E0B), "SUGGESTIVE")
                            MediaRating.ADULT -> Pair(RoseBadge, "ADULT")
                            else -> Pair(TextTertiary, "UNKNOWN")
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = ratingText,
                                color = ratingColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Resolution Chip
                        if (item.width != null && item.height != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${item.width} × ${item.height}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (!item.description.isNullOrBlank()) {
                        Text(
                            text = item.description,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }

                    // Tags FlowRow
                    if (item.tags.isNotEmpty()) {
                        Text(
                            text = "TAGS",
                            color = TextTertiary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E293B))
                                        .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                        .clickable {
                                            onClose()
                                            onTagClick(tag)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        color = NeonIndigoLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
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
