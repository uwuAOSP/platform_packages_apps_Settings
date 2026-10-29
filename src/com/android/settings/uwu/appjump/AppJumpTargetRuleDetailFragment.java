/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.app.ActivityTaskManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settingslib.widget.AppPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

public class AppJumpTargetRuleDetailFragment extends SettingsBasePreferenceFragment {
    private static final String ARG_SOURCE_PACKAGE = "source_package";
    private static final String ARG_TARGET_PACKAGE = "target_package";

    private AppJumpPolicyBackend mBackend;
    private String mSourcePackage;
    private String mTargetPackage;
    private AppPreference mHeaderPreference;
    private SelectorWithWidgetPreference mInheritPreference;
    private SelectorWithWidgetPreference mAlwaysAllowPreference;
    private SelectorWithWidgetPreference mAskPreference;
    private SelectorWithWidgetPreference mAlwaysBlockPreference;
    private FooterPreference mFooterPreference;

    public static AppJumpTargetRuleDetailFragment newInstance(
            String sourcePackage, String targetPackage) {
        AppJumpTargetRuleDetailFragment fragment = new AppJumpTargetRuleDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOURCE_PACKAGE, sourcePackage);
        args.putString(ARG_TARGET_PACKAGE, targetPackage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_jump_target_rule_detail);
        mBackend = new AppJumpPolicyBackend(requireContext());
        mSourcePackage = requireArguments().getString(ARG_SOURCE_PACKAGE);
        mTargetPackage = requireArguments().getString(ARG_TARGET_PACKAGE);
        requireActivity().setTitle(R.string.app_jump_target_rule_detail_title);

        mHeaderPreference = findPreference("app_jump_target_rule_header");
        mInheritPreference = findPreference("app_jump_target_rule_inherit");
        mAlwaysAllowPreference = findPreference("app_jump_target_rule_always_allow");
        mAskPreference = findPreference("app_jump_target_rule_ask");
        mAlwaysBlockPreference = findPreference("app_jump_target_rule_always_block");
        mFooterPreference = findPreference("app_jump_target_rule_footer");

        bindActions();
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadState();
    }

    private void bindActions() {
        if (mInheritPreference != null) {
            mInheritPreference.setOnClickListener(
                    pref -> updatePairMode(ActivityTaskManager.APP_JUMP_PAIR_MODE_INHERIT));
        }
        if (mAlwaysAllowPreference != null) {
            mAlwaysAllowPreference.setOnClickListener(
                    pref -> updatePairMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW));
        }
        if (mAskPreference != null) {
            mAskPreference.setOnClickListener(
                    pref -> updatePairMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK));
        }
        if (mAlwaysBlockPreference != null) {
            mAlwaysBlockPreference.setOnClickListener(
                    pref -> updatePairMode(ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK));
        }
    }

    private void reloadState() {
        if (mSourcePackage == null || mTargetPackage == null) {
            return;
        }
        try {
            final PackageManager pm = requireContext().getPackageManager();
            final android.content.pm.ApplicationInfo targetAppInfo =
                    pm.getApplicationInfo(mTargetPackage, PackageManager.MATCH_ALL);
            final CharSequence targetLabel = pm.getApplicationLabel(targetAppInfo);
            final android.content.pm.ApplicationInfo sourceAppInfo =
                    pm.getApplicationInfo(mSourcePackage, PackageManager.MATCH_ALL);
            final CharSequence sourceLabel = pm.getApplicationLabel(sourceAppInfo);
            final int pairMode = mBackend.getPairMode(mSourcePackage, mTargetPackage);
            if (mHeaderPreference != null) {
                mHeaderPreference.setTitle(targetLabel);
                mHeaderPreference.setSummary(mTargetPackage);
                mHeaderPreference.setIcon(pm.getApplicationIcon(targetAppInfo));
                requireActivity().setTitle(targetLabel);
            }
            if (mFooterPreference != null) {
                final StringBuilder footer =
                        new StringBuilder(
                                getString(R.string.app_jump_target_rule_footer, sourceLabel));
                if (mBackend.isSystemApp(targetAppInfo) || mBackend.isSystemApp(sourceAppInfo)) {
                    footer.append('\n').append(getString(R.string.app_jump_system_app_warning));
                }
                mFooterPreference.setTitle(footer.toString());
            }
            syncPairMode(pairMode);
        } catch (PackageManager.NameNotFoundException | RemoteException e) {
            Toast.makeText(requireContext(), R.string.app_jump_load_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void updatePairMode(int mode) {
        try {
            mBackend.setPairMode(mSourcePackage, mTargetPackage, mode);
            syncPairMode(mode);
        } catch (RemoteException e) {
            Toast.makeText(requireContext(), R.string.app_jump_update_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void syncPairMode(int mode) {
        if (mInheritPreference != null) {
            mInheritPreference.setChecked(mode == ActivityTaskManager.APP_JUMP_PAIR_MODE_INHERIT);
        }
        if (mAlwaysAllowPreference != null) {
            mAlwaysAllowPreference.setChecked(
                    mode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW);
        }
        if (mAskPreference != null) {
            mAskPreference.setChecked(mode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ASK);
        }
        if (mAlwaysBlockPreference != null) {
            mAlwaysBlockPreference.setChecked(
                    mode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK);
        }
    }
}
