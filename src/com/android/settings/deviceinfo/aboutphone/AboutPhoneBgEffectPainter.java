/*
 * Copyright (C) 2026 The uwuAOSP Project
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

package com.android.settings.deviceinfo.aboutphone;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import java.io.InputStream;
import java.util.Scanner;

final class AboutPhoneBgEffectPainter {

    private static final long FRAME_DELAY_MILLIS = 16L;
    private static final float ANIMATION_PERIOD_SECONDS = 62.831852f;

    private static final float[] DEFAULT_BOUND = new float[] {
            -0.08f, -0.08f, 1.16f, 1.16f
    };

    private static final float[] DEFAULT_POINTS = new float[] {
            0.82f, 0.16f, 0.96f,
            0.78f, 0.58f, 1.06f,
            0.18f, 0.66f, 1.04f,
            0.16f, 0.26f, 0.96f
    };

    private static final int[] REFERENCE_COLORS = new int[] {
            0xFFA5C9FF,
            0xFFA5EBFF,
            0xFF8E93EC,
            0xFFD4D8FF
    };

    private static final int[] DARK_REFERENCE_COLORS = new int[] {
            0xFF3D4C68,
            0xFF385465,
            0xFF40438A,
            0xFF4A4E67
    };

    private final Context mContext;
    private final RuntimeShader mShader;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mAnimationFrame = this::drawFrame;

    @Nullable
    private View mTargetView;

    private boolean mLastNightMode;
    private boolean mRunning;
    private long mStartNanos;

    AboutPhoneBgEffectPainter(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mLastNightMode = isNightMode();
        mShader = new RuntimeShader(loadShader(mContext.getResources(), R.raw.bg_frag));
        mShader.setFloatUniform("uTranslateY", 0.0f);
        mShader.setFloatUniform("uPoints", DEFAULT_POINTS);
        applyModeTuning();
        applyDynamicColors();
        mShader.setFloatUniform("uBound", DEFAULT_BOUND);
    }

    void attach(@NonNull View targetView) {
        maybeRefreshNightMode();
        applyDynamicColors();
        if (mTargetView == targetView) {
            mTargetView.setBackgroundResource(R.drawable.about_phone_bg_shader_fallback);
            return;
        }

        if (mTargetView != null) {
            mTargetView.setRenderEffect(null);
        }

        mTargetView = targetView;
        mTargetView.setBackgroundResource(R.drawable.about_phone_bg_shader_fallback);
    }

    void start() {
        if (mRunning || mTargetView == null) {
            return;
        }

        mRunning = true;
        mStartNanos = System.nanoTime();
        mHandler.removeCallbacks(mAnimationFrame);
        if (mTargetView.getWidth() > 0 && mTargetView.getHeight() > 0) {
            drawFrame();
            return;
        }
        mTargetView.post(mAnimationFrame);
    }

    void stop() {
        mRunning = false;
        mHandler.removeCallbacks(mAnimationFrame);
        if (mTargetView != null) {
            mTargetView.setRenderEffect(null);
        }
    }

    private void drawFrame() {
        if (!mRunning) {
            return;
        }

        maybeRefreshNightMode();

        final View targetView = mTargetView;
        if (targetView == null || !targetView.isAttachedToWindow()) {
            stop();
            return;
        }

        final int width = targetView.getWidth();
        final int height = targetView.getHeight();
        if (width <= 0 || height <= 0) {
            mHandler.postDelayed(mAnimationFrame, FRAME_DELAY_MILLIS);
            return;
        }

        final float elapsedSeconds = (System.nanoTime() - mStartNanos) / 1_000_000_000f;
        mShader.setFloatUniform("uAnimTime", elapsedSeconds % ANIMATION_PERIOD_SECONDS);
        mShader.setFloatUniform("uResolution", (float) width, (float) height);
        targetView.setRenderEffect(RenderEffect.createRuntimeShaderEffect(mShader, "uTex"));

        mHandler.postDelayed(mAnimationFrame, FRAME_DELAY_MILLIS);
    }

    private void applyDynamicColors() {
        mShader.setFloatUniform("uColors", resolveDynamicColors());
    }

    private void applyModeTuning() {
        if (mLastNightMode) {
            mShader.setFloatUniform("uNoiseScale", 1.05f);
            mShader.setFloatUniform("uPointOffset", 0.048f);
            mShader.setFloatUniform("uPointRadiusMulti", 0.92f);
            mShader.setFloatUniform("uSaturateOffset", 0.05f);
            mShader.setFloatUniform("uShadowColorMulti", 0.16f);
            mShader.setFloatUniform("uShadowColorOffset", 0.16f);
            mShader.setFloatUniform("uShadowOffset", 0.004f);
            mShader.setFloatUniform("uAlphaMulti", 0.88f);
            mShader.setFloatUniform("uLightOffset", -0.08f);
            mShader.setFloatUniform("uAlphaOffset", 0.28f);
            mShader.setFloatUniform("uShadowNoiseScale", 3.0f);
            return;
        }

        mShader.setFloatUniform("uNoiseScale", 1.5f);
        mShader.setFloatUniform("uPointOffset", 0.1f);
        mShader.setFloatUniform("uPointRadiusMulti", 1.0f);
        mShader.setFloatUniform("uSaturateOffset", 0.2f);
        mShader.setFloatUniform("uShadowColorMulti", 0.3f);
        mShader.setFloatUniform("uShadowColorOffset", 0.3f);
        mShader.setFloatUniform("uShadowOffset", 0.01f);
        mShader.setFloatUniform("uAlphaMulti", 1.0f);
        mShader.setFloatUniform("uLightOffset", 0.1f);
        mShader.setFloatUniform("uAlphaOffset", 0.5f);
        mShader.setFloatUniform("uShadowNoiseScale", 5.0f);
    }

    @NonNull
    private float[] resolveDynamicColors() {
        final int[] sourceColors = mLastNightMode
                ? new int[] {
                        getSystemColor("system_accent1_700", DARK_REFERENCE_COLORS[0]),
                        getSystemColor("system_accent2_700", DARK_REFERENCE_COLORS[1]),
                        getSystemColor("system_accent3_600", DARK_REFERENCE_COLORS[2]),
                        getSystemColor("system_accent1_800", DARK_REFERENCE_COLORS[3])
                }
                : new int[] {
                        getSystemColor("system_accent1_200", REFERENCE_COLORS[0]),
                        getSystemColor("system_accent2_200", REFERENCE_COLORS[1]),
                        getSystemColor("system_accent3_300", REFERENCE_COLORS[2]),
                        getSystemColor("system_accent1_100", REFERENCE_COLORS[3])
                };
        final float[] dynamicColors = new float[16];
        for (int i = 0; i < sourceColors.length; i++) {
            final int color = mLastNightMode
                    ? applyNightReferenceProfile(sourceColors[i], DARK_REFERENCE_COLORS[i])
                    : applyReferenceProfile(sourceColors[i], REFERENCE_COLORS[i],
                            0.78f, 0.84f);
            writeColor(dynamicColors, i * 4, color);
        }
        return dynamicColors;
    }

    private int getSystemColor(@NonNull String colorName, int fallbackColor) {
        final int colorResId = mContext.getResources().getIdentifier(colorName, "color", "android");
        if (colorResId == 0) {
            return fallbackColor;
        }
        try {
            return mContext.getColor(colorResId);
        } catch (Exception e) {
            return fallbackColor;
        }
    }

    private void maybeRefreshNightMode() {
        final boolean isNightMode = isNightMode();
        if (mLastNightMode == isNightMode) {
            return;
        }
        mLastNightMode = isNightMode;
        applyModeTuning();
        applyDynamicColors();
        if (mTargetView != null) {
            mTargetView.setBackgroundResource(R.drawable.about_phone_bg_shader_fallback);
        }
    }

    private boolean isNightMode() {
        return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    private static int applyReferenceProfile(int sourceColor, int referenceColor,
            float saturationAmount, float valueAmount) {
        final float[] sourceHsv = new float[3];
        final float[] referenceHsv = new float[3];
        Color.colorToHSV(sourceColor, sourceHsv);
        Color.colorToHSV(referenceColor, referenceHsv);

        sourceHsv[1] = lerp(sourceHsv[1], referenceHsv[1], saturationAmount);
        sourceHsv[2] = lerp(sourceHsv[2], referenceHsv[2], valueAmount);

        return Color.HSVToColor(Color.alpha(sourceColor), sourceHsv);
    }

    private static int applyNightReferenceProfile(int sourceColor, int referenceColor) {
        final float[] hsv = new float[3];
        Color.colorToHSV(
                applyReferenceProfile(sourceColor, referenceColor, 0.42f, 0.74f), hsv);
        hsv[1] = clamp(hsv[1], 0.18f, 0.55f);
        hsv[2] = clamp(hsv[2], 0.20f, 0.42f);
        return Color.HSVToColor(232, hsv);
    }

    private static float lerp(float start, float end, float amount) {
        return start + ((end - start) * amount);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void writeColor(@NonNull float[] destination, int startIndex, int color) {
        destination[startIndex] = Color.red(color) / 255f;
        destination[startIndex + 1] = Color.green(color) / 255f;
        destination[startIndex + 2] = Color.blue(color) / 255f;
        destination[startIndex + 3] = Color.alpha(color) / 255f;
    }

    @NonNull
    private static String loadShader(@NonNull Resources resources, int resourceId) {
        try (InputStream inputStream = resources.openRawResource(resourceId);
                Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load about phone shader", e);
        }
    }
}
