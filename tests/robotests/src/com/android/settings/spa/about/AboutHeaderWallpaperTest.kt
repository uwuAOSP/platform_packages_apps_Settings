/*
 * Copyright (C) 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.spa.about

import android.app.Activity
import android.app.WallpaperInfo
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AboutHeaderWallpaperTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = mock(WallpaperManager::class.java)

    @Test
    fun staticWallpaper_usesSystemImage() {
        val image = ColorDrawable(Color.BLUE)
        `when`(manager.getDrawable(WallpaperManager.FLAG_SYSTEM)).thenReturn(image)

        val source = loadHeaderWallpaperSource(context, manager)

        assertThat(source.live).isFalse()
        assertThat(source.drawable).isSameInstanceAs(image)
    }

    @Test
    fun liveWallpaper_neverLoadsServiceThumbnailOrUnrelatedStaticImage() {
        val info = mock(WallpaperInfo::class.java)
        `when`(manager.getWallpaperInfo(WallpaperManager.FLAG_SYSTEM)).thenReturn(info)

        val source = loadHeaderWallpaperSource(context, manager)

        assertThat(source.live).isTrue()
        assertThat(source.drawable).isInstanceOf(ColorDrawable::class.java)
        verify(info, never()).loadThumbnail(context.packageManager)
        verify(manager, never()).getDrawable(WallpaperManager.FLAG_SYSTEM)
    }

    @Test
    fun previewWindow_restoresOnlyTheFlagItAdded() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val window = activity.get().window
            window.addFlags(FLAG_KEEP_SCREEN_ON)
            val original = window.attributes.flags

            val preview = HeaderWallpaperWindow(window)
            assertThat(window.attributes.flags and FLAG_SHOW_WALLPAPER).isNotEqualTo(0)
            preview.close()

            assertThat(window.attributes.flags).isEqualTo(original)
        } finally {
            activity.pause().stop().destroy()
        }
    }

    @Test
    fun previewWindow_preservesWallpaperFlagOwnedByTheCaller() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val window = activity.get().window
            window.addFlags(FLAG_SHOW_WALLPAPER)
            val original = window.attributes.flags

            HeaderWallpaperWindow(window).close()

            assertThat(window.attributes.flags).isEqualTo(original)
        } finally {
            activity.pause().stop().destroy()
        }
    }
}
