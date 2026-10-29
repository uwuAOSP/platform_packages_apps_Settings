/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settingslib.widget.AppPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppJumpSourceRulesFragment extends SettingsBasePreferenceFragment {
    private static final String ARG_TARGET_PACKAGE = "target_package";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private AppJumpPolicyBackend mBackend;
    private String mTargetPackage;
    private PreferenceCategory mAppList;
    private FooterPreference mEmptyFooter;
    private final ArrayList<AppJumpPolicyBackend.AppEntry> mAllEntries = new ArrayList<>();
    private String mSearchQuery;

    public static AppJumpSourceRulesFragment newInstance(String targetPackage) {
        AppJumpSourceRulesFragment fragment = new AppJumpSourceRulesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TARGET_PACKAGE, targetPackage);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.app_jump_source_rules);
        mBackend = new AppJumpPolicyBackend(requireContext());
        mTargetPackage = requireArguments().getString(ARG_TARGET_PACKAGE);
        requireActivity().setTitle(R.string.app_jump_source_rules_title);
        mAppList = findPreference("app_jump_source_rules_apps");
        mEmptyFooter = findPreference("app_jump_source_rules_empty");
        if (mEmptyFooter != null) {
            mEmptyFooter.setTitle(R.string.app_jump_source_rules_empty);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadApps();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        AppJumpSearchUtils.addSearchMenuItem(
                inflater,
                menu,
                getViewLifecycleOwner(),
                mBackend,
                getString(R.string.app_jump_search_title),
                mSearchQuery,
                new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        mSearchQuery = newText;
                        applyFilter();
                        return true;
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (AppJumpSearchUtils.handleListOptionsItemSelected(
                item,
                mBackend,
                this::reloadApps,
                () -> requireActivity().invalidateOptionsMenu())) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadApps() {
        mExecutor.execute(
                () -> {
                    try {
                        final List<AppJumpPolicyBackend.AppEntry> apps =
                                mBackend.loadUserAppsExcluding(mTargetPackage);
                        final android.app.Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        activity.runOnUiThread(
                                () -> {
                                    mAllEntries.clear();
                                    mAllEntries.addAll(apps);
                                    applyFilter();
                                });
                    } catch (RuntimeException e) {
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

    private void applyFilter() {
        if (!isAdded() || getView() == null) {
            return;
        }
        final List<AppJumpPolicyBackend.AppEntry> entries;
        if (mSearchQuery == null || mSearchQuery.trim().isEmpty()) {
            entries = new ArrayList<>(mAllEntries);
        } else {
            final String query = mSearchQuery.trim().toLowerCase(Locale.ROOT);
            entries = new ArrayList<>();
            for (AppJumpPolicyBackend.AppEntry entry : mAllEntries) {
                if (entry.matchesQuery(query)) {
                    entries.add(entry);
                }
            }
        }
        rebuildPreferences(entries);
    }

    private void rebuildPreferences(List<AppJumpPolicyBackend.AppEntry> entries) {
        if (mAppList == null) {
            return;
        }
        mAppList.removeAll();
        for (AppJumpPolicyBackend.AppEntry app : entries) {
            AppPreference preference = new AppPreference(requireContext());
            preference.setTitle(app.getLabel());
            preference.setSummary(app.getPackageName());
            preference.setIcon(app.getIcon());
            preference.setOnPreferenceClickListener(
                    pref -> {
                        startActivity(
                                AppJumpSettingsActivity.createTargetRuleDetailIntent(
                                        requireContext(), app.getPackageName(), mTargetPackage));
                        return true;
                    });
            mAppList.addPreference(preference);
        }
        if (mEmptyFooter != null) {
            mEmptyFooter.setVisible(entries.isEmpty());
            mEmptyFooter.setTitle(
                    mSearchQuery == null || mSearchQuery.isEmpty()
                            ? getString(R.string.app_jump_source_rules_empty)
                            : getString(R.string.app_jump_search_empty));
        }
    }
}
