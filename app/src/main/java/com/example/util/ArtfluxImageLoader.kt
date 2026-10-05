package com.example.util

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ArtfluxImageLoader {
    @Volatile
    private var instance: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .addInterceptor { chain ->
                            val originalRequest = chain.request()
                            val host = originalRequest.url.host.lowercase()
                            val requestBuilder = originalRequest.newBuilder()

                            requestBuilder.header(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                            )

                            // Anti-hotlink Referer headers for Booru CDNs (prevents HTTP 403 Forbidden)
                            if (host.contains("gelbooru.com")) {
                                requestBuilder.header("Referer", "https://gelbooru.com/")
                            } else if (host.contains("danbooru") || host.contains("donmai.us")) {
                                requestBuilder.header("Referer", "https://danbooru.donmai.us/")
                            } else if (host.contains("safebooru.org") || host.contains("safebooru")) {
                                requestBuilder.header("Referer", "https://safebooru.org/")
                            } else if (host.contains("yande.re")) {
                                requestBuilder.header("Referer", "https://yande.re/")
                            } else {
                                try {
                                    requestBuilder.header("Referer", "${originalRequest.url.scheme}://${originalRequest.url.host}/")
                                } catch (_: Exception) {}
                            }

                            chain.proceed(requestBuilder.build())
                        }
                        .build()
                }
                .components {
                    if (Build.VERSION.SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("image_cache"))
                        .maxSizeBytes(150L * 1024 * 1024) // 150MB
                        .build()
                }
                .crossfade(true)
                .build().also { instance = it }
        }
    }
}
