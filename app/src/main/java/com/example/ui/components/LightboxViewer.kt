package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.MediaItem
import com.example.model.MediaRating
import com.example.model.MediaType
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.MagentaAccent
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonIndigoLight
import com.example.ui.theme.RoseBadge
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.util.ArtfluxImageLoader

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun LightboxViewer(
    item: MediaItem,
    currentIndex: Int,
    totalCount: Int,
    itemsList: List<MediaItem> = listOf(item),
    loopVideo: Boolean = true,
    onIndexChanged: (Int) -> Unit = {},
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onClose: () -> Unit,
    onDownload: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val context = LocalContext.current
    val effectiveItems = if (itemsList.isNotEmpty()) itemsList else listOf(item)
    val safeInitialPage = currentIndex.coerceIn(0, (effectiveItems.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitialPage) { effectiveItems.size }

    var showInfoSheet by remember { mutableStateOf(false) }
    var activeZoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) {
        onIndexChanged(pagerState.currentPage)
        activeZoomScale = 1f
    }

    val currentItem = effectiveItems.getOrNull(pagerState.currentPage) ?: item

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
        // Horizontal Pager for fluid swipe navigation
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = activeZoomScale <= 1.05f,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val pageItem = effectiveItems[pageIndex]

            if (pageItem.mediaType == MediaType.VIDEO) {
                // Video Player with ExoPlayer
                key(pageItem.id) {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    val isCurrentPage = pagerState.currentPage == pageIndex

                    val exoPlayer = remember(pageItem.id, isCurrentPage, loopVideo) {
                        if (isCurrentPage) {
                            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                                .setAllowCrossProtocolRedirects(true)
                                .setConnectTimeoutMs(15000)
                                .setReadTimeoutMs(20000)

                            if (pageItem.imageUrl.contains("gelbooru.com")) {
                                httpDataSourceFactory.setDefaultRequestProperties(mapOf("Referer" to "https://gelbooru.com/"))
                            } else if (pageItem.imageUrl.contains("danbooru")) {
                                httpDataSourceFactory.setDefaultRequestProperties(mapOf("Referer" to "https://danbooru.donmai.us/"))
                            }

                            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                                .setDataSourceFactory(httpDataSourceFactory)

                            ExoPlayer.Builder(context)
                                .setMediaSourceFactory(mediaSourceFactory)
                                .build().apply {
                                    val media3Item = Media3Item.fromUri(Uri.parse(pageItem.imageUrl))
                                    setMediaItem(media3Item)
                                    repeatMode = if (loopVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                                    playWhenReady = true
                                    prepare()
                                }
                        } else null
                    }

                    DisposableEffect(pageItem.id, isCurrentPage) {
                        onDispose {
                            exoPlayer?.pause()
                            exoPlayer?.stop()
                            exoPlayer?.clearMediaItems()
                            exoPlayer?.release()
                        }
                    }

                    DisposableEffect(lifecycleOwner, exoPlayer) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                                    exoPlayer?.pause()
                                }
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp, bottom = 80.dp, start = 8.dp, end = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Instant preview placeholder while video prepares/buffers
                        val videoPreviewUrl = pageItem.thumbnailUrl.ifBlank { pageItem.sampleUrl ?: "" }
                        if (videoPreviewUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(videoPreviewUrl)
                                    .crossfade(false)
                                    .build(),
                                imageLoader = ArtfluxImageLoader.get(context),
                                contentDescription = pageItem.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = true
                                        setShowNextButton(false)
                                        setShowPreviousButton(false)
                                        setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { playerView ->
                                    if (playerView.player != exoPlayer) {
                                        playerView.player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // Image / GIF Viewer with Pinch-to-Zoom & Pan
                var scale by remember(pageItem.id) { mutableFloatStateOf(1f) }
                var offsetX by remember(pageItem.id) { mutableFloatStateOf(0f) }
                var offsetY by remember(pageItem.id) { mutableFloatStateOf(0f) }

                LaunchedEffect(scale) {
                    if (pagerState.currentPage == pageIndex) {
                        activeZoomScale = scale
                    }
                }

                // Fullscreen URL selection: For images, use sample URL (~1080p) if available to avoid loading giant raw files
                val fullscreenDisplayUrl = if (pageItem.mediaType == MediaType.GIF) {
                    pageItem.imageUrl
                } else {
                    pageItem.sampleUrl?.takeIf { it.isNotBlank() } ?: pageItem.imageUrl
                }
                val previewThumbnailUrl = pageItem.thumbnailUrl.ifBlank { pageItem.sampleUrl ?: pageItem.imageUrl }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(pageItem.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 2.5f
                                    }
                                }
                            )
                        }
                        .pointerInput(pageItem.id) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val activeChanges = event.changes.filter { it.pressed }

                                    if (activeChanges.size >= 2) {
                                        // Two-finger pinch to zoom & pan
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                                        scale = newScale

                                        if (scale > 1.05f) {
                                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                            offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                                        } else {
                                            scale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                        event.changes.forEach { it.consume() }
                                    } else if (scale > 1.05f) {
                                        // Zoomed in: single finger pan
                                        val pan = event.calculatePan()
                                        val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                        val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                        event.changes.forEach { it.consume() }
                                    } else {
                                        // At 1x scale with 1 finger: DO NOT consume!
                                        // This allows HorizontalPager to handle horizontal swipe navigation seamlessly across images, GIFs, and videos!
                                    }
                                } while (event.changes.any { it.pressed })

                                if (scale <= 1.05f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fullscreenDisplayUrl)
                            .crossfade(true)
                            .placeholderMemoryCacheKey(previewThumbnailUrl)
                            .build(),
                        imageLoader = ArtfluxImageLoader.get(context),
                        contentDescription = pageItem.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 60.dp, bottom = 80.dp, start = 12.dp, end = 12.dp)
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
                                // Instant preview placeholder from already cached feed thumbnail/sample
                                if (previewThumbnailUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(previewThumbnailUrl)
                                            .crossfade(false)
                                            .build(),
                                        imageLoader = ArtfluxImageLoader.get(context),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                CircularProgressIndicator(
                                    color = NeonIndigoLight,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        // Top Bar: Clean, with Close button and Media Type Indicator (NO "1/75" counter)
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = statusBarPadding + 8.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkSurface.copy(alpha = 0.9f))
                    .border(1.dp, CardBorder, CircleShape)
                    .testTag("lightbox_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close viewer",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Media Type Badge
            when (currentItem.mediaType) {
                MediaType.GIF -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MagentaAccent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("GIF", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                MediaType.VIDEO -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyanAccent)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("VIDEO", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }

        // Bottom Action Bar: Info, Share, Download (Comfortable spacing & system insets)
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBarPadding + 16.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(24.dp)),
                color = DarkSurface.copy(alpha = 0.92f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Info Button
                    IconButton(
                        onClick = { showInfoSheet = !showInfoSheet },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (showInfoSheet) NeonIndigo else Color.Transparent)
                            .testTag("lightbox_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Media details",
                            tint = if (showInfoSheet) TextPrimary else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, currentItem.title)
                                putExtra(Intent.EXTRA_TEXT, "${currentItem.title} - ${currentItem.imageUrl}")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Media Link"))
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .testTag("lightbox_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share media",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Download Button
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonIndigo)
                            .testTag("lightbox_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download media file",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Media Details Panel / Sheet with Visible Dismissal Button
        if (showInfoSheet) {
            MediaDetailsSheet(
                item = currentItem,
                onDismiss = { showInfoSheet = false },
                onTagClick = { tag ->
                    showInfoSheet = false
                    onClose()
                    onTagClick(tag)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun MediaDetailsSheet(
    item: MediaItem,
    onDismiss: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = DarkSurface,
        tonalElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = navBarPadding + 16.dp)
                .heightIn(max = 500.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Title and Obvious Visible Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Media Details",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("details_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close details panel",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source, Rating & Type Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source Name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonIndigo.copy(alpha = 0.2f))
                        .border(1.dp, NeonIndigo.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = item.sourceName,
                        color = NeonIndigoLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Rating
                val (ratingColor, ratingText) = when (item.rating) {
                    MediaRating.SAFE -> Pair(EmeraldSafe, "SAFE")
                    MediaRating.SUGGESTIVE -> Pair(Color(0xFFF59E0B), "SUGGESTIVE")
                    MediaRating.ADULT -> Pair(RoseBadge, "ADULT")
                    else -> Pair(TextTertiary, "UNKNOWN")
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ratingColor.copy(alpha = 0.15f))
                        .border(1.dp, ratingColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = ratingText,
                        color = ratingColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Media Type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = item.mediaType.label,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title & Author & Post Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    if (!item.author.isNullOrBlank()) {
                        Text(
                            text = "Artist / Author: ${item.author}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

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
                            .background(DarkSurfaceVariant)
                            .testTag("details_open_browser_button")
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

            Spacer(modifier = Modifier.height(14.dp))

            // Organized Metadata Section
            Text(
                text = "METADATA",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetadataRow(label = "Post ID", value = item.id)

                    if (item.width != null && item.height != null) {
                        MetadataRow(label = "Resolution", value = "${item.width} × ${item.height}")
                    }

                    if (!item.fileExt.isNullOrBlank()) {
                        MetadataRow(label = "File Format", value = item.fileExt.uppercase())
                    }

                    if (item.fileSize != null && item.fileSize > 0) {
                        val formattedSize = if (item.fileSize > 1024 * 1024) {
                            String.format("%.1f MB", item.fileSize / (1024.0 * 1024.0))
                        } else {
                            "${item.fileSize / 1024} KB"
                        }
                        MetadataRow(label = "File Size", value = formattedSize)
                    }

                    MetadataRow(label = "Source", value = item.sourceName)
                }
            }

            // Description / Alt text
            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = item.description,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Scrollable Tags Section
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TAGS (${item.tags.size})",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                                .background(DarkSurfaceVariant)
                                .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                .clickable { onTagClick(tag) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("detail_tag_chip_$tag")
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

@Composable
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
