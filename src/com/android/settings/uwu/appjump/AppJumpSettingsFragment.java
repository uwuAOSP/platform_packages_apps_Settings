/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppJumpSettingsFragment extends SettingsBasePreferenceFragment {
    private static final String KEY_ENABLED = "app_jump_enabled";
    private static final String KEY_ALL_APPS = "app_jump_all_apps";
    private static final String KEY_SECTION_OUTGOING = "app_jump_section_outgoing";
    private static final String KEY_SECTION_INCOMING = "app_jump_section_incoming";
    private static final String KEY_SOURCE_ALLOW = "app_jump_always_allow";
    private static final String KEY_SOURCE_BLOCK = "app_jump_always_block";
    private static final String KEY_SOURCE_ASK = "app_jump_ask";
    private static final String KEY_TARGET_ALLOW = "app_jump_target_allow";
    private static final String KEY_TARGET_ASK = "app_jump_target_ask";
    private static final String KEY_TARGET_BLOCK = "app_jump_target_block";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private AppJumpPolicyBackend mBackend;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_jump_settings);
        requireActivity().setTitle(R.string.app_jump_settings_title);
        mBackend = new AppJumpPolicyBackend(requireContext());
        bindEnabledSwitch();
        bindAllApps();
        bindCategory(KEY_SOURCE_ALLOW, AppJumpPolicyBackend.CATEGORY_SOURCE_ALLOW);
        bindCategory(KEY_SOURCE_BLOCK, AppJumpPolicyBackend.CATEGORY_SOURCE_BLOCK);
        bindCategory(KEY_SOURCE_ASK, AppJumpPolicyBackend.CATEGORY_SOURCE_ASK);
        bindCategory(KEY_TARGET_ALLOW, AppJumpPolicyBackend.CATEGORY_TARGET_ALLOW);
        bindCategory(KEY_TARGET_ASK, AppJumpPolicyBackend.CATEGORY_TARGET_ASK);
        bindCategory(KEY_TARGET_BLOCK, AppJumpPolicyBackend.CATEGORY_TARGET_BLOCK);

        FooterPreference footerPreference = findPreference("app_jump_settings_footer");
        if (footerPreference != null) {
            footerPreference.setTitle(R.string.app_jump_settings_footer);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadEnabledState();
        reloadSummaries();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    private void bindCategory(String key, int category) {
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(
                pref -> {
                    startActivity(
                            AppJumpSettingsActivity.createCategoryIntent(
                                    requireContext(), category));
                    return true;
                });
    }

    private void bindEnabledSwitch() {
        SwitchPreferenceCompat preference = findPreference(KEY_ENABLED);
        if (preference == null) {
            return;
        }
        preference.setPersistent(false);
        preference.setOnPreferenceChangeListener(
                (pref, newValue) -> {
                    final boolean enabled = (Boolean) newValue;
                    try {
                        mBackend.setEnabled(enabled);
                        updateRulesEnabledState(enabled);
                        return true;
                    } catch (RemoteException | RuntimeException e) {
                        Toast.makeText(
                                        requireContext(),
                                        R.string.app_jump_update_failed,
                                        Toast.LENGTH_SHORT)
                                .show();
                        return false;
                    }
                });
    }

    private void bindAllApps() {
        Preference preference = findPreference(KEY_ALL_APPS);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(
                pref -> {
                    startActivity(AppJumpSettingsActivity.createAllAppsIntent(requireContext()));
                    return true;
                });
    }

    private void reloadEnabledState() {
        SwitchPreferenceCompat preference = findPreference(KEY_ENABLED);
        if (preference == null) {
            return;
        }
        try {
            final boolean enabled = mBackend.isEnabled();
            preference.setChecked(enabled);
            updateRulesEnabledState(enabled);
        } catch (RemoteException | RuntimeException e) {
            Toast.makeText(requireContext(), R.string.app_jump_load_failed, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void updateRulesEnabledState(boolean enabled) {
        setPreferenceEnabled(KEY_ALL_APPS, enabled);
        setPreferenceEnabled(KEY_SECTION_OUTGOING, enabled);
        setPreferenceEnabled(KEY_SECTION_INCOMING, enabled);
        setPreferenceEnabled(KEY_SOURCE_ALLOW, enabled);
        setPreferenceEnabled(KEY_SOURCE_BLOCK, enabled);
        setPreferenceEnabled(KEY_SOURCE_ASK, enabled);
        setPreferenceEnabled(KEY_TARGET_ALLOW, enabled);
        setPreferenceEnabled(KEY_TARGET_ASK, enabled);
        setPreferenceEnabled(KEY_TARGET_BLOCK, enabled);

        FooterPreference footerPreference = findPreference("app_jump_settings_footer");
        if (footerPreference != null) {
            footerPreference.setTitle(
                    enabled
                            ? R.string.app_jump_settings_footer
                            : R.string.app_jump_settings_disabled_footer);
        }
    }

    private void setPreferenceEnabled(String key, boolean enabled) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setEnabled(enabled);
        }
    }

    private void reloadSummaries() {
        mExecutor.execute(
                () -> {
                    try {
                        final List<AppJumpPolicyBackend.AppEntry> apps = mBackend.loadUserApps();
                        int sourceAllowCount = 0;
                        int sourceBlockCount = 0;
                        int sourceAskCount = 0;
                        int targetAllowCount = 0;
                        int targetAskCount = 0;
                        int targetBlockCount = 0;
                        for (AppJumpPolicyBackend.AppEntry app : apps) {
                            final AppJumpPolicyBackend.AppPolicyState state =
                                    mBackend.getPolicyState(app.getPackageName());
                            if (mBackend.matchesCategory(
                                    state, AppJumpPolicyBackend.CATEGORY_SOURCE_ALLOW)) {
                                sourceAllowCount++;
                            }
                            if (mBackend.matchesCategory(
                                    state, AppJumpPolicyBackend.CATEGORY_SOURCE_BLOCK)) {
                                sourceBlockCount++;
                            }
                            if (mBackend.matchesCategory(
                                    state, AppJumpPolicyBackend.CATEGORY_SOURCE_ASK)) {
                                sourceAskCount++;
                            }
                            if (mBackend.matchesCategory(
                                    state, AppJumpPolicyBackend.CATEGORY_TARGET_ALLOW)) {
                                targetAllowCount++;
                            }
                            if (mBackend.matchesCategory(
                                    state, AppJumpPolicyBackend.CATEGORY_TARGET_ASK)) {
                                targetAskCount++;
                            }
                            if (mBackend.matchesCategory(
                                    state, AppJumpPolicyBackend.CATEGORY_TARGET_BLOCK)) {
                                targetBlockCount++;
                            }
                        }
                        final int totalApps = apps.size();
                        final int finalSourceAllowCount = sourceAllowCount;
                        final int finalSourceBlockCount = sourceBlockCount;
                        final int finalSourceAskCount = sourceAskCount;
                        final int finalTargetAllowCount = targetAllowCount;
                        final int finalTargetAskCount = targetAskCount;
                        final int finalTargetBlockCount = targetBlockCount;
                        final android.app.Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        activity.runOnUiThread(
                                () -> {
                                    updateSummary(
                                            KEY_ALL_APPS,
                                            getString(
                                                    R.string.app_jump_all_apps_summary, totalApps));
                                    updateSummary(
                                            KEY_SOURCE_ALLOW, finalSourceAllowCount, totalApps);
                                    updateSummary(
                                            KEY_SOURCE_BLOCK, finalSourceBlockCount, totalApps);
                                    updateSummary(KEY_SOURCE_ASK, finalSourceAskCount, totalApps);
                                    updateSummary(
                                            KEY_TARGET_ALLOW, finalTargetAllowCount, totalApps);
                                    updateSummary(KEY_TARGET_ASK, finalTargetAskCount, totalApps);
                                    updateSummary(
                                            KEY_TARGET_BLOCK, finalTargetBlockCount, totalApps);
                                });
                    } catch (RemoteException | RuntimeException e) {
                        final android.app.Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        activity.runOnUiThread(
                                () ->
                                        Toast.makeText(
                                                        activity,
                                                        R.string.app_jump_load_failed,
                                                        Toast.LENGTH_SHORT)
                                                .show());
                    }
                });
    }

    private void updateSummary(String key, int count, int total) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(
                    getString(R.string.app_jump_category_count_summary, count, total));
        }
    }

    private void updateSummary(String key, String summary) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(summary);
        }
    }
}
