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

package com.android.settings.password;

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

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.android.settings.R;

import com.google.android.setupcompat.internal.TemplateLayout;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.view.ButtonBarLayout;
import com.google.android.setupdesign.template.FloatingBackButtonMixin;

/** Manual setup-wizard top chrome for lock setup pages hosted by Settings. */
final class SetupLockHeaderHelper {

    private static final String TAG_CHROME = "uwu_setup_lock_top_chrome";
    private static final int HEADER_HEIGHT_DP = 332;
    private static final int BACK_TOUCH_SIZE_DP = 56;
    private static final int BACK_BUTTON_SIZE_DP = 40;
    private static final int BACK_ICON_SIZE_DP = 16;
    private static final int HEADER_START_DP = 28;
    private static final int HEADER_TOP_DP = 108;
    private static final int HEADER_ICON_SIZE_DP = 48;
    private static final String TAG_BOTTOM_BAR = "uwu_setup_lock_bottom_bar";
    private static final int BOTTOM_BUTTON_HEIGHT_DP = 56;
    private static final int BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP = 28;
    private static final int BOTTOM_BUTTON_MARGIN_BOTTOM_DP = 24;
    private static final int BOTTOM_BUTTON_SPACING_DP = 12;

    private SetupLockHeaderHelper() {}

    static void apply(TemplateLayout layout, @StringRes int titleRes, @DrawableRes int iconRes) {
        apply(layout, layout.getContext().getText(titleRes), null, iconRes);
    }

    static void apply(TemplateLayout layout, CharSequence title, @DrawableRes int iconRes) {
        apply(layout, title, null, iconRes);
    }

    static void apply(TemplateLayout layout, @StringRes int titleRes, @StringRes int summaryRes,
            @DrawableRes int iconRes) {
        apply(layout, layout.getContext().getText(titleRes), layout.getContext().getText(summaryRes),
                iconRes);
    }

