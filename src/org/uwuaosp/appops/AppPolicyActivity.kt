/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.uwuaosp.appops

import android.app.AppOpsManager
import android.app.UwuAppPolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.settings.R
import org.uwuaosp.appops.appjump.AppJumpSettingsActivity
import org.uwuaosp.appops.clipboard.AppPolicyAppListFragment
import org.uwuaosp.appops.clipboard.ClipboardPolicyActivity
import org.uwuaosp.appops.clipboard.ClipboardPolicySecureSettings
import org.uwuaosp.appops.sensors.SensorPolicyActivity
import org.uwuaosp.appops.sensors.SensorPolicySecureSettings
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity
import com.android.settingslib.collapsingtoolbar.R as ToolbarR
import com.android.settingslib.widget.AppPreference
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.android.settingslib.widget.SettingsBasePreferenceFragment

class AppPolicyActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
        val feature = intent.getStringExtra(EXTRA_FEATURE)
        if (packageName.isNullOrBlank() || feature == null || feature !in FEATURES) {
            Toast.makeText(this, R.string.app_policy_load_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        if (feature == FEATURE_JUMP) {
            startActivity(AppJumpSettingsActivity.createAppDetailIntent(this, packageName))
            finish()
            return
        }
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    ToolbarR.id.content_frame,
                    AppPolicyDetailFragment.newInstance(packageName, feature!!),
                )
                .commit()
        }
    }

    companion object {
        const val EXTRA_FEATURE = "feature"
        private const val FEATURE_JUMP = "jump"
        private val FEATURES = setOf("clipboard", FEATURE_JUMP, "sensor")

        fun createIntent(context: Context, packageName: String, feature: String): Intent =
            Intent(context, AppPolicyActivity::class.java)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
                .putExtra(EXTRA_FEATURE, feature)
    }
}

class AppPolicyDetailFragment : SettingsBasePreferenceFragment() {
    private lateinit var packageName: String
    private lateinit var feature: String
    private lateinit var appHeader: AppPreference
    private var policyWatcher: AppOpsManager.OnOpChangedListener? = null
    private var editable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = requireArguments().getString(ARG_PACKAGE_NAME).orEmpty()
        feature = requireArguments().getString(ARG_FEATURE).orEmpty()
        require(feature == "clipboard" || feature == "sensor")
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.createPreferenceScreen(requireContext()).also { preferenceScreen = it }
        requireActivity().setTitle(if (feature == "clipboard") R.string.app_policy_clipboard else R.string.app_policy_sensor)
        appHeader = AppPreference(requireContext()).apply {
            key = "app_policy_header"
            isSelectable = false
        }
        preferenceScreen.addPreference(appHeader)

        if (feature == "clipboard") {
            addPolicyCategory(R.string.clipboard_policy_read, ClipboardPolicySecureSettings.OPERATION_READ)
            addPolicyCategory(R.string.clipboard_policy_write, ClipboardPolicySecureSettings.OPERATION_WRITE)
        } else {
            addPolicyCategory(R.string.sensor_policy_app_policy, UwuAppPolicyManager.SENSOR)
        }
        Preference(requireContext()).apply {
            key = "app_policy_see_all"
            setTitle(R.string.app_policy_see_all_apps)
            setOnPreferenceClickListener {
                val activity = if (feature == "clipboard") ClipboardPolicyActivity::class.java else SensorPolicyActivity::class.java
                startActivity(Intent(requireContext(), activity))
                true
            }
            preferenceScreen.addPreference(this)
        }
    }

    override fun onStart() {
        super.onStart()
        policyWatcher = android.app.UwuAppPolicyManager.watch(
            requireContext(), Handler(Looper.getMainLooper()),
        ) { user -> if (user == UserHandle.myUserId()) reloadState() }
    }

    override fun onStop() {
        policyWatcher?.let { requireContext().getSystemService(AppOpsManager::class.java).stopWatchingMode(it) }
        policyWatcher = null
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        reloadState()
    }

    private fun addPolicyCategory(title: Int, type: Int) {
        val category = PreferenceCategory(requireContext()).apply { setTitle(title) }
        preferenceScreen.addPreference(category)
        val labels = if (type == UwuAppPolicyManager.SENSOR) {
            listOf(R.string.sensor_policy_allow, R.string.sensor_policy_deny_on_launch, R.string.sensor_policy_deny_always)
        } else {
            listOf(R.string.clipboard_policy_allow, R.string.clipboard_policy_ask, R.string.clipboard_policy_deny)
        }
        labels.forEachIndexed { policy, label ->
            SelectorWithWidgetPreference(requireContext()).apply {
                key = "policy_${type}_$policy"
                isPersistent = false
                setTitle(label)
                setOnClickListener {
                    if (editable) {
                        val saved = if (type == UwuAppPolicyManager.SENSOR) {
                            SensorPolicySecureSettings.setPolicy(requireContext(), packageName, policy)
                        } else {
                            ClipboardPolicySecureSettings.setPolicy(requireContext(), packageName, type, policy)
                        }
                        if (!saved) Toast.makeText(requireContext(), R.string.app_policy_update_failed, Toast.LENGTH_SHORT).show()
                        reloadState()
                    }
                }
                category.addPreference(this)
            }
        }
    }

    private fun reloadState() {
        if (!isAdded || !::appHeader.isInitialized) return
        runCatching {
            val info = AppPolicyRepository.requireApp(requireContext(), packageName)
            appHeader.title = info.loadLabel(requireContext().packageManager)
            appHeader.summary = packageName
            appHeader.icon = info.loadIcon(requireContext().packageManager)
            editable = feature != "clipboard" || AppPolicyRepository.clipboardEditable(requireContext(), info)
            if (!editable) appHeader.summary = getString(R.string.app_policy_system_managed)
            val state = AppPolicyRepository.snapshot()
            if (feature == "clipboard") {
                for (type in listOf(ClipboardPolicySecureSettings.OPERATION_READ, ClipboardPolicySecureSettings.OPERATION_WRITE)) {
                    val effective = AppPolicyRepository.clipboardPolicy(requireContext(), info, state, type)
                    findPreference<SelectorWithWidgetPreference>("policy_${type}_$effective")?.isChecked = true
                    for (policy in 0..2) findPreference<SelectorWithWidgetPreference>("policy_${type}_$policy")?.isEnabled = editable
                }
            } else {
                val selected = UwuAppPolicyManager.policy(state, packageName, UwuAppPolicyManager.SENSOR)
                findPreference<SelectorWithWidgetPreference>("policy_${UwuAppPolicyManager.SENSOR}_$selected")?.isChecked = true
            }
        }.onFailure {
            Toast.makeText(requireContext(), R.string.app_policy_load_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val ARG_PACKAGE_NAME = "package_name"
        private const val ARG_FEATURE = "feature"
        fun newInstance(packageName: String, feature: String) = AppPolicyDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PACKAGE_NAME, packageName)
                putString(ARG_FEATURE, feature)
            }
        }
    }
}
