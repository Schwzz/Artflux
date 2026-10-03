package com.example.model

enum class MediaRating(val label: String) {
    ALL("All Ratings"),
    SAFE("Safe"),
    QUESTIONABLE("Questionable"),
    EXPLICIT("Explicit"),
    UNKNOWN("Unknown")
}

enum class MediaType(val label: String) {
    ALL("All Types"),
    IMAGE("Image"),
    GIF("GIF"),
    VIDEO("Video"),
    ART("Digital Art")
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
    val postUrl: String? = null,
    val tags: List<String> = emptyList(),
    val rating: MediaRating = MediaRating.SAFE,
    val mediaType: MediaType = MediaType.IMAGE,
    val width: Int? = null,
    val height: Int? = null,
    val author: String? = null,
    val sourceName: String = "",
    val description: String? = null
) {
    val aspectRatio: Float
        get() {
            return if (width != null && height != null && height > 0) {
                (width.toFloat() / height.toFloat()).coerceIn(0.6f, 1.8f)
            } else {
                1.0f
            }
        }
}
