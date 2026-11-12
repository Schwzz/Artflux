package com.swartzz.artflux.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.swartzz.artflux.model.MediaItem
import com.swartzz.artflux.util.ArtfluxImageLoader
import com.swartzz.artflux.util.ArtfluxNetwork

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
fun VideoPlayerContent(
    item: MediaItem,
    isCurrentPage: Boolean,
    loopVideo: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var playbackError by remember(item.id) { mutableStateOf<String?>(null) }
    var videoRetryTrigger by remember(item.id) { mutableIntStateOf(0) }

    var buffering by remember(item.libraryKey) { mutableStateOf(true) }

    val exoPlayer = remember(item.id, item.actualMediaUrl, isCurrentPage, videoRetryTrigger) {
        if (isCurrentPage) {
            val requestHeaders = ArtfluxNetwork.getHeadersForUrl(item.actualMediaUrl)

            val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                .setUserAgent(ArtfluxNetwork.DEFAULT_USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(20000)
                .setDefaultRequestProperties(requestHeaders)

            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

            // Allow ExoPlayer to fall back to alternative decoders when the primary
            // hardware decoder cannot initialize (for example a 3840x1600 60fps AVC
            // stream exceeding OMX.qcom.video.decoder.avc capabilities). Without this
            // the player reports ERROR_CODE_DECODER_INIT_FAILED instead of trying the
            // software decoder.
            val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)

            ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                    val media3ItemBuilder = Media3Item.Builder().setUri(Uri.parse(item.actualMediaUrl))
                    // Prefer the API-provided file extension metadata, then fall back to the URL.
                    val extension = item.fileExt?.lowercase()?.trim()?.removePrefix(".")?.takeIf { it.isNotBlank() }
                        ?: item.actualMediaUrl.substringBefore('?').substringBefore('#').substringAfterLast('.', "").lowercase()
                    when (extension) {
                        "mp4" -> media3ItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MP4)
                        "webm" -> media3ItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_WEBM)
                        "mkv" -> media3ItemBuilder.setMimeType(androidx.media3.common.MimeTypes.VIDEO_MATROSKA)
                    }
                    setMediaItem(media3ItemBuilder.build())
                    repeatMode = if (loopVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onPlayerError(error: PlaybackException) {
                            buffering = false
                            playbackError = MediaLoadMessage.forError(error, item.actualMediaUrl.startsWith("content:") || item.actualMediaUrl.startsWith("file:"))
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            buffering = playbackState == Player.STATE_BUFFERING
                            if (playbackState == Player.STATE_READY) {
                                playbackError = null
                            }
                        }
                    })
                    prepare()
                }
        } else null
    }

    LaunchedEffect(exoPlayer, loopVideo) {
        exoPlayer?.repeatMode = if (loopVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    DisposableEffect(exoPlayer) {
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
        modifier = modifier
            .fillMaxSize()
            .padding(top = 70.dp, bottom = 150.dp, start = 8.dp, end = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Instant preview placeholder while video prepares/buffers.
        // Uses the shared thumbnail helper so a video URL is never loaded as an image.
        val videoPreviewUrl = item.getThumbnailForQuality(com.swartzz.artflux.model.ThumbnailQuality.DEFAULT)
        if (videoPreviewUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(videoPreviewUrl)
                    .crossfade(false)
                    .build(),
                imageLoader = ArtfluxImageLoader.get(context),
                contentDescription = item.title,
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

        if (isCurrentPage && buffering && playbackError == null) CircularProgressIndicator(Modifier.align(Alignment.Center).size(32.dp))

        // Playback error overlay
        if (playbackError != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Playback Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Playback Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = playbackError ?: "Unable to stream media",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                    FilledTonalButton(
                        onClick = {
                            playbackError = null
                            buffering = true
                            videoRetryTrigger++
                        },
                        modifier = Modifier.testTag("video_retry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Retry Playback")
                    }
                }
            }
        }
    }
}
