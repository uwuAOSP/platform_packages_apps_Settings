/*
 * SPDX-FileCopyrightText: The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.uwu.apppolicies

import android.app.AppOpsManager
import android.app.UwuAppPolicyManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.uwuaosp.compose.framework.theme.SettingsTheme
import org.uwuaosp.compose.widget.app.AppListEmpty
import org.uwuaosp.compose.widget.app.AppListError
import org.uwuaosp.compose.widget.app.AppListItem
import org.uwuaosp.compose.widget.app.AppListLoading
import org.uwuaosp.compose.widget.app.AppListScaffold
import org.uwuaosp.compose.widget.preference.SettingsSectionHeader

class UwUAppPolicyAppsActivity : ComponentActivity() {
    private var feature: String? = null
    private var watcher: AppOpsManager.OnOpChangedListener? = null
    private var observing = false
    private val refresh = mutableIntStateOf(0)
    private val handler = Handler(Looper.getMainLooper())
    private val observer =
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                refresh.intValue++
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selected = intent.getStringExtra(AppPolicyRepository.EXTRA_FEATURE)
        if (selected == null || selected !in AppPolicyRepository.features) {
            Toast.makeText(this, R.string.app_policy_load_failed, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        feature = selected
        enableEdgeToEdge()
        setContent { SettingsTheme { AppPolicyAppsScreen(selected, refresh.intValue, ::finish) } }
    }

    override fun onStart() {
        super.onStart()
        if (feature == null) return
        watcher =
            UwuAppPolicyManager.watch(this, handler) { user ->
                if (user == UserHandle.myUserId()) refresh.intValue++
            }
        for (key in
            arrayOf(
                Settings.Secure.UWU_APP_CLIPBOARD_PROMPTS_ENABLED,
                Settings.Secure.DEFAULT_INPUT_METHOD,
            )) {
            contentResolver.registerContentObserver(Settings.Secure.getUriFor(key), false, observer)
        }
        observing = true
        refresh.intValue++
    }

    override fun onStop() {
        watcher?.let { getSystemService(AppOpsManager::class.java).stopWatchingMode(it) }
        watcher = null
        if (observing) contentResolver.unregisterContentObserver(observer)
        observing = false
        super.onStop()
    }

    companion object {
        @JvmStatic
        fun createIntent(context: Context, feature: String): Intent {
            require(feature in AppPolicyRepository.features)
            return Intent(context, UwUAppPolicyAppsActivity::class.java)
                .putExtra(AppPolicyRepository.EXTRA_FEATURE, feature)
        }
    }
}

@Composable
private fun AppPolicyAppsScreen(feature: String, refresh: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppPolicyAppEntry>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(feature, refresh, retry) {
        failed = false
        try {
            apps = withContext(Dispatchers.IO) { AppPolicyRepository.loadApps(context, feature) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            apps = null
            failed = true
        }
    }
    val groups =
        remember(apps, query) {
            apps
                ?.filter {
                    query.isBlank() ||
                        it.label.contains(query, true) ||
                        it.packageName.contains(query, true)
                }
                ?.groupBy { it.category }
        }
    AppListScaffold(
        title =
            stringResource(
                when (feature) {
                    "clipboard" -> R.string.app_policy_clipboard
                    "jump" -> R.string.app_jump_settings_title
                    else -> R.string.app_policy_sensor
                }
            ),
        searchQuery = query,
        searchPlaceholder = stringResource(R.string.clipboard_policy_search_apps),
        clearSearchContentDescription = stringResource(R.string.clipboard_policy_search_close),
        onSearchQueryChange = { query = it },
        onNavigateUp = onBack,
    ) {
        when {
            failed ->
                item {
                    AppListError(
                        text = stringResource(R.string.app_policy_load_failed),
                        retryText = stringResource(R.string.sensor_policy_retry),
                        onRetry = { retry++ },
                    )
                }
            groups == null -> item { AppListLoading() }
            groups.isEmpty() -> item { AppListEmpty(stringResource(R.string.app_policy_no_apps)) }
            else ->
                for ((category, title) in
                    listOf(
                        "allowed" to R.string.app_policy_allowed_header,
                        "ask" to R.string.app_policy_ask_header,
                        "denied" to R.string.app_policy_denied_header,
                    )) {
                    val entries = groups[category].orEmpty()
                    if (entries.isEmpty()) continue
                    item(key = "header:$category") {
                        SettingsSectionHeader(title = stringResource(title))
                    }
                    itemsIndexed(entries, key = { _, app -> app.packageName }) { index, app ->
                        AppListItem(
                            label = app.label,
                            packageName = app.packageName,
                            icon = app.icon.asImageBitmap(),
                            index = index,
                            itemCount = entries.size,
                            onClick = {
                                context.startActivity(
                                    Intent(context, AppPolicyActivity::class.java)
                                        .putExtra(Intent.EXTRA_PACKAGE_NAME, app.packageName)
                                        .putExtra(AppPolicyRepository.EXTRA_FEATURE, feature)
                                )
                            },
                        )
                    }
                }
        }
    }
}
