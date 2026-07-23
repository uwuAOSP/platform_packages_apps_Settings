/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.accessibility;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.template.Mixin;
import com.google.android.setupcompat.view.ButtonBarLayout;
import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.template.FloatingBackButtonMixin;

/** Provides utility methods to accessibility settings for Setup Wizard only. */
public class AccessibilitySetupWizardUtils {

    private static final String TAG_CHROME = "uwu_setup_accessibility_top_chrome";
    private static final int HEADER_HEIGHT_DP = 332;
    private static final int HEADER_START_DP = 28;
    private static final int HEADER_TOP_DP = 108;
    private static final int HEADER_ICON_SIZE_DP = 48;
    private static final int BUTTON_HEIGHT_DP = 56;
    private static final int BUTTON_HORIZONTAL_MARGIN_DP = 28;
    private static final int BUTTON_TOP_PADDING_DP = 12;
    private static final int BUTTON_BOTTOM_PADDING_DP = 24;
    private static final int BUTTON_SPACING_DP = 12;

    private AccessibilitySetupWizardUtils(){}

    /**
     * Applies the uwu Setup Wizard header, background, and list styling.
     *
     * @param layout The layout instance
     * @param title The text to be set as title
     * @param description The text to be set as description
     * @param icon The icon to be set
     */
    public static void updateGlifPreferenceLayout(Context context, GlifPreferenceLayout layout,
            @Nullable CharSequence title, @Nullable CharSequence description,
            @Nullable Drawable icon) {
        if (!TextUtils.isEmpty(title)) {
            layout.setHeaderText(title);
        }

        if (!TextUtils.isEmpty(description)) {
            layout.setDescriptionText(description);
        }

        if (icon != null) {
            layout.setIcon(icon);
        }
        layout.setDividerInsets(Integer.MAX_VALUE, 0);
        applyUwuStyle(context, layout, title, description, icon);
    }

    /**
     * Sets primary button for footer of the {@link GlifPreferenceLayout}.
     *
     * <p> This will be the initial by given material theme style.
     *
     * @param context A {@link Context}
     * @param mixin A {@link Mixin} for managing buttons.
     * @param text The {@code text} by resource.
     * @param runnable The {@link Runnable} to run.
     */
    public static void setPrimaryButton(Context context, FooterBarMixin mixin, @StringRes int text,
            Runnable runnable) {
        mixin.setPrimaryButton(
                new FooterButton.Builder(context)
                        .setText(text)
                        .setListener(l -> runnable.run())
                        .setButtonType(FooterButton.ButtonType.DONE)
                        .build());
        styleFooter(mixin);
    }

    /**
     * Sets secondary button for the footer of the {@link GlifPreferenceLayout}.
     *
     * <p> This will be the initial by given material theme style.
     *
     * @param context A {@link Context}
     * @param mixin A {@link Mixin} for managing buttons.
     * @param text The {@code text} by resource.
     * @param runnable The {@link Runnable} to run.
     */
    public static void setSecondaryButton(Context context, FooterBarMixin mixin,
            @StringRes int text, Runnable runnable) {
        mixin.setSecondaryButton(
                new FooterButton.Builder(context)
                        .setText(text)
                        .setListener(l -> runnable.run())
                        .setButtonType(FooterButton.ButtonType.CLEAR)
                        .build());
        styleFooter(mixin);
    }

