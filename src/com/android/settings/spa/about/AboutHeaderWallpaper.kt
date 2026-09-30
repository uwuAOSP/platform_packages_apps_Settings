/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.spa.about

import android.app.Activity
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.RemoteException
import android.util.Log
import android.view.Window
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
import android.view.WindowManagerGlobal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal data class HeaderWallpaperSource(val live: Boolean, val drawable: Drawable?)

/** A live wallpaper's service thumbnail is an icon/preview, not its current composition. */
internal fun loadHeaderWallpaperSource(
    context: Context,
    manager: WallpaperManager,
): HeaderWallpaperSource = if (manager.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM) != null) {
    val color = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()
        ?: Color.TRANSPARENT
    HeaderWallpaperSource(live = true, drawable = ColorDrawable(color))
} else {
    HeaderWallpaperSource(live = false, drawable = manager.getDrawable(WallpaperManager.FLAG_SYSTEM))
}

/** Reveal only the wallpaper layer while acquiring a frame; restore caller-owned window flags. */
internal class HeaderWallpaperWindow(private val window: Window) : AutoCloseable {
    private val alreadyShown = window.attributes.flags and FLAG_SHOW_WALLPAPER != 0

    init {
        if (!alreadyShown) window.addFlags(FLAG_SHOW_WALLPAPER)
    }

    override fun close() {
        if (!alreadyShown) window.clearFlags(FLAG_SHOW_WALLPAPER)
    }
}

@Composable
internal fun rememberAboutHeaderWallpaper(context: Context): Drawable? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var wallpaper by remember(context) { mutableStateOf<Drawable?>(null) }
    var revision by remember(context) { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { revision++ }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            Context.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(context, lifecycleOwner, revision) {
        // Pausing or leaving the page cancels acquisition and restores the window.
        // Resuming reloads the current wallpaper, including changes made in its editor.
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val source = try {
                withContext(Dispatchers.IO) {
                    loadHeaderWallpaperSource(context, WallpaperManager.getInstance(context))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Unable to load the header wallpaper", exception)
                wallpaper = null
                return@repeatOnLifecycle
            }
            wallpaper = source.drawable
            if (!source.live) return@repeatOnLifecycle
            val window = context.headerActivity()?.window ?: return@repeatOnLifecycle
            val previewWindow = HeaderWallpaperWindow(window)
            try {
                // Settings normally hides the live wallpaper. Allow WM and its engine
                // to produce a visible frame before capturing that layer alone.
                repeat(20) {
                    delay(150)
                    val bitmap = withContext(Dispatchers.IO) {
                        WindowManagerGlobal.getWindowManagerService()?.screenshotWallpaper()
                    }
                    if (bitmap != null) {
                        wallpaper = BitmapDrawable(context.resources, bitmap)
                        return@repeatOnLifecycle
                    }
                }
                // If the engine has no frame, keep its colors; never enlarge its icon.
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: RemoteException) {
                Log.w(TAG, "Unable to capture the live wallpaper layer", exception)
            } catch (exception: RuntimeException) {
                Log.w(TAG, "Unable to capture the live wallpaper layer", exception)
            } finally {
                previewWindow.close()
            }
        }
    }
    return wallpaper
}

private tailrec fun Context.headerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.headerActivity()
    else -> null
}

private const val TAG = "AboutHeaderWallpaper"