    static void apply(TemplateLayout layout, CharSequence title, CharSequence summary,
            @DrawableRes int iconRes) {
        if (layout == null) {
            return;
        }

        final FloatingBackButtonMixin backMixin = layout.getMixin(FloatingBackButtonMixin.class);
        if (backMixin != null) {
            backMixin.setVisibility(View.GONE);
        }

        hide(layout.findManagedViewById(com.google.android.setupdesign.R.id.sud_layout_icon));
        hide(layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_icon_container));
        hide(layout.findManagedViewById(com.google.android.setupdesign.R.id.suc_layout_title));
        final View statusView = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.suc_layout_status);
        if (statusView != null) {
            statusView.setFitsSystemWindows(false);
            statusView.setPadding(statusView.getPaddingLeft(), 0, statusView.getPaddingRight(),
                    statusView.getPaddingBottom());
        }
        if (summary != null) {
            hide(layout.findManagedViewById(
                    com.google.android.setupdesign.R.id.sud_layout_subtitle));
        }

        final View headerView = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_header);
        if (!(headerView instanceof LinearLayout header)) {
            return;
        }
        applyPageBackground(layout);
        header.setVisibility(View.VISIBLE);
        header.setPadding(0, 0, 0, header.getPaddingBottom());

        FrameLayout chrome = header.findViewWithTag(TAG_CHROME);
        if (chrome == null) {
            chrome = createChrome(layout.getContext(), iconRes, title, summary);
            chrome.setTag(TAG_CHROME);
            header.addView(chrome, 0);
        } else {
            updateChrome(chrome, iconRes, title, summary);
        }
    }

    static void hideStatusBar(Activity activity) {
        if (activity == null) {
            return;
        }
        final WindowInsetsController controller = activity.getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    static void updateTitle(TemplateLayout layout, CharSequence title) {
        if (layout == null) {
            return;
        }
        final View headerView = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_header);
        if (!(headerView instanceof LinearLayout header)) {
            return;
        }
        final FrameLayout chrome = header.findViewWithTag(TAG_CHROME);
        if (chrome != null) {
            final TextView titleView = chrome.findViewById(R.id.setup_lock_top_title);
            if (titleView != null) {
                titleView.setText(title);
            }
        }
    }

    static void updateSummary(TemplateLayout layout, CharSequence summary) {
        if (layout == null) {
            return;
        }
        final View headerView = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_header);
        if (!(headerView instanceof LinearLayout header)) {
            return;
        }
        final FrameLayout chrome = header.findViewWithTag(TAG_CHROME);
        if (chrome != null) {
            final TextView summaryView = chrome.findViewById(R.id.setup_lock_top_summary);
            if (summaryView != null) {
                summaryView.setText(summary);
                summaryView.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
            }
        }
    }

    static void compactPatternLayout(TemplateLayout layout) {
        final View headerView = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_header);
        if (headerView instanceof LinearLayout header) {
            final View chrome = header.findViewWithTag(TAG_CHROME);
            if (chrome != null) {
                final ViewGroup.LayoutParams params = chrome.getLayoutParams();
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                chrome.setLayoutParams(params);
                chrome.setPadding(0, 0, 0, dp(layout.getContext(), 16));
            }
            header.setPadding(0, 0, 0, 0);
        }

        final View topLayout = layout.findViewById(R.id.topLayout);
        if (topLayout != null) {
            topLayout.setPadding(0, 0, 0, 0);
            if (topLayout instanceof LinearLayout content) {
                content.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            }
        }

        final View pattern = layout.findViewById(R.id.lockPattern);
        if (pattern != null && pattern.getParent() instanceof View patternContainer) {
            final ViewGroup.LayoutParams params = patternContainer.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            if (params instanceof LinearLayout.LayoutParams linearParams) {
                linearParams.weight = 0;
                linearParams.topMargin = 0;
                linearParams.bottomMargin = 0;
            }
            patternContainer.setLayoutParams(params);
            patternContainer.setPadding(0, 0, 0, 0);
        }
    }

    static void installSkipButton(Activity activity, View.OnClickListener listener) {
        if (activity == null) {
            return;
        }
        final ViewGroup content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout root)) {
            return;
        }
        final int backgroundColor = activity.getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainer);
        activity.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                backgroundColor));
        root.setBackgroundColor(backgroundColor);

        FrameLayout bar = root.findViewWithTag(TAG_BOTTOM_BAR);
        if (bar == null) {
            bar = new FrameLayout(activity);
            bar.setTag(TAG_BOTTOM_BAR);
            bar.setBackgroundColor(backgroundColor);
            final FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 112), Gravity.BOTTOM);
            root.addView(bar, barLp);

            final TextView button = new TextView(activity);
            button.setText(R.string.skip_label);
            button.setTextColor(buttonTextColor(activity));
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            button.setGravity(Gravity.CENTER);
            button.setTypeface(Typeface.create(
                    activity.getString(com.android.internal.R.string.config_bodyFontFamily),
                    Typeface.BOLD));
            button.setClickable(true);
            button.setFocusable(true);
            button.setBackground(outlineButtonBackground(activity));
            button.setOnClickListener(listener);
            final FrameLayout.LayoutParams buttonLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, BOTTOM_BUTTON_HEIGHT_DP),
                    Gravity.BOTTOM);
            buttonLp.leftMargin = dp(activity, BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP);
            buttonLp.rightMargin = dp(activity, BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP);
            buttonLp.bottomMargin = dp(activity, BOTTOM_BUTTON_MARGIN_BOTTOM_DP);
            bar.addView(button, buttonLp);
        } else {
            bar.setBackgroundColor(backgroundColor);
            if (bar.getChildCount() > 0) {
                bar.getChildAt(0).setOnClickListener(listener);
            }
        }
    }

    private static FrameLayout createChrome(
            Context context, @DrawableRes int iconRes, CharSequence title, CharSequence summary) {
        final FrameLayout chrome = new FrameLayout(context);
        chrome.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, HEADER_HEIGHT_DP)));

        final FrameLayout backTouch = new FrameLayout(context);
        backTouch.setContentDescription(context.getText(R.string.previous_page_content_description));
        backTouch.setClickable(true);
        backTouch.setFocusable(true);
        backTouch.setOnClickListener(v -> {
            Activity activity = findActivity(context);
            if (activity != null) {
                activity.onBackPressed();
            }
        });
        final FrameLayout.LayoutParams backTouchLp = new FrameLayout.LayoutParams(
                dp(context, BACK_TOUCH_SIZE_DP), dp(context, BACK_TOUCH_SIZE_DP));
        backTouchLp.leftMargin = dp(context, 15);
        backTouchLp.topMargin = dp(context, 36);
        chrome.addView(backTouch, backTouchLp);

        final FrameLayout backButton = new FrameLayout(context);
        backButton.setBackground(roundRect(
                context.getColor(com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainerHighest),
                dp(context, BACK_BUTTON_SIZE_DP) / 2f));
        final FrameLayout.LayoutParams backButtonLp = new FrameLayout.LayoutParams(
                dp(context, BACK_BUTTON_SIZE_DP), dp(context, BACK_BUTTON_SIZE_DP),
                Gravity.CENTER);
        backTouch.addView(backButton, backButtonLp);

        final ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.ic_uwu_arrow_back);
        arrow.setImageTintList(ColorStateList.valueOf(
                context.getColor(com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorOnSurfaceVariant)));
        final FrameLayout.LayoutParams arrowLp = new FrameLayout.LayoutParams(
                dp(context, BACK_ICON_SIZE_DP), dp(context, BACK_ICON_SIZE_DP),
                Gravity.CENTER);
        backButton.addView(arrow, arrowLp);

        final LinearLayout pageHeader = new LinearLayout(context);
        pageHeader.setOrientation(LinearLayout.VERTICAL);
        final FrameLayout.LayoutParams headerLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.leftMargin = dp(context, HEADER_START_DP);
        headerLp.rightMargin = dp(context, HEADER_START_DP);
        headerLp.topMargin = dp(context, HEADER_TOP_DP);
        chrome.addView(pageHeader, headerLp);

        final ImageView icon = new ImageView(context);
        icon.setId(R.id.setup_lock_top_icon);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(
                color(context, android.R.attr.colorAccent, Color.DKGRAY)));
        pageHeader.addView(icon, new LinearLayout.LayoutParams(
                dp(context, HEADER_ICON_SIZE_DP), dp(context, HEADER_ICON_SIZE_DP)));

        final TextView titleView = new TextView(context);
        titleView.setId(R.id.setup_lock_top_title);
        titleView.setText(title);
        titleView.setTextColor(color(context, android.R.attr.textColorPrimary, Color.BLACK));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 36);
        titleView.setTypeface(Typeface.create(
                context.getString(com.android.internal.R.string.config_headlineFontFamily),
                Typeface.BOLD));
        titleView.setIncludeFontPadding(false);
        final LinearLayout.LayoutParams titleTextLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleTextLp.topMargin = dp(context, 24);
        pageHeader.addView(titleView, titleTextLp);

        if (summary != null) {
            final TextView summaryView = new TextView(context);
            summaryView.setId(R.id.setup_lock_top_summary);
            summaryView.setText(summary);
            summaryView.setTextColor(color(context, android.R.attr.textColorSecondary,
                    Color.DKGRAY));
            summaryView.setAlpha(0.74f);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            summaryView.setLineSpacing(dp(context, 2), 1f);
            summaryView.setTypeface(Typeface.create(
                    context.getString(com.android.internal.R.string.config_bodyFontFamily),
                    Typeface.NORMAL));
            final LinearLayout.LayoutParams summaryLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            summaryLp.topMargin = dp(context, 12);
            pageHeader.addView(summaryView, summaryLp);
        }

        return chrome;
    }

    private static void updateChrome(
            FrameLayout chrome, @DrawableRes int iconRes, CharSequence title,
            CharSequence summary) {
        final ImageView icon = chrome.findViewById(R.id.setup_lock_top_icon);
        if (icon != null) {
            icon.setImageResource(iconRes);
        }
        final TextView titleView = chrome.findViewById(R.id.setup_lock_top_title);
        if (titleView != null) {
            titleView.setText(title);
        }
        final TextView summaryView = chrome.findViewById(R.id.setup_lock_top_summary);
        if (summaryView != null && summary != null) {
            summaryView.setText(summary);
            summaryView.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
        }
    }

    private static void hide(View view) {
        if (view != null) {
            view.setVisibility(View.GONE);
        }
    }

    private static GradientDrawable roundRect(int color, float radius) {
        final GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private static Drawable outlineButtonBackground(Context context) {
        final int outlineColor = color(context, android.R.attr.textColorSecondary, Color.GRAY);
        final int disabledFillColor = colorWithAlpha(outlineColor, 0.08f);
        final int disabledOutlineColor = colorWithAlpha(outlineColor, 0.24f);
        final float radius = dp(context, BOTTOM_BUTTON_HEIGHT_DP) / 2f;

        final GradientDrawable enabled = roundRect(Color.TRANSPARENT, radius);
        enabled.setStroke(dp(context, 1), colorWithAlpha(outlineColor, 0.62f));

        final GradientDrawable disabled = roundRect(disabledFillColor, radius);
        disabled.setStroke(dp(context, 1), disabledOutlineColor);

        final StateListDrawable content = new StateListDrawable();
        content.addState(new int[] { -android.R.attr.state_enabled }, disabled);
        content.addState(new int[] {}, enabled);

        final GradientDrawable mask = roundRect(Color.WHITE, radius);
        return new RippleDrawable(
                ColorStateList.valueOf(colorWithAlpha(outlineColor, 0.18f)),
                content,
                mask);
    }

    private static ColorStateList buttonTextColor(Context context) {
        final int color = color(context, android.R.attr.textColorSecondary, Color.DKGRAY);
        return new ColorStateList(
                new int[][] {
                        new int[] { -android.R.attr.state_enabled },
                        new int[] {}
                },
                new int[] {
                        colorWithAlpha(color, 0.38f),
                        color
                });
    }

    static void styleNavigationButtons(TemplateLayout layout) {
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        if (mixin == null || mixin.getButtonContainer() == null) {
            return;
        }
        final LinearLayout container = mixin.getButtonContainer();
        final Context context = container.getContext();
        final int backgroundColor = context.getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainer);
        container.setBackgroundColor(backgroundColor);
        container.setElevation(0);
        container.setStateListAnimator(null);
        container.setForeground(null);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP), dp(context, 12),
                dp(context, BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP),
                dp(context, BOTTOM_BUTTON_MARGIN_BOTTOM_DP));

        final Button skip = mixin.getSecondaryButtonView();
        final Button next = mixin.getPrimaryButtonView();
        if (skip == null || next == null) {
            return;
        }
        container.removeAllViews();
        styleButton(skip, outlineButtonBackground(context), buttonTextColor(context));
        styleButton(next, filledButtonBackground(context), filledButtonTextColor(context));
        container.addView(skip);
        container.addView(next);
        refreshNavigationButtonLayout(layout);
    }

    static void refreshNavigationButtonLayout(TemplateLayout layout) {
        final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
        if (mixin == null || mixin.getButtonContainer() == null) {
            return;
        }
        final LinearLayout container = mixin.getButtonContainer();
        final Button skip = mixin.getSecondaryButtonView();
        final Button next = mixin.getPrimaryButtonView();
        if (skip == null || next == null) {
            return;
        }
        final Context context = container.getContext();
        applyFullWidthButtonLayout(context, container, skip, next);
        container.post(() -> applyFullWidthButtonLayout(context, container, skip, next));
    }

    private static void styleButton(
            Button button, Drawable background, ColorStateList textColor) {
        button.setBackground(background);
        button.setTextColor(textColor);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setTypeface(Typeface.create(
                button.getContext().getString(com.android.internal.R.string.config_bodyFontFamily),
                Typeface.BOLD));
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);
    }

    private static void applyFullWidthButtonLayout(
            Context context, LinearLayout container, Button skip, Button next) {
        if (container instanceof ButtonBarLayout buttonBar) {
            buttonBar.setStackedButtonForExpressiveStyle(true);
        }
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(context, BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP), dp(context, 12),
                dp(context, BOTTOM_BUTTON_MARGIN_HORIZONTAL_DP),
                dp(context, BOTTOM_BUTTON_MARGIN_BOTTOM_DP));

        final LinearLayout.LayoutParams skipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, BOTTOM_BUTTON_HEIGHT_DP));
        skipLp.bottomMargin = dp(context, BOTTOM_BUTTON_SPACING_DP);
        skip.setLayoutParams(skipLp);

        final LinearLayout.LayoutParams nextLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, BOTTOM_BUTTON_HEIGHT_DP));
        next.setLayoutParams(nextLp);
    }

    private static Drawable filledButtonBackground(Context context) {
        final int primary = context.getColor(com.android.settingslib.widget.theme.R.color
                .settingslib_materialColorPrimary);
        final float radius = dp(context, BOTTOM_BUTTON_HEIGHT_DP) / 2f;
        final StateListDrawable content = new StateListDrawable();
        content.addState(new int[] { -android.R.attr.state_enabled },
                roundRect(colorWithAlpha(primary, 0.38f), radius));
        content.addState(new int[] {}, roundRect(primary, radius));
        return content;
    }

    private static ColorStateList filledButtonTextColor(Context context) {
        final int color = context.getColor(com.android.settingslib.widget.theme.R.color
                .settingslib_materialColorOnPrimary);
        return new ColorStateList(
                new int[][] {
                        new int[] { -android.R.attr.state_enabled },
                        new int[] {}
                },
                new int[] {
                        colorWithAlpha(color, 0.38f),
                        color
                });
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

    private static void applyPageBackground(TemplateLayout layout) {
        final int backgroundColor = layout.getContext().getColor(
                com.android.settingslib.widget.theme.R.color
                        .settingslib_materialColorSurfaceContainer);
        layout.setBackgroundColor(backgroundColor);
        final View headerView = layout.findManagedViewById(
                com.google.android.setupdesign.R.id.sud_layout_header);
        if (headerView != null) {
            headerView.setBackgroundColor(backgroundColor);
        }
        final View recyclerView = layout.findManagedViewById(android.R.id.list);
        if (recyclerView != null) {
            recyclerView.setBackgroundColor(backgroundColor);
        }
    }

    private static int color(Context context, int attr, int fallback) {
        final TypedArray array = context.obtainStyledAttributes(new int[] { attr });
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
