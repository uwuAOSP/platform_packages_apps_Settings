/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.uwuaosp.appops.clipboard

import android.app.AppOpsManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.graphics.drawable.BitmapDrawable
import android.widget.SearchView
import android.widget.Toast
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import com.android.settings.R
import org.uwuaosp.appops.AppPolicyActivity
import org.uwuaosp.appops.sensors.SensorPolicyAppRepository
import com.android.settingslib.widget.AppPreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** Native Settings preference list for clipboard and sensor app policies. */
class AppPolicyAppListFragment : SettingsBasePreferenceFragment() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var feature: String
    private lateinit var appCategory: PreferenceCategory
    private lateinit var emptyFooter: FooterPreference
    private var searchQuery: String = ""
    private var appRows: List<AppRow> = emptyList()
    private var policyWatcher: AppOpsManager.OnOpChangedListener? = null
    private val promptsObserver = object : android.database.ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) { reloadApps() }
    }

    private data class AppRow(
        val label: String,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable,
        val summary: String,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        feature = requireArguments().getString(ARG_FEATURE).orEmpty()
        require(feature == FEATURE_CLIPBOARD || feature == FEATURE_SENSOR)
        setHasOptionsMenu(true)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.createPreferenceScreen(requireContext()).also { preferenceScreen = it }
        requireActivity().setTitle(
            if (feature == FEATURE_CLIPBOARD) R.string.clipboard_policy_title
            else R.string.sensor_policy_title,
        )
        if (feature == FEATURE_CLIPBOARD) {
            val prompts = SwitchPreference(requireContext()).apply {
                key = "clipboard_prompts_enabled"
                setTitle(R.string.clipboard_policy_prompt_enabled)
                isPersistent = false
                isChecked = ClipboardPolicySecureSettings.isPromptEnabled(requireContext())
                setOnPreferenceChangeListener { _, value ->
                    val enabled = value as Boolean
                    if (ClipboardPolicySecureSettings.setPromptEnabled(requireContext(), enabled)) {
                        reloadApps()
                        true
                    } else {
                        Toast.makeText(requireContext(), R.string.clipboard_policy_prompt_update_failed, Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }
            preferenceScreen.addPreference(prompts)
        }
        PreferenceCategory(requireContext()).also {
            it.setTitle(if (feature == FEATURE_CLIPBOARD) R.string.clipboard_policy_apps_category else R.string.sensor_policy_apps_category)
            appCategory = it
            preferenceScreen.addPreference(it)
        }
        FooterPreference(requireContext()).also {
            it.key = "uwu_app_policy_empty"
            emptyFooter = it
            it.isSelectable = false
            it.isVisible = false
            preferenceScreen.addPreference(it)
        }
    }

    override fun onResume() {
        super.onResume()
        if (feature == FEATURE_CLIPBOARD) {
            requireContext().contentResolver.registerContentObserver(
                android.provider.Settings.Secure.getUriFor(
                    android.provider.Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
                ), false, promptsObserver,
            )
        }
        policyWatcher = android.app.UwuAppPolicyManager.watch(
            requireContext(), mainHandler,
        ) { user -> if (user == android.os.UserHandle.myUserId()) reloadApps() }
        reloadApps()
    }

    override fun onPause() {
        policyWatcher?.let { requireContext().getSystemService(AppOpsManager::class.java).stopWatchingMode(it) }
        policyWatcher = null
        if (feature == FEATURE_CLIPBOARD) {
            requireContext().contentResolver.unregisterContentObserver(promptsObserver)
        }
        super.onPause()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        val search = menu.add(R.string.clipboard_policy_search_apps)
        search.setIcon(android.R.drawable.ic_menu_search)
        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        val searchView = SearchView(requireContext())
        search.actionView = searchView
        searchView.queryHint = getString(
            if (feature == FEATURE_CLIPBOARD) R.string.clipboard_policy_search_apps
            else R.string.sensor_policy_search_apps,
        )
        searchView.setQuery(searchQuery, false)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String) = true
            override fun onQueryTextChange(newText: String): Boolean {
                searchQuery = newText
                renderApps()
                return true
            }
        })
    }

    private fun reloadApps() {
        executor.execute {
            val result = runCatching {
                val context = requireContext()
                if (feature == FEATURE_CLIPBOARD) {
                    ClipboardPolicyAppRepository(context).loadApps().map { app ->
                        AppRow(
                            app.label, app.packageName,
                            BitmapDrawable(context.resources, app.icon),
                            context.getString(R.string.clipboard_policy_read_write_summary,
                                if (app.readPolicy == app.writePolicy) context.getString(org.uwuaosp.appops.AppPolicyRepository.clipboardLabel(app.readPolicy))
                                else context.getString(R.string.clipboard_policy_separate)),
                        )
                    }
                } else {
                    SensorPolicyAppRepository(context).loadApps().map { app ->
                        AppRow(app.label, app.packageName,
                            BitmapDrawable(context.resources, app.icon),
                            context.getString(org.uwuaosp.appops.AppPolicyRepository.sensorLabel(app.policy)),
                        )
                    }
                }
            }
            mainHandler.post {
                if (!isAdded) return@post
                result.onSuccess { appRows = it; renderApps() }
                    .onFailure {
                        Toast.makeText(requireContext(),
                            if (feature == FEATURE_CLIPBOARD) R.string.clipboard_policy_load_failed else R.string.sensor_policy_load_failed,
                            Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun renderApps() {
        if (!isAdded) return
        val filtered = appRows.filter {
            searchQuery.isBlank() || it.label.contains(searchQuery, true) || it.packageName.contains(searchQuery, true)
        }
        appCategory.removeAll()
        filtered.forEach { app ->
            AppPreference(requireContext()).apply {
                title = app.label
                summary = "${app.summary}\n${app.packageName}"
                icon = app.icon
                setOnPreferenceClickListener {
                    startActivity(AppPolicyActivity.createIntent(requireContext(), app.packageName, feature))
                    true
                }
                appCategory.addPreference(this)
            }
        }
        emptyFooter.isVisible = filtered.isEmpty()
        emptyFooter.setTitle(
            if (searchQuery.isBlank()) {
                if (feature == FEATURE_CLIPBOARD) R.string.clipboard_policy_no_apps else R.string.sensor_policy_no_apps
            } else R.string.app_jump_search_empty,
        )
        if (feature == FEATURE_CLIPBOARD) {
            findPreference<SwitchPreference>("clipboard_prompts_enabled")?.isChecked =
                ClipboardPolicySecureSettings.isPromptEnabled(requireContext())
        }
    }

    companion object {
        private const val ARG_FEATURE = "feature"
        private const val FEATURE_CLIPBOARD = "clipboard"
        private const val FEATURE_SENSOR = "sensor"

        fun newInstance(feature: String) = AppPolicyAppListFragment().apply {
            arguments = Bundle().apply { putString(ARG_FEATURE, feature) }
        }
    }
}
