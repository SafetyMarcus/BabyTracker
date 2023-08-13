package com.babytracker

import App
import AppTheme
import MainViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import cachedLoader
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.imageLoader
import okio.Path.Companion.toOkioPath

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        cachedLoader = ImageLoader {
            components {
                setupDefaultComponents(this@MainActivity)
            }
            interceptor {
                memoryCacheConfig {
                    // Set the max size to 25% of the app's available memory.
                    maxSizePercent(this@MainActivity, 0.25)
                }
                diskCacheConfig {
                    directory(this@MainActivity.cacheDir.resolve("image_cache").toOkioPath())
                    maxSizeBytes(512L * 1024 * 1024) // 512MB
                }
            }
        }
        setContent {
            AppTheme {
                App(viewModel = viewModel)
            }
        }
    }
}