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
    val fileExt: String? = null
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
                thumbnailUrl.ifBlank { sampleUrl ?: imageUrl }
            }
            ThumbnailQuality.Q720 -> {
                sampleUrl?.ifBlank { null } ?: thumbnailUrl.ifBlank { imageUrl }
            }
            ThumbnailQuality.Q1080 -> {
                sampleUrl?.ifBlank { null } ?: imageUrl.ifBlank { thumbnailUrl }
            }
        }
    }
}
