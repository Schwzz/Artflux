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
    val imageUrl: String,
    val thumbnailUrl: String,
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
    val aspectRatio: Float
        get() {
            return if (width != null && height != null && height > 0) {
                (width.toFloat() / height.toFloat()).coerceIn(0.6f, 1.8f)
            } else {
                1.0f
            }
        }

    fun getThumbnailForQuality(quality: ThumbnailQuality): String {
        return when (quality) {
            ThumbnailQuality.Q360, ThumbnailQuality.Q480 -> {
                // Best at or below 480p: thumbnailUrl (~150-360p). If missing, sampleUrl (~720p).
                // Never fall back to heavy original file or raw video for feed thumbnails if preview/sample exists.
                if (thumbnailUrl.isNotBlank()) {
                    thumbnailUrl
                } else if (!sampleUrl.isNullOrBlank()) {
                    sampleUrl!!
                } else if (mediaType != MediaType.VIDEO) {
                    imageUrl
                } else {
                    ""
                }
            }
            ThumbnailQuality.Q720 -> {
                // Best around 720p: sampleUrl (~720-1080p). If missing, fall back to thumbnailUrl.
                if (!sampleUrl.isNullOrBlank()) {
                    sampleUrl!!
                } else if (thumbnailUrl.isNotBlank()) {
                    thumbnailUrl
                } else if (mediaType != MediaType.VIDEO) {
                    imageUrl
                } else {
                    ""
                }
            }
            ThumbnailQuality.Q1080 -> {
                // Best at or around 1080p: sampleUrl. If missing, fall back to thumbnailUrl to avoid loading heavy original
                if (!sampleUrl.isNullOrBlank()) {
                    sampleUrl!!
                } else if (thumbnailUrl.isNotBlank()) {
                    thumbnailUrl
                } else if (mediaType != MediaType.VIDEO) {
                    imageUrl
                } else {
                    ""
                }
            }
        }
    }

    fun getThumbnailFallbackUrl(primaryQuality: ThumbnailQuality): String {
        val primary = getThumbnailForQuality(primaryQuality)
        return when {
            primary == thumbnailUrl && !sampleUrl.isNullOrBlank() -> sampleUrl!!
            primary == sampleUrl && thumbnailUrl.isNotBlank() -> thumbnailUrl
            thumbnailUrl.isNotBlank() -> thumbnailUrl
            !sampleUrl.isNullOrBlank() -> sampleUrl!!
            mediaType != MediaType.VIDEO -> imageUrl
            else -> primary
        }
    }

    fun getDownloadUrl(quality: DownloadQuality): String {
        // For animated GIFs and video files, always use full original media url to prevent format degradation
        if (mediaType == MediaType.GIF || mediaType == MediaType.VIDEO) {
            return imageUrl
        }

        return when (quality) {
            DownloadQuality.ORIGINAL -> imageUrl
            DownloadQuality.Q1080 -> sampleUrl?.ifBlank { null } ?: imageUrl
            DownloadQuality.Q720 -> sampleUrl?.ifBlank { null } ?: imageUrl
            DownloadQuality.Q480 -> thumbnailUrl.ifBlank { sampleUrl ?: imageUrl }
            DownloadQuality.Q360 -> thumbnailUrl.ifBlank { sampleUrl ?: imageUrl }
        }
    }
}
