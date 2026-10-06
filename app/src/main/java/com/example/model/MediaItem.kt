package com.example.model

enum class MediaRating(val label: String) {
    ALL("All Ratings"),
    SAFE("Safe"),
    SUGGESTIVE("Suggestive"),
    ADULT("Adult"),
    UNKNOWN("Unknown");

    companion object {
        val QUESTIONABLE: MediaRating get() = SUGGESTIVE
        val EXPLICIT: MediaRating get() = ADULT
    }
}

enum class MediaType(val label: String) {
    ALL("All Types"),
    IMAGE("Image"),
    GIF("GIF"),
    VIDEO("Video")
}

enum class Orientation(val label: String) {
    ALL("Any"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    SQUARE("Square")
}

enum class SortOption(val label: String) {
    LATEST("Latest"),
    POPULAR("Popular"),
    TOP_RATED("Top Rated"),
    RANDOM("Random")
}

enum class ThumbnailQuality(val label: String, val resolution: Int) {
    Q360("360p", 360),
    Q480("480p", 480),
    Q720("720p", 720),
    Q1080("1080p", 1080);

    companion object {
        val DEFAULT = Q720
    }
}

enum class DownloadQuality(val label: String, val description: String) {
    Q360("360p", "Preview resolution (~360p)"),
    Q480("480p", "Standard resolution (~480p)"),
    Q720("720p", "HD sample (~720p)"),
    Q1080("1080p", "Full HD sample (~1080p)"),
    ORIGINAL("Original", "Full original resolution");

    companion object {
        val DEFAULT = ORIGINAL
    }
}

enum class AppTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        val DEFAULT = DARK
    }
}

enum class ColorPalette(val label: String, val subtitle: String) {
    ARCTIC_SIGNAL("Arctic Signal", "Electric Cyan, Violet & Coral"),
    EMERALD_NOIR("Emerald Noir", "Emerald, Sapphire & Gold"),
    CRIMSON_FLUX("Crimson Flux", "Crimson, Electric Blue & Amber");

    companion object {
        val DEFAULT = ARCTIC_SIGNAL
    }
}

data class FilterState(
    val sort: SortOption = SortOption.LATEST,
    val rating: MediaRating = MediaRating.ALL,
    val orientation: Orientation = Orientation.ALL,
    val mediaType: MediaType = MediaType.ALL
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (sort != SortOption.LATEST) count++
            if (rating != MediaRating.ALL) count++
            if (orientation != Orientation.ALL) count++
            if (mediaType != MediaType.ALL) count++
            return count
        }
}

data class MediaItem(
    val id: String,
    val title: String,
    val actualMediaUrl: String,
    val previewUrl: String,
    val sampleUrl: String? = null,
    val postUrl: String? = null,
    val tags: List<String> = emptyList(),
    val rating: MediaRating = MediaRating.SAFE,
    val mediaType: MediaType = MediaType.IMAGE,
    val width: Int? = null,
    val height: Int? = null,
    val author: String? = null,
    val sourceName: String = "",
    val description: String? = null,
    val fileSize: Long? = null,
    val fileExt: String? = null,
    val score: Int? = null,
    val favorites: Int? = null,
    val views: Int? = null
) {
    // Backward compatibility accessors for existing consumers
    val imageUrl: String get() = actualMediaUrl
    val thumbnailUrl: String get() = previewUrl

    val aspectRatio: Float
        get() {
            return if (width != null && height != null && height > 0) {
                (width.toFloat() / height.toFloat()).coerceIn(0.6f, 1.8f)
            } else {
                1.0f
            }
        }

    private fun isVideoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.substringBefore('?').substringBefore('#').lowercase().trim()
        return clean.endsWith(".mp4") || clean.endsWith(".webm") ||
                clean.endsWith(".mkv") || clean.endsWith(".mov") || clean.endsWith(".avi")
    }

    /**
     * Deterministic feed thumbnail resolution:
     * - VIDEO: Always static preview image; never a video file.
     * - GIF: Always static preview image; avoids heavy animated downloads on feed grid.
     * - IMAGE: Preview image (or sample if higher quality chosen and valid).
     */
    fun getThumbnailForQuality(quality: ThumbnailQuality): String {
        // Video items must NEVER load a video URL as an image in the feed. Use static preview/poster.
        if (mediaType == MediaType.VIDEO) {
            return if (!isVideoUrl(previewUrl)) {
                if (quality in listOf(ThumbnailQuality.Q720, ThumbnailQuality.Q1080) && !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl)) {
                    sampleUrl
                } else {
                    previewUrl
                }
            } else ""
        }

        // GIF items: feed uses lightweight static preview image to keep scrolling fast and fluid
        if (mediaType == MediaType.GIF) {
            return if (quality in listOf(ThumbnailQuality.Q720, ThumbnailQuality.Q1080) && !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl)) {
                sampleUrl
            } else if (!isVideoUrl(previewUrl)) {
                previewUrl
            } else ""
        }

        // Image items: respect 360p/480p/720p/1080p quality preference while avoiding large/original files when preview exists
        return when (quality) {
            ThumbnailQuality.Q360, ThumbnailQuality.Q480 -> {
                if (previewUrl.isNotBlank() && !isVideoUrl(previewUrl)) {
                    previewUrl
                } else if (!sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl)) {
                    sampleUrl
                } else if (!isVideoUrl(actualMediaUrl)) {
                    actualMediaUrl
                } else ""
            }
            ThumbnailQuality.Q720, ThumbnailQuality.Q1080 -> {
                if (!sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl)) {
                    sampleUrl
                } else if (previewUrl.isNotBlank() && !isVideoUrl(previewUrl)) {
                    previewUrl
                } else if (!isVideoUrl(actualMediaUrl)) {
                    actualMediaUrl
                } else ""
            }
        }
    }

    fun getThumbnailFallbackUrl(primaryQuality: ThumbnailQuality): String {
        val primary = getThumbnailForQuality(primaryQuality)
        return when {
            primary == previewUrl && !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl
            primary == sampleUrl && previewUrl.isNotBlank() && !isVideoUrl(previewUrl) -> previewUrl
            previewUrl.isNotBlank() && !isVideoUrl(previewUrl) -> previewUrl
            !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl
            mediaType != MediaType.VIDEO && !isVideoUrl(actualMediaUrl) -> actualMediaUrl
            else -> primary
        }
    }

    fun getDownloadUrl(quality: DownloadQuality): String {
        // For animated GIFs and video files, always use the real original media URL
        if (mediaType == MediaType.GIF || mediaType == MediaType.VIDEO) {
            return actualMediaUrl
        }

        return when (quality) {
            DownloadQuality.ORIGINAL -> actualMediaUrl
            DownloadQuality.Q1080 -> sampleUrl?.ifBlank { null } ?: actualMediaUrl
            DownloadQuality.Q720 -> sampleUrl?.ifBlank { null } ?: actualMediaUrl
            DownloadQuality.Q480 -> previewUrl.ifBlank { sampleUrl ?: actualMediaUrl }
            DownloadQuality.Q360 -> previewUrl.ifBlank { sampleUrl ?: actualMediaUrl }
        }
    }
}
