/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.media;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AppVolume;
import android.media.AudioManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.settings.R;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.slider.TickVisibilityMode;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Renders active applications with wide platform-style volume sliders. */
public class AppVolumePanelView extends LinearLayout {

    private static final String TAG = "AppVolumePanelView";

    private final AudioManager mAudioManager;
    private final PackageManager mPackageManager;
    private final NumberFormat mPercentFormat = NumberFormat.getPercentInstance();

    public AppVolumePanelView(Context context) {
        this(context, null);
    }

    public AppVolumePanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        mAudioManager = context.getSystemService(AudioManager.class);
        mPackageManager = context.getPackageManager();
    }

    /** Refreshes the rows from AudioFlinger's active playback tracks. */
    public void bind() {
        removeAllViews();

        final Map<String, AppVolume> activeVolumes = new LinkedHashMap<>();
        for (AppVolume volume : mAudioManager.listAppVolumes()) {
            if (volume.isActive()) {
                activeVolumes.put(volume.getPackageName(), volume);
            }
        }

        for (AppVolume volume : activeVolumes.values()) {
            addView(createAppRow(volume), new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private LinearLayout createAppRow(AppVolume volume) {
        final String packageName = volume.getPackageName();
        final AppInfo appInfo = loadAppInfo(packageName);

        final LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(VERTICAL);
        row.setPadding(0, 0, 0, dpToPx(8));

        final LinearLayout title = new LinearLayout(getContext());
        title.setOrientation(HORIZONTAL);
        title.setGravity(Gravity.CENTER_VERTICAL);

        final ImageView icon = new ImageView(getContext());
        icon.setImageDrawable(appInfo.icon);
        title.addView(icon, new LayoutParams(dpToPx(32), dpToPx(32)));

        final TextView label = new TextView(getContext());
        label.setText(appInfo.label);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        final LayoutParams labelParams = new LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.setMarginStart(dpToPx(12));
        title.addView(label, labelParams);
        row.addView(title, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final Context sliderContext = new ContextThemeWrapper(getContext(),
                com.google.android.material.R.style.Theme_Material3_DynamicColors_DayNight);
        final Slider slider = new Slider(sliderContext);
        slider.setValueFrom(0f);
        slider.setValueTo(100f);
        slider.setStepSize(1f);
        slider.setValue(Math.round(volume.getVolume() * 100));
        slider.setLabelBehavior(LabelFormatter.LABEL_GONE);
        slider.setTickVisibilityMode(TickVisibilityMode.TICK_VISIBILITY_HIDDEN);
        slider.setTrackHeight(dpToPx(28));
        slider.setTrackCornerSize(dpToPx(12));
        slider.setTrackIconSize(dpToPx(20));
        slider.setTrackStopIndicatorSize(0);
        slider.setTrackIconActiveStart(R.drawable.ic_volume_up_filled);
        slider.setContentDescription(appInfo.label);
        updateStateDescription(slider, slider.getValue());
        slider.addOnChangeListener((changedSlider, value, fromUser) -> {
            updateStateDescription(changedSlider, value);
            if (fromUser) {
                mAudioManager.setAppVolume(packageName, value / 100f);
            }
        });

        final LayoutParams sliderParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        sliderParams.topMargin = dpToPx(8);
        row.addView(slider, sliderParams);
        return row;
    }

    private AppInfo loadAppInfo(String packageName) {
        try {
            final ApplicationInfo info = mPackageManager.getApplicationInfo(
                    packageName, PackageManager.MATCH_ANY_USER);
            return new AppInfo(info.loadLabel(mPackageManager), info.loadIcon(mPackageManager));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to load application info for " + packageName, e);
            return new AppInfo(packageName, mPackageManager.getDefaultActivityIcon());
        }
    }

    private void updateStateDescription(Slider slider, float value) {
        slider.setStateDescription(mPercentFormat.format(value / 100f));
    }

    private int dpToPx(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class AppInfo {
        final CharSequence label;
        final Drawable icon;

        AppInfo(CharSequence label, Drawable icon) {
            this.label = label;
            this.icon = icon;
        }
    }
}
