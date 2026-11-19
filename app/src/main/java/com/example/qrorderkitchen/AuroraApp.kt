package com.example.qrorderkitchen

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.qrorderkitchen.utils.SoundManager

/**
 * Custom Application class optimizing Coil image loading with dedicated memory and disk caches,
 * ensuring butter-smooth scrolling and zero-lag image rendering in menus.
 */
class AuroraApp : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: AuroraApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SoundManager.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use up to 25% of available app RAM for bitmap caching
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024) // 64 MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Cache images aggressively even if server omits Cache-Control
            .networkCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }
}