    private static void applyUwuStyle(Context context, GlifPreferenceLayout layout,
            CharSequence title, CharSequence description, Drawable icon) {
        final Activity activity = findActivity(context);
        if (activity != null) {
            final WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }

        final FloatingBackButtonMixin backMixin = layout.getMixin(FloatingBackButtonMixin.class);
        if (backMixin != null) {
            backMixin.setVisibility(View.GONE);
        }
        hide(layout.findManagedViewById(com.google.android.setupdesign.R.id.sud_layout_icon));
        hide(layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_icon_container));
        hide(layout.findManagedViewById(com.google.android.setupdesign.R.id.suc_layout_title));
        hide(layout.findManagedViewById(com.google.android.setupdesign.R.id.sud_layout_subtitle));
        final View status = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.suc_layout_status);
        if (status != null) {
            status.setFitsSystemWindows(false);
            status.setPadding(status.getPaddingLeft(), 0, status.getPaddingRight(),
                    status.getPaddingBottom());
        }

        final int background = context.getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainer);
        layout.setBackgroundColor(background);
        final View list = layout.findManagedViewById(android.R.id.list);
        if (list != null) {
            list.setBackgroundColor(background);
        }

        final LinearLayout header = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_header);
        if (header == null) {
            return;
        }
        header.setBackgroundColor(background);
        header.setPadding(0, 0, 0, 0);
        FrameLayout chrome = header.findViewWithTag(TAG_CHROME);
        if (chrome == null) {
            chrome = createChrome(context, title, description, icon);
            chrome.setTag(TAG_CHROME);
            header.addView(chrome, 0);
        }
    }

    private static FrameLayout createChrome(Context context, CharSequence title,
            CharSequence description, Drawable iconDrawable) {
        final FrameLayout chrome = new FrameLayout(context);
        chrome.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, HEADER_HEIGHT_DP)));

        final FrameLayout backTouch = new FrameLayout(context);
        backTouch.setContentDescription(context.getText(R.string.previous_page_content_description));
        backTouch.setClickable(true);
        backTouch.setFocusable(true);
        backTouch.setOnClickListener(v -> {
            final Activity activity = findActivity(context);
            if (activity != null) {
                activity.onBackPressed();
            }
        });
        final FrameLayout.LayoutParams backTouchLp = new FrameLayout.LayoutParams(
                dp(context, 56), dp(context, 56));
        backTouchLp.leftMargin = dp(context, 15);
        backTouchLp.topMargin = dp(context, 36);
        chrome.addView(backTouch, backTouchLp);

        final FrameLayout backButton = new FrameLayout(context);
        backButton.setBackground(roundRect(context.getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainerHighest), dp(context, 20)));
        backTouch.addView(backButton, new FrameLayout.LayoutParams(
                dp(context, 40), dp(context, 40), Gravity.CENTER));

        final ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.ic_uwu_arrow_back);
        arrow.setImageTintList(ColorStateList.valueOf(context.getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorOnSurfaceVariant)));
        backButton.addView(arrow, new FrameLayout.LayoutParams(
                dp(context, 16), dp(context, 16), Gravity.CENTER));

        final LinearLayout pageHeader = new LinearLayout(context);
        pageHeader.setOrientation(LinearLayout.VERTICAL);
        final FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.leftMargin = dp(context, HEADER_START_DP);
        headerLp.rightMargin = dp(context, HEADER_START_DP);
        headerLp.topMargin = dp(context, HEADER_TOP_DP);
        chrome.addView(pageHeader, headerLp);

        final ImageView icon = new ImageView(context);
        icon.setImageDrawable(iconDrawable);
        icon.setImageTintList(ColorStateList.valueOf(context.getColor(
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorPrimary)));
        pageHeader.addView(icon, new LinearLayout.LayoutParams(
                dp(context, HEADER_ICON_SIZE_DP), dp(context, HEADER_ICON_SIZE_DP)));

        final TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(color(context, android.R.attr.textColorPrimary, Color.BLACK));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        titleView.setTypeface(Typeface.create(
                context.getString(com.android.internal.R.string.config_headlineFontFamily),
                Typeface.BOLD));
        titleView.setIncludeFontPadding(false);
        final LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = dp(context, 24);
        pageHeader.addView(titleView, titleLp);

        if (!TextUtils.isEmpty(description)) {
            final TextView descriptionView = new TextView(context);
            descriptionView.setText(description);
            descriptionView.setTextColor(color(
                    context, android.R.attr.textColorSecondary, Color.DKGRAY));
            descriptionView.setAlpha(0.74f);
            descriptionView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            descriptionView.setLineSpacing(dp(context, 2), 1f);
            descriptionView.setTypeface(Typeface.create(
                    context.getString(com.android.internal.R.string.config_bodyFontFamily),
                    Typeface.NORMAL));
            final LinearLayout.LayoutParams descriptionLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            descriptionLp.topMargin = dp(context, 12);
            pageHeader.addView(descriptionView, descriptionLp);
        }
        return chrome;
    }

    private static void styleFooter(FooterBarMixin mixin) {
        final LinearLayout container = mixin.getButtonContainer();
        if (container == null) {
            return;
        }
        final Button primary = mixin.getPrimaryButtonView();
        if (primary == null) {
            return;
        }
        final Button secondary = mixin.getSecondaryButtonView();
        final Context context = container.getContext();
        container.setBackgroundColor(context.getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainer));
        container.setElevation(0);
        container.setStateListAnimator(null);
        container.setForeground(null);
        container.removeAllViews();
        if (secondary != null) {
            styleButton(secondary, outlineButtonBackground(context), buttonTextColor(context));
            container.addView(secondary);
        }
        styleButton(primary, filledButtonBackground(context), filledButtonTextColor(context));
        container.addView(primary);
        applyFooterLayout(container, secondary, primary);
        container.post(() -> applyFooterLayout(container, secondary, primary));
    }

    private static void applyFooterLayout(LinearLayout container, Button secondary,
            Button primary) {
        final Context context = container.getContext();
        if (container instanceof ButtonBarLayout buttonBar) {
            buttonBar.setStackedButtonForExpressiveStyle(secondary != null);
        }
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, BUTTON_HORIZONTAL_MARGIN_DP),
                dp(context, BUTTON_TOP_PADDING_DP), dp(context, BUTTON_HORIZONTAL_MARGIN_DP),
                dp(context, BUTTON_BOTTOM_PADDING_DP));
        if (secondary != null) {
            final LinearLayout.LayoutParams secondaryLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(context, BUTTON_HEIGHT_DP));
            secondaryLp.bottomMargin = dp(context, BUTTON_SPACING_DP);
            secondary.setLayoutParams(secondaryLp);
        }
        primary.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, BUTTON_HEIGHT_DP)));
    }

    private static void styleButton(Button button, Drawable background, ColorStateList textColor) {
        button.setBackground(background);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTypeface(Typeface.create(
                button.getContext().getString(com.android.internal.R.string.config_bodyFontFamily),
                Typeface.BOLD));
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
    }

    private static Drawable filledButtonBackground(Context context) {
        final int primary = context.getColor(
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorPrimary);
        final float radius = dp(context, BUTTON_HEIGHT_DP) / 2f;
        final StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {-android.R.attr.state_enabled},
                roundRect(colorWithAlpha(primary, 0.38f), radius));
        states.addState(new int[] {}, roundRect(primary, radius));
        return states;
    }

    private static Drawable outlineButtonBackground(Context context) {
        final int outline = color(context, android.R.attr.textColorSecondary, Color.GRAY);
        final float radius = dp(context, BUTTON_HEIGHT_DP) / 2f;
        final GradientDrawable enabled = roundRect(Color.TRANSPARENT, radius);
        enabled.setStroke(dp(context, 1), colorWithAlpha(outline, 0.62f));
        final GradientDrawable disabled = roundRect(colorWithAlpha(outline, 0.08f), radius);
        disabled.setStroke(dp(context, 1), colorWithAlpha(outline, 0.24f));
        final StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {-android.R.attr.state_enabled}, disabled);
        states.addState(new int[] {}, enabled);
        return new RippleDrawable(ColorStateList.valueOf(colorWithAlpha(outline, 0.18f)), states,
                roundRect(Color.WHITE, radius));
    }

    private static ColorStateList filledButtonTextColor(Context context) {
        return statefulTextColor(context.getColor(
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorOnPrimary));
    }

    private static ColorStateList buttonTextColor(Context context) {
        return statefulTextColor(color(
                context, android.R.attr.textColorSecondary, Color.DKGRAY));
    }

    private static ColorStateList statefulTextColor(int color) {
        return new ColorStateList(
                new int[][] {new int[] {-android.R.attr.state_enabled}, new int[] {}},
                new int[] {colorWithAlpha(color, 0.38f), color});
    }

    private static GradientDrawable roundRect(int color, float radius) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private static void hide(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper wrapper) {
            if (context instanceof Activity activity) {
                return activity;
            }
            context = wrapper.getBaseContext();
        }
        return context instanceof Activity activity ? activity : null;
    }

    private static int color(Context context, int attr, int fallback) {
        final TypedArray array = context.obtainStyledAttributes(new int[] {attr});
        try {
            return array.getColor(0, fallback);
        } finally {
            array.recycle();
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static int colorWithAlpha(int color, float alpha) {
        return Color.argb(Math.round(Color.alpha(color) * alpha), Color.red(color),
                Color.green(color), Color.blue(color));
    }
}
