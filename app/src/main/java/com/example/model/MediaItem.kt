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

    private fun isVideoUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val clean = url.substringBefore('?').substringBefore('#').lowercase().trim()
        return clean.endsWith(".mp4") || clean.endsWith(".webm") ||
                clean.endsWith(".mkv") || clean.endsWith(".mov") || clean.endsWith(".avi")
    }

    fun getThumbnailForQuality(quality: ThumbnailQuality): String {
        // Video items must NEVER load a video URL as an image in the feed. Use image preview/poster.
        if (mediaType == MediaType.VIDEO) {
            return when (quality) {
                ThumbnailQuality.Q360, ThumbnailQuality.Q480 -> {
                    when {
                        thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl) -> thumbnailUrl
                        !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl!!
                        !isVideoUrl(imageUrl) -> imageUrl
                        else -> ""
                    }
                }
                ThumbnailQuality.Q720, ThumbnailQuality.Q1080 -> {
                    when {
                        !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl!!
                        thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl) -> thumbnailUrl
                        !isVideoUrl(imageUrl) -> imageUrl
                        else -> ""
                    }
                }
            }
        }

        // GIF items: prefer lightweight image preview/sample for feed performance, never raw video
        if (mediaType == MediaType.GIF) {
            return when (quality) {
                ThumbnailQuality.Q360, ThumbnailQuality.Q480 -> {
                    when {
                        thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl) -> thumbnailUrl
                        !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl!!
                        else -> imageUrl
                    }
                }
                ThumbnailQuality.Q720, ThumbnailQuality.Q1080 -> {
                    when {
                        !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl!!
                        thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl) -> thumbnailUrl
                        else -> imageUrl
                    }
                }
            }
        }

        // Image items: respect 360p/480p/720p/1080p quality preference while avoiding large/original files when preview exists
        return when (quality) {
            ThumbnailQuality.Q360, ThumbnailQuality.Q480 -> {
                if (thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl)) {
                    thumbnailUrl
                } else if (!sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl)) {
                    sampleUrl!!
                } else {
                    imageUrl
                }
            }
            ThumbnailQuality.Q720, ThumbnailQuality.Q1080 -> {
                if (!sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl)) {
                    sampleUrl!!
                } else if (thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl)) {
                    thumbnailUrl
                } else {
                    imageUrl
                }
            }
        }
    }

    fun getThumbnailFallbackUrl(primaryQuality: ThumbnailQuality): String {
        val primary = getThumbnailForQuality(primaryQuality)
        return when {
            primary == thumbnailUrl && !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl!!
            primary == sampleUrl && thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl) -> thumbnailUrl
            thumbnailUrl.isNotBlank() && !isVideoUrl(thumbnailUrl) -> thumbnailUrl
            !sampleUrl.isNullOrBlank() && !isVideoUrl(sampleUrl) -> sampleUrl!!
            mediaType != MediaType.VIDEO && !isVideoUrl(imageUrl) -> imageUrl
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
