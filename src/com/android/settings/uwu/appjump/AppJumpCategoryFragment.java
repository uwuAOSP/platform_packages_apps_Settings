/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.os.Bundle;
import android.os.RemoteException;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppJumpCategoryFragment extends SettingsBasePreferenceFragment {
    private static final String ARG_CATEGORY = "category";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private AppJumpPolicyBackend mBackend;
    private int mCategory;
    private PreferenceCategory mAppList;
    private FooterPreference mEmptyFooter;
    private final ArrayList<AppJumpPolicyBackend.AppEntry> mAllApps = new ArrayList<>();
    private String mSearchQuery;

    public static AppJumpCategoryFragment newInstance(int category) {
        AppJumpCategoryFragment fragment = new AppJumpCategoryFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CATEGORY, category);
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
        addPreferencesFromResource(R.xml.app_jump_category_list);
        mBackend = new AppJumpPolicyBackend(requireContext());
        mCategory =
                requireArguments().getInt(ARG_CATEGORY, AppJumpPolicyBackend.CATEGORY_SOURCE_ASK);
        requireActivity().setTitle(AppJumpPolicyBackend.getCategoryTitleRes(mCategory));
        mAppList = findPreference("app_jump_category_apps");
        mEmptyFooter = findPreference("app_jump_category_empty");
        if (mEmptyFooter != null) {
            mEmptyFooter.setTitle(R.string.app_jump_category_empty);
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
                        final List<AppJumpPolicyBackend.AppEntry> apps = mBackend.loadUserApps();
                        final ArrayList<AppJumpPolicyBackend.AppEntry> matchingApps =
                                new ArrayList<>();
                        for (AppJumpPolicyBackend.AppEntry app : apps) {
                            AppJumpPolicyBackend.AppPolicyState state =
                                    mBackend.getPolicyState(app.getPackageName());
                            if (mBackend.matchesCategory(state, mCategory)) {
                                matchingApps.add(app);
                            }
                        }
                        final android.app.Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        activity.runOnUiThread(
                                () -> {
                                    mAllApps.clear();
                                    mAllApps.addAll(matchingApps);
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
        rebuildPreferences(mBackend.filterApps(mAllApps, mSearchQuery));
    }

    private void rebuildPreferences(List<AppJumpPolicyBackend.AppEntry> apps) {
        if (mAppList == null) {
            return;
        }
        mAppList.removeAll();
        for (AppJumpPolicyBackend.AppEntry app : apps) {
            AppPreference preference = new AppPreference(requireContext());
            preference.setTitle(app.getLabel());
            preference.setSummary(app.getPackageName());
            preference.setIcon(app.getIcon());
            preference.setOnPreferenceClickListener(
                    pref -> {
                        startActivity(
                                AppJumpSettingsActivity.createAppDetailIntent(
                                        requireContext(), app.getPackageName()));
                        return true;
                    });
            mAppList.addPreference(preference);
        }
        if (mEmptyFooter != null) {
            mEmptyFooter.setVisible(apps.isEmpty());
            mEmptyFooter.setTitle(
                    mSearchQuery == null || mSearchQuery.isEmpty()
                            ? getString(R.string.app_jump_category_empty)
                            : getString(R.string.app_jump_search_empty));
        }
    }
}
