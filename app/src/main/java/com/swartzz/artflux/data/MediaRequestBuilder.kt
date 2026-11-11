package com.swartzz.artflux.data

import com.swartzz.artflux.model.MediaSourceConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

object MediaRequestBuilder {

    fun buildFetchRequest(
        source: MediaSourceConfig,
        query: String = "",
        page: Int = 1
    ): Request {
        val urlBuilder = source.apiUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Invalid API URL: ${source.apiUrl}")

        val isWaifu = source.isWaifuFamily

        if (isWaifu) {
            // Waifu.im API v7 (community instances such as api.waifu.best).
            // The live API binds these parameters case-insensitively, but the
            // documented casing is used so future strict servers keep working.
            val actualPage = (page - 1) + source.pageStartsAt
            urlBuilder.addQueryParameter("page", actualPage.toString())
            urlBuilder.addQueryParameter("pageSize", source.defaultPageSize.toString())

            var cleanQuery = query.trim()
            // NSFW toggle arrives as a pseudo tag from the rating filters
            // ("isNsfw:false" / "isNsfw:true"; legacy configs may still carry
            // "is_nsfw:..."). It must be translated to the boolean IsNsfw
            // parameter, otherwise the filter is silently ignored.
            val nsfwRegex = Regex("is[_]?nsfw:(true|false)", RegexOption.IGNORE_CASE)
            nsfwRegex.find(cleanQuery)?.let { match ->
                cleanQuery = cleanQuery.replace(match.value, "", ignoreCase = true).trim()
                urlBuilder.addQueryParameter("isNsfw", match.groupValues[1].lowercase())
            }

            if (cleanQuery.isNotBlank()) {
                val tagTokens = cleanQuery.split("[\\s,]+".toRegex()).filter { it.isNotBlank() }
                for (tag in tagTokens) {
                    urlBuilder.addQueryParameter("includedTags", tag)
                }
            }
        } else {
            // Query parameter
            if (query.isNotBlank() && source.searchParam.isNotBlank()) {
                urlBuilder.addQueryParameter(source.searchParam, query.trim())
            }

            // Pagination parameter
            if (source.pageParam.isNotBlank()) {
                val actualPage = (page - 1) + source.pageStartsAt
                urlBuilder.addQueryParameter(source.pageParam, actualPage.toString())
            }

            // Page size parameter
            if (source.pageSizeParam.isNotBlank()) {
                urlBuilder.addQueryParameter(source.pageSizeParam, source.defaultPageSize.toString())
            }
        }

        // Effective Authentication Query Parameters (multi-parameter support including api_key, user_id, etc.)
        // Withheld on public cleartext HTTP endpoints to avoid transmitting secrets in the clear.
        val authParams = if (TransportSecurity.allowsCredentials(source.apiUrl)) {
            source.getEffectiveAuthQueryParams()
        } else {
            emptyMap()
        }
        for ((key, value) in authParams) {
            if (key.isNotBlank() && value.isNotBlank()) {
                urlBuilder.addQueryParameter(key, value)
            }
        }

        val requestBuilder = Request.Builder()
            .url(urlBuilder.build())
            .addHeader("User-Agent", "ArtfluxApp/2.0 (Android; MediaBrowser)")
            .addHeader("Accept", "application/json")

        if (isWaifu) {
            requestBuilder.addHeader("Accept-Version", "v7")
        }

        // Effective Authentication Headers (Bearer token, custom headers, etc.)
        // Withheld on public cleartext HTTP endpoints to avoid transmitting secrets in the clear.
        val headers = if (TransportSecurity.allowsCredentials(source.apiUrl)) {
            source.getEffectiveHeaders()
        } else {
            emptyMap()
        }
        for ((hName, hVal) in headers) {
            if (hName.isNotBlank() && hVal.isNotBlank()) {
                requestBuilder.addHeader(hName, hVal)
            }
        }

        return requestBuilder.build()
    }
}
