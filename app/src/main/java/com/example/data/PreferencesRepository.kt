package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ThumbnailQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("artflux_preferences", Context.MODE_PRIVATE)

    private val _isDarkAmoled = MutableStateFlow(prefs.getBoolean(KEY_DARK_AMOLED, true))
    val isDarkAmoled: StateFlow<Boolean> = _isDarkAmoled.asStateFlow()

    private val _thumbnailQuality = MutableStateFlow(
        try {
            ThumbnailQuality.valueOf(prefs.getString(KEY_THUMBNAIL_QUALITY, ThumbnailQuality.DEFAULT.name) ?: ThumbnailQuality.DEFAULT.name)
        } catch (e: Exception) {
            ThumbnailQuality.DEFAULT
        }
    )
    val thumbnailQuality: StateFlow<ThumbnailQuality> = _thumbnailQuality.asStateFlow()

    private val _loopVideo = MutableStateFlow(prefs.getBoolean(KEY_LOOP_VIDEO, true))
    val loopVideo: StateFlow<Boolean> = _loopVideo.asStateFlow()

    fun setDarkAmoled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_AMOLED, enabled).apply()
        _isDarkAmoled.value = enabled
    }

    fun setThumbnailQuality(quality: ThumbnailQuality) {
        prefs.edit().putString(KEY_THUMBNAIL_QUALITY, quality.name).apply()
        _thumbnailQuality.value = quality
    }

    fun setLoopVideo(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOOP_VIDEO, enabled).apply()
        _loopVideo.value = enabled
    }

    companion object {
        private const val KEY_DARK_AMOLED = "dark_amoled_theme"
        private const val KEY_THUMBNAIL_QUALITY = "thumbnail_quality"
        private const val KEY_LOOP_VIDEO = "loop_video_playback"
    }
}
