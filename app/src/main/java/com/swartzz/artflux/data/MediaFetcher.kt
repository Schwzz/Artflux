package com.swartzz.artflux.data

import com.swartzz.artflux.model.MediaItem
import com.swartzz.artflux.model.MediaSourceConfig
import com.swartzz.artflux.model.SourceDiagnosticReport

interface MediaFetcher {
    suspend fun fetchMedia(source: MediaSourceConfig, query: String = "", page: Int = 1): Result<List<MediaItem>>
    suspend fun diagnoseSource(source: MediaSourceConfig): SourceDiagnosticReport
}
