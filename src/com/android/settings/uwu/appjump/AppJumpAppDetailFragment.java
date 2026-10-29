/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.app.ActivityTaskManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.widget.AppPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class AppJumpAppDetailFragment extends SettingsBasePreferenceFragment {
    private static final String ARG_PACKAGE_NAME = "package_name";

    private android.app.AppOpsManager.OnOpChangedListener mPolicyWatcher;
    private AppJumpPolicyBackend mBackend;
    private String mPackageName;
    private AppPreference mHeaderPreference;
    private SelectorWithWidgetPreference mSourceAllowPreference;
    private SelectorWithWidgetPreference mSourceAskPreference;
    private SelectorWithWidgetPreference mSourceBlockPreference;
    private SelectorWithWidgetPreference mTargetAllowPreference;
    private SelectorWithWidgetPreference mTargetAskPreference;
    private SelectorWithWidgetPreference mTargetBlockPreference;
    private Preference mSourceRulesPreference;
    private Preference mTargetRulesPreference;
    private FooterPreference mFooterPreference;

    public static AppJumpAppDetailFragment newInstance(String packageName) {
        AppJumpAppDetailFragment fragment = new AppJumpAppDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE_NAME, packageName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_jump_app_detail);
        mBackend = new AppJumpPolicyBackend(requireContext());
        mPackageName = requireArguments().getString(ARG_PACKAGE_NAME);
        requireActivity().setTitle(R.string.app_jump_detail_title);

        mHeaderPreference = findPreference("app_jump_detail_header");
        mSourceAllowPreference = findPreference("app_jump_detail_always_allow");
        mSourceAskPreference = findPreference("app_jump_detail_ask");
        mSourceBlockPreference = findPreference("app_jump_detail_always_block");
        mTargetAllowPreference = findPreference("app_jump_detail_target_allow");
        mTargetAskPreference = findPreference("app_jump_detail_target_ask");
        mTargetBlockPreference = findPreference("app_jump_detail_target_block");
        mSourceRulesPreference = findPreference("app_jump_detail_source_rules");
        mTargetRulesPreference = findPreference("app_jump_detail_target_rules");
        mFooterPreference = findPreference("app_jump_detail_footer");

        bindActions();
    }

    @Override
    public void onStart() {
        super.onStart();
        mPolicyWatcher =
                android.app.UwuAppPolicyManager.watch(
                        requireContext(),
                        new android.os.Handler(android.os.Looper.getMainLooper()),
                        android.app.UwuAppPolicyManager.FEATURE_JUMP,
                        user -> {
                            if (isAdded() && user == android.os.UserHandle.myUserId())
                                reloadState();
                        });
    }

    @Override
    public void onStop() {
        requireContext()
                .getSystemService(android.app.AppOpsManager.class)
                .stopWatchingMode(mPolicyWatcher);
        mPolicyWatcher = null;
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadState();
    }

    private void bindActions() {
        if (mSourceAllowPreference != null) {
            mSourceAllowPreference.setOnClickListener(
                    pref -> updateSourceMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW));
        }
        if (mSourceAskPreference != null) {
            mSourceAskPreference.setOnClickListener(
                    pref -> updateSourceMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK));
        }
        if (mSourceBlockPreference != null) {
            mSourceBlockPreference.setOnClickListener(
                    pref -> updateSourceMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK));
        }
        if (mTargetAllowPreference != null) {
            mTargetAllowPreference.setOnClickListener(
                    pref -> updateTargetMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW));
        }
        if (mTargetAskPreference != null) {
            mTargetAskPreference.setOnClickListener(
                    pref -> updateTargetMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK));
        }
        if (mTargetBlockPreference != null) {
            mTargetBlockPreference.setOnClickListener(
                    pref -> updateTargetMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK));
        }
        if (mSourceRulesPreference != null) {
            mSourceRulesPreference.setOnPreferenceClickListener(
                    pref -> {
                        startActivity(
                                AppJumpSettingsActivity.createSourceRulesIntent(
                                        requireContext(), mPackageName));
                        return true;
                    });
        }
        if (mTargetRulesPreference != null) {
            mTargetRulesPreference.setOnPreferenceClickListener(
                    pref -> {
                        startActivity(
                                AppJumpSettingsActivity.createTargetRulesIntent(
                                        requireContext(), mPackageName));
                        return true;
                    });
        }
    }

    private void reloadState() {
        if (mPackageName == null) {
            return;
        }
        try {
            final AppJumpPolicyBackend.AppPolicyState state = mBackend.getPolicyState(mPackageName);
            final PackageManager pm = requireContext().getPackageManager();
            final ApplicationInfo appInfo =
                    pm.getApplicationInfo(mPackageName, PackageManager.MATCH_ALL);
            final boolean systemApp = mBackend.isSystemApp(appInfo);
            final CharSequence appLabel = pm.getApplicationLabel(appInfo);
            if (mHeaderPreference != null) {
                mHeaderPreference.setTitle(appLabel);
                mHeaderPreference.setSummary(mPackageName);
                mHeaderPreference.setIcon(pm.getApplicationIcon(appInfo));
                requireActivity().setTitle(appLabel);
            }
            if (mFooterPreference != null) {
                final StringBuilder footer =
                        new StringBuilder(getString(R.string.app_jump_settings_footer));
                if (systemApp) {
                    footer.append('\n').append(getString(R.string.app_jump_system_app_warning));
                }
                mFooterPreference.setTitle(footer.toString());
            }
            syncSourceMode(state.sourceMode);
            syncTargetMode(state.targetMode);
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            Toast.makeText(requireContext(), R.string.app_jump_load_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void updateSourceMode(int sourceMode) {
        try {
            mBackend.setSourceMode(mPackageName, sourceMode);
            syncSourceMode(sourceMode);
        } catch (RemoteException e) {
            Toast.makeText(requireContext(), R.string.app_jump_update_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void updateTargetMode(int targetMode) {
        try {
            mBackend.setTargetMode(mPackageName, targetMode);
            syncTargetMode(targetMode);
        } catch (RemoteException e) {
            Toast.makeText(requireContext(), R.string.app_jump_update_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void syncSourceMode(int sourceMode) {
        if (mSourceAllowPreference != null) {
            mSourceAllowPreference.setChecked(
                    sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW);
        }
        if (mSourceAskPreference != null) {
            mSourceAskPreference.setChecked(
                    sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK);
        }
        if (mSourceBlockPreference != null) {
            mSourceBlockPreference.setChecked(
                    sourceMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK);
        }
    }

    private void syncTargetMode(int targetMode) {
        if (mTargetAllowPreference != null) {
            mTargetAllowPreference.setChecked(
                    targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW);
        }
        if (mTargetAskPreference != null) {
            mTargetAskPreference.setChecked(
                    targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK);
        }
        if (mTargetBlockPreference != null) {
            mTargetBlockPreference.setChecked(
                    targetMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK);
        }
    }
}
