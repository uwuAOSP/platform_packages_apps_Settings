/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.uwu.appjump;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.R;

final class AppJumpSearchUtils {
    private AppJumpSearchUtils() {}

    static SearchView addSearchMenuItem(
            @NonNull MenuInflater inflater,
            @NonNull Menu menu,
            @NonNull LifecycleOwner viewLifecycleOwner,
            @NonNull AppJumpPolicyBackend backend,
            @NonNull CharSequence hint,
            @Nullable String currentQuery,
            @NonNull SearchView.OnQueryTextListener listener) {
        inflater.inflate(com.android.settings.R.menu.app_jump_search, menu);
        final MenuItem systemAppsItem =
                menu.findItem(com.android.settings.R.id.app_jump_system_apps_menu);
        if (systemAppsItem != null) {
            systemAppsItem.setTitle(
                    backend.isShowSystemAppsEnabled()
                            ? R.string.app_jump_hide_system_apps_title
                            : R.string.app_jump_show_system_apps_title);
        }
        final MenuItem searchItem = menu.findItem(com.android.settings.R.id.app_jump_search_menu);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint(hint);
        searchView.setOnQueryTextListener(listener);
        // Menu teardown can collapse the search view after the fragment has detached.
        viewLifecycleOwner
                .getLifecycle()
                .addObserver(
                        new DefaultLifecycleObserver() {
                            @Override
                            public void onDestroy(@NonNull LifecycleOwner owner) {
                                searchView.setOnQueryTextListener(null);
                                owner.getLifecycle().removeObserver(this);
                            }
                        });
        searchView.setMaxWidth(Integer.MAX_VALUE);
        final int searchTextId =
                searchView
                        .getContext()
                        .getResources()
                        .getIdentifier("search_src_text", "id", "android");
        final TextView searchText =
                searchTextId != 0 ? searchView.findViewById(searchTextId) : null;
        if (searchText != null) {
            searchText.setTextCursorDrawable(
                    searchText.getContext().getDrawable(R.drawable.search_cursor));
        }
        if (currentQuery != null && !currentQuery.isEmpty()) {
            searchItem.expandActionView();
            searchView.setIconified(false);
            searchView.setQuery(currentQuery, false);
            searchView.clearFocus();
        }
        return searchView;
    }

    static boolean handleListOptionsItemSelected(
            @NonNull MenuItem item,
            @NonNull AppJumpPolicyBackend backend,
            @NonNull Runnable reloadCallback,
            @NonNull Runnable invalidateMenuCallback) {
        if (item.getItemId() != R.id.app_jump_system_apps_menu) {
            return false;
        }
        backend.setShowSystemAppsEnabled(!backend.isShowSystemAppsEnabled());
        invalidateMenuCallback.run();
        reloadCallback.run();
        return true;
    }
}
