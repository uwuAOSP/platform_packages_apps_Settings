/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.app.ActivityTaskManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settingslib.widget.AppPreference;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppJumpTargetRulesFragment extends SettingsBasePreferenceFragment {
    private static final String ARG_SOURCE_PACKAGE = "source_package";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private AppJumpPolicyBackend mBackend;
    private String mSourcePackage;
    private PreferenceCategory mBlockedAppList;
    private PreferenceCategory mAllowedAppList;
    private PreferenceCategory mDefaultAppList;
    private FooterPreference mEmptyFooter;
    private final ArrayList<TargetRuleEntry> mAllEntries = new ArrayList<>();
    private String mSearchQuery;

    public static AppJumpTargetRulesFragment newInstance(String sourcePackage) {
        AppJumpTargetRulesFragment fragment = new AppJumpTargetRulesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SOURCE_PACKAGE, sourcePackage);
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
        addPreferencesFromResource(R.xml.app_jump_target_rules);
        mBackend = new AppJumpPolicyBackend(requireContext());
        mSourcePackage = requireArguments().getString(ARG_SOURCE_PACKAGE);
        requireActivity().setTitle(R.string.app_jump_target_rules_title);
        mBlockedAppList = findPreference("app_jump_target_rules_blocked");
        mAllowedAppList = findPreference("app_jump_target_rules_allowed");
        mDefaultAppList = findPreference("app_jump_target_rules_default");
        mEmptyFooter = findPreference("app_jump_target_rules_empty");
        if (mEmptyFooter != null) {
            mEmptyFooter.setTitle(R.string.app_jump_target_rules_empty);
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
                                mBackend.loadUserAppsExcluding(mSourcePackage);
                        final ArrayList<TargetRuleEntry> entries = new ArrayList<>();
                        for (AppJumpPolicyBackend.AppEntry app : apps) {
                            final int pairMode =
                                    mBackend.getPairMode(mSourcePackage, app.getPackageName());
                            entries.add(new TargetRuleEntry(app, pairMode));
                        }
                        final android.app.Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        activity.runOnUiThread(
                                () -> {
                                    mAllEntries.clear();
                                    mAllEntries.addAll(entries);
                                    applyFilter();
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

    private void applyFilter() {
        if (!isAdded() || getView() == null) {
            return;
        }
        final List<TargetRuleEntry> entries;
        if (mSearchQuery == null || mSearchQuery.trim().isEmpty()) {
            entries = new ArrayList<>(mAllEntries);
        } else {
            final String query = mSearchQuery.trim().toLowerCase(Locale.ROOT);
            entries = new ArrayList<>();
            for (TargetRuleEntry entry : mAllEntries) {
                if (entry.app.matchesQuery(query)) {
                    entries.add(entry);
                }
            }
        }
        rebuildPreferences(entries);
    }

    private void rebuildPreferences(List<TargetRuleEntry> entries) {
        if (mBlockedAppList == null || mAllowedAppList == null || mDefaultAppList == null) {
            return;
        }
        mBlockedAppList.removeAll();
        mAllowedAppList.removeAll();
        mDefaultAppList.removeAll();
        for (TargetRuleEntry entry : entries) {
            final AppJumpPolicyBackend.AppEntry app = entry.app;
            AppPreference preference = new AppPreference(requireContext());
            preference.setTitle(app.getLabel());
            preference.setSummary(app.getPackageName());
            preference.setIcon(app.getIcon());
            preference.setOnPreferenceClickListener(
                    pref -> {
                        startActivity(
                                AppJumpSettingsActivity.createTargetRuleDetailIntent(
                                        requireContext(), mSourcePackage, app.getPackageName()));
                        return true;
                    });
            getCategoryForEntry(entry).addPreference(preference);
        }
        syncCategoryVisibility(mBlockedAppList);
        syncCategoryVisibility(mAllowedAppList);
        syncCategoryVisibility(mDefaultAppList);
        if (mEmptyFooter != null) {
            mEmptyFooter.setVisible(entries.isEmpty());
            mEmptyFooter.setTitle(
                    mSearchQuery == null || mSearchQuery.isEmpty()
                            ? getString(R.string.app_jump_target_rules_empty)
                            : getString(R.string.app_jump_search_empty));
        }
    }

    private PreferenceCategory getCategoryForEntry(TargetRuleEntry entry) {
        if (entry.pairMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_BLOCK) {
            return mBlockedAppList;
        }
        if (entry.pairMode == ActivityTaskManager.APP_JUMP_SOURCE_MODE_ALLOW) {
            return mAllowedAppList;
        }
        return mDefaultAppList;
    }

    private void syncCategoryVisibility(@NonNull PreferenceGroup category) {
        category.setVisible(category.getPreferenceCount() > 0);
    }

    private static final class TargetRuleEntry {
        final AppJumpPolicyBackend.AppEntry app;
        final int pairMode;

        TargetRuleEntry(AppJumpPolicyBackend.AppEntry app, int pairMode) {
            this.app = app;
            this.pairMode = pairMode;
        }
    }
}
