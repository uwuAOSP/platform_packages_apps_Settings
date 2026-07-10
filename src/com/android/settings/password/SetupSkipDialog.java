/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.password;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_SUW;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_USE_EXPRESSIVE_STYLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

public class SetupSkipDialog extends InstrumentedDialogFragment
        implements DialogInterface.OnClickListener {

    public static final String EXTRA_FRP_SUPPORTED = ":settings:frp_supported";

    private static final String ARG_FRP_SUPPORTED = "frp_supported";
    // The key indicates type of screen lock credential types(PIN/Pattern/Password)
    private static final String ARG_LOCK_CREDENTIAL_TYPE = "lock_credential_type";
    // The key indicates type of lock screen setup is alphanumeric for password setup.
    private static final String TAG_SKIP_DIALOG = "skip_dialog";
    public static final int RESULT_SKIP = Activity.RESULT_FIRST_USER + 10;

    public static SetupSkipDialog newInstance(@LockPatternUtils.CredentialType int credentialType,
            boolean isFrpSupported, boolean forFingerprint, boolean forFace,
            boolean forBiometrics, boolean isSuw, boolean isExpressiveStyle) {
        SetupSkipDialog dialog = new SetupSkipDialog();
        Bundle args = new Bundle();
        args.putInt(ARG_LOCK_CREDENTIAL_TYPE, credentialType);
        args.putBoolean(ARG_FRP_SUPPORTED, isFrpSupported);
        args.putBoolean(EXTRA_KEY_FOR_FINGERPRINT, forFingerprint);
        args.putBoolean(EXTRA_KEY_FOR_FACE, forFace);
        args.putBoolean(EXTRA_KEY_FOR_BIOMETRICS, forBiometrics);
        args.putBoolean(EXTRA_KEY_IS_SUW, isSuw);
        args.putBoolean(EXTRA_KEY_USE_EXPRESSIVE_STYLE, isExpressiveStyle);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_FINGERPRINT_SKIP_SETUP;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return onCreateDialogBuilder().create();
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog instanceof AlertDialog alertDialog) {
            styleDialogTitle(alertDialog);
            styleDialogButton(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE),
                    /* filled= */ false);
            styleDialogButton(alertDialog.getButton(DialogInterface.BUTTON_POSITIVE),
                    /* filled= */ true);
        }
    }

    private AlertDialog.Builder getBiometricsBuilder(
            @LockPatternUtils.CredentialType int credentialType, boolean isSuw, boolean hasFace,
            boolean hasFingerprint, boolean isExpressiveStyle) {
        final boolean isFaceSupported = hasFace && (!isSuw || BiometricUtils.isFaceSupportedInSuw(
                getContext()));
        final int msgResId;
        final int screenLockResId;
        switch (credentialType) {
            case CREDENTIAL_TYPE_PATTERN:
                screenLockResId = R.string.unlock_set_unlock_pattern_title;
                msgResId = getPatternSkipMessageRes(hasFace && isFaceSupported, hasFingerprint);
                break;
            case CREDENTIAL_TYPE_PASSWORD:
                screenLockResId = R.string.unlock_set_unlock_password_title;
                msgResId = getPasswordSkipMessageRes(hasFace && isFaceSupported, hasFingerprint);
                break;
            case CREDENTIAL_TYPE_PIN:
            default:
                screenLockResId = R.string.unlock_set_unlock_pin_title;
                msgResId = getPinSkipMessageRes(hasFace && isFaceSupported, hasFingerprint);
                break;
        }
        return new AlertDialog.Builder(isExpressiveStyle ? getExpressiveContext() : getContext())
                .setPositiveButton(R.string.skip_lock_screen_dialog_button_label, this)
                .setNegativeButton(R.string.cancel_lock_screen_dialog_button_label, this)
                .setTitle(getSkipSetupTitle(screenLockResId, hasFingerprint,
                        hasFace && isFaceSupported))
                .setMessage(msgResId);
    }

    private Context getExpressiveContext() {
        return new ContextThemeWrapper(getContext(), R.style.Theme_LockSettings_Expressive);
    }

    @NonNull
    public AlertDialog.Builder onCreateDialogBuilder() {
        Bundle args = getArguments();
        final boolean isSuw = args.getBoolean(EXTRA_KEY_IS_SUW);
        final boolean isExpressiveStyle = args.getBoolean(EXTRA_KEY_USE_EXPRESSIVE_STYLE);
        final boolean forBiometrics = args.getBoolean(EXTRA_KEY_FOR_BIOMETRICS);
        final boolean forFace = args.getBoolean(EXTRA_KEY_FOR_FACE);
        final boolean forFingerprint = args.getBoolean(EXTRA_KEY_FOR_FINGERPRINT);
        @LockPatternUtils.CredentialType
        final int credentialType = args.getInt(ARG_LOCK_CREDENTIAL_TYPE);

        if (forFace || forFingerprint || forBiometrics) {
            final boolean hasFace = Utils.hasFaceHardware(getContext());
            final boolean hasFingerprint = Utils.hasFingerprintHardware(getContext());
            return getBiometricsBuilder(credentialType, isSuw, hasFace, hasFingerprint,
                    isExpressiveStyle);
        }

        return new AlertDialog.Builder(isExpressiveStyle ? getExpressiveContext() : getContext())
                .setPositiveButton(R.string.skip_anyway_button_label, this)
                .setNegativeButton(R.string.go_back_button_label, this)
                .setTitle(R.string.lock_screen_intro_skip_title)
                .setMessage(args.getBoolean(ARG_FRP_SUPPORTED) ?
                        R.string.lock_screen_intro_skip_dialog_text_frp :
                        R.string.lock_screen_intro_skip_dialog_text);
    }

    @StringRes
    private int getPatternSkipMessageRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_pattern_skip_biometrics_message;
        } else if (hasFace) {
            return R.string.lock_screen_pattern_skip_face_message;
        } else if (hasFingerprint) {
            return R.string.lock_screen_pattern_skip_fingerprint_message;
        } else {
            return R.string.lock_screen_pattern_skip_message;
        }
    }

    @StringRes
    private int getPasswordSkipMessageRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_password_skip_biometrics_message;
        } else if (hasFace) {
            return R.string.lock_screen_password_skip_face_message;
        } else if (hasFingerprint) {
            return R.string.lock_screen_password_skip_fingerprint_message;
        } else {
            return R.string.lock_screen_password_skip_message;
        }
    }

    @StringRes
    private int getPinSkipMessageRes(boolean hasFace, boolean hasFingerprint) {
        if (hasFace && hasFingerprint) {
            return R.string.lock_screen_pin_skip_biometrics_message;
        } else if (hasFace) {
            return R.string.lock_screen_pin_skip_face_message;
        } else if (hasFingerprint) {
            return R.string.lock_screen_pin_skip_fingerprint_message;
        } else {
            return R.string.lock_screen_pin_skip_message;
        }
    }

    private String getSkipSetupTitle(int screenTypeResId, boolean hasFingerprint,
            boolean hasFace) {
        return getString(R.string.lock_screen_skip_setup_title,
                BiometricUtils.getCombinedScreenLockOptions(getContext(),
                        getString(screenTypeResId), hasFingerprint, hasFace));
    }

    @Override
    public void onClick(DialogInterface dialog, int button) {
        Activity activity = getActivity();
        switch (button) {
            case DialogInterface.BUTTON_POSITIVE:
                activity.setResult(RESULT_SKIP);
                activity.finish();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                View view = activity.getCurrentFocus();
                if(view != null) {
                    view.requestFocus();
                    InputMethodManager imm = (InputMethodManager) activity
                            .getSystemService(Activity.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
                }
                break;
        }
    }

    public void show(FragmentManager manager) {
        show(manager, TAG_SKIP_DIALOG);
    }

    private void styleDialogTitle(AlertDialog dialog) {
        final int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        final View title = titleId == 0 ? null : dialog.findViewById(titleId);
        if (title instanceof TextView titleView) {
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            titleView.setTypeface(Typeface.create(
                    getString(com.android.internal.R.string.config_headlineFontFamily),
                    Typeface.BOLD));
        }
    }

    private void styleDialogButton(Button button, boolean filled) {
        if (button == null) {
            return;
        }
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(dp(48));
        button.setPadding(dp(24), 0, dp(24), 0);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(Typeface.create(
                getString(com.android.internal.R.string.config_bodyFontFamily),
                Typeface.BOLD));
        if (filled) {
            button.setBackgroundResource(com.android.settingslib.widget.theme.R.drawable
                    .settingslib_expressive_button_background_filled);
            button.setBackgroundTintList(ColorStateList.valueOf(getColor(
                    com.android.settingslib.widget.theme.R.color.settingslib_materialColorPrimary)));
            button.setTextColor(getColor(
                    com.android.settingslib.widget.theme.R.color.settingslib_materialColorOnPrimary));
        } else {
            button.setBackgroundResource(com.android.settingslib.widget.theme.R.drawable
                    .settingslib_expressive_button_background_outline);
            button.setBackgroundTintList(null);
            button.setTextColor(getColor(
                    com.android.settingslib.widget.theme.R.color.settingslib_materialColorPrimary));
        }
    }

    private int getColor(int resId) {
        return requireContext().getColor(resId);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
